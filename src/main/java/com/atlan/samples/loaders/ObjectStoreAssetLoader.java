/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.atlan.exception.AtlanException;
import com.atlan.model.assets.*;
import com.atlan.model.enums.AtlanConnectorType;
import com.atlan.samples.loaders.models.*;
import com.atlan.samples.readers.ExcelReader;
import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectStoreAssetLoader extends AbstractLoader implements RequestHandler<Map<String, String>, String> {

    private static final String SHEET_NAME = "Object Store Assets";

    public static void main(String[] args) {
        ObjectStoreAssetLoader osal = new ObjectStoreAssetLoader();
        osal.handleRequest(System.getenv(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleRequest(Map<String, String> event, Context context) {

        try {

            log.info("Retrieving configuration and context...");
            if (context != null && context.getClientContext() != null) {
                log.debug(
                        " ... client environment: {}",
                        context.getClientContext().getEnvironment());
                log.debug(" ... client custom: {}", context.getClientContext().getCustom());
            }
            parseParametersFromEvent(event);

            log.info("Loading assets from: {}::{}", getFilename(), SHEET_NAME);

            ExcelReader xlsx = new ExcelReader(getFilename());
            List<Map<String, String>> data = xlsx.getRowsFromSheet(SHEET_NAME, 1);

            // Fastest way will be to load in batches by level of the asset hierarchy,
            // even though this means multiple passes over the data (it's all in-memory)

            // 1. Upsert connections for each unique combination of values in the Connection columns
            Map<String, ConnectionDetails> connections = new LinkedHashMap<>();
            for (Map<String, String> row : data) {
                ConnectionDetails details = ConnectionDetails.getFromRow(row, getDelimiter());
                if (details != null) {
                    // Only overwrite any details about the connection if none previously existed
                    String identity = details.getIdentity();
                    ConnectionDetails existing = connections.get(identity);
                    if (existing == null || existing.isStub()) {
                        connections.put(identity, details);
                    }
                }
            }
            Map<ConnectionDetails, String> connectionCache = ConnectionDetails.upsert(connections, getBatchSize());

            // 2. Create accounts for each unique value in the Account column
            Map<String, AccountDetails> accounts = new LinkedHashMap<>();
            for (Map<String, String> row : data) {
                AccountDetails details = AccountDetails.getFromRow(connectionCache, row, getDelimiter());
                if (details != null) {
                    // Only overwrite any details about the account if none previously existed
                    String identity = details.getIdentity();
                    AccountDetails existing = accounts.get(identity);
                    if (existing == null || existing.isStub()) {
                        accounts.put(identity, details);
                    }
                }
            }
            AccountDetails.upsert(accounts, getBatchSize());

            // 3. Create buckets for each unique value in the Container column
            Map<String, BucketDetails> buckets = new LinkedHashMap<>();
            for (Map<String, String> row : data) {
                BucketDetails details = BucketDetails.getFromRow(connectionCache, row, getDelimiter());
                if (details != null) {
                    // Only overwrite any details about the bucket if none previously existed
                    String identity = details.getIdentity();
                    BucketDetails existing = buckets.get(identity);
                    if (existing == null || existing.isStub()) {
                        buckets.put(identity, details);
                    }
                }
            }
            BucketDetails.upsert(buckets, getBatchSize());

            // 3. Create objects for each unique value in the Object column
            Map<String, ObjectDetails> objects = new LinkedHashMap<>();
            for (Map<String, String> row : data) {
                ObjectDetails details = ObjectDetails.getFromRow(connectionCache, row, getDelimiter());
                if (details != null) {
                    // Only overwrite any details about the database if none previously existed
                    String identity = details.getIdentity();
                    ObjectDetails existing = objects.get(identity);
                    if (existing == null || existing.isStub()) {
                        objects.put(identity, details);
                    }
                }
            }
            Set<String> bucketCountsToUpdate = ObjectDetails.upsert(objects, getBatchSize());

            // 6. Finally, update each of the objects that tracks counts with their counts
            log.info("Updating assets with counts...");
            AssetBatch bucketsToUpdate = new AssetBatch("bucket", getBatchSize());
            for (String bucketQualifiedName : bucketCountsToUpdate) {
                try {
                    AtlanConnectorType type = Connection.getConnectorTypeFromQualifiedName(bucketQualifiedName);
                    switch (type) {
                        case S3:
                            S3Bucket s3f = S3Bucket.retrieveByQualifiedName(bucketQualifiedName);
                            long s3c = s3f.getObjects().size();
                            S3Bucket s3u =
                                    s3f.trimToRequired().s3ObjectCount(s3c).build();
                            bucketsToUpdate.add(s3u);
                            break;
                        case GCS:
                            GCSBucket gcsf = GCSBucket.retrieveByQualifiedName(bucketQualifiedName);
                            long gcsc = gcsf.getGcsObjects().size();
                            GCSBucket gcsu =
                                    gcsf.trimToRequired().gcsObjectCount(gcsc).build();
                            bucketsToUpdate.add(gcsu);
                            break;
                        case ADLS:
                            // Do nothing, counts are not tracked on ADLS containers
                            break;
                        default:
                            log.error("Invalid connector type found for an object store bucket: {}", type);
                            break;
                    }
                } catch (AtlanException e) {
                    log.error("Unable to re-calculate objects in bucket: {}", bucketQualifiedName, e);
                }
            }
            bucketsToUpdate.flush();

        } catch (IOException e) {
            log.error("Failed to read Excel file from: {}", getFilename(), e);
            System.exit(1);
        }

        return getFilename();
    }
}
