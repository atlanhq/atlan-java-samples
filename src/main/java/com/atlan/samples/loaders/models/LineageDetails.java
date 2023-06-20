/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.model.assets.ICatalog;
import com.atlan.model.assets.LineageProcess;
import com.atlan.model.enums.AtlanAnnouncementType;
import com.atlan.model.enums.AtlanConnectorType;
import com.atlan.model.enums.CertificateStatus;
import com.atlan.util.AssetBatch;
import java.util.*;
import java.util.regex.Pattern;
import lombok.Builder;
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
public class LineageDetails extends AssetDetails {

    public static final String COL_S_CONNECTOR = "CONNECTOR (S)";
    public static final String COL_S_CONNECTION = "CONNECTION (S)";
    public static final String COL_S_ASSET_TYPE = "SOURCE ASSET TYPE";
    public static final String COL_S_ASSET = "SOURCE ASSET";
    public static final String COL_ORCHESTRATOR = "ORCHESTRATOR";
    public static final String COL_PROCESS_ID = "PROCESS ID";
    public static final String COL_PROCESS_TYPE = "PROCESS TYPE";
    public static final String COL_T_ASSET = "TARGET ASSET";
    public static final String COL_T_ASSET_TYPE = "TARGET ASSET TYPE";
    public static final String COL_T_CONNECTOR = "CONNECTOR (T)";
    public static final String COL_T_CONNECTION = "CONNECTION (T)";
    public static final String COL_SQL_CODE = "SQL / CODE";
    public static final String COL_PROCESS_URL = "PROCESS URL";

    private static final Pattern CONNECTION_QN_PREFIX = Pattern.compile("default/[a-z0-9]+/[0-9]{10}/.*");

    private static final List<String> REQUIRED = List.of(
            COL_S_ASSET_TYPE,
            COL_S_ASSET,
            COL_ORCHESTRATOR,
            COL_PROCESS_TYPE,
            COL_PROCESS_ID,
            COL_T_ASSET,
            COL_T_ASSET_TYPE);

    @ToString.Include
    private AssetHeader sourceAsset;

    @ToString.Include
    private String processConnectionQualifiedName;

    @ToString.Include
    private String processId;

    @ToString.Include
    private AssetHeader targetAsset;

    @ToString.Include
    private String sqlCode;

    @ToString.Include
    private String processUrl;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentity() {
        return processConnectionQualifiedName + "/" + processId;
    }

    /**
     * Retrieve the orchestrator details from the provided row.
     *
     * @param row of data
     * @return the connection details for the orchestrator for that row
     */
    public static ConnectionDetails getOrchestratorFromRow(Map<String, String> row) {
        if (getMissingFields(row, List.of(COL_ORCHESTRATOR, COL_PROCESS_TYPE)).isEmpty()) {
            String processType = row.get(COL_PROCESS_TYPE);
            AtlanConnectorType connectorType = AtlanConnectorType.fromValue(processType);
            if (connectorType != null) {
                return ConnectionDetails.getHeader(row.get(COL_ORCHESTRATOR), connectorType);
            }
        }
        return null;
    }

    /**
     * Retrieve the source connection details from the provided row.
     *
     * @param row of data
     * @return the connection details for the source asset on that row
     */
    public static ConnectionDetails getSourceConnectionFromRow(Map<String, String> row) {
        if (getMissingFields(row, List.of(COL_S_CONNECTOR, COL_S_CONNECTION)).isEmpty()) {
            return ConnectionDetails.getHeader(
                    row.get(COL_S_CONNECTION),
                    AtlanConnectorType.fromValue(row.get(COL_S_CONNECTOR).toLowerCase()));
        }
        return null;
    }

    /**
     * Retrieve the target connection details from the provided row.
     *
     * @param row of data
     * @return the connection details for the target asset on that row
     */
    public static ConnectionDetails getTargetConnectionFromRow(Map<String, String> row) {
        if (getMissingFields(row, List.of(COL_T_CONNECTOR, COL_T_CONNECTION)).isEmpty()) {
            return ConnectionDetails.getHeader(
                    row.get(COL_T_CONNECTION),
                    AtlanConnectorType.fromValue(row.get(COL_T_CONNECTOR).toLowerCase()));
        }
        return null;
    }

    /**
     * Construct the qualifiedName of a connection that will be used for lineage processes.
     *
     * @param connectionCache cache of connections
     * @param row of data
     * @return the qualifiedName for the connection for lineage processes (if it exists), or null if it does not yet exist
     */
    static String getProcessConnectionQN(Map<ConnectionDetails, String> connectionCache, Map<String, String> row) {
        ConnectionDetails connection = getOrchestratorFromRow(row);
        return connectionCache.getOrDefault(connection, null);
    }

    /**
     * Build up details about the lineage on the provided row.
     *
     * @param connectionCache a cache of connections that have first been resolved across the spreadsheet
     * @param row a row of data from the spreadsheet, as a map from column name to value
     * @param delim delimiter used in cells that can contain multiple values
     * @return the lineage details for that row
     */
    public static LineageDetails getFromRow(
            Map<ConnectionDetails, String> connectionCache, Map<String, String> row, String delim) {

        LineageDetailsBuilder<?, ?> builder = getFromRow(LineageDetails.builder(), row, delim);

        if (getMissingFields(row, REQUIRED).isEmpty()) {

            String sourceAsset = row.get(COL_S_ASSET);
            String sourceType = row.get(COL_S_ASSET_TYPE);
            String targetAsset = row.get(COL_T_ASSET);
            String targetType = row.get(COL_T_ASSET_TYPE);

            // Check if we were given fully-qualified names for the source and/or target assets
            // (if so, use them and ignore the connection details)
            String sourceAssetQN = null;
            String targetAssetQN = null;
            if (CONNECTION_QN_PREFIX.matcher(sourceAsset).matches()) {
                sourceAssetQN = sourceAsset;
            }
            if (CONNECTION_QN_PREFIX.matcher(targetAsset).matches()) {
                targetAssetQN = targetAsset;
            }

            // If either of the provided names are not fully-qualified, look up the connections
            // to prepend them
            if (sourceAssetQN == null) {
                String sourceConnectionQN =
                        ConnectionDetails.getQualifiedName(connectionCache, row, COL_S_CONNECTOR, COL_S_CONNECTION);
                if (sourceConnectionQN != null) {
                    sourceAssetQN = sourceConnectionQN + "/" + sourceAsset;
                }
            }
            if (targetAssetQN == null) {
                String targetConnectionQN =
                        ConnectionDetails.getQualifiedName(connectionCache, row, COL_T_CONNECTOR, COL_T_CONNECTION);
                if (targetConnectionQN != null) {
                    targetAssetQN = targetConnectionQN + "/" + targetAsset;
                }
            }

            // Then build up the process qualifiedName
            String processId = row.get(COL_PROCESS_ID);
            String processConnectionQN = getProcessConnectionQN(connectionCache, row);

            AssetHeader source = AssetHeader.of(sourceType, sourceAssetQN);
            AssetHeader target = AssetHeader.of(targetType, targetAssetQN);

            if (source != null && target != null && processConnectionQN != null) {
                return builder.sourceAsset(source)
                        .targetAsset(target)
                        .processConnectionQualifiedName(processConnectionQN)
                        .processId(processId)
                        .sqlCode(row.get(COL_SQL_CODE))
                        .processUrl(row.get(COL_PROCESS_URL))
                        .stub(false)
                        .build();
            }
        }
        return null;
    }

    /**
     * Create lineage processes in bulk, if they do not exist, or update them if they do (idempotent).
     *
     * @param processes the map of processes to ensure exist, keyed by processQualifiedName
     * @param batchSize maximum number of processes to create per batch
     */
    public static void upsert(Map<String, Set<LineageDetails>> processes, int batchSize) {
        AssetBatch batch = new AssetBatch(LineageProcess.TYPE_NAME, batchSize);
        Map<String, List<String>> toTag = new HashMap<>();

        for (Map.Entry<String, Set<LineageDetails>> entry : processes.entrySet()) {
            Set<LineageDetails> assets = entry.getValue();
            Set<ICatalog> inputs = new HashSet<>();
            Set<ICatalog> outputs = new HashSet<>();
            Set<String> atlanTagNames = new HashSet<>();
            String description = null;
            CertificateStatus certificate = null;
            String certificateMessage = null;
            AtlanAnnouncementType announcementType = null;
            String announcementTitle = null;
            String announcementMessage = null;
            Set<String> ownerUsers = new HashSet<>();
            Set<String> ownerGroups = new HashSet<>();
            String sqlCode = null;
            String processUrl = null;
            String processId = null;
            String processConnectionQN = null;
            for (LineageDetails details : assets) {
                processId = details.getProcessId();
                processConnectionQN = details.getProcessConnectionQualifiedName();
                AssetHeader source = details.getSourceAsset();
                AssetHeader target = details.getTargetAsset();
                ICatalog input = ICatalog.getLineageReference(source.getTypeName(), source.getQualifiedName());
                ICatalog output = ICatalog.getLineageReference(target.getTypeName(), target.getQualifiedName());
                inputs.add(input);
                outputs.add(output);
                if (details.getDescription() != null && details.getDescription().length() > 0) {
                    description = details.getDescription();
                }
                if (details.getCertificate() != null) {
                    certificate = details.getCertificate();
                }
                if (details.getCertificateStatusMessage() != null
                        && details.getCertificateStatusMessage().length() > 0) {
                    certificateMessage = details.getCertificateStatusMessage();
                }
                if (details.getAnnouncementType() != null) {
                    announcementType = details.getAnnouncementType();
                }
                if (details.getAnnouncementTitle() != null
                        && details.getAnnouncementTitle().length() > 0) {
                    announcementTitle = details.getAnnouncementTitle();
                }
                if (details.getAnnouncementMessage() != null
                        && details.getAnnouncementMessage().length() > 0) {
                    announcementMessage = details.getAnnouncementMessage();
                }
                if (details.getOwnerUsers() != null) {
                    ownerUsers.addAll(details.getOwnerUsers());
                }
                if (details.getOwnerGroups() != null) {
                    ownerGroups.addAll(details.getOwnerGroups());
                }
                if (details.getSqlCode() != null && details.getSqlCode().length() > 0) {
                    sqlCode = details.getSqlCode();
                }
                if (details.getProcessUrl() != null && details.getProcessUrl().length() > 0) {
                    processUrl = details.getProcessUrl();
                }
                if (details.getAtlanTags() != null) {
                    atlanTagNames.addAll(details.getAtlanTags());
                }
            }
            if (processConnectionQN != null) {
                LineageProcess.LineageProcessBuilder<?, ?> builder = LineageProcess.creator(
                                processId,
                                processConnectionQN,
                                processId,
                                new ArrayList<>(inputs),
                                new ArrayList<>(outputs),
                                null)
                        .description(description)
                        .certificateStatus(certificate)
                        .certificateStatusMessage(certificateMessage)
                        .announcementType(announcementType)
                        .announcementTitle(announcementTitle)
                        .announcementMessage(announcementMessage)
                        .ownerUsers(ownerUsers)
                        .ownerGroups(ownerGroups)
                        .sql(sqlCode)
                        .code(sqlCode)
                        .sourceURL(processUrl);
                LineageProcess process = builder.build();
                if (!atlanTagNames.isEmpty()) {
                    toTag.put(process.getQualifiedName(), new ArrayList<>(atlanTagNames));
                }
                batch.add(process);
            }
        }
        // And don't forget to flush out any that remain
        batch.flush();

        // Atlan tags must be added in a second pass, after the asset exists
        appendAtlanTags(toTag, LineageProcess.TYPE_NAME);
    }

    @Getter
    @Builder
    @EqualsAndHashCode
    public static class AssetHeader {
        private String typeName;
        private String qualifiedName;

        public static AssetHeader of(String typeName, String qualifiedName) {
            if (typeName != null && typeName.length() > 0 && qualifiedName != null && qualifiedName.length() > 0) {
                return AssetHeader.builder()
                        .typeName(typeName)
                        .qualifiedName(qualifiedName)
                        .build();
            }
            return null;
        }
    }
}
