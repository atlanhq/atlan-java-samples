/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.Atlan;
import com.atlan.exception.AtlanException;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.*;
import com.atlan.model.enums.AtlanConnectorType;
import com.atlan.util.AssetBatch;
import java.util.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for capturing the full details provided about a table.
 */
@Slf4j
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class BucketDetails extends AssetDetails {
    public static final String COL_BUCKET_NAME = "BUCKET NAME";
    public static final String COL_BUCKET_ARN = "BUCKET ARN";

    private static final List<String> REQUIRED =
            List.of(ConnectionDetails.COL_CONNECTOR, ConnectionDetails.COL_CONNECTION, COL_BUCKET_NAME);
    private static final List<String> REQUIRED_EMPTY = List.of(ObjectDetails.COL_OBJECT_NAME);

    @ToString.Include
    private String connectionQualifiedName;

    @ToString.Include
    private String accountName;

    @ToString.Include
    private String name;

    @ToString.Include
    private String arn;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentity() {
        return connectionQualifiedName + "/" + (accountName == null ? "" : accountName) + "/" + name;
    }

    /**
     * Construct a bucket's qualifiedName from the row of data and cache of connections.
     *
     * @param connectionCache cache of connections
     * @param row of data
     * @return the qualifiedName for the bucket on that row of data
     */
    public static String getQualifiedName(Map<ConnectionDetails, String> connectionCache, Map<String, String> row) {
        String connectionQualifiedName = ConnectionDetails.getQualifiedName(connectionCache, row);
        AtlanConnectorType type = Connection.getConnectorTypeFromQualifiedName(connectionQualifiedName);
        String bucketName = row.get(COL_BUCKET_NAME);
        switch (type) {
            case S3:
                String bucketARN = row.get(COL_BUCKET_ARN);
                if (bucketARN != null && bucketARN.length() > 0 && bucketName != null && bucketName.length() > 0) {
                    return IS3.generateQualifiedName(connectionQualifiedName, bucketARN);
                }
                break;
            case GCS:
                if (bucketName != null && bucketName.length() > 0) {
                    return connectionQualifiedName + "/" + bucketName;
                }
                break;
            case ADLS:
                String accountQN = AccountDetails.getQualifiedName(connectionCache, row);
                if (accountQN != null && accountQN.length() > 0 && bucketName != null && bucketName.length() > 0) {
                    return accountQN + "/" + bucketName;
                }
                break;
            default:
                log.error("Unknown connector type for object stores: {}", type);
                break;
        }
        return null;
    }

    /**
     * Build up details about the bucket on the provided row.
     *
     * @param connectionCache a cache of connections that have first been resolved across the spreadsheet
     * @param row a row of data from the spreadsheet, as a map from column name to value
     * @param delim delimiter used in cells that can contain multiple values
     * @return the bucket details for that row
     */
    public static BucketDetails getFromRow(
            Map<ConnectionDetails, String> connectionCache, Map<String, String> row, String delim) {
        if (getMissingFields(row, REQUIRED).isEmpty()) {
            String connectionQualifiedName = ConnectionDetails.getQualifiedName(connectionCache, row);
            if (getRequiredEmptyFields(row, REQUIRED_EMPTY).isEmpty()) {
                return getFromRow(BucketDetails.builder(), row, delim)
                        .connectionQualifiedName(connectionQualifiedName)
                        .accountName(row.get(AccountDetails.COL_ACCOUNT))
                        .name(row.get(COL_BUCKET_NAME))
                        .arn(row.get(COL_BUCKET_ARN))
                        .build();
            } else {
                return BucketDetails.builder()
                        .connectionQualifiedName(connectionQualifiedName)
                        .accountName(row.get(AccountDetails.COL_ACCOUNT))
                        .name(row.get(COL_BUCKET_NAME))
                        .arn(row.get(COL_BUCKET_ARN))
                        .build();
            }
        }
        return null;
    }

    /**
     * Create buckets in bulk, if they do not exist, or update them if they do (idempotent).
     *
     * @param buckets the set of buckets to ensure exist
     * @param batchSize maximum number of buckets to create per batch
     * @param updateOnly if true, only attempt to update existing assets, otherwise allow assets to be created as well
     */
    public static void upsert(Map<String, BucketDetails> buckets, int batchSize, boolean updateOnly) {
        AssetBatch batch = new AssetBatch(Atlan.getDefaultClient(), "bucket", batchSize);
        Map<String, List<String>> toClassifyS3 = new HashMap<>();
        Map<String, List<String>> toClassifyGCS = new HashMap<>();
        Map<String, List<String>> toClassifyADLS = new HashMap<>();

        try {
            for (BucketDetails details : buckets.values()) {
                String connectionQualifiedName = details.getConnectionQualifiedName();
                String accountName = details.getAccountName();
                String bucketName = details.getName();
                String bucketARN = details.getArn();
                AtlanConnectorType bucketType = Connection.getConnectorTypeFromQualifiedName(connectionQualifiedName);
                switch (bucketType) {
                    case S3:
                        if (bucketARN != null && bucketARN.length() > 0) {
                            if (updateOnly) {
                                String qualifiedName = IS3.generateQualifiedName(connectionQualifiedName, bucketARN);
                                try {
                                    Asset.retrieveMinimal(S3Bucket.TYPE_NAME, qualifiedName);
                                    S3Bucket toUpdate = S3Bucket.updater(qualifiedName, bucketName)
                                            .description(details.getDescription())
                                            .certificateStatus(details.getCertificate())
                                            .certificateStatusMessage(details.getCertificateStatusMessage())
                                            .announcementType(details.getAnnouncementType())
                                            .announcementTitle(details.getAnnouncementTitle())
                                            .announcementMessage(details.getAnnouncementMessage())
                                            .ownerUsers(details.getOwnerUsers())
                                            .ownerGroups(details.getOwnerGroups())
                                            .build();
                                    if (!details.getAtlanTags().isEmpty()) {
                                        toClassifyS3.put(toUpdate.getQualifiedName(), details.getAtlanTags());
                                    }
                                    batch.add(toUpdate);
                                } catch (NotFoundException e) {
                                    log.warn("Unable to find existing bucket — skipping: {}", qualifiedName, e);
                                } catch (AtlanException e) {
                                    log.error("Unable to lookup whether bucket exists or not.", e);
                                }
                            } else {
                                S3Bucket s3 = S3Bucket.creator(bucketName, connectionQualifiedName, bucketARN)
                                        .description(details.getDescription())
                                        .certificateStatus(details.getCertificate())
                                        .certificateStatusMessage(details.getCertificateStatusMessage())
                                        .announcementType(details.getAnnouncementType())
                                        .announcementTitle(details.getAnnouncementTitle())
                                        .announcementMessage(details.getAnnouncementMessage())
                                        .ownerUsers(details.getOwnerUsers())
                                        .ownerGroups(details.getOwnerGroups())
                                        .build();
                                if (!details.getAtlanTags().isEmpty()) {
                                    toClassifyS3.put(s3.getQualifiedName(), details.getAtlanTags());
                                }
                                batch.add(s3);
                            }
                        } else {
                            log.error("Unable to create or update an S3 bucket without an ARN: {}", details);
                        }
                        break;
                    case GCS:
                        if (updateOnly) {
                            String qualifiedName = GCSBucket.generateQualifiedName(bucketName, connectionQualifiedName);
                            try {
                                Asset.retrieveMinimal(S3Bucket.TYPE_NAME, qualifiedName);
                                GCSBucket toUpdate = GCSBucket.updater(qualifiedName, bucketName)
                                        .description(details.getDescription())
                                        .certificateStatus(details.getCertificate())
                                        .certificateStatusMessage(details.getCertificateStatusMessage())
                                        .announcementType(details.getAnnouncementType())
                                        .announcementTitle(details.getAnnouncementTitle())
                                        .announcementMessage(details.getAnnouncementMessage())
                                        .ownerUsers(details.getOwnerUsers())
                                        .ownerGroups(details.getOwnerGroups())
                                        .build();
                                if (!details.getAtlanTags().isEmpty()) {
                                    toClassifyGCS.put(toUpdate.getQualifiedName(), details.getAtlanTags());
                                }
                                batch.add(toUpdate);
                            } catch (NotFoundException e) {
                                log.warn("Unable to find existing bucket — skipping: {}", qualifiedName, e);
                            } catch (AtlanException e) {
                                log.error("Unable to lookup whether bucket exists or not.", e);
                            }
                        } else {
                            GCSBucket gcs = GCSBucket.creator(bucketName, connectionQualifiedName)
                                    .description(details.getDescription())
                                    .certificateStatus(details.getCertificate())
                                    .certificateStatusMessage(details.getCertificateStatusMessage())
                                    .announcementType(details.getAnnouncementType())
                                    .announcementTitle(details.getAnnouncementTitle())
                                    .announcementMessage(details.getAnnouncementMessage())
                                    .ownerUsers(details.getOwnerUsers())
                                    .ownerGroups(details.getOwnerGroups())
                                    .build();
                            if (!details.getAtlanTags().isEmpty()) {
                                toClassifyGCS.put(gcs.getQualifiedName(), details.getAtlanTags());
                            }
                            batch.add(gcs);
                        }
                        break;
                    case ADLS:
                        if (accountName != null && accountName.length() > 0) {
                            String accountQN = ADLSAccount.generateQualifiedName(accountName, connectionQualifiedName);
                            if (updateOnly) {
                                String qualifiedName = ADLSContainer.generateQualifiedName(bucketName, accountQN);
                                try {
                                    Asset.retrieveMinimal(ADLSContainer.TYPE_NAME, qualifiedName);
                                    ADLSContainer toUpdate = ADLSContainer.updater(qualifiedName, bucketName)
                                            .description(details.getDescription())
                                            .certificateStatus(details.getCertificate())
                                            .certificateStatusMessage(details.getCertificateStatusMessage())
                                            .announcementType(details.getAnnouncementType())
                                            .announcementTitle(details.getAnnouncementTitle())
                                            .announcementMessage(details.getAnnouncementMessage())
                                            .ownerUsers(details.getOwnerUsers())
                                            .ownerGroups(details.getOwnerGroups())
                                            .build();
                                    if (!details.getAtlanTags().isEmpty()) {
                                        toClassifyADLS.put(toUpdate.getQualifiedName(), details.getAtlanTags());
                                    }
                                    batch.add(toUpdate);
                                } catch (NotFoundException e) {
                                    log.warn("Unable to find existing container — skipping: {}", qualifiedName, e);
                                } catch (AtlanException e) {
                                    log.error("Unable to lookup whether container exists or not.", e);
                                }
                            } else {
                                ADLSContainer adls = ADLSContainer.creator(bucketName, accountQN)
                                        .description(details.getDescription())
                                        .certificateStatus(details.getCertificate())
                                        .certificateStatusMessage(details.getCertificateStatusMessage())
                                        .announcementType(details.getAnnouncementType())
                                        .announcementTitle(details.getAnnouncementTitle())
                                        .announcementMessage(details.getAnnouncementMessage())
                                        .ownerUsers(details.getOwnerUsers())
                                        .ownerGroups(details.getOwnerGroups())
                                        .build();
                                if (!details.getAtlanTags().isEmpty()) {
                                    toClassifyADLS.put(adls.getQualifiedName(), details.getAtlanTags());
                                }
                                batch.add(adls);
                            }
                        } else {
                            log.error("Unable to create or update an ADLS container without an account: {}", details);
                        }
                        break;
                    default:
                        log.error("Invalid bucket type ({}) — skipping: {}", bucketType, details);
                        break;
                }
            }
            // And don't forget to flush out any that remain
            batch.flush();
        } catch (AtlanException e) {
            log.error("Unable to bulk-upsert bucket details.", e);
        }

        // Classifications must be added in a second pass, after the asset exists
        appendAtlanTags(toClassifyS3, S3Bucket.TYPE_NAME);
        appendAtlanTags(toClassifyGCS, GCSBucket.TYPE_NAME);
        appendAtlanTags(toClassifyADLS, ADLSContainer.TYPE_NAME);
    }
}
