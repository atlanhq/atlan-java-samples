/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.exception.AtlanException;
import com.atlan.model.assets.*;
import com.atlan.model.core.AssetMutationResponse;
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
public class AssetEnrichmentDetails extends AssetDetails {

    public static final String COL_QUALIFIED_NAME = "QUALIFIED NAME";
    public static final String COL_TYPE = "TYPE";
    public static final String COL_NAME = "NAME";
    public static final String COL_USER_DESCRIPTION = "USER DESCRIPTION";
    public static final String COL_README = "README";
    public static final String COL_ASSIGNED_TERMS = "ASSIGNED TERMS";

    private static final List<String> REQUIRED = List.of(COL_QUALIFIED_NAME, COL_TYPE, COL_NAME);

    @ToString.Include
    private String qualifiedName;

    @ToString.Include
    private String type;

    @ToString.Include
    private String name;

    @ToString.Include
    private String userDescription;

    @ToString.Include
    private String readme;

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
                    .readme(row.get(COL_README));
            List<String> termIdentities = getMultiValuedList(row.get(COL_ASSIGNED_TERMS), delim);
            List<Asset> terms = new ArrayList<>();
            for (String termIdentity : termIdentities) {
                Asset term = termCache.get(termIdentity);
                if (term != null) {
                    terms.add(term);
                } else {
                    log.warn(
                            "Unable to find term {} for asset {} — skipping this assignment.",
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
     */
    public static void upsert(Map<String, AssetEnrichmentDetails> assets, int batchSize) {
        Map<String, Map<String, List<String>>> toClassifyMap = new HashMap<>();
        AssetBatch batch = new AssetBatch("asset", batchSize);
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
            Asset asset = builder.build();
            if (!details.getClassifications().isEmpty()) {
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
        }
        cacheResult(assetIdentityToResult, batch.flush(), null);

        // Classifications must be added in a second pass, after the asset exists
        for (Map.Entry<String, Map<String, List<String>>> entry : toClassifyMap.entrySet()) {
            String typeName = entry.getKey();
            Map<String, List<String>> toClassify = entry.getValue();
            appendClassifications(toClassify, typeName);
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
                log.error("Unable to find asset GUID for {} — cannot add README.", assetIdentity);
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
                // Cache for no-ops as well — where we will only have an entry in the GUID map
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
