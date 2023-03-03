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
public class DocumentationTemplateLoader extends AbstractLoader implements RequestHandler<Map<String, String>, String> {

    private static final String TABULAR_SHEET = "Tabular Assets";
    private static final String OBJECT_SHEET = "Object Store Assets";
    private static final String LINEAGE_SHEET = "Lineage";

    public static void main(String[] args) {
        TabularAssetLoader al = new TabularAssetLoader();
        Map<String, String> event = new HashMap<>(System.getenv());
        if (!event.containsKey("FILENAME")) {
            event.put("FILENAME", "atlan-documentation-template.xlsx");
        }
        al.handleRequest(event, null);
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

            processTabularAssets();
            processObjectStoreAssets();
            processLineage();

        } catch (IOException e) {
            log.error("Failed to read Excel file from: {}", getFilename(), e);
            System.exit(1);
        }

        return getFilename();
    }

    public void processTabularAssets() throws IOException {
        log.info("Loading tabular assets from: {}::{}", getFilename(), TABULAR_SHEET);

        ExcelReader xlsx = new ExcelReader(getFilename());
        List<Map<String, String>> data = xlsx.getRowsFromSheet(TABULAR_SHEET, 1);

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

        // 2. Create databases for each unique value in the Database column
        Map<String, DatabaseDetails> databases = new LinkedHashMap<>();
        for (Map<String, String> row : data) {
            DatabaseDetails details = DatabaseDetails.getFromRow(connectionCache, row, getDelimiter());
            if (details != null) {
                // Only overwrite any details about the database if none previously existed
                String identity = details.getIdentity();
                DatabaseDetails existing = databases.get(identity);
                if (existing == null || existing.isStub()) {
                    databases.put(identity, details);
                }
            }
        }
        DatabaseDetails.upsert(databases, getBatchSize());

        // 3. Create schemas for each unique value in the Schema column
        Map<String, SchemaDetails> schemas = new LinkedHashMap<>();
        for (Map<String, String> row : data) {
            SchemaDetails details = SchemaDetails.getFromRow(connectionCache, row, getDelimiter());
            if (details != null) {
                // Only overwrite any details about the schema if none previously existed
                String identity = details.getIdentity();
                SchemaDetails existing = schemas.get(identity);
                if (existing == null || existing.isStub()) {
                    schemas.put(identity, details);
                }
            }
        }
        Set<String> databaseCountsToUpdate = SchemaDetails.upsert(schemas, getBatchSize());

        // 4. Create table-level assets for each unique value in the Table / view columns
        Map<String, ContainerDetails> containers = new LinkedHashMap<>();
        for (Map<String, String> row : data) {
            ContainerDetails details = ContainerDetails.getFromRow(connectionCache, row, getDelimiter());
            if (details != null) {
                // Only overwrite any details about the table if none previously existed
                String identity = details.getIdentity();
                ContainerDetails existing = containers.get(identity);
                if (existing == null || existing.isStub()) {
                    containers.put(identity, details);
                }
            }
        }
        Set<String> schemaCountsToUpdate = ContainerDetails.upsert(containers, getBatchSize());

        // 5. Create columns for each unique value in the Column columns
        int colIdx = 1;
        String lastTableQN = null;
        Map<String, ColumnDetails> columns = new LinkedHashMap<>();
        for (Map<String, String> row : data) {
            String tableQN = ContainerDetails.getQualifiedName(connectionCache, row);
            if (lastTableQN == null || !lastTableQN.equals(tableQN)) {
                // If we're on a new table-level asset, reset the column index
                colIdx = 1;
                lastTableQN = tableQN;
            } else {
                // Otherwise, just increment it
                colIdx++;
            }
            ColumnDetails details = ColumnDetails.getFromRow(connectionCache, row, getDelimiter(), colIdx);
            if (details != null) {
                String identity = details.getIdentity();
                ColumnDetails existing = columns.put(identity, details);
                if (existing != null) {
                    log.warn("Duplicate columns found, keeping only the last one defined: {}", identity);
                }
            } else {
                // If we skipped the column for some reason, decrement the index
                colIdx--;
            }
        }
        Set<ContainerDetails> containerCountsToUpdate = ColumnDetails.upsert(columns, getBatchSize());

        // 6. Finally, update each of the objects that tracks counts with their counts
        log.info("Updating assets with counts...");
        AssetBatch databasesToUpdate = new AssetBatch(Database.TYPE_NAME, getBatchSize());
        for (String databaseQualifiedName : databaseCountsToUpdate) {
            try {
                Database complete = Database.retrieveByQualifiedName(databaseQualifiedName);
                int schemaCount = complete.getSchemas().size();
                Database toUpdate =
                        complete.trimToRequired().schemaCount(schemaCount).build();
                databasesToUpdate.add(toUpdate);
            } catch (AtlanException e) {
                log.error("Unable to re-calculate schemas in database: {}", databaseQualifiedName, e);
            }
        }
        databasesToUpdate.flush();

        AssetBatch schemasToUpdate = new AssetBatch(Schema.TYPE_NAME, getBatchSize());
        for (String schemaQualifiedName : schemaCountsToUpdate) {
            try {
                Schema complete = Schema.retrieveByQualifiedName(schemaQualifiedName);
                int tableCount = complete.getTables().size();
                int viewCount = complete.getViews().size();
                Schema toUpdate = complete.trimToRequired()
                        .tableCount(tableCount)
                        .viewCount(viewCount)
                        .build();
                schemasToUpdate.add(toUpdate);
            } catch (AtlanException e) {
                log.error("Unable to re-calculate tables and views in schema: {}", schemaQualifiedName, e);
            }
        }
        schemasToUpdate.flush();

        AssetBatch containersToUpdate = new AssetBatch("container", getBatchSize());
        for (ContainerDetails container : containerCountsToUpdate) {
            String containerType = container.getType();
            String containerQN = container.getName();
            try {
                Asset toUpdate = null;
                switch (containerType) {
                    case Table.TYPE_NAME:
                        Table table = Table.retrieveByQualifiedName(containerQN);
                        long tc = table.getColumns().size();
                        toUpdate = table.trimToRequired().columnCount(tc).build();
                        break;
                    case View.TYPE_NAME:
                        View view = View.retrieveByQualifiedName(containerQN);
                        long vc = view.getColumns().size();
                        toUpdate = view.trimToRequired().columnCount(vc).build();
                        break;
                    case MaterializedView.TYPE_NAME:
                        MaterializedView mv = MaterializedView.retrieveByQualifiedName(containerQN);
                        long mvc = mv.getColumns().size();
                        toUpdate = mv.trimToRequired().columnCount(mvc).build();
                        break;
                    default:
                        log.error("Unknown parent container type, cannot update counts: {}", containerType);
                        break;
                }
                if (toUpdate != null) {
                    containersToUpdate.add(toUpdate);
                }
            } catch (AtlanException e) {
                log.error("Unable to re-calculate columns in container: {}", containerQN, e);
            }
        }
        containersToUpdate.flush();
    }

    public void processObjectStoreAssets() throws IOException {
        log.info("Loading object store assets from: {}::{}", getFilename(), OBJECT_SHEET);

        ExcelReader xlsx = new ExcelReader(getFilename());
        List<Map<String, String>> data = xlsx.getRowsFromSheet(OBJECT_SHEET, 1);

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
                        S3Bucket s3u = s3f.trimToRequired().s3ObjectCount(s3c).build();
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
    }

    public void processLineage() throws IOException {
        log.info("Loading lineage from: {}::{}", getFilename(), LINEAGE_SHEET);

        ExcelReader xlsx = new ExcelReader(getFilename());
        List<Map<String, String>> data = xlsx.getRowsFromSheet(LINEAGE_SHEET, 1);

        // Fastest way will be to load in batches by level of the asset hierarchy,
        // even though this means multiple passes over the data (it's all in-memory)

        // 1. Upsert connections for each unique orchestrator name
        Map<String, ConnectionDetails> connections = new LinkedHashMap<>();
        for (Map<String, String> row : data) {
            ConnectionDetails details = LineageDetails.getOrchestratorFromRow(row);
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

        // 2. Add existing source and target connections into the cache
        for (Map<String, String> row : data) {
            ConnectionDetails source = LineageDetails.getSourceConnectionFromRow(row);
            if (source != null) {
                try {
                    ConnectionDetails.findAndCache(connectionCache, source.getName(), source.getType());
                } catch (AtlanException e) {
                    log.error("Unable to find source connection: {}", source, e);
                }
            }
            ConnectionDetails target = LineageDetails.getTargetConnectionFromRow(row);
            if (target != null) {
                try {
                    ConnectionDetails.findAndCache(connectionCache, target.getName(), target.getType());
                } catch (AtlanException e) {
                    log.error("Unable to find target connection: {}", target, e);
                }
            }
        }

        // 3. Build up map of unique processes, with all their input and output details,
        //    and then bulk-upsert them
        Map<String, Set<LineageDetails>> lineage = new LinkedHashMap<>();
        for (Map<String, String> row : data) {
            LineageDetails details = LineageDetails.getFromRow(connectionCache, row, getDelimiter());
            if (details != null) {
                String processQN = details.getProcessConnectionQualifiedName() + "/" + details.getProcessId();
                if (!lineage.containsKey(processQN)) {
                    lineage.put(processQN, new LinkedHashSet<>());
                }
                lineage.get(processQN).add(details);
            }
        }
        LineageDetails.upsert(lineage, getBatchSize());
    }
}
