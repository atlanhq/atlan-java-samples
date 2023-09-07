/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.Atlan;
import com.atlan.exception.AtlanException;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.Schema;
import com.atlan.util.AssetBatch;
import java.util.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for capturing the full details provided about a schema.
 */
@Slf4j
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class SchemaDetails extends AssetDetails {
    public static final String COL_SCHEMA = "SCHEMA NAME";

    private static final List<String> REQUIRED = List.of(
            ConnectionDetails.COL_CONNECTOR, ConnectionDetails.COL_CONNECTION, DatabaseDetails.COL_DB, COL_SCHEMA);
    private static final List<String> REQUIRED_EMPTY =
            List.of(ContainerDetails.COL_CONTAINER, ColumnDetails.COL_COLUMN);

    @ToString.Include
    private String databaseQualifiedName;

    @ToString.Include
    private String name;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentity() {
        return databaseQualifiedName + "/" + name;
    }

    /**
     * Construct a schema's qualifiedName from the row of data and cache of connections.
     *
     * @param connectionCache cache of connections
     * @param row of data
     * @return the qualifiedName for the schema on that row of data
     */
    public static String getQualifiedName(Map<ConnectionDetails, String> connectionCache, Map<String, String> row) {
        String databaseQN = DatabaseDetails.getQualifiedName(connectionCache, row);
        if (databaseQN != null) {
            String schemaName = row.get(COL_SCHEMA);
            if (schemaName != null) {
                return databaseQN + "/" + schemaName;
            }
        }
        return null;
    }

    /**
     * Build up details about the schema on the provided row.
     *
     * @param connectionCache a cache of connections that have first been resolved across the spreadsheet
     * @param row a row of data from the spreadsheet, as a map from column name to value
     * @param delim delimiter used in cells that can contain multiple values
     * @return the schema details for that row
     */
    public static SchemaDetails getFromRow(
            Map<ConnectionDetails, String> connectionCache, Map<String, String> row, String delim) {
        if (getMissingFields(row, REQUIRED).isEmpty()) {
            String databaseQualifiedName = DatabaseDetails.getQualifiedName(connectionCache, row);
            if (getRequiredEmptyFields(row, REQUIRED_EMPTY).isEmpty()) {
                return getFromRow(SchemaDetails.builder(), row, delim)
                        .databaseQualifiedName(databaseQualifiedName)
                        .name(row.get(COL_SCHEMA))
                        .stub(false)
                        .build();
            } else {
                return SchemaDetails.builder()
                        .databaseQualifiedName(databaseQualifiedName)
                        .name(row.get(COL_SCHEMA))
                        .stub(true)
                        .build();
            }
        }
        return null;
    }

    /**
     * Create schemas in bulk, if they do not exist, or update them if they do (idempotent).
     *
     * @param schemas the set of schemas to ensure exist
     * @param batchSize maximum number of schemas to create per batch
     * @param updateOnly if true, only attempt to update existing assets, otherwise allow assets to be created as well
     * @return qualifiedNames of all parent databases in which assets were created or updated
     */
    public static Set<String> upsert(Map<String, SchemaDetails> schemas, int batchSize, boolean updateOnly) {
        Set<String> parents = new HashSet<>();
        AssetBatch batch = new AssetBatch(Atlan.getDefaultClient(), Schema.TYPE_NAME, batchSize);
        Map<String, List<String>> toClassify = new HashMap<>();

        long totalResults = schemas.size();
        long localCount = 0;

        try {
            for (SchemaDetails details : schemas.values()) {
                String databaseQualifiedName = details.getDatabaseQualifiedName();
                String schemaName = details.getName();
                if (updateOnly) {
                    String qualifiedName = Schema.generateQualifiedName(schemaName, databaseQualifiedName);
                    try {
                        Schema.get(Atlan.getDefaultClient(), qualifiedName, false);
                        Schema toUpdate = Schema.updater(qualifiedName, schemaName)
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
                            toClassify.put(toUpdate.getQualifiedName(), details.getAtlanTags());
                        }
                        localCount++;
                        if (batch.add(toUpdate) != null) {
                            log.info(
                                    " ... processed {}/{} ({}%)",
                                    localCount, totalResults, Math.round(((double) localCount / totalResults) * 100));
                        }
                        parents.add(databaseQualifiedName);
                    } catch (NotFoundException e) {
                        log.warn("Unable to find existing schema — skipping: {}", qualifiedName, e);
                    } catch (AtlanException e) {
                        log.error("Unable to lookup whether schema exists or not.", e);
                    }
                } else {
                    Schema schema = Schema.creator(schemaName, databaseQualifiedName)
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
                        toClassify.put(schema.getQualifiedName(), details.getAtlanTags());
                    }
                    localCount++;
                    if (batch.add(schema) != null) {
                        log.info(
                                " ... processed {}/{} ({}%)",
                                localCount, totalResults, Math.round(((double) localCount / totalResults) * 100));
                    }
                    parents.add(databaseQualifiedName);
                }
            }
            // And don't forget to flush out any that remain
            if (batch.flush() != null) {
                log.info(
                        " ... processed {}/{} ({}%)",
                        localCount, totalResults, Math.round(((double) localCount / totalResults) * 100));
            }
        } catch (AtlanException e) {
            log.error("Unable to bulk-upsert schema details.", e);
        }

        // Classifications must be added in a second pass, after the asset exists
        appendAtlanTags(toClassify, Schema.TYPE_NAME);

        return parents;
    }
}
