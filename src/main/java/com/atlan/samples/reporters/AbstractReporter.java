/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.reporters;

import static com.atlan.util.QueryFactory.*;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.atlan.Atlan;
import com.atlan.model.assets.*;
import com.atlan.model.relations.Reference;
import com.atlan.util.StringUtils;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;

@Slf4j
public abstract class AbstractReporter {

    static final Query beGlossaryAsset =
            beOneOfTypes(List.of(Glossary.TYPE_NAME, GlossaryCategory.TYPE_NAME, GlossaryTerm.TYPE_NAME));
    static final Query beTableLevel =
            beOneOfTypes(List.of(Table.TYPE_NAME, View.TYPE_NAME, MaterializedView.TYPE_NAME));

    // BI assets that connect to underlying data sources via lineage
    static final Query beBIAsset = beOneOfTypes(
            List.of(LookerView.TYPE_NAME, PowerBITable.TYPE_NAME, TableauDatasource.TYPE_NAME, SigmaDataset.TYPE_NAME));
    static final Query beBIDashboard = beOneOfTypes(List.of(
            LookerDashboard.TYPE_NAME, PowerBIDashboard.TYPE_NAME, TableauDashboard.TYPE_NAME, SigmaPage.TYPE_NAME));

    static final List<String> TLA_ATTRIBUTES =
            List.of("connectorName", "databaseName", "schemaName", "name", "rowCount", "sizeBytes");

    static final double BYTES_IN_GB = 1073741824.0;
    static final String TIMESTAMP_FORMAT = "uuuuMMdd-HHmmss-SSS";

    private int _batchSize = 50;
    private String _delimiter = "|";
    private Region _region = null;
    private String _bucket = null;
    private String _filename = null;

    /**
     * Extract the configuration parameters to use from the lambda payload (or equivalent).
     *
     * @param event configuration payload from the lambda function (or equivalent)
     */
    protected void parseParametersFromEvent(Map<String, String> event) {
        if (event != null) {
            log.debug(" ... event: {}", event);
            String batchSize = event.getOrDefault("BATCH_SIZE", "50");
            try {
                _batchSize = Integer.parseInt(batchSize);
            } catch (NumberFormatException e) {
                log.warn(
                        "Unable to determine a number from the BATCH_SIZE value of '{}', falling back to a default of 50.",
                        batchSize);
                _batchSize = 50;
            }
            _delimiter = event.getOrDefault("DELIMITER", "|");
            String region = event.getOrDefault("REGION", "ap-south-1");
            _region = Region.of(region);
            _bucket = event.getOrDefault("BUCKET", null);
            Atlan.setBaseUrl(event.getOrDefault("ATLAN_BASE_URL", null));
            Atlan.setApiToken(event.getOrDefault("ATLAN_API_KEY", null));
            String maxRetries = event.getOrDefault("MAX_RETRIES", "20");
            int _maxRetries = 20;
            try {
                _maxRetries = Integer.parseInt(maxRetries);
            } catch (NumberFormatException e) {
                log.warn(
                        "Unable to determine a number from the MAX_RETRIES value of '{}', falling back to a default of 20.",
                        maxRetries);
            }
            Atlan.setMaxNetworkRetries(_maxRetries);
        }
    }

    protected void setFilenameWithPrefix(Map<String, String> event, String prefix) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));
        String filename = event.getOrDefault("FILE_PREFIX", prefix);
        if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
            filename = filename.substring(0, filename.lastIndexOf(".xls"));
        }
        filename += "-" + timestamp + ".xlsx";
        setFilename(filename);
    }

    static String getFormattedDateTime(Long ts) {
        if (ts != null && ts > 0L) {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else {
            return "";
        }
    }

    static String getAssetLink(String guid) {
        return Atlan.getBaseUrlSafe() + "/assets/" + guid + "/overview";
    }

    static String getDescription(Asset asset) {
        String description = asset.getUserDescription();
        if (description == null || description.length() == 0) {
            description = asset.getDescription();
        }
        return description == null ? "" : description;
    }

    static String getUserOwners(Asset asset) {
        return getCommaSeparatedList(asset.getOwnerUsers());
    }

    static String getGroupOwners(Asset asset) {
        return getCommaSeparatedList(asset.getOwnerGroups());
    }

    static <T extends Reference> int getCount(Collection<T> collection) {
        if (collection == null) {
            return 0;
        } else {
            return collection.size();
        }
    }

    static String getREADME(Asset asset) {
        Readme readme = asset.getReadme();
        if (readme != null) {
            String content = readme.getDescription();
            if (content != null && content.length() > 0) {
                try {
                    return StringUtils.decodeContent(content);
                } catch (IllegalArgumentException e) {
                    log.warn("Unable to decode the README — returning as plain text.", e);
                    return content;
                }
            }
        }
        return "";
    }

    static String getClassifications(Asset asset) {
        return getCommaSeparatedList(asset.getClassificationNames());
    }

    static String getCommaSeparatedList(Collection<String> items) {
        if (items == null) {
            return "";
        } else {
            return String.join(",", items);
        }
    }

    public void setFilename(String _filename) {
        this._filename = _filename;
    }

    public String getFilename() {
        return _filename;
    }

    public String getBucket() {
        return _bucket;
    }

    public Region getRegion() {
        return _region;
    }

    public int getBatchSize() {
        return _batchSize;
    }

    public String getDelimiter() {
        return _delimiter;
    }
}
