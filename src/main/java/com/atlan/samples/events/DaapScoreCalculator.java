/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.atlan.Atlan;
import com.atlan.AtlanClient;
import com.atlan.events.AtlanEventHandler;
import com.atlan.exception.AtlanException;
import com.atlan.exception.ConflictException;
import com.atlan.exception.ErrorCode;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.*;
import com.atlan.model.core.CustomMetadataAttributes;
import com.atlan.model.enums.AtlanCustomAttributePrimitiveType;
import com.atlan.model.enums.BadgeComparisonOperator;
import com.atlan.model.enums.BadgeConditionColor;
import com.atlan.model.enums.CertificateStatus;
import com.atlan.model.events.AtlanEvent;
import com.atlan.model.structs.BadgeCondition;
import com.atlan.model.typedefs.AttributeDef;
import com.atlan.model.typedefs.CustomMetadataDef;
import com.atlan.model.typedefs.CustomMetadataOptions;
import java.util.*;
import org.slf4j.Logger;

/**
 * An example to calculate a Data as a Product (DaaP) completeness score based on
 * the level of enrichment of an asset.
 */
public class DaapScoreCalculator implements AtlanEventHandler {

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
            "outputFromProcesses",
            "assignedEntities",
            "seeAlso",
            "links");

    /** Singleton for reuse */
    private static final DaapScoreCalculator INSTANCE = createInstance();

    private static DaapScoreCalculator createInstance() {
        return new DaapScoreCalculator();
    }

    public static DaapScoreCalculator getInstance() {
        return INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean validatePrerequisites(AtlanEvent event, Logger log) {
        return createCMIfNotExists(log) != null
                && event.getPayload() != null
                && event.getPayload().getAsset() != null;
    }

    /** {@inheritDoc} */
    @Override
    public Asset getCurrentState(AtlanClient client, Asset fromEvent, Logger log) throws AtlanException {
        Set<String> searchAttrs = new HashSet<>(SCORED_ATTRS);
        searchAttrs.addAll(client.getCustomMetadataCache().getAttributesForSearchResults(CM_DAAP));
        Asset asset = AtlanEventHandler.getCurrentViewOfAsset(client, fromEvent, searchAttrs, true, true);
        if (asset == null) {
            throw new NotFoundException(
                    ErrorCode.ASSET_NOT_FOUND_BY_QN, fromEvent.getQualifiedName(), fromEvent.getTypeName());
        }
        return asset;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<Asset> calculateChanges(Asset asset, Logger log) throws AtlanException {

        // Calculate the score
        double score = -1.0;
        if (asset instanceof GlossaryTerm) {
            GlossaryTerm term = (GlossaryTerm) asset;
            int sDescription = AtlanEventHandler.hasDescription(term) ? 15 : 0;
            int sRelatedTerm = (term.getSeeAlso() != null && !term.getSeeAlso().isEmpty()) ? 10 : 0;
            int sLinks = (term.getLinks() != null && !term.getLinks().isEmpty()) ? 10 : 0;
            int sRelatedAsset = (term.getAssignedEntities() != null
                            && !term.getAssignedEntities().isEmpty())
                    ? 20
                    : 0;
            int sCertificate = 0;
            if (asset.getCertificateStatus() == CertificateStatus.DRAFT) {
                sCertificate = 15;
            } else if (asset.getCertificateStatus() == CertificateStatus.VERIFIED) {
                sCertificate = 25;
            }
            int sReadme = 0;
            IReadme readme = asset.getReadme();
            if (readme != null && readme.getGuid() != null) {
                readme = Readme.get(readme.getGuid());
                String description = readme.getDescription();
                if (description != null) {
                    if (description.length() > 1000) {
                        sReadme = 20;
                    } else if (description.length() > 500) {
                        sReadme = 10;
                    } else if (description.length() > 100) {
                        sReadme = 5;
                    }
                }
            }
            score = (sDescription + sRelatedTerm + sLinks + sRelatedAsset + sCertificate + sReadme);
        } else if (!asset.getTypeName().startsWith("AtlasGlossary")) {
            // We will not score glossaries or categories
            int sDescription = AtlanEventHandler.hasDescription(asset) ? 20 : 0;
            int sOwner = AtlanEventHandler.hasOwner(asset) ? 20 : 0;
            int sTerms = AtlanEventHandler.hasAssignedTerms(asset) ? 20 : 0;
            int sTags = AtlanEventHandler.hasAtlanTags(asset) ? 20 : 0;
            int sLineage = AtlanEventHandler.hasLineage(asset) ? 20 : 0;
            score = (sDescription + sOwner + sLineage + sTerms + sTags);
        }

        if (score >= 0) {
            CustomMetadataAttributes cma = CustomMetadataAttributes.builder()
                    .attribute(CM_ATTR_DAAP_SCORE, score)
                    .build();
            Asset revised = asset.trimToRequired().customMetadata(CM_DAAP, cma).build();
            return hasChanges(asset, revised, log) ? Set.of(revised) : Collections.emptySet();
        } else {
            return Collections.emptySet();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasChanges(Asset original, Asset modified, Logger log) {
        double scoreOriginal = -1.0;
        double scoreModified = -1.0;
        Map<String, CustomMetadataAttributes> customMetadata = original.getCustomMetadataSets();
        if (customMetadata != null && customMetadata.containsKey(CM_DAAP)) {
            Map<String, Object> attrs = customMetadata.get(CM_DAAP).getAttributes();
            scoreOriginal = (Double) attrs.getOrDefault(CM_ATTR_DAAP_SCORE, -1.0);
        }
        customMetadata = modified.getCustomMetadataSets();
        if (customMetadata != null && customMetadata.containsKey(CM_DAAP)) {
            Map<String, Object> attrs = customMetadata.get(CM_DAAP).getAttributes();
            scoreModified = (Double) attrs.getOrDefault(CM_ATTR_DAAP_SCORE, -1.0);
        }
        return scoreOriginal != scoreModified;
    }

    // Note: can reuse default upsertChanges

    /**
     * Check if the custom metadata already exists, and if so simply return.
     * If not, go ahead and create the custom metadata structure and an associated badge.
     *
     * @param log for logging information
     * @return the internal hashed-string name of the custom metadata
     */
    static String createCMIfNotExists(Logger log) {
        try {
            return Atlan.getDefaultClient().getCustomMetadataCache().getIdForName(CM_DAAP);
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
                    badge.save();
                    log.info("Created DaaP completeness score badge.");
                } catch (AtlanException eBadge) {
                    log.error("Unable to create badge over the DaaP score.", eBadge);
                }
                return Atlan.getDefaultClient().getCustomMetadataCache().getIdForName(CM_DAAP);
            } catch (ConflictException conflict) {
                // Handle cross-thread race condition that the typedef has since been created
                try {
                    return Atlan.getDefaultClient().getCustomMetadataCache().getIdForName(CM_DAAP);
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
}
