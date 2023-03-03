/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.model.assets.*;
import com.atlan.model.enums.AtlanConnectorType;
import com.atlan.samples.loaders.AssetBatch;
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
     * @return qualifiedNames of all parent buckets in which assets were created or updated
     */
    public static Set<String> upsert(Map<String, ObjectDetails> objects, int batchSize) {
        Set<String> parents = new HashSet<>();
        AssetBatch batch = new AssetBatch("object", batchSize);
        Map<String, List<String>> toClassifyS3 = new HashMap<>();
        Map<String, List<String>> toClassifyGCS = new HashMap<>();
        Map<String, List<String>> toClassifyADLS = new HashMap<>();

        for (ObjectDetails details : objects.values()) {
            String parentQN = details.getContainerQualifiedName();
            String objectName = details.getName();
            String objectARN = details.getArn();
            AtlanConnectorType objectType = Connection.getConnectorTypeFromQualifiedName(parentQN);
            switch (objectType) {
                case S3:
                    String connectionQN = details.getConnectionQualifiedName();
                    if (objectARN != null && objectARN.length() > 0) {
                        // Note: unlike other object stores, an S3Object must
                        // manually be placed in its bucket, beyond the creator
                        S3Object s3 = S3Object.creator(objectName, connectionQN, objectARN)
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
                                .bucket(S3Bucket.refByQualifiedName(parentQN))
                                .s3BucketName(details.getBucketName())
                                .s3BucketQualifiedName(parentQN)
                                .build();
                        if (!details.getClassifications().isEmpty()) {
                            toClassifyS3.put(s3.getQualifiedName(), details.getClassifications());
                        }
                        parents.add(parentQN);
                        batch.add(s3);
                    } else {
                        log.error("Unable to create an S3 object without an ARN: {}", details);
                    }
                    break;
                case GCS:
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
                    if (!details.getClassifications().isEmpty()) {
                        toClassifyGCS.put(gcs.getQualifiedName(), details.getClassifications());
                    }
                    parents.add(parentQN);
                    batch.add(gcs);
                    break;
                case ADLS:
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
                    if (!details.getClassifications().isEmpty()) {
                        toClassifyADLS.put(adls.getQualifiedName(), details.getClassifications());
                    }
                    parents.add(parentQN);
                    batch.add(adls);
                    break;
                default:
                    log.error("Invalid object type ({}) — skipping: {}", objectType, details);
                    break;
            }
        }
        // And don't forget to flush out any that remain
        batch.flush();

        // Classifications must be added in a second pass, after the asset exists
        appendClassifications(toClassifyS3, S3Object.TYPE_NAME);
        appendClassifications(toClassifyGCS, GCSObject.TYPE_NAME);
        appendClassifications(toClassifyADLS, ADLSObject.TYPE_NAME);

        return parents;
    }
}
