/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.exception.AtlanException;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.Asset;
import com.atlan.model.assets.Database;
import com.atlan.util.AssetBatch;
import java.util.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for capturing the full details provided about a database.
 */
@Slf4j
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class DatabaseDetails extends AssetDetails {
    public static final String COL_DB = "DATABASE NAME";

    private static final List<String> REQUIRED =
            List.of(ConnectionDetails.COL_CONNECTOR, ConnectionDetails.COL_CONNECTION, COL_DB);
    private static final List<String> REQUIRED_EMPTY =
            List.of(SchemaDetails.COL_SCHEMA, ContainerDetails.COL_CONTAINER, ColumnDetails.COL_COLUMN);

    @ToString.Include
    private String connectionQualifiedName;

    @ToString.Include
    private String name;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentity() {
        return connectionQualifiedName + "/" + name;
    }

    /**
     * Construct a database's qualifiedName from the row of data and cache of connections.
     *
     * @param connectionCache cache of connections
     * @param row of data
     * @return the qualifiedName for the database on that row of data
     */
    public static String getQualifiedName(Map<ConnectionDetails, String> connectionCache, Map<String, String> row) {
        String connectionQN = ConnectionDetails.getQualifiedName(connectionCache, row);
        if (connectionQN != null) {
            String dbName = row.get(COL_DB);
            if (dbName != null) {
                return connectionQN + "/" + dbName;
            }
        }
        return null;
    }

    /**
     * Build up details about the database on the provided row.
     *
     * @param connectionCache a cache of connections that have first been resolved across the spreadsheet
     * @param row a row of data from the spreadsheet, as a map from column name to value
     * @param delim delimiter used in cells that can contain multiple values
     * @return the database details for that row
     */
    public static DatabaseDetails getFromRow(
            Map<ConnectionDetails, String> connectionCache, Map<String, String> row, String delim) {
        if (getMissingFields(row, REQUIRED).isEmpty()) {
            String connectionQualifiedName = ConnectionDetails.getQualifiedName(connectionCache, row);
            if (getRequiredEmptyFields(row, REQUIRED_EMPTY).isEmpty()) {
                return getFromRow(DatabaseDetails.builder(), row, delim)
                        .connectionQualifiedName(connectionQualifiedName)
                        .name(row.get(COL_DB))
                        .stub(false)
                        .build();
            } else {
                return DatabaseDetails.builder()
                        .connectionQualifiedName(connectionQualifiedName)
                        .name(row.get(COL_DB))
                        .stub(true)
                        .build();
            }
        }
        return null;
    }

    /**
     * Create databases in bulk, if they do not exist, or update them if they do (idempotent).
     *
     * @param databases the set of databases to ensure exist
     * @param batchSize maximum number of databases to create per batch
     * @param updateOnly if true, only attempt to update existing assets, otherwise allow assets to be created as well
     */
    public static void upsert(Map<String, DatabaseDetails> databases, int batchSize, boolean updateOnly) {
        AssetBatch batch = new AssetBatch(Database.TYPE_NAME, batchSize);
        Map<String, List<String>> toClassify = new HashMap<>();

        for (DatabaseDetails details : databases.values()) {
            String connectionQualifiedName = details.getConnectionQualifiedName();
            String databaseName = details.getName();
            if (updateOnly) {
                String qualifiedName = Database.generateQualifiedName(databaseName, connectionQualifiedName);
                try {
                    Asset.retrieveMinimal(Database.TYPE_NAME, qualifiedName);
                    Database toUpdate = Database.updater(qualifiedName, databaseName)
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
                    batch.add(toUpdate);
                } catch (NotFoundException e) {
                    log.warn("Unable to find existing database — skipping: {}", qualifiedName, e);
                } catch (AtlanException e) {
                    log.error("Unable to lookup whether database exists or not.", e);
                }
            } else {
                Database database = Database.creator(databaseName, connectionQualifiedName)
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
                    toClassify.put(database.getQualifiedName(), details.getAtlanTags());
                }
                batch.add(database);
            }
        }
        // And don't forget to flush out any that remain
        batch.flush();

        // Classifications must be added in a second pass, after the asset exists
        appendAtlanTags(toClassify, Database.TYPE_NAME);
    }
}
