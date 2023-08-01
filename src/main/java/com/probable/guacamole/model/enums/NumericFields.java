/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.probable.guacamole.model.enums;

import com.atlan.model.enums.AtlanSearchableField;
import javax.annotation.processing.Generated;
import lombok.Getter;

@Generated(value = "com.probable.guacamole.generators.POJOGenerator")
public enum NumericFields implements AtlanSearchableField {
    /** TBC */
    ADLS_OBJECT_ACCESS_TIER_LAST_MODIFIED_TIME("adlsObjectAccessTierLastModifiedTime"),
    /** TBC */
    ADLS_OBJECT_COUNT("adlsObjectCount"),
    /** TBC */
    ADLS_OBJECT_SIZE("adlsObjectSize"),
    /** TBC */
    ANNOUNCEMENT_UPDATED_AT("announcementUpdatedAt"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN("assetDbtJobLastRun"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_CREATED_AT("assetDbtJobLastRunCreatedAt"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_DEQUED_AT("assetDbtJobLastRunDequedAt"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_STARTED_AT("assetDbtJobLastRunStartedAt"),
    /** TBC */
    ASSET_DBT_JOB_LAST_RUN_UPDATED_AT("assetDbtJobLastRunUpdatedAt"),
    /** TBC */
    ASSET_DBT_JOB_NEXT_RUN("assetDbtJobNextRun"),
    /** TBC */
    ASSET_MC_LAST_SYNC_RUN_AT("assetMcLastSyncRunAt"),
    /** TBC */
    AUTH_SERVICE_POLICY_LAST_SYNC("authServicePolicyLastSync"),
    /** TBC */
    CERTIFICATE_UPDATED_AT("certificateUpdatedAt"),
    /** TBC */
    COLUMN_AVERAGE("columnAverage"),
    /** TBC */
    COLUMN_AVERAGE_LENGTH("columnAverageLength"),
    /** TBC */
    COLUMN_COUNT("columnCount"),
    /** Level of nesting, used for STRUCT/NESTED columns */
    COLUMN_DEPTH_LEVEL("columnDepthLevel"),
    /** TBC */
    COLUMN_DISTINCT_VALUES_COUNT("columnDistinctValuesCount"),
    /** TBC */
    COLUMN_DISTINCT_VALUES_COUNT_LONG("columnDistinctValuesCountLong"),
    /** TBC */
    COLUMN_DUPLICATE_VALUES_COUNT("columnDuplicateValuesCount"),
    /** TBC */
    COLUMN_DUPLICATE_VALUES_COUNT_LONG("columnDuplicateValuesCountLong"),
    /** TBC */
    COLUMN_MAX("columnMax"),
    /** TBC */
    COLUMN_MAXIMUM_STRING_LENGTH("columnMaximumStringLength"),
    /** TBC */
    COLUMN_MEAN("columnMean"),
    /** TBC */
    COLUMN_MEDIAN("columnMedian"),
    /** TBC */
    COLUMN_MIN("columnMin"),
    /** TBC */
    COLUMN_MINIMUM_STRING_LENGTH("columnMinimumStringLength"),
    /** TBC */
    COLUMN_MISSING_VALUES_COUNT("columnMissingValuesCount"),
    /** TBC */
    COLUMN_MISSING_VALUES_COUNT_LONG("columnMissingValuesCountLong"),
    /** TBC */
    COLUMN_MISSING_VALUES_PERCENTAGE("columnMissingValuesPercentage"),
    /** TBC */
    COLUMN_STANDARD_DEVIATION("columnStandardDeviation"),
    /** TBC */
    COLUMN_SUM("columnSum"),
    /** TBC */
    COLUMN_UNIQUENESS_PERCENTAGE("columnUniquenessPercentage"),
    /** TBC */
    COLUMN_UNIQUE_VALUES_COUNT("columnUniqueValuesCount"),
    /** TBC */
    COLUMN_UNIQUE_VALUES_COUNT_LONG("columnUniqueValuesCountLong"),
    /** TBC */
    COLUMN_VARIANCE("columnVariance"),
    /** TBC */
    DASHBOARD_COUNT("dashboardCount"),
    /** TBC */
    DATAFLOW_COUNT("dataflowCount"),
    /** TBC */
    DATASET_COUNT("datasetCount"),
    /** TBC */
    DBT_JOB_LAST_RUN("dbtJobLastRun"),
    /** TBC */
    DBT_JOB_NEXT_RUN("dbtJobNextRun"),
    /** TBC */
    DBT_MODEL_COLUMN_ORDER("dbtModelColumnOrder"),
    /** TBC */
    DBT_MODEL_COMPILE_COMPLETED_AT("dbtModelCompileCompletedAt"),
    /** TBC */
    DBT_MODEL_COMPILE_STARTED_AT("dbtModelCompileStartedAt"),
    /** TBC */
    DBT_MODEL_EXECUTE_COMPLETED_AT("dbtModelExecuteCompletedAt"),
    /** TBC */
    DBT_MODEL_EXECUTE_STARTED_AT("dbtModelExecuteStartedAt"),
    /** TBC */
    DBT_MODEL_EXECUTION_TIME("dbtModelExecutionTime"),
    /** TBC */
    DBT_MODEL_RUN_ELAPSED_TIME("dbtModelRunElapsedTime"),
    /** TBC */
    DBT_MODEL_RUN_GENERATED_AT("dbtModelRunGeneratedAt"),
    /** TBC */
    END_TIME("endTime"),
    /** fieldCount is the number of fields in the object entity */
    FIELD_COUNT("fieldCount"),
    /** TBC */
    GCS_BUCKET_RETENTION_EFFECTIVE_TIME("gcsBucketRetentionEffectiveTime"),
    /** TBC */
    GCS_BUCKET_RETENTION_PERIOD("gcsBucketRetentionPeriod"),
    /** TBC */
    GCS_META_GENERATION_ID("gcsMetaGenerationId"),
    /** TBC */
    GCS_OBJECT_COUNT("gcsObjectCount"),
    /** TBC */
    GCS_OBJECT_DATA_LAST_MODIFIED_TIME("gcsObjectDataLastModifiedTime"),
    /** TBC */
    GCS_OBJECT_GENERATION_ID("gcsObjectGenerationId"),
    /** TBC */
    GCS_OBJECT_RETENTION_EXPIRATION_DATE("gcsObjectRetentionExpirationDate"),
    /** TBC */
    GCS_OBJECT_SIZE("gcsObjectSize"),
    /** TBC */
    GOOGLE_PROJECT_NUMBER("googleProjectNumber"),
    /** Time (epoch) when this column was imagined, in milliseconds. */
    GUACAMOLE_CONCEPTUALIZED("guacamoleConceptualized"),
    /** Consolidated quantification metric spanning number of columns, rows, and sparsity of population. */
    GUACAMOLE_SIZE("guacamoleSize"),
    /** Maximum size of a Guacamole column. */
    GUACAMOLE_WIDTH("guacamoleWidth"),
    /** TBC */
    KAFKA_CONSUMER_GROUP_MEMBER_COUNT("kafkaConsumerGroupMemberCount"),
    /** TBC */
    KAFKA_TOPIC_PARTITIONS_COUNT("kafkaTopicPartitionsCount"),
    /** TBC */
    KAFKA_TOPIC_RECORD_COUNT("kafkaTopicRecordCount"),
    /** TBC */
    KAFKA_TOPIC_REPLICATION_FACTOR("kafkaTopicReplicationFactor"),
    /** TBC */
    KAFKA_TOPIC_SEGMENT_BYTES("kafkaTopicSegmentBytes"),
    /** TBC */
    KAFKA_TOPIC_SIZE_IN_BYTES("kafkaTopicSizeInBytes"),
    /** TBC */
    LAST_PROFILED_AT("lastProfiledAt"),
    /** Timestamp of last operation that inserted/updated/deleted rows. Google Sheets, Mysql table etc */
    LAST_ROW_CHANGED_AT("lastRowChangedAt"),
    /** TBC */
    LAST_SYNC_RUN_AT("lastSyncRunAt"),
    /** TBC */
    LOOKER_TIMES_USED("lookerTimesUsed"),
    /** TBC */
    LOOK_ID("lookId"),
    /** TBC */
    MAX_LENGTH("maxLength"),
    /** TBC */
    MC_MONITOR_BREACH_RATE("mcMonitorBreachRate"),
    /** TBC */
    MC_MONITOR_INCIDENT_COUNT("mcMonitorIncidentCount"),
    /** TBC */
    MC_MONITOR_RULE_NEXT_EXECUTION_TIME("mcMonitorRuleNextExecutionTime"),
    /** TBC */
    MC_MONITOR_RULE_PREVIOUS_EXECUTION_TIME("mcMonitorRulePreviousExecutionTime"),
    /** TBC */
    METABASE_DASHBOARD_COUNT("metabaseDashboardCount"),
    /** TBC */
    METABASE_QUESTION_COUNT("metabaseQuestionCount"),
    /** Certified date in MicroStrategy */
    MICRO_STRATEGY_CERTIFIED_AT("microStrategyCertifiedAt"),
    /** TBC */
    MODE_CHART_COUNT("modeChartCount"),
    /** TBC */
    MODE_COLLECTION_COUNT("modeCollectionCount"),
    /** TBC */
    MODE_QUERY_COUNT("modeQueryCount"),
    /** TBC */
    MODE_REPORT_IMPORT_COUNT("modeReportImportCount"),
    /** TBC */
    MODE_REPORT_PUBLISHED_AT("modeReportPublishedAt"),
    /** Time (in milliseconds) when the asset was last updated. */
    MODIFICATION_TIMESTAMP("__modificationTimestamp"),
    /** TBC */
    NESTED_COLUMN_COUNT("nestedColumnCount"),
    /** TBC */
    NUMERIC_SCALE("numericScale"),
    /** TBC */
    OPERATION_END_TIME("operationEndTime"),
    /** TBC */
    OPERATION_START_TIME("operationStartTime"),
    /** TBC */
    ORDER("order"),
    /** TBC */
    PAGE_COUNT("pageCount"),
    /** TBC */
    PARTITION_COUNT("partitionCount"),
    /** TBC */
    PARTITION_ORDER("partitionOrder"),
    /** TBC */
    PINNED_AT("pinnedAt"),
    /** TBC */
    POLICY_PRIORITY("policyPriority"),
    /** Number of days we are calculating popularity for, eg: 30 days */
    POPULARITY_INSIGHTS_TIMEFRAME("popularityInsightsTimeframe"),
    /** TBC */
    POPULARITY_SCORE("popularityScore"),
    /** TBC */
    PORT("port"),
    /** TBC */
    POWER_BI_TABLE_COLUMN_COUNT("powerBITableColumnCount"),
    /** TBC */
    POWER_BI_TABLE_MEASURE_COUNT("powerBITableMeasureCount"),
    /** Total number of digits allowed */
    PRECISION("precision"),
    /** TBC */
    PRESET_DASHBOARD_CHART_COUNT("presetDashboardChartCount"),
    /** TBC */
    PRESET_DASHBOARD_ID("presetDashboardId"),
    /** TBC */
    PRESET_DATASET_ID("presetDatasetId"),
    /** TBC */
    PRESET_WORKSPACE_CLUSTER_ID("presetWorkspaceClusterId"),
    /** TBC */
    PRESET_WORKSPACE_DASHBOARD_COUNT("presetWorkspaceDashboardCount"),
    /** TBC */
    PRESET_WORKSPACE_DATASET_COUNT("presetWorkspaceDatasetCount"),
    /** TBC */
    PRESET_WORKSPACE_DEPLOYMENT_ID("presetWorkspaceDeploymentId"),
    /** TBC */
    PRESET_WORKSPACE_ID("presetWorkspaceId"),
    /** Static space taken by a qlik app */
    QLIK_APP_STATIC_BYTE_SIZE("qlikAppStaticByteSize"),
    /** TBC */
    QUERY_COUNT("queryCount"),
    /** TBC */
    QUERY_COUNT_UPDATED_AT("queryCountUpdatedAt"),
    /** TBC */
    QUERY_ID("queryID"),
    /** TBC */
    QUERY_TIMEOUT("queryTimeout"),
    /** TBC */
    QUERY_USER_COUNT("queryUserCount"),
    /** Last published time of dashboard */
    QUICK_SIGHT_DASHBOARD_LAST_PUBLISHED_TIME("quickSightDashboardLastPublishedTime"),
    /** Version number of the dashboard published */
    QUICK_SIGHT_DASHBOARD_PUBLISHED_VERSION_NUMBER("quickSightDashboardPublishedVersionNumber"),
    /** Quicksight dataset column count indicates number of columns present in the dataset */
    QUICK_SIGHT_DATASET_COLUMN_COUNT("quickSightDatasetColumnCount"),
    /** Number of widgets in the Redash Dashboard */
    REDASH_DASHBOARD_WIDGET_COUNT("redashDashboardWidgetCount"),
    /** Time when the Redash Query was last executed */
    REDASH_QUERY_LAST_EXECUTED_AT("redashQueryLastExecutedAt"),
    /** Runtime of Redash Query */
    REDASH_QUERY_LAST_EXECUTION_RUNTIME("redashQueryLastExecutionRuntime"),
    /** TBC */
    REPORT_COUNT("reportCount"),
    /** TBC */
    RESULT_COUNT("resultCount"),
    /** TBC */
    RESULT_MAKER_ID("resultMakerID"),
    /** TBC */
    ROW_COUNT("rowCount"),
    /** TBC */
    ROW_LIMIT("rowLimit"),
    /** TBC */
    S3OBJECT_COUNT("s3ObjectCount"),
    /** TBC */
    S3OBJECT_LAST_MODIFIED_TIME("s3ObjectLastModifiedTime"),
    /** TBC */
    S3OBJECT_SIZE("s3ObjectSize"),
    /** TBC */
    SCHEMA_COUNT("schemaCount"),
    /** TBC */
    SIGMA_DATASET_COLUMN_COUNT("sigmaDatasetColumnCount"),
    /** TBC */
    SIGMA_DATA_ELEMENT_COUNT("sigmaDataElementCount"),
    /** TBC */
    SIGMA_DATA_ELEMENT_FIELD_COUNT("sigmaDataElementFieldCount"),
    /** TBC */
    SIGMA_PAGE_COUNT("sigmaPageCount"),
    /** TBC */
    SIZE_BYTES("sizeBytes"),
    /** TBC */
    SNOWFLAKE_STREAM_STALE_AFTER("snowflakeStreamStaleAfter"),
    /** TBC */
    SOURCELAST_UPDATER_ID("sourcelastUpdaterId"),
    /** TBC */
    SOURCE_CHILD_COUNT("sourceChildCount"),
    /** TBC */
    SOURCE_CONTENT_METADATA_ID("sourceContentMetadataId"),
    /** TBC */
    SOURCE_CREATED_AT("sourceCreatedAt"),
    /** TBC */
    SOURCE_CREATOR_ID("sourceCreatorId"),
    /** TBC */
    SOURCE_LAST_ACCESSED_AT("sourceLastAccessedAt"),
    /** Timestamp of most recent read operation */
    SOURCE_LAST_READ_AT("sourceLastReadAt"),
    /** TBC */
    SOURCE_LAST_VIEWED_AT("sourceLastViewedAt"),
    /** TBC */
    SOURCE_METADATA_ID("sourceMetadataId"),
    /** TBC */
    SOURCE_PARENT_ID("sourceParentID"),
    /** TBC */
    SOURCE_QUERY_ID("sourceQueryId"),
    /** Total count of all read operations at source */
    SOURCE_READ_COUNT("sourceReadCount"),
    /** Total cost of read queries at source */
    SOURCE_READ_QUERY_COST("sourceReadQueryCost"),
    /** Total number of unique users that read data from asset */
    SOURCE_READ_USER_COUNT("sourceReadUserCount"),
    /** Total cost of all operations at source */
    SOURCE_TOTAL_COST("sourceTotalCost"),
    /** TBC */
    SOURCE_UPDATED_AT("sourceUpdatedAt"),
    /** TBC */
    SOURCE_USER_ID("sourceUserId"),
    /** TBC */
    SOURCE_VIEW_COUNT("sourceViewCount"),
    /** TBC */
    STALE_SINCE_DATE("staleSinceDate"),
    /** TBC */
    START_TIME("startTime"),
    /** TBC */
    TABLE_COUNT("tableCount"),
    /** TBC */
    TILE_COUNT("tileCount"),
    /** Time (in milliseconds) when the asset was created. */
    TIMESTAMP("__timestamp"),
    /** TBC */
    VIEW_COUNT("viewsCount"),
    /** TBC */
    VIEW_SCORE("viewScore"),
    ;

    @Getter(onMethod_ = {@Override})
    private final String indexedFieldName;

    NumericFields(String indexedFieldName) {
        this.indexedFieldName = indexedFieldName;
    }
}
