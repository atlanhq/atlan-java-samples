/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.atlan.cache.CustomMetadataCache;
import com.atlan.exception.AtlanException;
import com.atlan.exception.ConflictException;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.*;
import com.atlan.model.core.CustomMetadataAttributes;
import com.atlan.model.enums.AtlanCustomAttributePrimitiveType;
import com.atlan.model.enums.BadgeComparisonOperator;
import com.atlan.model.enums.BadgeConditionColor;
import com.atlan.model.typedefs.AttributeDef;
import com.atlan.model.typedefs.CustomMetadataDef;
import com.atlan.model.typedefs.CustomMetadataOptions;
import io.numaproj.numaflow.function.Datum;
import io.numaproj.numaflow.function.FunctionServer;
import io.numaproj.numaflow.function.MessageList;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * An example to calculate a Data as a Product (DaaP) completeness score based on
 * the level of enrichment of an asset.
 */
@Slf4j
public class DaapScoreCalculator extends AbstractEventHandler {

    private static final String CM_DAAP = "DaaP";
    private static final String CM_ATTR_DAAP_SCORE = "Score";
    private static final List<String> SCORED_ATTRS = List.of(
            "description",
            "userDescription",
            "ownerUsers",
            "ownerGroups",
            "meanings",
            "__hasLineage",
            "classifications",
            "inputToProcesses",
            "outputFromProcesses");

    /**
     * Logic to apply to each event we receive.
     *
     * @param keys unique key of the event
     * @param data details of the event (including its payload)
     * @return an array of messages that can be passed to further vertexes in the pipeline
     */
    public MessageList processMessage(String[] keys, Datum data) {

        // 1. Ensure the DaaP custom metadata exists
        if (createCMIfNotExists() == null) {
            return failed(keys, data);
        }

        // 2. Ensure there's an Atlan event payload present
        Asset fromEvent = getAssetFromEvent(data);
        if (fromEvent == null) {
            return failed(keys, data);
        }

        // 3. Retrieve the current details about the asset from Atlan
        //    (in case the processing of this event was delayed or a later retry and
        //    the asset has since changed in Atlan — don't want to calculate based on
        //    stale information)
        Asset asset;
        try {
            Set<String> searchAttrs = new HashSet<>(SCORED_ATTRS);
            searchAttrs.addAll(CustomMetadataCache.getAttributesForSearchResults(CM_DAAP));
            asset = getCurrentViewOfAsset(fromEvent, searchAttrs, true, true);
        } catch (AtlanException e) {
            log.error("Unable to find the asset in Atlan: {}", fromEvent.getQualifiedName(), e);
            return failed(keys, data);
        }
        if (asset == null) {
            log.error("No current view of asset found (deleted or not yet available in search index): {}", fromEvent);
            return failed(keys, data);
        }

        // 4. Look at each individual component that should make up the score
        int sDescription = hasDescription(asset) ? 1 : 0;
        int sOwner = hasOwner(asset) ? 1 : 0;
        int sTerms = hasAssignedTerms(asset) ? 1 : 0;
        int sClassifications = hasClassifications(asset) ? 1 : 0;
        int sLineage = hasLineage(asset) ? 1 : 0;

        // 5. Calculate the score
        //    (glossary objects cannot have lineage, so exclude lineage from their score)
        double score;
        if (asset.getTypeName().startsWith("AtlasGlossary")) {
            score = ((sDescription + sOwner + sTerms + sClassifications) / 4.0) * 100;
        } else {
            score = ((sDescription + sOwner + sLineage + sTerms + sClassifications) / 5.0) * 100;
        }

        // 6. Only attempt to update the asset if the score has changed
        //    (otherwise we will create an infinite loop of updating the asset, Atlan
        //    generating a new event from that update, and so on)
        if (!scoreHasChanged(asset, score)) {
            log.info("No change in DaaP completeness score for: {}", asset.getQualifiedName());
            return drop();
        } else {
            CustomMetadataAttributes cma = CustomMetadataAttributes.builder()
                    .attribute(CM_ATTR_DAAP_SCORE, score)
                    .build();
            try {
                Asset.updateCustomMetadataAttributes(asset.getGuid(), CM_DAAP, cma);
                log.info("Updated DaaP completeness score for {} to: {}", asset.getQualifiedName(), score);
                return succeeded(keys, data);
            } catch (AtlanException e) {
                log.error("Unable to update DaaP completeness score for {} to: {}", asset.getQualifiedName(), score, e);
                return failed(keys, data);
            }
        }
    }

    /**
     * Register the event processing function.
     *
     * @param args (unused)
     * @throws IOException on any errors starting the event processor
     */
    public static void main(String[] args) throws IOException {
        new FunctionServer().registerMapHandler(new DaapScoreCalculator()).start();
    }

    /**
     * Check if the custom metadata already exists, and if so simply return.
     * If not, go ahead and create the custom metadata structure and an associated badge.
     *
     * @return the internal hashed-string name of the custom metadata
     */
    private String createCMIfNotExists() {
        try {
            return CustomMetadataCache.getIdForName(CM_DAAP);
        } catch (NotFoundException e) {
            try {
                CustomMetadataDef customMetadataDef = CustomMetadataDef.creator(CM_DAAP)
                        .attributeDef(
                                AttributeDef.of(
                                                CM_ATTR_DAAP_SCORE,
                                                AtlanCustomAttributePrimitiveType.DECIMAL,
                                                null,
                                                false)
                                        .toBuilder()
                                        .description("Data as a Product completeness score for this asset")
                                        .build())
                        .options(CustomMetadataOptions.builder()
                                .logoType("emoji")
                                .emoji("\uD83D\uDD16")
                                .build())
                        .build();
                customMetadataDef.create();
                log.info("Created DaaP custom metadata structure.");
                Badge badge = Badge.creator(CM_ATTR_DAAP_SCORE, CM_DAAP, CM_ATTR_DAAP_SCORE)
                        .userDescription(
                                "Data as a Product completeness score. Indicates how enriched and ready for re-use this asset is, out of a total possible score of 100.")
                        .badgeCondition(BadgeCondition.of(BadgeComparisonOperator.GTE, "75", BadgeConditionColor.GREEN))
                        .badgeCondition(BadgeCondition.of(BadgeComparisonOperator.LT, "75", BadgeConditionColor.YELLOW))
                        .badgeCondition(BadgeCondition.of(BadgeComparisonOperator.LTE, "25", BadgeConditionColor.RED))
                        .build();
                try {
                    badge.upsert();
                    log.info("Created DaaP completeness score badge.");
                } catch (AtlanException eBadge) {
                    log.error("Unable to create badge over the DaaP score.", eBadge);
                }
                return CustomMetadataCache.getIdForName(CM_DAAP);
            } catch (ConflictException conflict) {
                // Handle cross-thread race condition that the typedef has since been created
                try {
                    return CustomMetadataCache.getIdForName(CM_DAAP);
                } catch (AtlanException eConflict) {
                    log.error(
                            "Unable to look up DaaP custom metadata, even though it should already exist.", eConflict);
                }
            } catch (AtlanException eStruct) {
                log.error("Unable to create DaaP custom metadata structure.", eStruct);
            }
        } catch (AtlanException e) {
            log.error("Unable to look up DaaP custom metadata.", e);
        }
        return null;
    }

    /**
     * Check if the score calculated for the asset has changed.
     *
     * @param asset to check the score against
     * @param score calculated by this latest event
     * @return true if the score calculated by this event is different from what is already present on the asset
     */
    private boolean scoreHasChanged(Asset asset, double score) {
        Map<String, CustomMetadataAttributes> customMetadata = asset.getCustomMetadataSets();
        if (customMetadata != null && customMetadata.containsKey(CM_DAAP)) {
            Map<String, Object> attrs = customMetadata.get(CM_DAAP).getAttributes();
            return attrs.containsKey(CM_ATTR_DAAP_SCORE)
                    && !attrs.get(CM_ATTR_DAAP_SCORE).equals(score);
        }
        return true;
    }
}
