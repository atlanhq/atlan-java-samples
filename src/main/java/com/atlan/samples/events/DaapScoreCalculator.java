/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.atlan.cache.CustomMetadataCache;
import com.atlan.exception.AtlanException;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.AbstractProcess;
import com.atlan.model.assets.Asset;
import com.atlan.model.assets.Catalog;
import com.atlan.model.core.CustomMetadataAttributes;
import com.atlan.model.enums.AtlanCustomAttributePrimitiveType;
import com.atlan.model.events.*;
import com.atlan.model.typedefs.AttributeDef;
import com.atlan.model.typedefs.CustomMetadataDef;
import com.atlan.model.typedefs.CustomMetadataOptions;
import io.numaproj.numaflow.function.Datum;
import io.numaproj.numaflow.function.FunctionServer;
import io.numaproj.numaflow.function.Message;
import io.numaproj.numaflow.function.map.MapFunc;
import java.io.IOException;
import java.util.List;
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
            "__meanings",
            "__hasLineage",
            "classifications",
            "inputToProcesses",
            "outputFromProcesses");

    private static void createCMIfNotExists() throws AtlanException {
        try {
            CustomMetadataCache.getIdForName(CM_DAAP);
        } catch (NotFoundException e) {
            CustomMetadataDef customMetadataDef = CustomMetadataDef.creator(CM_DAAP)
                    .attributeDef(
                            AttributeDef.of(CM_ATTR_DAAP_SCORE, AtlanCustomAttributePrimitiveType.DECIMAL, null, false)
                                    .toBuilder()
                                    .description("Data as a Product completeness score for this asset")
                                    .build())
                    .options(CustomMetadataOptions.builder()
                            .logoType("emoji")
                            .emoji("\uD83D\uDD16")
                            .build())
                    .build();
            CustomMetadataDef response = customMetadataDef.create();
        }
        // TODO: Ensure a badge also exists for this score?
    }

    private static boolean hasDescription(Asset asset) {
        String description = asset.getUserDescription();
        if (description == null || description.length() == 0) {
            description = asset.getDescription();
        }
        return description != null && description.length() > 0;
    }

    private static boolean hasOwner(Asset asset) {
        Set<String> ownerUsers = asset.getOwnerUsers();
        Set<String> ownerGroups = asset.getOwnerGroups();
        return (ownerUsers != null && !ownerUsers.isEmpty()) || (ownerGroups != null && !ownerGroups.isEmpty());
    }

    private static boolean hasAssignedTerms(Asset asset) {
        return asset.getAssignedTerms() != null && !asset.getAssignedTerms().isEmpty();
    }

    private static boolean hasClassifications(Asset asset) {
        return asset.getClassifications() != null && !asset.getClassifications().isEmpty();
    }

    private static boolean hasLineage(Asset asset) {
        if (asset instanceof Catalog) {
            // If possible, look directly on inputs and outputs rather than the __hasLineage flag
            Catalog details = (Catalog) asset;
            List<AbstractProcess> downstream = details.getInputToProcesses();
            List<AbstractProcess> upstream = details.getOutputFromProcesses();
            return (downstream != null && !downstream.isEmpty()) || (upstream != null && !upstream.isEmpty());
        } else {
            return asset.getHasLineage();
        }
    }

    private static Message[] process(String key, Datum data) {
        try {
            createCMIfNotExists();
        } catch (AtlanException e) {
            log.error("Unable to create DaaP custom metadata.", e);
            return failed(data);
        }
        AtlanEvent event = getAtlanEvent(data);
        if (event == null || event.getPayload() == null || event.getPayload().getAsset() == null) {
            return failed(data);
        }
        try {
            Asset asset = getCurrentViewOfAsset(event, SCORED_ATTRS, true, true);
            if (asset == null) {
                return failed(data);
            }
            int sDescription = hasDescription(asset) ? 1 : 0;
            int sOwner = hasOwner(asset) ? 1 : 0;
            int sTerms = hasAssignedTerms(asset) ? 1 : 0;
            int sClassifications = hasClassifications(asset) ? 1 : 0;
            int sLineage = hasLineage(asset) ? 1 : 0;
            double score;
            if (asset.getTypeName().startsWith("AtlasGlossary")) {
                // Exclude lineage from the calculation for glossary objects
                score = ((sDescription + sOwner + sTerms + sClassifications) / 4.0) * 100;
            } else {
                // Include lineage in the calculation for all other objects,
                // but base it directly on inputs and outputs rather than the __hasLineage flag
                score = ((sDescription + sOwner + sLineage + sTerms + sClassifications) / 4.0) * 100;
            }
            // Set the score on the asset
            log.info("Updating DaaP completeness score for {} to: {}", asset.getQualifiedName(), score);
            CustomMetadataAttributes cma = CustomMetadataAttributes.builder()
                    .attribute(CM_ATTR_DAAP_SCORE, score)
                    .build();
            Asset.updateCustomMetadataAttributes(asset.getGuid(), CM_DAAP, cma);
            return succeeded(data);
        } catch (AtlanException e) {
            log.error(
                    "Unable to find the asset in Atlan: {}",
                    event.getPayload().getAsset().getQualifiedName(),
                    e);
            return failed(data);
        }
    }

    public static void main(String[] args) throws IOException {
        new FunctionServer()
                .registerMapper(new MapFunc(DaapScoreCalculator::process))
                .start();
    }
}
