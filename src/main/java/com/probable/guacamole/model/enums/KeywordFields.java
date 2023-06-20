/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.probable.guacamole.model.enums;

import com.atlan.model.enums.AtlanSearchableField;
import javax.annotation.processing.Generated;
import lombok.Getter;

@Generated(value = "com.probable.guacamole.generators.POJOGenerator")
public enum KeywordFields implements AtlanSearchableField {
    /** TBC */
    ABBREVIATION("abbreviation"),
    /** TBC */
    ADDITIONAL_ATTRIBUTES("additionalAttributes"),
    /** TBC */
    ADDITIONAL_INFO("additionalInfo"),
    /** TBC */
    ADLS_ACCOUNT_ACCESS_TIER("adlsAccountAccessTier"),
    /** TBC */
    ADLS_ACCOUNT_KIND("adlsAccountKind"),
    /** TBC */
    ADLS_ACCOUNT_PERFORMANCE("adlsAccountPerformance"),
    /** TBC */
    ADLS_ACCOUNT_PROVISION_STATE("adlsAccountProvisionState"),
    /** TBC */
    ADLS_ACCOUNT_QUALIFIED_NAME("adlsAccountQualifiedName"),
    /** TBC */
    ADLS_ACCOUNT_REPLICATION("adlsAccountReplication"),
    /** TBC */
    ADLS_ACCOUNT_RESOURCE_GROUP("adlsAccountResourceGroup.keyword"),
    /** TBC */
    ADLS_ACCOUNT_SECONDARY_LOCATION("adlsAccountSecondaryLocation"),
    /** TBC */
    ADLS_ACCOUNT_SUBSCRIPTION("adlsAccountSubscription.keyword"),
    /** TBC */
    ADLS_CONTAINER_ENCRYPTION_SCOPE("adlsContainerEncryptionScope"),
    /** TBC */
    ADLS_CONTAINER_LEASE_STATE("adlsContainerLeaseState"),
    /** TBC */
    ADLS_CONTAINER_LEASE_STATUS("adlsContainerLeaseStatus"),
    /** TBC */
    ADLS_CONTAINER_QUALIFIED_NAME("adlsContainerQualifiedName"),
    /** TBC */
    ADLS_CONTAINER_URL("adlsContainerUrl.keyword"),
    /** TBC */
    ADLS_ENCRYPTION_TYPE("adlsEncryptionType"),
    /** TBC */
    ADLS_E_TAG("adlsETag"),
    /** TBC */
    ADLS_OBJECT_ACCESS_TIER("adlsObjectAccessTier"),
    /** TBC */
    ADLS_OBJECT_ARCHIVE_STATUS("adlsObjectArchiveStatus"),
    /** TBC */
    ADLS_OBJECT_CONTENT_LANGUAGE("adlsObjectContentLanguage.keyword"),
    /** TBC */
    ADLS_OBJECT_CONTENT_MD5HASH("adlsObjectContentMD5Hash"),
    /** TBC */
    ADLS_OBJECT_LEASE_STATE("adlsObjectLeaseState"),
    /** TBC */
    ADLS_OBJECT_LEASE_STATUS("adlsObjectLeaseStatus"),
    /** TBC */
    ADLS_OBJECT_METADATA("adlsObjectMetadata"),
    /** TBC */
    ADLS_OBJECT_TYPE("adlsObjectType"),
    /** TBC */
    ADLS_OBJECT_URL("adlsObjectUrl.keyword"),
    /** TBC */
    ADLS_OBJECT_VERSION_ID("adlsObjectVersionId"),
    /** TBC */
    ADLS_PRIMARY_DISK_STATE("adlsPrimaryDiskState"),
    /** TBC */
    ADMIN_GROUPS("adminGroups"),
    /** TBC */
    ADMIN_ROLES("adminRoles"),
    /** TBC */
    ADMIN_USERS("adminUsers"),
    /** TBC */
    ALIAS("alias"),
    /** TBC */
    ANNOUNCEMENT_MESSAGE("announcementMessage"),
    /** TBC */
    ANNOUNCEMENT_TITLE("announcementTitle"),
    /** TBC */
    ANNOUNCEMENT_TYPE("announcementType"),
    /** TBC */
    ANNOUNCEMENT_UPDATED_BY("announcementUpdatedBy"),
    /** TBC */
    API_EXTERNAL_DOCS("apiExternalDocs"),
    /** TBC */
    API_NAME("apiName.keyword"),
    /** TBC */
    API_PATH_AVAILABLE_OPERATIONS("apiPathAvailableOperations"),
    /** TBC */
    API_PATH_AVAILABLE_RESPONSE_CODES("apiPathAvailableResponseCodes"),
    /** TBC */
    API_PATH_RAW_URI("apiPathRawURI"),
    /** TBC */
    API_SPEC_CONTACT_EMAIL("apiSpecContactEmail"),
    /** TBC */
    API_SPEC_CONTACT_NAME("apiSpecContactName.keyword"),
    /** TBC */
    API_SPEC_CONTACT_URL("apiSpecContactURL"),
    /** TBC */
    API_SPEC_CONTRACT_VERSION("apiSpecContractVersion"),
    /** TBC */
    API_SPEC_LICENSE_NAME("apiSpecLicenseName.keyword"),
    /** TBC */
    API_SPEC_LICENSE_URL("apiSpecLicenseURL"),
    /** TBC */
    API_SPEC_NAME("apiSpecName.keyword"),
    /** TBC */
    API_SPEC_QUALIFIED_NAME("apiSpecQualifiedName"),
    /** TBC */
    API_SPEC_SERVICE_ALIAS("apiSpecServiceAlias"),
    /** TBC */
    API_SPEC_TERMS_OF_SERVICE_URL("apiSpecTermsOfServiceURL"),
    /** TBC */
    API_SPEC_TYPE("apiSpecType"),
    /** TBC */
    API_SPEC_VERSION("apiSpecVersion"),
    /** TBC */
    ASSET_DBT_ACCOUNT_NAME("assetDbtAccountName.keyword"),
    /** TBC */
    ASSET_DBT_ALIAS("assetDbtAlias.keyword"),
    /** TBC */
    ASSET_DBT_ENVIRONMENT_DBT_VERSION("assetDbtEnvironmentDbtVersion"),
    /** TBC */
    ASSET_DBT_ENVIRONMENT_NAME("assetDbtEnvironmentName.keyword"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_ARTIFACT_S3PATH("assetDbtJobLastRunArtifactS3Path"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_EXECUTED_BY_THREAD_ID("assetDbtJobLastRunExecutedByThreadId"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_GIT_BRANCH("assetDbtJobLastRunGitBranch"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_GIT_SHA("assetDbtJobLastRunGitSha"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_OWNER_THREAD_ID("assetDbtJobLastRunOwnerThreadId"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_QUEUED_DURATION("assetDbtJobLastRunQueuedDuration"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_QUEUED_DURATION_HUMANIZED("assetDbtJobLastRunQueuedDurationHumanized"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_RUN_DURATION("assetDbtJobLastRunRunDuration"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_RUN_DURATION_HUMANIZED("assetDbtJobLastRunRunDurationHumanized"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_STATUS_MESSAGE("assetDbtJobLastRunStatusMessage.keyword"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_TOTAL_DURATION("assetDbtJobLastRunTotalDuration"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_TOTAL_DURATION_HUMANIZED("assetDbtJobLastRunTotalDurationHumanized"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_URL("assetDbtJobLastRunUrl"),
    /** TBC */
    ASSET_DBT_JOB_NAME("assetDbtJobName.keyword"),
    /** TBC */
    ASSET_DBT_JOB_NEXT_RUN_HUMANIZED("assetDbtJobNextRunHumanized.keyword"),
    /** TBC */
    ASSET_DBT_JOB_SCHEDULE("assetDbtJobSchedule"),
    /** TBC */
    ASSET_DBT_JOB_STATUS("assetDbtJobStatus"),
    /** TBC */
    ASSET_DBT_META("assetDbtMeta"),
    /** TBC */
    ASSET_DBT_PACKAGE_NAME("assetDbtPackageName.keyword"),
    /** TBC */
    ASSET_DBT_PROJECT_NAME("assetDbtProjectName.keyword"),
    /** TBC */
    ASSET_DBT_SEMANTIC_LAYER_PROXY_URL("assetDbtSemanticLayerProxyUrl"),
    /** TBC */
    ASSET_DBT_SOURCE_FRESHNESS_CRITERIA("assetDbtSourceFreshnessCriteria"),
    /** TBC */
    ASSET_DBT_TAGS("assetDbtTags"),
    /** TBC */
    ASSET_DBT_UNIQUE_ID("assetDbtUniqueId.keyword"),
    /** TBC */
    ASSET_MC_INCIDENT_NAMES("assetMcIncidentNames.keyword"),
    /** TBC */
    ASSET_MC_INCIDENT_QUALIFIED_NAMES("assetMcIncidentQualifiedNames"),
    /** TBC */
    ASSET_MC_INCIDENT_SEVERITIES("assetMcIncidentSeverities"),
    /** TBC */
    ASSET_MC_INCIDENT_STATES("assetMcIncidentStates"),
    /** TBC */
    ASSET_MC_INCIDENT_SUB_TYPES("assetMcIncidentSubTypes"),
    /** TBC */
    ASSET_MC_INCIDENT_TYPES("assetMcIncidentTypes"),
    /** TBC */
    ASSET_MC_MONITOR_NAMES("assetMcMonitorNames.keyword"),
    /** TBC */
    ASSET_MC_MONITOR_QUALIFIED_NAMES("assetMcMonitorQualifiedNames"),
    /** MonteCarlo Monitor schedule type */
    ASSET_MC_MONITOR_SCHEDULE_TYPES("assetMcMonitorScheduleTypes"),
    /** All associated monitors statuses */
    ASSET_MC_MONITOR_STATUSES("assetMcMonitorStatuses"),
    /** All associated monitor types */
    ASSET_MC_MONITOR_TYPES("assetMcMonitorTypes"),
    /** TBC */
    ASSET_TAGS("assetTags"),
    /** All terms attached to an asset, searchable by the term's qualifiedName. */
    ASSIGNED_TERMS("__meanings"),
    /** TBC */
    AST("ast"),
    /** TBC */
    ATLAS_SERVER_DISPLAY_NAME("AtlasServer.displayName"),
    /** TBC */
    ATLAS_SERVER_NAME("AtlasServer.name"),
    /** TBC */
    ATLAS_USER_PROFILE_NAME("__AtlasUserProfile.name"),
    /** TBC */
    ATLAS_USER_SAVED_SEARCH_NAME("__AtlasUserSavedSearch.name"),
    /** TBC */
    AUTH_SERVICE_CONFIG("authServiceConfig"),
    /** TBC */
    AUTH_SERVICE_TYPE("authServiceType"),
    /** TBC */
    AWS_ACCOUNT_ID("awsAccountId"),
    /** TBC */
    AWS_ARN("awsArn"),
    /** TBC */
    AWS_OWNER_ID("awsOwnerId"),
    /** TBC */
    AWS_OWNER_NAME("awsOwnerName"),
    /** TBC */
    AWS_PARTITION("awsPartition"),
    /** TBC */
    AWS_REGION("awsRegion"),
    /** TBC */
    AWS_RESOURCE_ID("awsResourceId"),
    /** TBC */
    AWS_SERVICE("awsService"),
    /** TBC */
    AWS_TAGS("awsTags"),
    /** TBC */
    AZURE_LOCATION("azureLocation"),
    /** TBC */
    AZURE_RESOURCE_ID("azureResourceId"),
    /** TBC */
    AZURE_TAGS("azureTags"),
    /** TBC */
    BADGE_CONDITIONS("badgeConditions"),
    /** TBC */
    BADGE_METADATA_ATTRIBUTE("badgeMetadataAttribute"),
    /** WAREHOUSE, RDBMS, LAKE, BI */
    CATEGORY("category"),
    /** TBC */
    CERTIFICATE_STATUS("certificateStatus"),
    /** TBC */
    CERTIFICATE_STATUS_MESSAGE("certificateStatusMessage"),
    /** TBC */
    CERTIFICATE_UPDATED_BY("certificateUpdatedBy"),
    /** TBC */
    CERTIFICATION_NOTE("certificationNote"),
    /** TBC */
    CERTIFIER("certifier"),
    /** TBC */
    CERTIFIER_DISPLAY_NAME("certifierDisplayName"),
    /** TBC */
    CHANNEL_LINK("channelLink"),
    /** TBC */
    CLIENT_ID("clientId"),
    /** TBC */
    CODE("code"),
    /** TBC */
    COLLECTION_QUALIFIED_NAME("collectionQualifiedName"),
    /** TBC */
    COLUMN_HISTOGRAM("columnHistogram"),
    /** TBC */
    COLUMN_MAXS("columnMaxs"),
    /** TBC */
    COLUMN_MINS("columnMins"),
    /** TBC */
    COLUMN_TOP_VALUES("columnTopValues"),
    /** TBC */
    CONNECTION_DBT_ENVIRONMENTS("connectionDbtEnvironments"),
    /** TBC */
    CONNECTION_DETAILS("connectionDetails"),
    /** TBC */
    CONNECTION_NAME("connectionName"),
    /** TBC */
    CONNECTION_QUALIFIED_NAME("connectionQualifiedName"),
    /** TBC */
    CONNECTION_SSO_CREDENTIAL_GUID("connectionSSOCredentialGuid"),
    /** TBC */
    CONNECTOR_ICON("connectorIcon"),
    /** TBC */
    CONNECTOR_IMAGE("connectorImage"),
    /** TBC */
    CONNECTOR_TYPE("connectorName"),
    /** TBC */
    CONSTRAINT("constraint"),
    /** Atlan user who created this asset. */
    CREATED_BY("__createdBy"),
    /** TBC */
    CREDENTIAL_STRATEGY("credentialStrategy"),
    /** TBC */
    DASHBOARD_QUALIFIED_NAME("dashboardQualifiedName"),
    /** dashboardType is the type of dashboard in salesforce */
    DASHBOARD_TYPE("dashboardType"),
    /** TBC */
    DATABASE_NAME("databaseName.keyword"),
    /** TBC */
    DATABASE_QUALIFIED_NAME("databaseQualifiedName"),
    /** TBC */
    DATASET_QUALIFIED_NAME("datasetQualifiedName"),
    /** TBC */
    DATASOURCE_FIELD_TYPE("datasourceFieldType"),
    /** TBC */
    DATASOURCE_QUALIFIED_NAME("datasourceQualifiedName"),
    /** TBC */
    DATA_CATEGORY("dataCategory"),
    /** TBC */
    DATA_STUDIO_ASSET_OWNER("dataStudioAssetOwner"),
    /** TBC */
    DATA_STUDIO_ASSET_TITLE("dataStudioAssetTitle.keyword"),
    /** TBC */
    DATA_STUDIO_ASSET_TYPE("dataStudioAssetType"),
    /** data type of the field */
    DATA_TYPE("dataType"),
    /** TBC */
    DBT_ACCOUNT_NAME("dbtAccountName.keyword"),
    /** TBC */
    DBT_ALIAS("dbtAlias.keyword"),
    /** TBC */
    DBT_COLUMN_PROCESS_JOB_STATUS("dbtColumnProcessJobStatus"),
    /** TBC */
    DBT_COMPILED_SQL("dbtCompiledSQL"),
    /** TBC */
    DBT_CONNECTION_CONTEXT("dbtConnectionContext"),
    /** TBC */
    DBT_ENVIRONMENT_DBT_VERSION("dbtEnvironmentDbtVersion.keyword"),
    /** TBC */
    DBT_ENVIRONMENT_NAME("dbtEnvironmentName.keyword"),
    /** TBC */
    DBT_ERROR("dbtError"),
    /** TBC */
    DBT_FRESHNESS_CRITERIA("dbtFreshnessCriteria"),
    /** TBC */
    DBT_JOB_NAME("dbtJobName.keyword"),
    /** TBC */
    DBT_JOB_NEXT_RUN_HUMANIZED("dbtJobNextRunHumanized.keyword"),
    /** TBC */
    DBT_JOB_SCHEDULE("dbtJobSchedule"),
    /** TBC */
    DBT_JOB_SCHEDULE_CRON_HUMANIZED("dbtJobScheduleCronHumanized.keyword"),
    /** TBC */
    DBT_JOB_STATUS("dbtJobStatus"),
    /** TBC */
    DBT_MATERIALIZATION_TYPE("dbtMaterializationType"),
    /** TBC */
    DBT_META("dbtMeta"),
    /** TBC */
    DBT_METRIC_FILTERS("dbtMetricFilters"),
    /** TBC */
    DBT_MODEL_COLUMN_DATA_TYPE("dbtModelColumnDataType"),
    /** TBC */
    DBT_MODEL_QUALIFIED_NAME("dbtModelQualifiedName"),
    /** TBC */
    DBT_PACKAGE_NAME("dbtPackageName.keyword"),
    /** TBC */
    DBT_PROCESS_JOB_STATUS("dbtProcessJobStatus"),
    /** TBC */
    DBT_PROJECT_NAME("dbtProjectName.keyword"),
    /** TBC */
    DBT_QUALIFIED_NAME("dbtQualifiedName"),
    /** TBC */
    DBT_RAW_SQL("dbtRawSQL"),
    /** TBC */
    DBT_SEMANTIC_LAYER_PROXY_URL("dbtSemanticLayerProxyUrl"),
    /** TBC */
    DBT_STATE("dbtState"),
    /** TBC */
    DBT_STATS("dbtStats"),
    /** TBC */
    DBT_STATUS("dbtStatus"),
    /** TBC */
    DBT_TAGS("dbtTags"),
    /** TBC */
    DBT_UNIQUE_ID("dbtUniqueId.keyword"),
    /** TBC */
    DEFAULT_CREDENTIAL_GUID("defaultCredentialGuid"),
    /** TBC */
    DEFAULT_DATABASE_QUALIFIED_NAME("defaultDatabaseQualifiedName"),
    /** TBC */
    DEFAULT_SCHEMA_QUALIFIED_NAME("defaultSchemaQualifiedName"),
    /** TBC */
    DEFAULT_VALUE("defaultValue"),
    /** TBC */
    DEFAULT_VALUE_FORMULA("defaultValueFormula"),
    /** TBC */
    DEFINITION("definition"),
    /** TBC */
    DENY_ASSET_TABS("denyAssetTabs"),
    /** TBC */
    DENY_CUSTOM_METADATA_GUIDS("denyCustomMetadataGuids"),
    /** TBC */
    DESCRIPTION("description.keyword"),
    /** detailColumns is a list of column names on the report */
    DETAIL_COLUMNS("detailColumns"),
    /** TBC */
    DISPLAY_NAME("displayName.keyword"),
    /** TBC */
    EXAMPLES("examples"),
    /** TBC */
    EXTERNAL_LOCATION("externalLocation"),
    /** TBC */
    EXTERNAL_LOCATION_FORMAT("externalLocationFormat"),
    /** TBC */
    EXTERNAL_LOCATION_REGION("externalLocationRegion"),
    /** TBC */
    FIELDS("fields"),
    /** TBC */
    FILE_PATH("filePath"),
    /** TBC */
    FILE_TYPE("fileType"),
    /** TBC */
    FOLDER_NAME("folderName"),
    /** TBC */
    FORMULA("formula"),
    /** TBC */
    FULLY_QUALIFIED_NAME("fullyQualifiedName"),
    /** TBC */
    FULL_NAME("fullName"),
    /** TBC */
    GCS_ACCESS_CONTROL("gcsAccessControl"),
    /** TBC */
    GCS_BUCKET_NAME("gcsBucketName.keyword"),
    /** TBC */
    GCS_BUCKET_QUALIFIED_NAME("gcsBucketQualifiedName"),
    /** TBC */
    GCS_ENCRYPTION_TYPE("gcsEncryptionType"),
    /** TBC */
    GCS_E_TAG("gcsETag"),
    /** TBC */
    GCS_OBJECT_CONTENT_DISPOSITION("gcsObjectContentDisposition"),
    /** TBC */
    GCS_OBJECT_CONTENT_ENCODING("gcsObjectContentEncoding"),
    /** TBC */
    GCS_OBJECT_CONTENT_LANGUAGE("gcsObjectContentLanguage"),
    /** TBC */
    GCS_OBJECT_CONTENT_TYPE("gcsObjectContentType"),
    /** TBC */
    GCS_OBJECT_CRC32C_HASH("gcsObjectCRC32CHash"),
    /** TBC */
    GCS_OBJECT_HOLD_TYPE("gcsObjectHoldType"),
    /** TBC */
    GCS_OBJECT_KEY("gcsObjectKey"),
    /** TBC */
    GCS_OBJECT_MD5HASH("gcsObjectMD5Hash"),
    /** TBC */
    GCS_OBJECT_MEDIA_LINK("gcsObjectMediaLink"),
    /** TBC */
    GCS_STORAGE_CLASS("gcsStorageClass"),
    /** Glossary in which the asset is contained, searchable by the qualifiedName of the glossary. */
    GLOSSARY("__glossary"),
    /** TBC */
    GOOGLE_LABELS("googleLabels"),
    /** TBC */
    GOOGLE_LOCATION("googleLocation"),
    /** TBC */
    GOOGLE_LOCATION_TYPE("googleLocationType"),
    /** TBC */
    GOOGLE_PROJECT_ID("googleProjectId"),
    /** TBC */
    GOOGLE_PROJECT_NAME("googleProjectName"),
    /** TBC */
    GOOGLE_SERVICE("googleService"),
    /** TBC */
    GOOGLE_TAGS("googleTags"),
    /** Rough measure of the IOPS allocated to the table's processing. */
    GUACAMOLE_TEMPERATURE("guacamoleTemperature"),
    /** Globally unique identifier (GUID) of any object in Atlan. */
    GUID("__guid"),
    /** TBC */
    HOST("host"),
    /** TBC */
    ICON("icon"),
    /** TBC */
    ICON_TYPE("iconType"),
    /** TBC */
    INPUTS("inputs"),
    /** TBC */
    INPUT_FIELDS("inputFields"),
    /** TBC */
    KAFKA_CONSUMER_GROUP_TOPIC_CONSUMPTION_PROPERTIES("kafkaConsumerGroupTopicConsumptionProperties"),
    /** TBC */
    KAFKA_TOPIC_CLEANUP_POLICY("kafkaTopicCleanupPolicy"),
    /** TBC */
    KAFKA_TOPIC_COMPRESSION_TYPE("kafkaTopicCompressionType"),
    /** TBC */
    KAFKA_TOPIC_NAMES("kafkaTopicNames"),
    /** TBC */
    KAFKA_TOPIC_QUALIFIED_NAMES("kafkaTopicQualifiedNames"),
    /** TBC */
    LANGUAGE("language"),
    /** TBC */
    LAST_SYNC_RUN("lastSyncRun"),
    /** TBC */
    LAST_SYNC_WORKFLOW_NAME("lastSyncWorkflowName"),
    /** TBC */
    LINK("link"),
    /** TBC */
    LONG_DESCRIPTION("longDescription"),
    /** TBC */
    LOOKER_EXPLORE_QUALIFIED_NAME("lookerExploreQualifiedName"),
    /** TBC */
    LOOKER_FIELD_DATA_TYPE("lookerFieldDataType"),
    /** TBC */
    LOOKER_VIEW_QUALIFIED_NAME("lookerViewQualifiedName"),
    /** TBC */
    LOOKML_LINK_ID("lookmlLinkId"),
    /** Mapped atlan classification name */
    MAPPED_ATLAN_TAG_NAME("mappedClassificationName"),
    /** TBC */
    MC_ASSET_QUALIFIED_NAMES("mcAssetQualifiedNames"),
    /** TBC */
    MC_INCIDENT_ID("mcIncidentId"),
    /** TBC */
    MC_INCIDENT_SEVERITY("mcIncidentSeverity"),
    /** TBC */
    MC_INCIDENT_STATE("mcIncidentState"),
    /** TBC */
    MC_INCIDENT_SUB_TYPES("mcIncidentSubTypes"),
    /** TBC */
    MC_INCIDENT_TYPE("mcIncidentType"),
    /** Incident warehouse name */
    MC_INCIDENT_WAREHOUSE("mcIncidentWarehouse"),
    /** TBC */
    MC_LABELS("mcLabels"),
    /** Monitor Id */
    MC_MONITOR_ID("mcMonitorId"),
    /** Monitor namespace */
    MC_MONITOR_NAMESPACE("mcMonitorNamespace.keyword"),
    /** TBC */
    MC_MONITOR_RULE_COMPARISONS("mcMonitorRuleComparisons"),
    /** custom sql query */
    MC_MONITOR_RULE_CUSTOM_SQL("mcMonitorRuleCustomSql"),
    /** TBC */
    MC_MONITOR_RULE_SCHEDULE_CONFIG("mcMonitorRuleScheduleConfig"),
    /** TBC */
    MC_MONITOR_RULE_TYPE("mcMonitorRuleType"),
    /** Monitor schedule type */
    MC_MONITOR_SCHEDULE_TYPE("mcMonitorScheduleType"),
    /** Monitor status */
    MC_MONITOR_STATUS("mcMonitorStatus"),
    /** Monitor type */
    MC_MONITOR_TYPE("mcMonitorType"),
    /** Monitor warehouse name */
    MC_MONITOR_WAREHOUSE("mcMonitorWarehouse"),
    /** TBC */
    MERGE_RESULT_ID("mergeResultId"),
    /** TBC */
    METABASE_COLLECTION_NAME("metabaseCollectionName.keyword"),
    /** TBC */
    METABASE_COLLECTION_QUALIFIED_NAME("metabaseCollectionQualifiedName"),
    /** TBC */
    METABASE_COLOR("metabaseColor"),
    /** TBC */
    METABASE_NAMESPACE("metabaseNamespace"),
    /** TBC */
    METABASE_QUERY("metabaseQuery.keyword"),
    /** TBC */
    METABASE_QUERY_TYPE("metabaseQueryType"),
    /** TBC */
    METABASE_SLUG("metabaseSlug"),
    /** TBC */
    METRIC_SQL("metricSQL"),
    /** TBC */
    METRIC_TYPE("metricType"),
    /** Attribute form name, description, displayFormat and expression as JSON string */
    MICRO_STRATEGY_ATTRIBUTE_FORMS("microStrategyAttributeForms"),
    /** Related attribute name list */
    MICRO_STRATEGY_ATTRIBUTE_NAMES("microStrategyAttributeNames.keyword"),
    /** Related attribute qualified name list */
    MICRO_STRATEGY_ATTRIBUTE_QUALIFIED_NAMES("microStrategyAttributeQualifiedNames"),
    /** User who certified in MicroStrategy */
    MICRO_STRATEGY_CERTIFIED_BY("microStrategyCertifiedBy"),
    /** Related cube name list */
    MICRO_STRATEGY_CUBE_NAMES("microStrategyCubeNames.keyword"),
    /** Related cube qualified name list */
    MICRO_STRATEGY_CUBE_QUALIFIED_NAMES("microStrategyCubeQualifiedNames"),
    /** The query used to create the cube */
    MICRO_STRATEGY_CUBE_QUERY("microStrategyCubeQuery"),
    /** Whether the cube is an OLAP or MTDI cube */
    MICRO_STRATEGY_CUBE_TYPE("microStrategyCubeType"),
    /** Dossier chapter name list */
    MICRO_STRATEGY_DOSSIER_CHAPTER_NAMES("microStrategyDossierChapterNames"),
    /** Parent dossier name */
    MICRO_STRATEGY_DOSSIER_NAME("microStrategyDossierName.keyword"),
    /** Parent dossier qualified name */
    MICRO_STRATEGY_DOSSIER_QUALIFIED_NAME("microStrategyDossierQualifiedName"),
    /** Fact expression list */
    MICRO_STRATEGY_FACT_EXPRESSIONS("microStrategyFactExpressions"),
    /** Related fact name list */
    MICRO_STRATEGY_FACT_NAMES("microStrategyFactNames.keyword"),
    /** Related fact qualified name list */
    MICRO_STRATEGY_FACT_QUALIFIED_NAMES("microStrategyFactQualifiedNames"),
    /** Location path in MicroStrategy */
    MICRO_STRATEGY_LOCATION("microStrategyLocation"),
    /** Metric expression text */
    MICRO_STRATEGY_METRIC_EXPRESSION("microStrategyMetricExpression"),
    /** Related parent metric name list */
    MICRO_STRATEGY_METRIC_PARENT_NAMES("microStrategyMetricParentNames.keyword"),
    /** Related parent metric qualified name list */
    MICRO_STRATEGY_METRIC_PARENT_QUALIFIED_NAMES("microStrategyMetricParentQualifiedNames"),
    /** Related project name */
    MICRO_STRATEGY_PROJECT_NAME("microStrategyProjectName.keyword"),
    /** Related project qualified name */
    MICRO_STRATEGY_PROJECT_QUALIFIED_NAME("microStrategyProjectQualifiedName"),
    /** Related report name list */
    MICRO_STRATEGY_REPORT_NAMES("microStrategyReportNames.keyword"),
    /** Related report qualified name list */
    MICRO_STRATEGY_REPORT_QUALIFIED_NAMES("microStrategyReportQualifiedNames"),
    /** Whether the report is a Grid or Chart report */
    MICRO_STRATEGY_REPORT_TYPE("microStrategyReportType"),
    /** Visualization type name */
    MICRO_STRATEGY_VISUALIZATION_TYPE("microStrategyVisualizationType"),
    /** TBC */
    MODEL_NAME("modelName"),
    /** TBC */
    MODE_CHART_TYPE("modeChartType"),
    /** TBC */
    MODE_COLLECTION_STATE("modeCollectionState"),
    /** TBC */
    MODE_COLLECTION_TOKEN("modeCollectionToken"),
    /** TBC */
    MODE_COLLECTION_TYPE("modeCollectionType"),
    /** TBC */
    MODE_ID("modeId"),
    /** TBC */
    MODE_QUERY_NAME("modeQueryName.keyword"),
    /** TBC */
    MODE_QUERY_QUALIFIED_NAME("modeQueryQualifiedName"),
    /** TBC */
    MODE_REPORT_NAME("modeReportName.keyword"),
    /** TBC */
    MODE_REPORT_QUALIFIED_NAME("modeReportQualifiedName"),
    /** TBC */
    MODE_TOKEN("modeToken"),
    /** TBC */
    MODE_WORKSPACE_NAME("modeWorkspaceName.keyword"),
    /** TBC */
    MODE_WORKSPACE_QUALIFIED_NAME("modeWorkspaceQualifiedName"),
    /** TBC */
    MODE_WORKSPACE_USERNAME("modeWorkspaceUsername"),
    /** Atlan user who last updated the asset. */
    MODIFIED_BY("__modifiedBy"),
    /** TBC */
    NAME("name.keyword"),
    /** TBC */
    NOTE_TEXT("noteText"),
    /** TBC */
    OBJECT_QUALIFIED_NAME("objectQualifiedName"),
    /** TBC */
    OPERATION("operation"),
    /** TBC */
    OPERATION_PARAMS("operationParams"),
    /** TBC */
    ORGANIZATION_QUALIFIED_NAME("organizationQualifiedName"),
    /** TBC */
    OUTPUTS("outputs"),
    /** TBC */
    OUTPUT_FIELDS("outputFields"),
    /** TBC */
    OUTPUT_STEPS("outputSteps"),
    /** TBC */
    OWNER_GROUPS("ownerGroups"),
    /** TBC */
    OWNER_NAME("ownerName"),
    /** TBC */
    OWNER_USERS("ownerUsers"),
    /** TBC */
    PARAMS("params"),
    /** TBC */
    PARENT_COLUMN_NAME("parentColumnName.keyword"),
    /** TBC */
    PARENT_COLUMN_QUALIFIED_NAME("parentColumnQualifiedName"),
    /** TBC */
    PARENT_QUALIFIED_NAME("parentQualifiedName"),
    /** TBC */
    PARTITION_LIST("partitionList"),
    /** TBC */
    PARTITION_STRATEGY("partitionStrategy"),
    /** TBC */
    PERSONA_GROUPS("personaGroups"),
    /** TBC */
    PERSONA_USERS("personaUsers"),
    /** picklistValues is a list of values from which a user can pick from while adding a record */
    PICKLIST_VALUES("picklistValues"),
    /** TBC */
    PINNED_BY("pinnedBy"),
    /** TBC */
    POLICY_ACTIONS("policyActions"),
    /** TBC */
    POLICY_CATEGORY("policyCategory"),
    /** TBC */
    POLICY_CONDITIONS("policyConditions"),
    /** TBC */
    POLICY_GROUPS("policyGroups"),
    /** TBC */
    POLICY_MASK_TYPE("policyMaskType"),
    /** TBC */
    POLICY_RESOURCES("policyResources"),
    /** TBC */
    POLICY_RESOURCE_CATEGORY("policyResourceCategory"),
    /** TBC */
    POLICY_RESOURCE_SIGNATURE("policyResourceSignature"),
    /** TBC */
    POLICY_ROLES("policyRoles"),
    /** TBC */
    POLICY_SERVICE_NAME("policyServiceName"),
    /** TBC */
    POLICY_STRATEGY("policyStrategy"),
    /** TBC */
    POLICY_SUB_CATEGORY("policySubCategory"),
    /** TBC */
    POLICY_TYPE("policyType"),
    /** TBC */
    POLICY_USERS("policyUsers"),
    /** TBC */
    POLICY_VALIDITY_SCHEDULE("policyValiditySchedule"),
    /** TBC */
    POWER_BI_COLUMN_DATA_CATEGORY("powerBIColumnDataCategory"),
    /** TBC */
    POWER_BI_COLUMN_DATA_TYPE("powerBIColumnDataType"),
    /** TBC */
    POWER_BI_COLUMN_SUMMARIZE_BY("powerBIColumnSummarizeBy"),
    /** TBC */
    POWER_BI_ENDORSEMENT("powerBIEndorsement"),
    /** TBC */
    POWER_BI_FORMAT_STRING("powerBIFormatString"),
    /** TBC */
    POWER_BI_SORT_BY_COLUMN("powerBISortByColumn"),
    /** TBC */
    POWER_BI_TABLE_QUALIFIED_NAME("powerBITableQualifiedName"),
    /** TBC */
    POWER_BI_TABLE_SOURCE_EXPRESSIONS("powerBITableSourceExpressions"),
    /** TBC */
    PRESET_CHART_FORM_DATA("presetChartFormData"),
    /** TBC */
    PRESET_DASHBOARD_CHANGED_BY_NAME("presetDashboardChangedByName.keyword"),
    /** TBC */
    PRESET_DASHBOARD_CHANGED_BY_URL("presetDashboardChangedByURL"),
    /** TBC */
    PRESET_DASHBOARD_QUALIFIED_NAME("presetDashboardQualifiedName"),
    /** TBC */
    PRESET_DASHBOARD_THUMBNAIL_URL("presetDashboardThumbnailURL"),
    /** TBC */
    PRESET_DATASET_DATASOURCE_NAME("presetDatasetDatasourceName.keyword"),
    /** TBC */
    PRESET_DATASET_TYPE("presetDatasetType"),
    /** TBC */
    PRESET_WORKSPACE_HOSTNAME("presetWorkspaceHostname"),
    /** TBC */
    PRESET_WORKSPACE_QUALIFIED_NAME("presetWorkspaceQualifiedName"),
    /** TBC */
    PRESET_WORKSPACE_REGION("presetWorkspaceRegion"),
    /** TBC */
    PRESET_WORKSPACE_STATUS("presetWorkspaceStatus"),
    /** TBC */
    PREVIEW_CREDENTIAL_STRATEGY("previewCredentialStrategy"),
    /** TBC */
    PROJECT_HIERARCHY("projectHierarchy"),
    /** TBC */
    PROJECT_NAME("projectName"),
    /** TBC */
    PROJECT_QUALIFIED_NAME("projectQualifiedName"),
    /** All propagated Atlan tags that exist on an asset, searchable by the internal hashed-string ID of the Atlan tag. */
    PROPAGATED_TRAIT_NAMES("__propagatedTraitNames"),
    /** TBC */
    PURPOSE_ATLAN_TAGS("purposeClassifications"),
    /** qID of a app where the qlik object belongs */
    QLIK_APP_ID("qlikAppId"),
    /** qualifiedName of an app where the qlik object belongs to */
    QLIK_APP_QUALIFIED_NAME("qlikAppQualifiedName"),
    /** Orientation of a qlik chart */
    QLIK_CHART_ORIENTATION("qlikChartOrientation"),
    /** Subtype of an qlik chart. E.g. bar, graph, pie etc */
    QLIK_CHART_TYPE("qlikChartType"),
    /** Subtype of an qlik dataset asset */
    QLIK_DATASET_SUBTYPE("qlikDatasetSubtype"),
    /** Technical name of a qlik data asset */
    QLIK_DATASET_TECHNICAL_NAME("qlikDatasetTechnicalName.keyword"),
    /** Type of an qlik data asset. E.g. qix-df, snowflake etc */
    QLIK_DATASET_TYPE("qlikDatasetType"),
    /** URI of a qlik dataset */
    QLIK_DATASET_URI("qlikDatasetUri"),
    /** qID/guid of the qlik object */
    QLIK_ID("qlikId"),
    /** originAppId value for a qlik app */
    QLIK_ORIGIN_APP_ID("qlikOriginAppId"),
    /** Owner's guid of the qlik object */
    QLIK_OWNER_ID("qlikOwnerId"),
    /** QRI of the qlik object, kind of like qualifiedName on Atlan */
    QLIK_QRI("qlikQRI"),
    /** qID of a space where the qlik object belongs to */
    QLIK_SPACE_ID("qlikSpaceId"),
    /** qualifiedName of a space where the qlik object belongs to */
    QLIK_SPACE_QUALIFIED_NAME("qlikSpaceQualifiedName"),
    /** Type of a qlik space. E.g. Private, Shared etc */
    QLIK_SPACE_TYPE("qlikSpaceType"),
    /** Unique fully-qualified name of the asset in Atlan. */
    QUALIFIED_NAME("qualifiedName"),
    /** TBC */
    QUERY_CONFIG("queryConfig"),
    /** TBC */
    QUERY_PREVIEW_CONFIG("queryPreviewConfig"),
    /** TBC */
    QUERY_USERNAME_STRATEGY("queryUsernameStrategy"),
    /** TBC */
    QUERY_USER_MAP("queryUserMap"),
    /** TBC */
    QUICK_SIGHT_ANALYSIS_CALCULATED_FIELDS("quickSightAnalysisCalculatedFields"),
    /** TBC */
    QUICK_SIGHT_ANALYSIS_FILTER_GROUPS("quickSightAnalysisFilterGroups"),
    /** TBC */
    QUICK_SIGHT_ANALYSIS_PARAMETER_DECLARATIONS("quickSightAnalysisParameterDeclarations"),
    /** TBC */
    QUICK_SIGHT_ANALYSIS_QUALIFIED_NAME("quickSightAnalysisQualifiedName"),
    /** TBC */
    QUICK_SIGHT_ANALYSIS_STATUS("quickSightAnalysisStatus"),
    /** TBC */
    QUICK_SIGHT_DASHBOARD_QUALIFIED_NAME("quickSightDashboardQualifiedName"),
    /** TBC */
    QUICK_SIGHT_DATASET_FIELD_TYPE("quickSightDatasetFieldType"),
    /** TBC */
    QUICK_SIGHT_DATASET_IMPORT_MODE("quickSightDatasetImportMode"),
    /** TBC */
    QUICK_SIGHT_DATASET_QUALIFIED_NAME("quickSightDatasetQualifiedName"),
    /** TBC */
    QUICK_SIGHT_FOLDER_HIERARCHY("quickSightFolderHierarchy"),
    /** TBC */
    QUICK_SIGHT_FOLDER_TYPE("quickSightFolderType"),
    /** TBC */
    QUICK_SIGHT_ID("quickSightId"),
    /** TBC */
    QUICK_SIGHT_SHEET_ID("quickSightSheetId"),
    /** TBC */
    QUICK_SIGHT_SHEET_NAME("quickSightSheetName.keyword"),
    /** TBC */
    RAW_DATA_TYPE_DEFINITION("rawDataTypeDefinition"),
    /** TBC */
    RAW_QUERY("rawQuery"),
    /** Redash Query from which visualization is created */
    REDASH_QUERY_NAME("redashQueryName.keyword"),
    /** Parameters of Redash Query */
    REDASH_QUERY_PARAMETERS("redashQueryParameters"),
    /** Qualified name of the Redash Query from which visualization is created */
    REDASH_QUERY_QUALIFIED_NAME("redashQueryQualifiedName"),
    /** Schedule of Redash Query */
    REDASH_QUERY_SCHEDULE("redashQuerySchedule"),
    /** Query schedule for overview tab and filtering. */
    REDASH_QUERY_SCHEDULE_HUMANIZED("redashQueryScheduleHumanized"),
    /** SQL code of Redash Query */
    REDASH_QUERY_SQL("redashQuerySQL"),
    /** Redash Visualization Type */
    REDASH_VISUALIZATION_TYPE("redashVisualizationType"),
    /** TBC */
    REFERENCE("reference"),
    /** TBC */
    REFRESH_METHOD("refreshMethod"),
    /** TBC */
    REFRESH_MODE("refreshMode"),
    /** TBC */
    REPLICATED_FROM("replicatedFrom"),
    /** TBC */
    REPLICATED_TO("replicatedTo"),
    /** TBC */
    REPORT_QUALIFIED_NAME("reportQualifiedName"),
    /** reportType is the type of report in salesforce */
    REPORT_TYPE("reportType"),
    /** TBC */
    RESOURCE_METADATA("resourceMetadata"),
    /** TBC */
    RESULT("result"),
    /** TBC */
    RESULT_SUMMARY("resultSummary"),
    /** TBC */
    ROLE("role"),
    /** TBC */
    ROLE_ID("roleId"),
    /** TBC */
    S3BUCKET_NAME("s3BucketName"),
    /** TBC */
    S3BUCKET_QUALIFIED_NAME("s3BucketQualifiedName"),
    /** TBC */
    S3ENCRYPTION("s3Encryption"),
    /** TBC */
    S3E_TAG("s3ETag"),
    /** TBC */
    S3OBJECT_CONTENT_DISPOSITION("s3ObjectContentDisposition"),
    /** TBC */
    S3OBJECT_CONTENT_TYPE("s3ObjectContentType"),
    /** TBC */
    S3OBJECT_KEY("s3ObjectKey"),
    /** TBC */
    S3OBJECT_STORAGE_CLASS("s3ObjectStorageClass"),
    /** TBC */
    S3OBJECT_VERSION_ID("s3ObjectVersionId"),
    /** TBC */
    SAMPLE_DATA_URL("sampleDataUrl"),
    /** TBC */
    SAVED_SEARCHES("savedSearches"),
    /** TBC */
    SCHEMA_NAME("schemaName.keyword"),
    /** TBC */
    SCHEMA_QUALIFIED_NAME("schemaQualifiedName"),
    /** TBC */
    SEARCH_PARAMETERS("searchParameters"),
    /** TBC */
    SEARCH_TYPE("searchType"),
    /** TBC */
    SHORT_DESCRIPTION("shortDescription"),
    /** TBC */
    SIGMA_DATASET_NAME("sigmaDatasetName.keyword"),
    /** TBC */
    SIGMA_DATASET_QUALIFIED_NAME("sigmaDatasetQualifiedName"),
    /** TBC */
    SIGMA_DATA_ELEMENT_NAME("sigmaDataElementName.keyword"),
    /** TBC */
    SIGMA_DATA_ELEMENT_QUALIFIED_NAME("sigmaDataElementQualifiedName"),
    /** TBC */
    SIGMA_DATA_ELEMENT_QUERY("sigmaDataElementQuery"),
    /** TBC */
    SIGMA_DATA_ELEMENT_TYPE("sigmaDataElementType"),
    /** TBC */
    SIGMA_PAGE_NAME("sigmaPageName.keyword"),
    /** TBC */
    SIGMA_PAGE_QUALIFIED_NAME("sigmaPageQualifiedName"),
    /** TBC */
    SIGMA_WORKBOOK_NAME("sigmaWorkbookName.keyword"),
    /** TBC */
    SIGMA_WORKBOOK_QUALIFIED_NAME("sigmaWorkbookQualifiedName"),
    /** TBC */
    SITE_QUALIFIED_NAME("siteQualifiedName"),
    /** TBC */
    SNOWFLAKE_PIPE_NOTIFICATION_CHANNEL_NAME("snowflakePipeNotificationChannelName"),
    /** TBC */
    SNOWFLAKE_STREAM_MODE("snowflakeStreamMode"),
    /** TBC */
    SNOWFLAKE_STREAM_SOURCE_TYPE("snowflakeStreamSourceType"),
    /** TBC */
    SNOWFLAKE_STREAM_TYPE("snowflakeStreamType"),
    /** TBC */
    SOURCE_CONNECTION_NAME("sourceConnectionName"),
    /** The unit of measure for sourceTotalCost */
    SOURCE_COST_UNIT("sourceCostUnit"),
    /** TBC */
    SOURCE_CREATED_BY("sourceCreatedBy"),
    /** TBC */
    SOURCE_DEFINITION("sourceDefinition"),
    /** TBC */
    SOURCE_DEFINITION_DATABASE("sourceDefinitionDatabase"),
    /** TBC */
    SOURCE_DEFINITION_SCHEMA("sourceDefinitionSchema"),
    /** TBC */
    SOURCE_EMBED_URL("sourceEmbedURL"),
    /** sourceId is the Id of the report entity on salesforce */
    SOURCE_ID("sourceId"),
    /** TBC */
    SOURCE_LOGO("sourceLogo"),
    /** TBC */
    SOURCE_OWNERS("sourceOwners"),
    /** List of most expensive warehouse names */
    SOURCE_QUERY_COMPUTE_COSTS("sourceQueryComputeCostList"),
    /** List of most expensive warehouses with extra insights */
    SOURCE_QUERY_COMPUTE_COST_RECORDS("sourceQueryComputeCostRecordList"),
    /** List of the most expensive queries that accessed this asset */
    SOURCE_READ_EXPENSIVE_QUERY_RECORDS("sourceReadExpensiveQueryRecordList"),
    /** List of the most popular queries that accessed this asset */
    SOURCE_READ_POPULAR_QUERY_RECORDS("sourceReadPopularQueryRecordList"),
    /** List of usernames of the most recent users who read the asset */
    SOURCE_READ_RECENT_USERS("sourceReadRecentUserList"),
    /** List of usernames with extra insights for the most recent users who read the asset */
    SOURCE_READ_RECENT_USER_RECORDS("sourceReadRecentUserRecordList"),
    /** List of the slowest queries that accessed this asset */
    SOURCE_READ_SLOW_QUERY_RECORDS("sourceReadSlowQueryRecordList"),
    /** List of usernames of the top users who read the asset the most */
    SOURCE_READ_TOP_USERS("sourceReadTopUserList"),
    /** List of usernames with extra insights for the top users who read the asset the most */
    SOURCE_READ_TOP_USER_RECORDS("sourceReadTopUserRecordList"),
    /** TBC */
    SOURCE_SERVER_NAME("sourceServerName"),
    /** TBC */
    SOURCE_UPDATED_BY("sourceUpdatedBy"),
    /** TBC */
    SOURCE_URL("sourceURL"),
    /** TBC */
    SQL("sql"),
    /** TBC */
    SQL_TABLE_NAME("sqlTableName"),
    /** TBC */
    STALENESS("staleness"),
    /** TBC */
    STARRED_BY("starredBy"),
    /** Asset status in Atlan (active vs deleted). */
    STATE("__state"),
    /** TBC */
    SUBTITLE_TEXT("subtitleText"),
    /** WAREHOUSE, RDBMS, LAKE, BI */
    SUB_CATEGORY("subCategory"),
    /** TBC */
    SUB_DATA_TYPE("subDataType"),
    /** TBC */
    SUB_TYPE("subType"),
    /** All super types of an asset. */
    SUPER_TYPE_NAMES("__superTypeNames.keyword"),
    /** TBC */
    TABLEAU_DATASOURCE_FIELD_BIN_SIZE("tableauDatasourceFieldBinSize"),
    /** TBC */
    TABLEAU_DATASOURCE_FIELD_DATA_CATEGORY("tableauDatasourceFieldDataCategory"),
    /** TBC */
    TABLEAU_DATASOURCE_FIELD_DATA_TYPE("tableauDatasourceFieldDataType"),
    /** TBC */
    TABLEAU_DATASOURCE_FIELD_FORMULA("tableauDatasourceFieldFormula"),
    /** TBC */
    TABLEAU_DATASOURCE_FIELD_ROLE("tableauDatasourceFieldRole"),
    /** TBC */
    TABLEAU_DATA_TYPE("tableauDataType"),
    /** TBC */
    TABLE_NAME("tableName.keyword"),
    /** TBC */
    TABLE_QUALIFIED_NAME("tableQualifiedName"),
    /** Allowed values for the tag at source. De-normalised from sourceTagAttributed for ease of querying */
    TAG_ALLOWED_VALUES("tagAllowedValues"),
    /** Source tag attributes */
    TAG_ATTRIBUTES("tagAttributes"),
    /** Unique source tag identifier */
    TAG_ID("tagId"),
    /** TBC */
    TAG_SERVICE("tagService"),
    /** TBC */
    TARGET_SERVER_NAME("targetServerName"),
    /** TBC */
    TENANT_ID("tenantId"),
    /** TBC */
    THOUGHTSPOT_CHART_TYPE("thoughtspotChartType"),
    /** TBC */
    THOUGHTSPOT_LIVEBOARD_NAME("thoughtspotLiveboardName.keyword"),
    /** TBC */
    THOUGHTSPOT_LIVEBOARD_QUALIFIED_NAME("thoughtspotLiveboardQualifiedName"),
    /** TBC */
    TOP_LEVEL_PROJECT_NAME("topLevelProjectName"),
    /** TBC */
    TOP_LEVEL_PROJECT_QUALIFIED_NAME("topLevelProjectQualifiedName"),
    /** All directly-assigned Atlan tags that exist on an asset, searchable by the internal hashed-string ID of the Atlan tag. */
    TRAIT_NAMES("__traitNames"),
    /** Type of the asset. For example Table, Column, and so on. */
    TYPE_NAME("__typeName.keyword"),
    /** TBC */
    UI_PARAMETERS("uiParameters"),
    /** TBC */
    UNIQUE_NAME("uniqueName"),
    /** TBC */
    UPSTREAM_COLUMNS("upstreamColumns"),
    /** TBC */
    UPSTREAM_DATASOURCES("upstreamDatasources"),
    /** TBC */
    UPSTREAM_FIELDS("upstreamFields"),
    /** TBC */
    UPSTREAM_TABLES("upstreamTables"),
    /** TBC */
    URLS("urls"),
    /** TBC */
    USAGE("usage"),
    /** TBC */
    USER_DESCRIPTION("userDescription.keyword"),
    /** TBC */
    USER_NAME("userName"),
    /** TBC */
    VALIDATIONS("validations"),
    /** TBC */
    VARIABLES_SCHEMA_BASE64("variablesSchemaBase64"),
    /** TBC */
    VIEWER_GROUPS("viewerGroups"),
    /** TBC */
    VIEWER_USERS("viewerUsers"),
    /** TBC */
    VIEW_NAME("viewName"),
    /** TBC */
    VIEW_QUALIFIED_NAME("viewQualifiedName"),
    /** TBC */
    VISUAL_BUILDER_SCHEMA_BASE64("visualBuilderSchemaBase64"),
    /** TBC */
    WEB_URL("webUrl"),
    /** TBC */
    WORKBOOK_QUALIFIED_NAME("workbookQualifiedName"),
    /** TBC */
    WORKSPACE_QUALIFIED_NAME("workspaceQualifiedName"),
    ;

    @Getter(onMethod_ = {@Override})
    private final String indexedFieldName;

    KeywordFields(String indexedFieldName) {
        this.indexedFieldName = indexedFieldName;
    }
}
