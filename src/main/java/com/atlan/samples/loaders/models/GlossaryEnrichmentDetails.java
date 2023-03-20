/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.exception.AtlanException;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.Asset;
import com.atlan.model.assets.Glossary;
import com.atlan.model.assets.Readme;
import com.atlan.model.core.AssetMutationResponse;
import com.atlan.model.core.CustomMetadataAttributes;
import com.atlan.samples.loaders.*;
import java.util.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for capturing the enrichment details provided about a glossary.
 */
@Slf4j
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class GlossaryEnrichmentDetails extends EnrichmentDetails {

    public static final String COL_GLOSSARY = "GLOSSARY NAME";

    private static final List<String> REQUIRED = List.of(COL_GLOSSARY);

    @ToString.Include
    private String name;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentity() {
        return name;
    }

    /**
     * Build up details about the glossary on the provided row.
     *
     * @param row a row of data from the spreadsheet, as a map from column name to value
     * @param delim delimiter used in cells that can contain multiple values
     * @return the glossary enrichment details for that row
     */
    public static GlossaryEnrichmentDetails getFromRow(Map<String, String> row, String delim) {
        GlossaryEnrichmentDetailsBuilder<?, ?> builder = getFromRow(GlossaryEnrichmentDetails.builder(), row, delim);
        if (getMissingFields(row, REQUIRED).isEmpty()) {
            builder = builder.name(row.get(COL_GLOSSARY))
                    .userDescription(row.get(COL_USER_DESCRIPTION))
                    .readme(row.get(COL_README))
                    .customMetadataValues(getCustomMetadataValuesFromRow(row, delim));
            return builder.stub(false).build();
        }
        return null;
    }

    /**
     * Create glossaries in bulk, if they do not exist, or update them if they do (idempotent).
     *
     * @param glossaries the set of glossaries to ensure exist
     * @param batchSize maximum number of glossaries to create per batch
     * @param replaceClassifications if true, the classifications in the spreadsheet will overwrite all existing classifications on the asset; otherwise they will only be appended
     * @param replaceCM if true, the custom metadata in the spreadsheet will overwrite all custom metadata on the asset; otherwise only the attributes with values will be updated
     * @param updateOnly if true, only attempt to update existing assets, otherwise allow assets to be created as well
     * @return a cache of glossaries
     */
    public static Map<String, Asset> upsert(
            Map<String, GlossaryEnrichmentDetails> glossaries,
            int batchSize,
            boolean replaceClassifications,
            boolean replaceCM,
            boolean updateOnly) {
        Map<String, String> readmes = new HashMap<>();
        Map<String, Map<String, CustomMetadataAttributes>> cmToUpdate = new HashMap<>();
        Map<String, Asset> glossaryNameToResult = new HashMap<>();

        for (GlossaryEnrichmentDetails details : glossaries.values()) {
            String glossaryName = details.getName();
            Glossary.GlossaryBuilder<?, ?> builder = null;
            if (!glossaryNameToResult.containsKey(glossaryName)) {
                try {
                    Glossary found = Glossary.findByName(glossaryName, null);
                    builder = found.trimToRequired();
                } catch (NotFoundException e) {
                    if (updateOnly) {
                        log.warn("Unable to find existing glossary — skipping: {}", glossaryName);
                    } else {
                        builder = Glossary.creator(glossaryName);
                    }
                } catch (AtlanException e) {
                    log.error("Unable to even search for the glossary: {}", glossaryName, e);
                }
            }
            if (builder != null) {
                builder = builder.description(details.getDescription())
                        .userDescription(details.getUserDescription())
                        .certificateStatus(details.getCertificate())
                        .certificateStatusMessage(details.getCertificateStatusMessage())
                        .announcementType(details.getAnnouncementType())
                        .announcementTitle(details.getAnnouncementTitle())
                        .announcementMessage(details.getAnnouncementMessage())
                        .ownerUsers(details.getOwnerUsers())
                        .ownerGroups(details.getOwnerGroups());
                if (details.getCustomMetadataValues() != null) {
                    builder = builder.customMetadataSets(details.getCustomMetadataValues());
                }
                Glossary glossary = builder.build();
                String readmeContents = details.getReadme();
                if (readmeContents != null && readmeContents.length() > 0) {
                    readmes.put(details.getIdentity(), readmeContents);
                }
                try {
                    AssetMutationResponse result = glossary.upsert(replaceClassifications, replaceCM);
                    if (result != null) {
                        List<Asset> created = result.getCreatedAssets();
                        for (Asset one : created) {
                            if (one instanceof Glossary && one.getName().equals(glossaryName)) {
                                glossaryNameToResult.put(glossaryName, one);
                            }
                        }
                        List<Asset> updated = result.getUpdatedAssets();
                        for (Asset one : updated) {
                            if (one instanceof Glossary && one.getName().equals(glossaryName)) {
                                glossaryNameToResult.put(glossaryName, one);
                            }
                        }
                    }
                    if (!glossaryNameToResult.containsKey(glossaryName)) {
                        // If it was a no-op because nothing changed, pass-through the glossary we already found
                        glossaryNameToResult.put(glossaryName, glossary);
                    }
                } catch (AtlanException e) {
                    log.error("Unable to upsert glossary: {}", details.getIdentity());
                }
                if (!replaceCM && !details.getCustomMetadataValues().isEmpty()) {
                    // Note that the GUID is only resolved after the asset is
                    // created (or updated) above
                    Asset resolved = glossaryNameToResult.get(details.getIdentity());
                    if (resolved != null) {
                        cmToUpdate.put(resolved.getGuid(), details.getCustomMetadataValues());
                    }
                }
            }
        }

        // If we did not replace custom metadata, it must be selectively updated one-by-one
        if (!replaceCM) {
            selectivelyUpdateCustomMetadata(cmToUpdate);
        }

        // Then go through and create any the READMEs linked to these assets...
        AssetBatch readmeBatch = new AssetBatch(Readme.TYPE_NAME, batchSize);
        for (Map.Entry<String, String> entry : readmes.entrySet()) {
            String glossaryName = entry.getKey();
            String readmeContent = entry.getValue();
            Asset glossary = glossaryNameToResult.get(glossaryName);
            if (glossary != null) {
                Readme readme =
                        Readme.creator(glossary, glossaryName, readmeContent).build();
                readmeBatch.add(readme);
            } else {
                log.error("Unable to find glossary GUID for {} — cannot add README.", glossaryName);
            }
        }
        readmeBatch.flush();

        return glossaryNameToResult;
    }
}
