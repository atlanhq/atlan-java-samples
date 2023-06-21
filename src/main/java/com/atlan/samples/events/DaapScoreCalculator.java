/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.atlan.cache.CustomMetadataCache;
import com.atlan.events.AtlanEventHandler;
import com.atlan.exception.*;
import com.atlan.model.assets.*;
import com.atlan.model.core.CustomMetadataAttributes;
import com.atlan.model.enums.AtlanCustomAttributePrimitiveType;
import com.atlan.model.enums.BadgeComparisonOperator;
import com.atlan.model.enums.BadgeConditionColor;
import com.atlan.model.events.AtlanEvent;
import com.atlan.model.structs.BadgeCondition;
import com.atlan.model.typedefs.AttributeDef;
import com.atlan.model.typedefs.CustomMetadataDef;
import com.atlan.model.typedefs.CustomMetadataOptions;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

/**
 * An example to calculate a Data as a Product (DaaP) completeness score based on
 * the level of enrichment of an asset.
 */
public interface DaapScoreCalculator {

    String CM_DAAP = "DaaP";
    String CM_ATTR_DAAP_SCORE = "Score";
    List<String> SCORED_ATTRS = List.of(
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
     * Validate the necessary inputs for calculating the score are present in the payload.
     *
     * @param event the event from Atlan
     * @param log for logging information
     * @return true only if custom metadata could be created (or already exists) and there is an asset in the event
     */
    static boolean validatePrerequisites(AtlanEvent event, Logger log) {
        return createCMIfNotExists(log) != null
                && event.getPayload() != null
                && event.getPayload().getAsset() != null;
    }

    /**
     * Calculate the revised score, and apply it to the asset.
     *
     * @param fromEvent the asset to calculate a revised score against
     * @param log for logging information
     * @return true if the score was changed, false if there was no change to apply
     * @throws AtlanException on any issues retrieving the asset or applying its revised score
     */
    static boolean calculateScore(Asset fromEvent, Logger log) throws AtlanException {

        // Retrieve the current details about the asset from Atlan
        // (in case the processing of this event was delayed or a later retry and
        // the asset has since changed in Atlan — don't want to calculate based on
        // stale information)
        Set<String> searchAttrs = new HashSet<>(SCORED_ATTRS);
        searchAttrs.addAll(CustomMetadataCache.getAttributesForSearchResults(CM_DAAP));
        Asset asset = AtlanEventHandler.getCurrentViewOfAsset(fromEvent, searchAttrs, true, true);

        if (asset == null) {
            throw new NotFoundException(
                    ErrorCode.ASSET_NOT_FOUND_BY_QN, fromEvent.getQualifiedName(), fromEvent.getTypeName());
        }

        // Look at each individual component that should make up the score
        int sDescription = AtlanEventHandler.hasDescription(asset) ? 1 : 0;
        int sOwner = AtlanEventHandler.hasOwner(asset) ? 1 : 0;
        int sTerms = AtlanEventHandler.hasAssignedTerms(asset) ? 1 : 0;
        int sClassifications = AtlanEventHandler.hasAtlanTags(asset) ? 1 : 0;
        int sLineage = AtlanEventHandler.hasLineage(asset) ? 1 : 0;

        // Calculate the score
        // (glossary objects cannot have lineage, so exclude lineage from their score)
        double score;
        if (asset.getTypeName().startsWith("AtlasGlossary")) {
            score = ((sDescription + sOwner + sTerms + sClassifications) / 4.0) * 100;
        } else {
            score = ((sDescription + sOwner + sLineage + sTerms + sClassifications) / 5.0) * 100;
        }

        // Only attempt to update the asset if the score has changed
        // (otherwise we will create an infinite loop of updating the asset, Atlan
        // generating a new event from that update, and so on)
        if (!scoreHasChanged(asset, score)) {
            log.info("No change in DaaP completeness score for: {}", asset.getQualifiedName());
            return false;
        } else {
            CustomMetadataAttributes cma = CustomMetadataAttributes.builder()
                    .attribute(CM_ATTR_DAAP_SCORE, score)
                    .build();
            Asset.updateCustomMetadataAttributes(asset.getGuid(), CM_DAAP, cma);
            log.info("Updated DaaP completeness score for {} to: {}", asset.getQualifiedName(), score);
            return true;
        }
    }

    /**
     * Check if the custom metadata already exists, and if so simply return.
     * If not, go ahead and create the custom metadata structure and an associated badge.
     *
     * @param log for logging information
     * @return the internal hashed-string name of the custom metadata
     */
    static String createCMIfNotExists(Logger log) {
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
                        .options(CustomMetadataOptions.withLogoAsEmoji("\uD83D\uDD16"))
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
    static boolean scoreHasChanged(Asset asset, double score) {
        Map<String, CustomMetadataAttributes> customMetadata = asset.getCustomMetadataSets();
        if (customMetadata != null && customMetadata.containsKey(CM_DAAP)) {
            Map<String, Object> attrs = customMetadata.get(CM_DAAP).getAttributes();
            return attrs.containsKey(CM_ATTR_DAAP_SCORE)
                    && !attrs.get(CM_ATTR_DAAP_SCORE).equals(score);
        }
        return true;
    }
}
