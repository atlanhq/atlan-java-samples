/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders;

import com.atlan.Atlan;
import com.atlan.api.EntityUniqueAttributesEndpoint;
import com.atlan.exception.AtlanException;
import com.atlan.model.assets.Asset;
import com.atlan.model.core.Classification;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;

@Slf4j
public abstract class AbstractLoader {

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
            _filename = event.getOrDefault("FILENAME", "atlan-documentation-template.xlsx");
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

    /**
     * Retrieve a mapping of the assets that need to be reclassified.
     * The method will first look up the provided assets to determine only the missing classifications
     * that need to be appended (rather than attempting to blindly append all classifications).
     *
     * @param assetMap mapping of assets to consider, keyed by qualifiedName with the value a list of classification names to add the asset to
     * @param typeName of all the assets
     */
    protected void appendClassifications(Map<String, List<String>> assetMap, String typeName) {
        Map<String, List<String>> toReclassify = new HashMap<>();
        if (!assetMap.isEmpty()) {
            for (Map.Entry<String, List<String>> details : assetMap.entrySet()) {
                String qn = details.getKey();
                List<String> classifications = new ArrayList<>(details.getValue());
                try {
                    Asset column = Asset.retrieveMinimal(typeName, qn);
                    Set<Classification> existing = column.getClassifications();
                    List<String> toRemove = new ArrayList<>();
                    for (Classification one : existing) {
                        if (classifications.contains(one.getTypeName())) {
                            toRemove.add(one.getTypeName());
                        }
                    }
                    classifications.removeAll(toRemove);
                    if (!classifications.isEmpty()) {
                        toReclassify.put(qn, classifications);
                    }
                } catch (AtlanException e) {
                    log.error("Unable to find {} {} — cannot reclassify it.", typeName, qn, e);
                }
            }
        }
        if (!toReclassify.isEmpty()) {
            log.info("... classifying {} {}s:", toReclassify.size(), typeName);
            for (Map.Entry<String, List<String>> details : toReclassify.entrySet()) {
                String qn = details.getKey();
                List<String> classifications = details.getValue();
                try {
                    log.info("...... classifying: {}", qn);
                    EntityUniqueAttributesEndpoint.addClassifications(typeName, qn, classifications);
                } catch (AtlanException e) {
                    log.error("Unable to classify {} {} with: {}", typeName, qn, classifications, e);
                }
            }
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
