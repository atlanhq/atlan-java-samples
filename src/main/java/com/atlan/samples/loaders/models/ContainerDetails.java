/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.Atlan;
import com.atlan.exception.AtlanException;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.*;
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
public class ContainerDetails extends AssetDetails {
    public static final String COL_CONTAINER = "CONTAINER NAME";
    public static final String COL_CONTAINER_TYPE = "CONTAINER TYPE";

    private static final List<String> REQUIRED = List.of(
            ConnectionDetails.COL_CONNECTOR,
            ConnectionDetails.COL_CONNECTION,
            DatabaseDetails.COL_DB,
            SchemaDetails.COL_SCHEMA,
            COL_CONTAINER,
            COL_CONTAINER_TYPE);
    private static final List<String> REQUIRED_EMPTY = List.of(ColumnDetails.COL_COLUMN);

    @ToString.Include
    private String schemaQualifiedName;

    @ToString.Include
    private String name;

    @ToString.Include
    private String type;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentity() {
        return schemaQualifiedName + "/" + name;
    }

    /**
     * Construct a container's qualifiedName from the row of data and cache of connections.
     *
     * @param connectionCache cache of connections
     * @param row of data
     * @return the qualifiedName for the container on that row of data
     */
    public static String getQualifiedName(Map<ConnectionDetails, String> connectionCache, Map<String, String> row) {
        String schemaQN = SchemaDetails.getQualifiedName(connectionCache, row);
        if (schemaQN != null) {
            String containerName = row.get(COL_CONTAINER);
            if (containerName != null) {
                return schemaQN + "/" + containerName;
            }
        }
        return null;
    }

    /**
     * Return the minimal information required to determine the container.
     *
     * @param name of the container
     * @param type of the container
     * @return the minimal information needed to determine the container
     */
    static ContainerDetails getHeader(String name, String type) {
        return ContainerDetails.builder().name(name).type(type).stub(true).build();
    }

    /**
     * Build up details about the container on the provided row.
     *
     * @param connectionCache a cache of connections that have first been resolved across the spreadsheet
     * @param row a row of data from the spreadsheet, as a map from column name to value
     * @param delim delimiter used in cells that can contain multiple values
     * @return the container details for that row
     */
    public static ContainerDetails getFromRow(
            Map<ConnectionDetails, String> connectionCache, Map<String, String> row, String delim) {
        if (getMissingFields(row, REQUIRED).isEmpty()) {
            String schemaQualifiedName = SchemaDetails.getQualifiedName(connectionCache, row);
            if (getRequiredEmptyFields(row, REQUIRED_EMPTY).isEmpty()) {
                return getFromRow(ContainerDetails.builder(), row, delim)
                        .schemaQualifiedName(schemaQualifiedName)
                        .name(row.get(COL_CONTAINER))
                        .type(row.get(COL_CONTAINER_TYPE))
                        .stub(false)
                        .build();
            } else {
                return ContainerDetails.builder()
                        .schemaQualifiedName(schemaQualifiedName)
                        .name(row.get(COL_CONTAINER))
                        .type(row.get(COL_CONTAINER_TYPE))
                        .stub(true)
                        .build();
            }
        } else {
            return null;
        }
    }

    /**
     * Create containers in bulk, if they do not exist, or update them if they do (idempotent).
     *
     * @param containers the set of containers to ensure exist
     * @param batchSize maximum number of containers to create per batch
     * @param updateOnly if true, only attempt to update existing assets, otherwise allow assets to be created as well
     * @return qualifiedNames of all parent schemas in which assets were created or updated
     */
    public static Set<String> upsert(Map<String, ContainerDetails> containers, int batchSize, boolean updateOnly) {
        Set<String> parents = new HashSet<>();
        AssetBatch batchContainers = new AssetBatch(Atlan.getDefaultClient(), "container", batchSize);
        Map<String, List<String>> toClassifyTables = new HashMap<>();
        Map<String, List<String>> toClassifyViews = new HashMap<>();
        Map<String, List<String>> toClassifyMVs = new HashMap<>();

        long totalResults = containers.size();
        long localCount = 0;

        try {
            for (ContainerDetails details : containers.values()) {
                String schemaQualifiedName = details.getSchemaQualifiedName();
                String containerName = details.getName();
                String containerType = details.getType();
                parents.add(schemaQualifiedName);
                switch (containerType) {
                    case Table.TYPE_NAME:
                        if (updateOnly) {
                            String qualifiedName = Table.generateQualifiedName(containerName, schemaQualifiedName);
                            try {
                                Table.get(Atlan.getDefaultClient(), qualifiedName, false);
                                Table toUpdate = Table.updater(qualifiedName, containerName)
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
                                    toClassifyTables.put(toUpdate.getQualifiedName(), details.getAtlanTags());
                                }
                                localCount++;
                                if (batchContainers.add(toUpdate) != null) {
                                    log.info(
                                            " ... processed {}/{} ({}%)",
                                            localCount,
                                            totalResults,
                                            Math.round(((double) localCount / totalResults) * 100));
                                }
                            } catch (NotFoundException e) {
                                log.warn("Unable to find existing table — skipping: {}", qualifiedName, e);
                            } catch (AtlanException e) {
                                log.error("Unable to lookup whether table exists or not.", e);
                            }
                        } else {
                            Table table = Table.creator(containerName, schemaQualifiedName)
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
                                toClassifyTables.put(table.getQualifiedName(), details.getAtlanTags());
                            }
                            localCount++;
                            if (batchContainers.add(table) != null) {
                                log.info(
                                        " ... processed {}/{} ({}%)",
                                        localCount,
                                        totalResults,
                                        Math.round(((double) localCount / totalResults) * 100));
                            }
                        }
                        break;
                    case View.TYPE_NAME:
                        if (updateOnly) {
                            String qualifiedName = View.generateQualifiedName(containerName, schemaQualifiedName);
                            try {
                                View.get(Atlan.getDefaultClient(), qualifiedName, false);
                                View toUpdate = View.updater(qualifiedName, containerName)
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
                                    toClassifyViews.put(toUpdate.getQualifiedName(), details.getAtlanTags());
                                }
                                localCount++;
                                if (batchContainers.add(toUpdate) != null) {
                                    log.info(
                                            " ... processed {}/{} ({}%)",
                                            localCount,
                                            totalResults,
                                            Math.round(((double) localCount / totalResults) * 100));
                                }
                            } catch (NotFoundException e) {
                                log.warn("Unable to find existing view — skipping: {}", qualifiedName, e);
                            } catch (AtlanException e) {
                                log.error("Unable to lookup whether view exists or not.", e);
                            }
                        } else {
                            View view = View.creator(containerName, schemaQualifiedName)
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
                                toClassifyViews.put(view.getQualifiedName(), details.getAtlanTags());
                            }
                            localCount++;
                            if (batchContainers.add(view) != null) {
                                log.info(
                                        " ... processed {}/{} ({}%)",
                                        localCount,
                                        totalResults,
                                        Math.round(((double) localCount / totalResults) * 100));
                            }
                        }
                        break;
                    case MaterializedView.TYPE_NAME:
                        if (updateOnly) {
                            String qualifiedName =
                                    MaterializedView.generateQualifiedName(containerName, schemaQualifiedName);
                            try {
                                MaterializedView.get(Atlan.getDefaultClient(), qualifiedName, false);
                                MaterializedView toUpdate = MaterializedView.updater(qualifiedName, containerName)
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
                                    toClassifyMVs.put(toUpdate.getQualifiedName(), details.getAtlanTags());
                                }
                                localCount++;
                                if (batchContainers.add(toUpdate) != null) {
                                    log.info(
                                            " ... processed {}/{} ({}%)",
                                            localCount,
                                            totalResults,
                                            Math.round(((double) localCount / totalResults) * 100));
                                }
                            } catch (NotFoundException e) {
                                log.warn("Unable to find existing view — skipping: {}", qualifiedName, e);
                            } catch (AtlanException e) {
                                log.error("Unable to lookup whether view exists or not.", e);
                            }
                        } else {
                            MaterializedView mv = MaterializedView.creator(containerName, schemaQualifiedName)
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
                                toClassifyMVs.put(mv.getQualifiedName(), details.getAtlanTags());
                            }
                            localCount++;
                            if (batchContainers.add(mv) != null) {
                                log.info(
                                        " ... processed {}/{} ({}%)",
                                        localCount,
                                        totalResults,
                                        Math.round(((double) localCount / totalResults) * 100));
                            }
                        }
                        break;
                    default:
                        log.error("Invalid container type ({}) — skipping: {}", containerType, details);
                        break;
                }
            }
            // And don't forget to flush out any that remain
            if (batchContainers.flush() != null) {
                log.info(
                        " ... processed {}/{} ({}%)",
                        localCount, totalResults, Math.round(((double) localCount / totalResults) * 100));
            }
        } catch (AtlanException e) {
            log.error("Unable to bulk-upsert container details.", e);
        }

        // Classifications must be added in a second pass, after the asset exists
        appendAtlanTags(toClassifyTables, Table.TYPE_NAME);
        appendAtlanTags(toClassifyViews, View.TYPE_NAME);
        appendAtlanTags(toClassifyMVs, MaterializedView.TYPE_NAME);

        return parents;
    }
}
