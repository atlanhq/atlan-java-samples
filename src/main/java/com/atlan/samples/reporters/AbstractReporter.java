/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.reporters;

import com.atlan.Atlan;
import com.atlan.model.assets.*;
import com.atlan.model.core.Classification;
import com.atlan.model.relations.Reference;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;

@Slf4j
public abstract class AbstractReporter {

    public static final double BYTES_IN_GB = 1073741824.0;
    public static final String TIMESTAMP_FORMAT = "uuuuMMdd-HHmmss-SSS";

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
        String timestamp = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));
        String filename = event.getOrDefault("FILE_PREFIX", prefix);
        if (filename.endsWith(".xls") || filename.endsWith(".xlsx")) {
            filename = filename.substring(0, filename.lastIndexOf(".xls"));
        }
        filename += "-" + timestamp + ".xlsx";
        setFilename(filename);
    }

    protected static String getFormattedDateTime(Long ts) {
        if (ts != null && ts > 0L) {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else {
            return "";
        }
    }

    protected static String getAssetLink(String guid) {
        return Atlan.getBaseUrlSafe() + "/assets/" + guid + "/overview";
    }

    protected static String getDescription(Asset asset) {
        String description = asset.getUserDescription();
        if (description == null || description.length() == 0) {
            description = asset.getDescription();
        }
        return description == null ? "" : description;
    }

    protected static String getUserOwners(Asset asset, String delimiter) {
        return getDelimitedList(asset.getOwnerUsers(), delimiter);
    }

    protected static String getGroupOwners(Asset asset, String delimiter) {
        return getDelimitedList(asset.getOwnerGroups(), delimiter);
    }

    protected static <T extends Reference> int getCount(Collection<T> collection) {
        if (collection == null) {
            return 0;
        } else {
            return collection.size();
        }
    }

    protected static String getREADME(Asset asset) {
        Readme readme = asset.getReadme();
        if (readme != null) {
            String content = readme.getDescription();
            if (content != null && content.length() > 0) {
                return content;
            }
        }
        return "";
    }

    /**
     * Retrieve a comma-separated list of all classifications assigned to the provided asset,
     * whether directly or through propagation.
     *
     * @param asset for which to find classifications
     * @param delimiter the separator to use between multiple values
     * @return comma-separated list of the classifications
     */
    protected static String getClassifications(Asset asset, String delimiter) {
        Set<Classification> classifications = asset.getClassifications();
        if (classifications != null) {
            Set<String> classificationNames =
                    classifications.stream().map(Classification::getTypeName).collect(Collectors.toSet());
            return getDelimitedList(classificationNames, delimiter);
        }
        return "";
    }

    /**
     * Retrieve a comma-separated list of only the directly-assigned (not propagated) classifications
     * to the provided asset.
     *
     * @param asset for which to find direct classifications
     * @param delimiter the separator to use between multiple values
     * @return comma-separated list of the direct classifications
     */
    protected static String getDirectClassifications(Asset asset, String delimiter) {
        Set<String> classificationNames = new TreeSet<>();
        if (asset != null) {
            Set<Classification> classifications = asset.getClassifications();
            if (classifications != null) {
                for (Classification classification : classifications) {
                    String classifiedEntity = classification.getEntityGuid();
                    if (asset.getGuid().equals(classifiedEntity)) {
                        classificationNames.add(classification.getTypeName());
                    }
                }
            }
        }
        return getDelimitedList(classificationNames, delimiter);
    }

    /**
     * Retrieve a list of multiple values as a single string, separated by the provided delimiter.
     *
     * @param items to combine into a single string
     * @param delimiter to use to separate each item
     * @return a single string of all items separated by the delimiter
     */
    protected static String getDelimitedList(Collection<String> items, String delimiter) {
        if (items == null) {
            return "";
        } else {
            return String.join(delimiter, items);
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
