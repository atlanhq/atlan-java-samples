/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.probable.guacamole.model.enums;

import com.atlan.model.enums.AtlanSearchableField;
import javax.annotation.processing.Generated;
import lombok.Getter;

@Generated(value = "com.probable.guacamole.generators.POJOGenerator")
public enum BooleanFields implements AtlanSearchableField {
    /** TBC */
    ADLS_CONTAINER_VERSION_LEVEL_IMMUTABILITY_SUPPORT("adlsContainerVersionLevelImmutabilitySupport"),
    /** TBC */
    ADLS_OBJECT_SERVER_ENCRYPTED("adlsObjectServerEncrypted"),
    /** TBC */
    ADLS_OBJECT_VERSION_LEVEL_IMMUTABILITY_SUPPORT("adlsObjectVersionLevelImmutabilitySupport"),
    /** TBC */
    ALLOW_QUERY("allowQuery"),
    /** TBC */
    ALLOW_QUERY_PREVIEW("allowQueryPreview"),
    /** TBC */
    API_IS_AUTH_OPTIONAL("apiIsAuthOptional"),
    /** TBC */
    API_PATH_IS_INGRESS_EXPOSED("apiPathIsIngressExposed"),
    /** TBC */
    API_PATH_IS_TEMPLATED("apiPathIsTemplated"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_ARTIFACTS_SAVED("assetDbtJobLastRunArtifactsSaved"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_HAS_DOCS_GENERATED("assetDbtJobLastRunHasDocsGenerated"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_HAS_SOURCES_GENERATED("assetDbtJobLastRunHasSourcesGenerated"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_NOTIFICATIONS_SENT("assetDbtJobLastRunNotificationsSent"),
    /** TBC */
    AUTH_SERVICE_IS_ENABLED("authServiceIsEnabled"),
    /** TBC */
    GCS_BUCKET_RETENTION_LOCKED("gcsBucketRetentionLocked"),
    /** TBC */
    GCS_BUCKET_VERSIONING_ENABLED("gcsBucketVersioningEnabled"),
    /** TBC */
    GCS_REQUESTER_PAYS("gcsRequesterPays"),
    /** Whether this table is currently archived (true) or not (false). */
    GUACAMOLE_ARCHIVED("guacamoleArchived"),
    /** TBC */
    HAS_EXTRACTS("hasExtracts"),
    /** TBC */
    HAS_LINEAGE("__hasLineage"),
    /** Boolean flag to tell if connection has popularity insights or not */
    HAS_POPULARITY_INSIGHTS("hasPopularityInsights"),
    /** TBC */
    IS_ACCESS_CONTROL_ENABLED("isAccessControlEnabled"),
    /** TBC */
    IS_CALCULATED("isCalculated"),
    /** TBC */
    IS_CASE_SENSITIVE("isCaseSensitive"),
    /** TBC */
    IS_CERTIFIED("isCertified"),
    /** TBC */
    IS_CLUSTERED("isClustered"),
    /** isCustom captures whether the object is a custom object or not */
    IS_CUSTOM("isCustom"),
    /** TBC */
    IS_DISCOVERABLE("isDiscoverable"),
    /** TBC */
    IS_DIST("isDist"),
    /** TBC */
    IS_EDITABLE("isEditable"),
    /** TBC */
    IS_ENCRYPTED("isEncrypted"),
    /** TBC */
    IS_FOREIGN("isForeign"),
    /** TBC */
    IS_GLOBAL("isGlobal"),
    /** TBC */
    IS_INDEXED("isIndexed"),
    /** TBC */
    IS_MERGABLE("isMergable"),
    /** TBC */
    IS_NULLABLE("isNullable"),
    /** TBC */
    IS_PARTITION("isPartition"),
    /** TBC */
    IS_PARTITIONED("isPartitioned"),
    /** TBC */
    IS_PINNED("isPinned"),
    /** TBC */
    IS_POLICY_ENABLED("isPolicyEnabled"),
    /** isPolymorphicForeignKey captures whether the field references to record of multiple objects */
    IS_POLYMORPHIC_FOREIGN_KEY("isPolymorphicForeignKey"),
    /** TBC */
    IS_PRIMARY("isPrimary"),
    /** TBC */
    IS_PRIVATE("isPrivate"),
    /** TBC */
    IS_PROFILED("isProfiled"),
    /** TBC */
    IS_PUBLISHED("isPublished"),
    /** TBC */
    IS_QUERYABLE("isQueryable"),
    /** TBC */
    IS_QUERY_PREVIEW("isQueryPreview"),
    /** TBC */
    IS_SAMPLE_DATA_PREVIEW_ENABLED("isSampleDataPreviewEnabled"),
    /** TBC */
    IS_SORT("isSort"),
    /** TBC */
    IS_SQL_SNIPPET("isSqlSnippet"),
    /** TBC */
    IS_TEMPORARY("isTemporary"),
    /** TBC */
    IS_TOP_LEVEL_PROJECT("isTopLevelProject"),
    /** TBC */
    IS_TRASHED_DATA_STUDIO_ASSET("isTrashedDataStudioAsset"),
    /** TBC */
    IS_UNIQUE("isUnique"),
    /** TBC */
    IS_VISUAL_QUERY("isVisualQuery"),
    /** TBC */
    KAFKA_TOPIC_IS_INTERNAL("kafkaTopicIsInternal"),
    /** TBC */
    MC_MONITOR_RULE_IS_SNOOZED("mcMonitorRuleIsSnoozed"),
    /** TBC */
    METABASE_IS_PERSONAL_COLLECTION("metabaseIsPersonalCollection"),
    /** Whether certified in MicroStrategy */
    MICRO_STRATEGY_IS_CERTIFIED("microStrategyIsCertified"),
    /** TBC */
    MODE_IS_PUBLIC("modeIsPublic"),
    /** TBC */
    MODE_IS_SHARED("modeIsShared"),
    /** TBC */
    POLICY_DELEGATE_ADMIN("policyDelegateAdmin"),
    /** TBC */
    POWER_BI_IS_EXTERNAL_MEASURE("powerBIIsExternalMeasure"),
    /** TBC */
    POWER_BI_IS_HIDDEN("powerBIIsHidden"),
    /** TBC */
    PRESET_DASHBOARD_IS_MANAGED_EXTERNALLY("presetDashboardIsManagedExternally"),
    /** TBC */
    PRESET_DASHBOARD_IS_PUBLISHED("presetDashboardIsPublished"),
    /** TBC */
    PRESET_WORKSPACE_IS_IN_MAINTENANCE_MODE("presetWorkspaceIsInMaintenanceMode"),
    /** TBC */
    PRESET_WORKSPACE_PUBLIC_DASHBOARDS_ALLOWED("presetWorkspacePublicDashboardsAllowed"),
    /** Whether section access/data masking is enabled on source */
    QLIK_HAS_SECTION_ACCESS("qlikHasSectionAccess"),
    /** Whether a qlik app is in direct query mode */
    QLIK_IS_DIRECT_QUERY_MODE("qlikIsDirectQueryMode"),
    /** Whether a qlik app is encrypted */
    QLIK_IS_ENCRYPTED("qlikIsEncrypted"),
    /** If the qlik object is published */
    QLIK_IS_PUBLISHED("qlikIsPublished"),
    /** Whether a qlik sheet is approved */
    QLIK_SHEET_IS_APPROVED("qlikSheetIsApproved"),
    /** Status whether the asset is published or not on source */
    REDASH_IS_PUBLISHED("redashIsPublished"),
    /** TBC */
    S3BUCKET_VERSIONING_ENABLED("s3BucketVersioningEnabled"),
    /** TBC */
    SIGMA_DATA_ELEMENT_FIELD_IS_HIDDEN("sigmaDataElementFieldIsHidden"),
    /** TBC */
    SNOWFLAKE_PIPE_IS_AUTO_INGEST_ENABLED("snowflakePipeIsAutoIngestEnabled"),
    /** TBC */
    SNOWFLAKE_STREAM_IS_STALE("snowflakeStreamIsStale"),
    ;

    @Getter(onMethod_ = {@Override})
    private final String indexedFieldName;

    BooleanFields(String indexedFieldName) {
        this.indexedFieldName = indexedFieldName;
    }
}
