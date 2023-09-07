/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.Atlan;
import com.atlan.exception.AtlanException;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.Asset;
import com.atlan.model.assets.Connection;
import com.atlan.model.core.AssetMutationResponse;
import com.atlan.model.enums.AtlanConnectorType;
import com.atlan.util.AssetBatch;
import java.util.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for capturing the full details provided about a column.
 */
@Slf4j
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class ConnectionDetails extends AssetDetails {
    protected static final String COL_CONNECTOR = "CONNECTOR";
    protected static final String COL_CONNECTION = "CONNECTION NAME";

    private static final List<String> REQUIRED = List.of(COL_CONNECTOR, COL_CONNECTION);
    private static final List<String> REQUIRED_EMPTY = List.of(
            DatabaseDetails.COL_DB,
            SchemaDetails.COL_SCHEMA,
            ContainerDetails.COL_CONTAINER,
            ColumnDetails.COL_COLUMN,
            AccountDetails.COL_ACCOUNT,
            BucketDetails.COL_BUCKET_NAME,
            ObjectDetails.COL_OBJECT_NAME);

    @ToString.Include
    private String name;

    @ToString.Include
    private AtlanConnectorType type;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentity() {
        return type.getValue() + "/" + name;
    }

    /**
     * Construct a connection's qualifiedName from the row of data and cache of connections.
     *
     * @param connectionCache cache of connections
     * @param row of data
     * @return the qualifiedName for the connection on that row of data
     */
    public static String getQualifiedName(Map<ConnectionDetails, String> connectionCache, Map<String, String> row) {
        return getQualifiedName(connectionCache, row, COL_CONNECTOR, COL_CONNECTION);
    }

    /**
     * Construct a connection's qualifiedName from the row of data and cache of connections,
     * using the specified columns for the connection details.
     *
     * @param connectionCache cache of connections
     * @param row of data
     * @param connectorCol the column containing the connector type
     * @param connectionCol the column containing the connection name
     * @return the qualifiedName for the connection on that row of data (if it exists), or null if it does not yet exist
     */
    static String getQualifiedName(
            Map<ConnectionDetails, String> connectionCache,
            Map<String, String> row,
            String connectorCol,
            String connectionCol) {
        ConnectionDetails connection = ConnectionDetails.getHeaderFromRow(row, connectorCol, connectionCol);
        return connectionCache.getOrDefault(connection, null);
    }

    /**
     * Build up details about the column on the provided row.
     *
     * @param row a row of data from the spreadsheet, as a map from column name to value
     * @param delim delimiter used in cells that can contain multiple values
     * @return the column details for that row
     */
    public static ConnectionDetails getFromRow(Map<String, String> row, String delim) {
        if (getMissingFields(row, REQUIRED).isEmpty()) {
            String type = row.get(COL_CONNECTOR);
            String name = row.get(COL_CONNECTION);
            if (getRequiredEmptyFields(row, REQUIRED_EMPTY).isEmpty()) {
                return getFromRow(ConnectionDetails.builder(), row, delim)
                        .type(AtlanConnectorType.fromValue(type.toLowerCase(Locale.ROOT)))
                        .name(name)
                        .stub(false)
                        .build();
            } else {
                return ConnectionDetails.builder()
                        .type(AtlanConnectorType.fromValue(type.toLowerCase(Locale.ROOT)))
                        .name(name)
                        .stub(true)
                        .build();
            }
        }
        return null;
    }

    /**
     * Return the minimal information required to determine the connection.
     *
     * @param row of data from which to retrieve connection details
     * @param connectorCol name of column containing connector type
     * @param connectionCol name of column containing connection name
     * @return the minimal connection details
     */
    static ConnectionDetails getHeaderFromRow(Map<String, String> row, String connectorCol, String connectionCol) {
        if (getMissingFields(row, List.of(connectorCol, connectionCol)).isEmpty()) {
            return getHeader(
                    row.get(connectionCol),
                    AtlanConnectorType.fromValue(row.get(connectorCol).toLowerCase(Locale.ROOT)));
        } else {
            return null;
        }
    }

    /**
     * Return the minimal information required to determine a connection.
     *
     * @param name name of the connection
     * @param type type of the connection
     * @return the minimal connection details
     */
    static ConnectionDetails getHeader(String name, AtlanConnectorType type) {
        return ConnectionDetails.builder().name(name).type(type).stub(true).build();
    }

    /**
     * Create connections in bulk, if they do not exist, or simply cache them if they do (idempotent).
     *
     * @param connections details of the connections to create
     * @param batchSize maximum number of connections to create per batch
     * @param updateOnly if true, only attempt to update existing assets, otherwise allow assets to be created as well
     * @return a mapping of connection headers to their qualifiedName (as created or found)
     */
    public static Map<ConnectionDetails, String> upsert(
            Map<String, ConnectionDetails> connections, int batchSize, boolean updateOnly) {

        Map<ConnectionDetails, String> cache = new HashMap<>();
        long totalResults = connections.size();
        long localCount = 0;

        // 1. Search for existing connection with the name, so we can avoid creating if it
        //    already exists (since connection qualifiedNames have a time-based component, they
        //    are not automatically idempotent across create and update)
        AssetBatch batch = new AssetBatch(Atlan.getDefaultClient(), Connection.TYPE_NAME, batchSize);
        log.info("... looking for existing ({}) connections...", totalResults);
        try {
            for (ConnectionDetails details : connections.values()) {
                String name = details.getName();
                AtlanConnectorType type = details.getType();
                ConnectionDetails header = getHeader(name, type);
                // Only continue if we have not already processed this connection...
                if (!cache.containsKey(header)) {
                    try {
                        findAndCache(cache, name, type);
                        Connection toUpdate = Connection.updater(cache.get(header), name)
                                .description(details.getDescription())
                                .certificateStatus(details.getCertificate())
                                .certificateStatusMessage(details.getCertificateStatusMessage())
                                .announcementType(details.getAnnouncementType())
                                .announcementTitle(details.getAnnouncementTitle())
                                .announcementMessage(details.getAnnouncementMessage())
                                .build();
                        localCount++;
                        if (batch.add(toUpdate) != null) {
                            log.info(
                                    " ... processed {}/{} ({}%)",
                                    localCount, totalResults, Math.round(((double) localCount / totalResults) * 100));
                        }
                        // No need to re-cache, the qualifiedName won't change
                    } catch (NotFoundException e) {
                        if (!updateOnly) {
                            try {
                                List<String> leftOverGroups = new ArrayList<>();
                                List<String> ownerRoles = new ArrayList<>();
                                List<String> ownerUsers = details.getOwnerUsers();
                                List<String> ownerGroups = details.getOwnerGroups();
                                if (!ownerGroups.isEmpty()) {
                                    for (String groupName : ownerGroups) {
                                        if (groupName.startsWith("$")) {
                                            ownerRoles.add(Atlan.getDefaultClient()
                                                    .getRoleCache()
                                                    .getIdForName(groupName));
                                        } else {
                                            leftOverGroups.add(groupName);
                                        }
                                    }
                                }
                                if (leftOverGroups.isEmpty() && ownerRoles.isEmpty() && ownerUsers.isEmpty()) {
                                    // If no owners have been specified at all (no connection admins),
                                    // then fallback to setting All Admins as the connection owner
                                    ownerRoles.add(Atlan.getDefaultClient()
                                            .getRoleCache()
                                            .getIdForName("$admin"));
                                }
                                Connection toCreate = Connection.creator(
                                                header.getName(),
                                                header.getType(),
                                                ownerRoles,
                                                leftOverGroups,
                                                ownerUsers)
                                        .description(details.getDescription())
                                        .certificateStatus(details.getCertificate())
                                        .certificateStatusMessage(details.getCertificateStatusMessage())
                                        .announcementType(details.getAnnouncementType())
                                        .announcementTitle(details.getAnnouncementTitle())
                                        .announcementMessage(details.getAnnouncementMessage())
                                        .build();
                                localCount++;
                                AssetMutationResponse response = batch.add(toCreate);
                                if (response != null) {
                                    log.info(
                                            " ... processed {}/{} ({}%)",
                                            localCount,
                                            totalResults,
                                            Math.round(((double) localCount / totalResults) * 100));
                                }
                                cacheConnections(cache, response);
                                // Need to add some delay, otherwise the connections will overlap as they could be
                                // created
                                // within the
                                // same millisecond (same qualifiedName)
                                Thread.sleep(2000);
                            } catch (AtlanException | InterruptedException inner) {
                                log.error(
                                        "Unexpected exception while trying to create connection ({}) or batch: {}",
                                        header,
                                        batch,
                                        inner);
                            }
                        } else {
                            log.warn("Unable to find existing connection — skipping: {}/{}", type, name);
                        }
                    }
                }
            }
            AssetMutationResponse response = batch.flush();
            if (response != null) {
                log.info(
                        " ... processed {}/{} ({}%)",
                        localCount, totalResults, Math.round(((double) localCount / totalResults) * 100));
            }
            cacheConnections(cache, response);
        } catch (AtlanException e) {
            log.error("Unable to bulk-upsert connection details.", e);
        }

        // 3. Retrieve each connection in turn to ensure async permissions have been set for them
        for (String qualifiedName : cache.values()) {
            try {
                Connection.get(Atlan.getDefaultClient(), qualifiedName, false);
            } catch (AtlanException e) {
                log.error("Unable to access connection: {}", qualifiedName, e);
            }
        }
        return cache;
    }

    /**
     * Find any existing connection with the provided name and type, and add it to the cache (if it exists).
     *
     * @param cache to which to add the connection details
     * @param name of the connection
     * @param type of the connector for the connection
     * @throws AtlanException on any error finding the connection, including if it does not exist
     */
    public static void findAndCache(Map<ConnectionDetails, String> cache, String name, AtlanConnectorType type)
            throws AtlanException {
        ConnectionDetails header = getHeader(name, type);
        if (!cache.containsKey(header)) {
            // Only run the search if we don't already have the connection details in the cache
            List<Connection> found = Connection.findByName(name, type);
            if (found.size() == 1) {
                log.info("...... found: {} ({})", name, found.get(0).getQualifiedName());
                cache.put(header, found.get(0).getQualifiedName());
            } else if (found.size() > 1) {
                log.warn("...... found multiple connections with name {} — using only the first: {}", name, found);
                cache.put(header, found.get(0).getQualifiedName());
            }
        }
    }

    private static void cacheConnections(Map<ConnectionDetails, String> cache, AssetMutationResponse response) {
        if (response != null) {
            // If any assets were created, then cache these newly-created connections
            List<Asset> results = response.getCreatedAssets();
            for (Asset created : results) {
                if (created instanceof Connection) {
                    Connection connection = (Connection) created;
                    cache.put(
                            getHeader(connection.getName(), connection.getConnectorType()),
                            connection.getQualifiedName());
                }
            }
        }
    }
}
