/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.exception.AtlanException;
import com.atlan.model.assets.*;
import com.atlan.model.core.AssetMutationResponse;
import com.atlan.model.core.Classification;
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
public class AssetEnrichmentDetails extends EnrichmentDetails {

    public static final String COL_QUALIFIED_NAME = "QUALIFIED NAME";
    public static final String COL_TYPE = "TYPE";
    public static final String COL_NAME = "NAME";
    public static final String COL_ASSIGNED_TERMS = "ASSIGNED TERMS";

    private static final List<String> REQUIRED = List.of(COL_QUALIFIED_NAME, COL_TYPE, COL_NAME);

    @ToString.Include
    private String qualifiedName;

    @ToString.Include
    private String type;

    @ToString.Include
    private String name;

    @ToString.Include
    private List<Asset> terms;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentity() {
        return getIdentity(type, qualifiedName);
    }

    public static String getIdentity(String typeName, String qualifiedName) {
        return qualifiedName + TermEnrichmentDetails.glossaryDelimiter + typeName;
    }

    /**
     * Build up details about the asset on the provided row.
     *
     * @param termCache cache of terms keyed by term identity
     * @param row a row of data from the spreadsheet, as a map from column name to value
     * @param delim delimiter used in cells that can contain multiple values
     * @return the asset enrichment details for that row
     */
    public static AssetEnrichmentDetails getFromRow(
            Map<String, Asset> termCache, Map<String, String> row, String delim) {
        AssetEnrichmentDetailsBuilder<?, ?> builder = getFromRow(AssetEnrichmentDetails.builder(), row, delim);
        if (getMissingFields(row, REQUIRED).isEmpty()) {
            String qualifiedName = row.get(COL_QUALIFIED_NAME);
            builder = builder.qualifiedName(qualifiedName)
                    .type(row.get(COL_TYPE))
                    .name(row.get(COL_NAME))
                    .userDescription(row.get(COL_USER_DESCRIPTION))
                    .readme(row.get(COL_README))
                    .customMetadataValues(getCustomMetadataValuesFromRow(row, delim));
            List<String> termIdentities = getMultiValuedList(row.get(COL_ASSIGNED_TERMS), delim);
            List<Asset> terms = new ArrayList<>();
            for (String termIdentity : termIdentities) {
                Asset term = termCache.get(termIdentity);
                if (term != null) {
                    terms.add(term);
                } else {
                    log.warn(
                            "Unable to find term {} for asset {} ??? skipping this assignment.",
                            termIdentity,
                            qualifiedName);
                }
            }
            builder = builder.terms(terms);
            return builder.stub(false).build();
        }
        return null;
    }

    /**
     * Create assets in bulk, if they do not exist, or update them if they do (idempotent).
     *
     * @param assets the set of assets to ensure exist
     * @param batchSize maximum number of assets to create per batch
     * @param replaceClassifications if true, the classifications in the spreadsheet will overwrite all existing classifications on the asset; otherwise they will only be appended
     * @param replaceCM if true, the custom metadata in the spreadsheet will overwrite all custom metadata on the asset; otherwise only the attributes with values will be updated
     */
    public static void upsert(
            Map<String, AssetEnrichmentDetails> assets,
            int batchSize,
            boolean replaceClassifications,
            boolean replaceCM) {
        Map<String, Map<String, List<String>>> toClassifyMap = new HashMap<>();
        Map<String, Map<String, CustomMetadataAttributes>> cmToUpdate = new HashMap<>();
        AssetBatch batch = new AssetBatch("asset", batchSize, replaceClassifications, replaceCM);
        Map<String, String> readmes = new HashMap<>();
        Map<String, Asset> assetIdentityToResult = new HashMap<>();

        for (AssetEnrichmentDetails details : assets.values()) {
            Asset.AssetBuilder<?, ?> builder = IndistinctAsset.builder()
                    .typeName(details.getType())
                    .qualifiedName(details.getQualifiedName())
                    .name(details.getName())
                    .description(details.getDescription())
                    .userDescription(details.getUserDescription())
                    .certificateStatus(details.getCertificate())
                    .certificateStatusMessage(details.getCertificateStatusMessage())
                    .announcementType(details.getAnnouncementType())
                    .announcementTitle(details.getAnnouncementTitle())
                    .announcementMessage(details.getAnnouncementMessage())
                    .ownerUsers(details.getOwnerUsers())
                    .ownerGroups(details.getOwnerGroups());
            for (Asset term : details.getTerms()) {
                builder = builder.assignedTerm(GlossaryTerm.refByGuid(term.getGuid()));
            }
            if (details.getCustomMetadataValues() != null) {
                builder = builder.customMetadataSets(details.getCustomMetadataValues());
            }
            if (details.getClassifications() != null) {
                List<String> clsNames = details.getClassifications();
                for (String clsName : clsNames) {
                    builder = builder.classification(Classification.of(clsName));
                }
            }
            Asset asset = builder.build();
            if (!replaceClassifications && !details.getClassifications().isEmpty()) {
                if (!toClassifyMap.containsKey(details.getType())) {
                    toClassifyMap.put(details.getType(), new HashMap<>());
                }
                List<String> existing = toClassifyMap
                        .get(details.getType())
                        .put(details.getQualifiedName(), details.getClassifications());
                if (existing != null) {
                    log.warn("Multiple entries with the same qualifiedName: {}", details.getQualifiedName());
                }
            }
            String readmeContents = details.getReadme();
            if (readmeContents != null && readmeContents.length() > 0) {
                readmes.put(details.getIdentity(), readmeContents);
                assetIdentityToResult.put(details.getIdentity(), asset);
            }
            cacheResult(assetIdentityToResult, batch.add(asset), asset);
            if (!replaceCM && !details.getCustomMetadataValues().isEmpty()) {
                cmToUpdate.put(details.getIdentity(), details.getCustomMetadataValues());
            }
        }
        cacheResult(assetIdentityToResult, batch.flush(), null);

        // If we did not replace the classifications, they must be added in a second pass, after the asset exists
        if (!replaceClassifications) {
            for (Map.Entry<String, Map<String, List<String>>> entry : toClassifyMap.entrySet()) {
                String typeName = entry.getKey();
                Map<String, List<String>> toClassify = entry.getValue();
                appendClassifications(toClassify, typeName);
            }
        }

        // If we did not replace custom metadata, it must be selectively updated one-by-one
        if (!replaceCM) {
            // Note that the GUID is only resolved after the asset is
            // created (or updated) above, so we need to translate the identities in our
            // map into resolved assets...
            Map<String, Map<String, CustomMetadataAttributes>> toUpdate = new HashMap<>();
            for (Map.Entry<String, Map<String, CustomMetadataAttributes>> entry : cmToUpdate.entrySet()) {
                String identity = entry.getKey();
                Asset resolved = assetIdentityToResult.get(identity);
                if (resolved != null) {
                    toUpdate.put(resolved.getGuid(), entry.getValue());
                }
            }
            selectivelyUpdateCustomMetadata(toUpdate);
        }

        // Then go through and create any the READMEs linked to these assets...
        AssetBatch readmeBatch = new AssetBatch(Readme.TYPE_NAME, batchSize);
        for (Map.Entry<String, String> entry : readmes.entrySet()) {
            String assetIdentity = entry.getKey();
            String readmeContent = entry.getValue();
            Asset asset = assetIdentityToResult.get(assetIdentity);
            if (asset != null) {
                Readme readme =
                        Readme.creator(asset, asset.getName(), readmeContent).build();
                readmeBatch.add(readme);
            } else {
                log.error("Unable to find asset GUID for {} ??? cannot add README.", assetIdentity);
            }
        }
        readmeBatch.flush();
    }

    private static void cacheResult(Map<String, Asset> cache, AssetMutationResponse response, Asset asset) {
        Set<String> cachedGuids = new HashSet<>();
        if (response != null) {
            List<Asset> created = response.getCreatedAssets();
            for (Asset one : created) {
                String identity = getIdentity(one.getTypeName(), one.getQualifiedName());
                cache.put(identity, one);
                cachedGuids.add(one.getGuid());
            }
            List<Asset> updated = response.getUpdatedAssets();
            for (Asset one : updated) {
                String identity = getIdentity(one.getTypeName(), one.getQualifiedName());
                cache.put(identity, one);
                cachedGuids.add(one.getGuid());
            }
            for (String guid : response.getGuidAssignments().values()) {
                // Cache for no-ops as well ??? where we will only have an entry in the GUID map
                if (!cachedGuids.contains(guid)) {
                    try {
                        Asset minimal = Asset.retrieveMinimal(guid);
                        String identity = getIdentity(minimal.getTypeName(), minimal.getQualifiedName());
                        cache.put(identity, minimal);
                    } catch (AtlanException e) {
                        log.warn("Mapped GUID from bulk upsert does not appear to exist in Atlan: {}", guid, e);
                    }
                }
            }
        }
    }
}
