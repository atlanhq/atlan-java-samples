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
public class ObjectDetails extends AssetDetails {
    public static final String COL_OBJECT_NAME = "OBJECT NAME";
    public static final String COL_OBJECT_ARN = "OBJECT ARN";
    public static final String COL_OBJECT_PATH = "OBJECT PATH";
    public static final String COL_OBJECT_SIZE = "OBJECT SIZE";
    public static final String COL_CONTENT_TYPE = "CONTENT TYPE";

    private static final List<String> REQUIRED = List.of(
            ConnectionDetails.COL_CONNECTOR,
            ConnectionDetails.COL_CONNECTION,
            BucketDetails.COL_BUCKET_NAME,
            COL_OBJECT_NAME);

    @ToString.Include
    private String connectionQualifiedName;

    @ToString.Include
    private String containerQualifiedName;

    @ToString.Include
    private String bucketName;

    @ToString.Include
    private String name;

    @ToString.Include
    private String arn;

    @ToString.Include
    private String path;

    @ToString.Include
    private Long size;

    @ToString.Include
    private String contentType;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentity() {
        return containerQualifiedName + "/" + name;
    }

    /**
     * Build up details about the object on the provided row.
     *
     * @param connectionCache a cache of connections that have first been resolved across the spreadsheet
     * @param row a row of data from the spreadsheet, as a map from column name to value
     * @param delim delimiter used in cells that can contain multiple values
     * @return the object details for that row
     */
    public static ObjectDetails getFromRow(
            Map<ConnectionDetails, String> connectionCache, Map<String, String> row, String delim) {
        ObjectDetailsBuilder<?, ?> builder = getFromRow(ObjectDetails.builder(), row, delim);
        if (getMissingFields(row, REQUIRED).isEmpty()) {
            String connectionQN = ConnectionDetails.getQualifiedName(connectionCache, row);
            String bucketQN = BucketDetails.getQualifiedName(connectionCache, row);
            if (bucketQN != null && bucketQN.length() > 0) {
                builder = builder.connectionQualifiedName(connectionQN)
                        .containerQualifiedName(bucketQN)
                        .bucketName(row.get(BucketDetails.COL_BUCKET_NAME))
                        .name(row.get(COL_OBJECT_NAME))
                        .arn(row.get(COL_OBJECT_ARN))
                        .path(row.get(COL_OBJECT_PATH))
                        .contentType(row.get(COL_CONTENT_TYPE));
                String size = row.get(COL_OBJECT_SIZE);
                try {
                    builder = builder.size(Double.valueOf(size).longValue());
                } catch (NumberFormatException e) {
                    log.error("Unable to translate provided object size to a number: {}", size, e);
                }
                return builder.stub(false).build();
            }
        }
        return null;
    }

    /**
     * Create objects in bulk, if they do not exist, or update them if they do (idempotent).
     *
     * @param objects the set of objects to ensure exist
     * @param batchSize maximum number of objects to create per batch
     * @param updateOnly if true, only attempt to update existing assets, otherwise allow assets to be created as well
     * @return qualifiedNames of all parent buckets in which assets were created or updated
     */
    public static Set<String> upsert(Map<String, ObjectDetails> objects, int batchSize, boolean updateOnly) {
        Set<String> parents = new HashSet<>();
        AssetBatch batch = new AssetBatch(Atlan.getDefaultClient(), "object", batchSize);
        Map<String, List<String>> toClassifyS3 = new HashMap<>();
        Map<String, List<String>> toClassifyGCS = new HashMap<>();
        Map<String, List<String>> toClassifyADLS = new HashMap<>();

        try {
            for (ObjectDetails details : objects.values()) {
                String parentQN = details.getContainerQualifiedName();
                String bucketName = details.getBucketName();
                String objectName = details.getName();
                String objectARN = details.getArn();
                AtlanConnectorType objectType = Connection.getConnectorTypeFromQualifiedName(parentQN);
                switch (objectType) {
                    case S3:
                        String connectionQN = details.getConnectionQualifiedName();
                        if (objectARN != null && objectARN.length() > 0) {
                            if (updateOnly) {
                                String qualifiedName = IS3.generateQualifiedName(connectionQN, objectARN);
                                try {
                                    Asset.retrieveMinimal(S3Object.TYPE_NAME, qualifiedName);
                                    S3Object toUpdate = S3Object.updater(qualifiedName, objectName)
                                            .description(details.getDescription())
                                            .certificateStatus(details.getCertificate())
                                            .certificateStatusMessage(details.getCertificateStatusMessage())
                                            .announcementType(details.getAnnouncementType())
                                            .announcementTitle(details.getAnnouncementTitle())
                                            .announcementMessage(details.getAnnouncementMessage())
                                            .ownerUsers(details.getOwnerUsers())
                                            .ownerGroups(details.getOwnerGroups())
                                            .s3ObjectKey(details.getPath())
                                            .s3ObjectSize(details.getSize())
                                            .s3ObjectContentType(details.getContentType())
                                            .build();
                                    if (!details.getAtlanTags().isEmpty()) {
                                        toClassifyS3.put(toUpdate.getQualifiedName(), details.getAtlanTags());
                                    }
                                    parents.add(parentQN);
                                    batch.add(toUpdate);
                                } catch (NotFoundException e) {
                                    log.warn("Unable to find existing object — skipping: {}", qualifiedName, e);
                                } catch (AtlanException e) {
                                    log.error("Unable to lookup whether object exists or not.", e);
                                }
                            } else {
                                S3Object s3 = S3Object.creator(objectName, parentQN, bucketName, objectARN)
                                        .description(details.getDescription())
                                        .certificateStatus(details.getCertificate())
                                        .certificateStatusMessage(details.getCertificateStatusMessage())
                                        .announcementType(details.getAnnouncementType())
                                        .announcementTitle(details.getAnnouncementTitle())
                                        .announcementMessage(details.getAnnouncementMessage())
                                        .ownerUsers(details.getOwnerUsers())
                                        .ownerGroups(details.getOwnerGroups())
                                        .s3ObjectKey(details.getPath())
                                        .s3ObjectSize(details.getSize())
                                        .s3ObjectContentType(details.getContentType())
                                        .build();
                                if (!details.getAtlanTags().isEmpty()) {
                                    toClassifyS3.put(s3.getQualifiedName(), details.getAtlanTags());
                                }
                                parents.add(parentQN);
                                batch.add(s3);
                            }
                        } else {
                            log.error("Unable to create an S3 object without an ARN: {}", details);
                        }
                        break;
                    case GCS:
                        if (updateOnly) {
                            String qualifiedName = GCSObject.generateQualifiedName(objectName, parentQN);
                            try {
                                Asset.retrieveMinimal(GCSObject.TYPE_NAME, qualifiedName);
                                GCSObject toUpdate = GCSObject.updater(qualifiedName, objectName)
                                        .description(details.getDescription())
                                        .certificateStatus(details.getCertificate())
                                        .certificateStatusMessage(details.getCertificateStatusMessage())
                                        .announcementType(details.getAnnouncementType())
                                        .announcementTitle(details.getAnnouncementTitle())
                                        .announcementMessage(details.getAnnouncementMessage())
                                        .ownerUsers(details.getOwnerUsers())
                                        .ownerGroups(details.getOwnerGroups())
                                        .gcsObjectKey(details.getPath())
                                        .gcsObjectSize(details.getSize())
                                        .gcsObjectContentType(details.getContentType())
                                        .build();
                                if (!details.getAtlanTags().isEmpty()) {
                                    toClassifyGCS.put(toUpdate.getQualifiedName(), details.getAtlanTags());
                                }
                                parents.add(parentQN);
                                batch.add(toUpdate);
                            } catch (NotFoundException e) {
                                log.warn("Unable to find existing object — skipping: {}", qualifiedName, e);
                            } catch (AtlanException e) {
                                log.error("Unable to lookup whether object exists or not.", e);
                            }
                        } else {
                            GCSObject gcs = GCSObject.creator(objectName, parentQN)
                                    .description(details.getDescription())
                                    .certificateStatus(details.getCertificate())
                                    .certificateStatusMessage(details.getCertificateStatusMessage())
                                    .announcementType(details.getAnnouncementType())
                                    .announcementTitle(details.getAnnouncementTitle())
                                    .announcementMessage(details.getAnnouncementMessage())
                                    .ownerUsers(details.getOwnerUsers())
                                    .ownerGroups(details.getOwnerGroups())
                                    .gcsObjectKey(details.getPath())
                                    .gcsObjectSize(details.getSize())
                                    .gcsObjectContentType(details.getContentType())
                                    .build();
                            if (!details.getAtlanTags().isEmpty()) {
                                toClassifyGCS.put(gcs.getQualifiedName(), details.getAtlanTags());
                            }
                            parents.add(parentQN);
                            batch.add(gcs);
                        }
                        break;
                    case ADLS:
                        if (updateOnly) {
                            String qualifiedName = ADLSObject.generateQualifiedName(objectName, parentQN);
                            try {
                                Asset.retrieveMinimal(ADLSObject.TYPE_NAME, qualifiedName);
                                ADLSObject toUpdate = ADLSObject.updater(qualifiedName, objectName)
                                        .description(details.getDescription())
                                        .certificateStatus(details.getCertificate())
                                        .certificateStatusMessage(details.getCertificateStatusMessage())
                                        .announcementType(details.getAnnouncementType())
                                        .announcementTitle(details.getAnnouncementTitle())
                                        .announcementMessage(details.getAnnouncementMessage())
                                        .ownerUsers(details.getOwnerUsers())
                                        .ownerGroups(details.getOwnerGroups())
                                        .adlsObjectUrl(details.getPath())
                                        .adlsObjectSize(details.getSize())
                                        .adlsObjectContentType(details.getContentType())
                                        .build();
                                if (!details.getAtlanTags().isEmpty()) {
                                    toClassifyADLS.put(toUpdate.getQualifiedName(), details.getAtlanTags());
                                }
                                parents.add(parentQN);
                                batch.add(toUpdate);
                            } catch (NotFoundException e) {
                                log.warn("Unable to find existing object — skipping: {}", qualifiedName, e);
                            } catch (AtlanException e) {
                                log.error("Unable to lookup whether object exists or not.", e);
                            }
                        } else {
                            ADLSObject adls = ADLSObject.creator(objectName, parentQN)
                                    .description(details.getDescription())
                                    .certificateStatus(details.getCertificate())
                                    .certificateStatusMessage(details.getCertificateStatusMessage())
                                    .announcementType(details.getAnnouncementType())
                                    .announcementTitle(details.getAnnouncementTitle())
                                    .announcementMessage(details.getAnnouncementMessage())
                                    .ownerUsers(details.getOwnerUsers())
                                    .ownerGroups(details.getOwnerGroups())
                                    .adlsObjectUrl(details.getPath())
                                    .adlsObjectSize(details.getSize())
                                    .adlsObjectContentType(details.getContentType())
                                    .build();
                            if (!details.getAtlanTags().isEmpty()) {
                                toClassifyADLS.put(adls.getQualifiedName(), details.getAtlanTags());
                            }
                            parents.add(parentQN);
                            batch.add(adls);
                        }
                        break;
                    default:
                        log.error("Invalid object type ({}) — skipping: {}", objectType, details);
                        break;
                }
            }
            // And don't forget to flush out any that remain
            batch.flush();
        } catch (AtlanException e) {
            log.error("Unable to bulk-upsert object details.", e);
        }

        // Classifications must be added in a second pass, after the asset exists
        appendAtlanTags(toClassifyS3, S3Object.TYPE_NAME);
        appendAtlanTags(toClassifyGCS, GCSObject.TYPE_NAME);
        appendAtlanTags(toClassifyADLS, ADLSObject.TYPE_NAME);

        return parents;
    }
}
