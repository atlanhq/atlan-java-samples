/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.probable.guacamole.model.enums;

import com.atlan.model.enums.AtlanSearchableField;
import javax.annotation.processing.Generated;
import lombok.Getter;

@Generated(value = "com.probable.guacamole.generators.POJOGenerator")
public enum TextFields implements AtlanSearchableField {
    /** TBC */
    ADLS_ACCOUNT_QUALIFIED_NAME("adlsAccountQualifiedName.text"),
    /** TBC */
    ADLS_ACCOUNT_RESOURCE_GROUP("adlsAccountResourceGroup"),
    /** TBC */
    ADLS_ACCOUNT_SUBSCRIPTION("adlsAccountSubscription"),
    /** TBC */
    ADLS_CONTAINER_QUALIFIED_NAME("adlsContainerQualifiedName.text"),
    /** TBC */
    ADLS_CONTAINER_URL("adlsContainerUrl"),
    /** TBC */
    ADLS_OBJECT_CACHE_CONTROL("adlsObjectCacheControl"),
    /** TBC */
    ADLS_OBJECT_CONTENT_LANGUAGE("adlsObjectContentLanguage"),
    /** TBC */
    ADLS_OBJECT_CONTENT_TYPE("adlsObjectContentType"),
    /** TBC */
    ADLS_OBJECT_URL("adlsObjectUrl"),
    /** TBC */
    API_NAME("apiName"),
    /** TBC */
    API_PATH_RAW_URI("apiPathRawURI.text"),
    /** TBC */
    API_PATH_SUMMARY("apiPathSummary"),
    /** TBC */
    API_SPEC_CONTACT_EMAIL("apiSpecContactEmail.text"),
    /** TBC */
    API_SPEC_CONTACT_NAME("apiSpecContactName"),
    /** TBC */
    API_SPEC_CONTACT_URL("apiSpecContactURL.text"),
    /** TBC */
    API_SPEC_LICENSE_NAME("apiSpecLicenseName"),
    /** TBC */
    API_SPEC_LICENSE_URL("apiSpecLicenseURL.text"),
    /** TBC */
    API_SPEC_NAME("apiSpecName"),
    /** TBC */
    API_SPEC_QUALIFIED_NAME("apiSpecQualifiedName.text"),
    /** TBC */
    API_SPEC_SERVICE_ALIAS("apiSpecServiceAlias.text"),
    /** TBC */
    API_SPEC_TERMS_OF_SERVICE_URL("apiSpecTermsOfServiceURL.text"),
    /** TBC */
    ASSET_DBT_ACCOUNT_NAME("assetDbtAccountName"),
    /** TBC */
    ASSET_DBT_ALIAS("assetDbtAlias"),
    /** TBC */
    ASSET_DBT_ENVIRONMENT_NAME("assetDbtEnvironmentName"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_GIT_BRANCH("assetDbtJobLastRunGitBranch.text"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_STATUS_MESSAGE("assetDbtJobLastRunStatusMessage"),
    /** TBC */
    ASSET_DBT_JOB_NAME("assetDbtJobName"),
    /** TBC */
    ASSET_DBT_JOB_NEXT_RUN_HUMANIZED("assetDbtJobNextRunHumanized"),
    /** TBC */
    ASSET_DBT_JOB_SCHEDULE_CRON_HUMANIZED("assetDbtJobScheduleCronHumanized"),
    /** TBC */
    ASSET_DBT_PACKAGE_NAME("assetDbtPackageName"),
    /** TBC */
    ASSET_DBT_PROJECT_NAME("assetDbtProjectName"),
    /** TBC */
    ASSET_DBT_TAGS("assetDbtTags.text"),
    /** TBC */
    ASSET_DBT_UNIQUE_ID("assetDbtUniqueId"),
    /** TBC */
    ASSET_MC_INCIDENT_NAMES("assetMcIncidentNames"),
    /** TBC */
    ASSET_MC_INCIDENT_QUALIFIED_NAMES("assetMcIncidentQualifiedNames.text"),
    /** TBC */
    ASSET_MC_MONITOR_NAMES("assetMcMonitorNames"),
    /** TBC */
    ASSET_MC_MONITOR_QUALIFIED_NAMES("assetMcMonitorQualifiedNames.text"),
    /** TBC */
    ASSET_TAGS("assetTags.text"),
    /** All Atlan tags that exist on an asset, whether directly assigned or propagated, searchable by the internal hashed-string ID of the Atlan tag. */
    ATLAN_TAGS_TEXT("__classificationsText"),
    /** TBC */
    AWS_ARN("awsArn.text"),
    /** TBC */
    AWS_OWNER_NAME("awsOwnerName.text"),
    /** TBC */
    AZURE_RESOURCE_ID("azureResourceId.text"),
    /** TBC */
    CERTIFICATE_STATUS("certificateStatus.text"),
    /** TBC */
    COLLECTION_QUALIFIED_NAME("collectionQualifiedName.text"),
    /** TBC */
    CONNECTION_NAME("connectionName.text"),
    /** TBC */
    CONNECTION_QUALIFIED_NAME("connectionQualifiedName.text"),
    /** TBC */
    DATABASE_NAME("databaseName"),
    /** TBC */
    DATA_STUDIO_ASSET_TITLE("dataStudioAssetTitle"),
    /** data type of the field */
    DATA_TYPE("dataType.text"),
    /** TBC */
    DBT_ACCOUNT_NAME("dbtAccountName"),
    /** TBC */
    DBT_ALIAS("dbtAlias"),
    /** TBC */
    DBT_ENVIRONMENT_DBT_VERSION("dbtEnvironmentDbtVersion"),
    /** TBC */
    DBT_ENVIRONMENT_NAME("dbtEnvironmentName"),
    /** TBC */
    DBT_JOB_NAME("dbtJobName"),
    /** TBC */
    DBT_JOB_NEXT_RUN_HUMANIZED("dbtJobNextRunHumanized"),
    /** TBC */
    DBT_JOB_SCHEDULE_CRON_HUMANIZED("dbtJobScheduleCronHumanized"),
    /** TBC */
    DBT_MODEL_QUALIFIED_NAME("dbtModelQualifiedName.text"),
    /** TBC */
    DBT_PACKAGE_NAME("dbtPackageName"),
    /** TBC */
    DBT_PROJECT_NAME("dbtProjectName"),
    /** TBC */
    DBT_QUALIFIED_NAME("dbtQualifiedName.text"),
    /** TBC */
    DBT_UNIQUE_ID("dbtUniqueId"),
    /** TBC */
    DEFAULT_DATABASE_QUALIFIED_NAME("defaultDatabaseQualifiedName.text"),
    /** TBC */
    DEFAULT_SCHEMA_QUALIFIED_NAME("defaultSchemaQualifiedName.text"),
    /** TBC */
    DESCRIPTION("description"),
    /** TBC */
    DISPLAY_NAME("displayName"),
    /** TBC */
    GCS_BUCKET_LIFECYCLE_RULES("gcsBucketLifecycleRules"),
    /** TBC */
    GCS_BUCKET_NAME("gcsBucketName"),
    /** TBC */
    GCS_BUCKET_QUALIFIED_NAME("gcsBucketQualifiedName.text"),
    /** TBC */
    GCS_BUCKET_RETENTION_POLICY("gcsBucketRetentionPolicy"),
    /** TBC */
    GCS_OBJECT_KEY("gcsObjectKey.text"),
    /** TBC */
    GCS_OBJECT_MEDIA_LINK("gcsObjectMediaLink.text"),
    /** TBC */
    GOOGLE_PROJECT_ID("googleProjectId.text"),
    /** TBC */
    GOOGLE_PROJECT_NAME("googleProjectName.text"),
    /** TBC */
    INLINE_HELP_TEXT("inlineHelpText"),
    /** TBC */
    LOOKER_EXPLORE_QUALIFIED_NAME("lookerExploreQualifiedName.text"),
    /** TBC */
    LOOKER_VIEW_QUALIFIED_NAME("lookerViewQualifiedName.text"),
    /** TBC */
    MC_MONITOR_ALERT_CONDITION("mcMonitorAlertCondition"),
    /** Monitor namespace */
    MC_MONITOR_NAMESPACE("mcMonitorNamespace"),
    /** TBC */
    MC_MONITOR_RULE_SCHEDULE_CONFIG_HUMANIZED("mcMonitorRuleScheduleConfigHumanized"),
    /** All terms attached to an asset, as a single comma-separated string. */
    MEANINGS_TEXT("__meaningsText"),
    /** TBC */
    METABASE_COLLECTION_NAME("metabaseCollectionName"),
    /** TBC */
    METABASE_COLLECTION_QUALIFIED_NAME("metabaseCollectionQualifiedName.text"),
    /** TBC */
    METABASE_NAMESPACE("metabaseNamespace.text"),
    /** TBC */
    METABASE_QUERY("metabaseQuery"),
    /** TBC */
    METABASE_QUERY_TYPE("metabaseQueryType.text"),
    /** TBC */
    METABASE_SLUG("metabaseSlug.text"),
    /** TBC */
    METRIC_FILTERS("metricFilters"),
    /** TBC */
    METRIC_TIME_GRAINS("metricTimeGrains"),
    /** Related attribute name list */
    MICRO_STRATEGY_ATTRIBUTE_NAMES("microStrategyAttributeNames"),
    /** Related attribute qualified name list */
    MICRO_STRATEGY_ATTRIBUTE_QUALIFIED_NAMES("microStrategyAttributeQualifiedNames.text"),
    /** Related cube name list */
    MICRO_STRATEGY_CUBE_NAMES("microStrategyCubeNames"),
    /** Related cube qualified name list */
    MICRO_STRATEGY_CUBE_QUALIFIED_NAMES("microStrategyCubeQualifiedNames.text"),
    /** Parent dossier name */
    MICRO_STRATEGY_DOSSIER_NAME("microStrategyDossierName"),
    /** Parent dossier qualified name */
    MICRO_STRATEGY_DOSSIER_QUALIFIED_NAME("microStrategyDossierQualifiedName.text"),
    /** Related fact name list */
    MICRO_STRATEGY_FACT_NAMES("microStrategyFactNames"),
    /** Related fact qualified name list */
    MICRO_STRATEGY_FACT_QUALIFIED_NAMES("microStrategyFactQualifiedNames.text"),
    /** Related parent metric name list */
    MICRO_STRATEGY_METRIC_PARENT_NAMES("microStrategyMetricParentNames"),
    /** Related parent metric qualified name list */
    MICRO_STRATEGY_METRIC_PARENT_QUALIFIED_NAMES("microStrategyMetricParentQualifiedNames.text"),
    /** Related project name */
    MICRO_STRATEGY_PROJECT_NAME("microStrategyProjectName"),
    /** Related project qualified name */
    MICRO_STRATEGY_PROJECT_QUALIFIED_NAME("microStrategyProjectQualifiedName.text"),
    /** Related report name list */
    MICRO_STRATEGY_REPORT_NAMES("microStrategyReportNames"),
    /** Related report qualified name list */
    MICRO_STRATEGY_REPORT_QUALIFIED_NAMES("microStrategyReportQualifiedNames.text"),
    /** TBC */
    MODE_QUERY_NAME("modeQueryName"),
    /** TBC */
    MODE_QUERY_PREVIEW("modeQueryPreview"),
    /** TBC */
    MODE_QUERY_QUALIFIED_NAME("modeQueryQualifiedName.text"),
    /** TBC */
    MODE_RAW_QUERY("modeRawQuery"),
    /** TBC */
    MODE_REPORT_NAME("modeReportName"),
    /** TBC */
    MODE_REPORT_QUALIFIED_NAME("modeReportQualifiedName.text"),
    /** TBC */
    MODE_TOKEN("modeToken.text"),
    /** TBC */
    MODE_WORKSPACE_NAME("modeWorkspaceName"),
    /** TBC */
    MODE_WORKSPACE_QUALIFIED_NAME("modeWorkspaceQualifiedName.text"),
    /** TBC */
    MODE_WORKSPACE_USERNAME("modeWorkspaceUsername.text"),
    /** TBC */
    NAME("name"),
    /** TBC */
    PARENT_COLUMN_NAME("parentColumnName"),
    /** TBC */
    PARENT_COLUMN_QUALIFIED_NAME("parentColumnQualifiedName.text"),
    /** TBC */
    PARENT_QUALIFIED_NAME("parentQualifiedName.text"),
    /** TBC */
    POWER_BI_MEASURE_EXPRESSION("powerBIMeasureExpression"),
    /** TBC */
    POWER_BI_TABLE_QUALIFIED_NAME("powerBITableQualifiedName.text"),
    /** TBC */
    PRESET_CHART_DESCRIPTION_MARKDOWN("presetChartDescriptionMarkdown"),
    /** TBC */
    PRESET_DASHBOARD_CHANGED_BY_NAME("presetDashboardChangedByName"),
    /** TBC */
    PRESET_DASHBOARD_QUALIFIED_NAME("presetDashboardQualifiedName.text"),
    /** TBC */
    PRESET_DATASET_DATASOURCE_NAME("presetDatasetDatasourceName"),
    /** TBC */
    PRESET_WORKSPACE_HOSTNAME("presetWorkspaceHostname.text"),
    /** TBC */
    PRESET_WORKSPACE_QUALIFIED_NAME("presetWorkspaceQualifiedName.text"),
    /** TBC */
    PRESET_WORKSPACE_REGION("presetWorkspaceRegion.text"),
    /** qualifiedName of an app where the qlik object belongs to */
    QLIK_APP_QUALIFIED_NAME("qlikAppQualifiedName.text"),
    /** Footnote of a qlik chart */
    QLIK_CHART_FOOTNOTE("qlikChartFootnote"),
    /** Subtitle of a qlik chart */
    QLIK_CHART_SUBTITLE("qlikChartSubtitle"),
    /** Technical name of a qlik data asset */
    QLIK_DATASET_TECHNICAL_NAME("qlikDatasetTechnicalName"),
    /** URI of a qlik dataset */
    QLIK_DATASET_URI("qlikDatasetUri.text"),
    /** QRI of the qlik object, kind of like qualifiedName on Atlan */
    QLIK_QRI("qlikQRI.text"),
    /** qualifiedName of a space where the qlik object belongs to */
    QLIK_SPACE_QUALIFIED_NAME("qlikSpaceQualifiedName.text"),
    /** Unique fully-qualified name of the asset in Atlan. */
    QUALIFIED_NAME("qualifiedName.text"),
    /** TBC */
    QUICK_SIGHT_ANALYSIS_QUALIFIED_NAME("quickSightAnalysisQualifiedName.text"),
    /** TBC */
    QUICK_SIGHT_DASHBOARD_QUALIFIED_NAME("quickSightDashboardQualifiedName.text"),
    /** TBC */
    QUICK_SIGHT_DATASET_QUALIFIED_NAME("quickSightDatasetQualifiedName.text"),
    /** TBC */
    QUICK_SIGHT_SHEET_NAME("quickSightSheetName"),
    /** Redash Query from which visualization is created */
    REDASH_QUERY_NAME("redashQueryName"),
    /** Qualified name of the Redash Query from which visualization is created */
    REDASH_QUERY_QUALIFIED_NAME("redashQueryQualifiedName.text"),
    /** Query schedule for overview tab and filtering. */
    REDASH_QUERY_SCHEDULE_HUMANIZED("redashQueryScheduleHumanized.text"),
    /** TBC */
    S3BUCKET_NAME("s3BucketName.text"),
    /** TBC */
    S3E_TAG("s3ETag.text"),
    /** TBC */
    S3OBJECT_KEY("s3ObjectKey.text"),
    /** TBC */
    SAMPLE_DATA_URL("sampleDataUrl.text"),
    /** TBC */
    SCHEMA_NAME("schemaName"),
    /** TBC */
    SIGMA_DATASET_NAME("sigmaDatasetName"),
    /** TBC */
    SIGMA_DATASET_QUALIFIED_NAME("sigmaDatasetQualifiedName.text"),
    /** TBC */
    SIGMA_DATA_ELEMENT_FIELD_FORMULA("sigmaDataElementFieldFormula"),
    /** TBC */
    SIGMA_DATA_ELEMENT_NAME("sigmaDataElementName"),
    /** TBC */
    SIGMA_DATA_ELEMENT_QUALIFIED_NAME("sigmaDataElementQualifiedName.text"),
    /** TBC */
    SIGMA_PAGE_NAME("sigmaPageName"),
    /** TBC */
    SIGMA_PAGE_QUALIFIED_NAME("sigmaPageQualifiedName.text"),
    /** TBC */
    SIGMA_WORKBOOK_NAME("sigmaWorkbookName"),
    /** TBC */
    SIGMA_WORKBOOK_QUALIFIED_NAME("sigmaWorkbookQualifiedName.text"),
    /** TBC */
    SNOWFLAKE_PIPE_NOTIFICATION_CHANNEL_NAME("snowflakePipeNotificationChannelName.text"),
    /** All super types of an asset. */
    SUPER_TYPE_NAMES("__superTypeNames"),
    /** TBC */
    TABLEAU_DATASOURCE_FIELD_DATA_TYPE("tableauDatasourceFieldDataType.text"),
    /** TBC */
    TABLEAU_DATA_TYPE("tableauDataType.text"),
    /** TBC */
    TABLE_NAME("tableName"),
    /** Allowed values for the tag at source. De-normalised from sourceTagAttributed for ease of querying */
    TAG_ALLOWED_VALUES("tagAllowedValues.text"),
    /** TBC */
    THOUGHTSPOT_LIVEBOARD_NAME("thoughtspotLiveboardName"),
    /** TBC */
    THOUGHTSPOT_LIVEBOARD_QUALIFIED_NAME("thoughtspotLiveboardQualifiedName.text"),
    /** TBC */
    THOUGHTSPOT_QUESTION_TEXT("thoughtspotQuestionText"),
    /** Type of the asset. For example Table, Column, and so on. */
    TYPE_NAME("__typeName"),
    /** TBC */
    USER_DESCRIPTION("userDescription"),
    /** TBC */
    VIEW_NAME("viewName"),
    ;

    @Getter(onMethod_ = {@Override})
    private final String indexedFieldName;

    TextFields(String indexedFieldName) {
        this.indexedFieldName = indexedFieldName;
    }
}
