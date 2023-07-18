/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders;

import com.atlan.Atlan;
import com.atlan.exception.AtlanException;
import com.atlan.model.assets.Asset;
import com.atlan.model.core.AtlanTag;
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
    private boolean _updateOnly = false;

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
            String updateOnly = event.getOrDefault("UPDATE_ONLY", "false");
            _updateOnly = updateOnly.toUpperCase(Locale.ROOT).equals("TRUE");
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
     * Retrieve a mapping of the assets that need to be retagged.
     * The method will first look up the provided assets to determine only the missing Atlan tags
     * that need to be appended (rather than attempting to blindly append all Atlan tags).
     *
     * @param assetMap mapping of assets to consider, keyed by qualifiedName with the value a list of Atlan tag names to add the asset to
     * @param typeName of all the assets
     */
    protected void appendAtlanTags(Map<String, List<String>> assetMap, String typeName) {
        Map<String, List<String>> toRetag = new HashMap<>();
        if (!assetMap.isEmpty()) {
            for (Map.Entry<String, List<String>> details : assetMap.entrySet()) {
                String qn = details.getKey();
                List<String> atlanTags = new ArrayList<>(details.getValue());
                try {
                    Asset column = Asset.retrieveMinimal(typeName, qn);
                    Set<AtlanTag> existing = column.getAtlanTags();
                    List<String> toRemove = new ArrayList<>();
                    for (AtlanTag one : existing) {
                        if (atlanTags.contains(one.getTypeName())) {
                            toRemove.add(one.getTypeName());
                        }
                    }
                    atlanTags.removeAll(toRemove);
                    if (!atlanTags.isEmpty()) {
                        toRetag.put(qn, atlanTags);
                    }
                } catch (AtlanException e) {
                    log.error("Unable to find {} {} — cannot retag it.", typeName, qn, e);
                }
            }
        }
        if (!toRetag.isEmpty()) {
            log.info("... tagging {} {}s:", toRetag.size(), typeName);
            for (Map.Entry<String, List<String>> details : toRetag.entrySet()) {
                String qn = details.getKey();
                List<String> atlanTags = details.getValue();
                try {
                    log.info("...... tagging: {}", qn);
                    Atlan.getDefaultClient().assets().addAtlanTags(typeName, qn, atlanTags);
                } catch (AtlanException e) {
                    log.error("Unable to tag {} {} with: {}", typeName, qn, atlanTags, e);
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

    public boolean isUpdateOnly() {
        return _updateOnly;
    }
}
