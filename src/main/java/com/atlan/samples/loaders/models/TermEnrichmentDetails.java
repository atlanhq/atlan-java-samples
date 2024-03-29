/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.Atlan;
import com.atlan.exception.AtlanException;
import com.atlan.exception.InvalidRequestException;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.*;
import com.atlan.model.core.AssetMutationResponse;
import com.atlan.model.core.AtlanTag;
import com.atlan.model.core.CustomMetadataAttributes;
import com.atlan.samples.loaders.caches.CategoryCache;
import com.atlan.samples.loaders.caches.GlossaryCache;
import com.atlan.samples.loaders.caches.TermCache;
import com.atlan.util.AssetBatch;
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
public class TermEnrichmentDetails extends EnrichmentDetails {

    public static final String COL_GLOSSARY = "GLOSSARY NAME";
    public static final String COL_TERM_NAME = "TERM NAME*";
    public static final String COL_CATEGORIES = "CATEGORIES";
    public static final String COL_T_RELATED = "RELATED TERMS";
    public static final String COL_T_RECOMMENDED = "RECOMMENDED TERMS";
    public static final String COL_T_SYNONYMS = "SYNONYMS";
    public static final String COL_T_ANTONYMS = "ANTONYMS";
    public static final String COL_T_TRANSLATED = "TRANSLATED TERMS";
    public static final String COL_T_VALID_VALUES = "VALID VALUES FOR";
    public static final String COL_T_CLASSIFIES = "CLASSIFIES";

    private static final List<String> REQUIRED = List.of(COL_GLOSSARY, COL_TERM_NAME);

    static final String glossaryDelimiter = "@";

    @ToString.Include
    private Asset glossary;

    @ToString.Include
    private String name;

    @ToString.Include
    private String userDescription;

    @ToString.Include
    private List<Asset> categories;

    @ToString.Include
    private String readme;

    @ToString.Include
    private List<String> relatedTerms;

    @ToString.Include
    private List<String> recommendedTerms;

    @ToString.Include
    private List<String> synonyms;

    @ToString.Include
    private List<String> antonyms;

    @ToString.Include
    private List<String> translatedTerms;

    @ToString.Include
    private List<String> validValuesFor;

    @ToString.Include
    private List<String> classifies;

    private boolean hasTermToTermRelationships() {
        return !relatedTerms.isEmpty()
                || !recommendedTerms.isEmpty()
                || !synonyms.isEmpty()
                || !antonyms.isEmpty()
                || !translatedTerms.isEmpty()
                || !validValuesFor.isEmpty()
                || !classifies.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentity() {
        return name + glossaryDelimiter + glossary.getName();
    }

    /**
     * Retrieve the name of the term from its identity.
     *
     * @param identity of the term
     * @return only the name of the term
     */
    public static String getNameFromIdentity(String identity) {
        if (identity != null && identity.contains(glossaryDelimiter)) {
            return identity.substring(0, identity.indexOf(glossaryDelimiter));
        }
        return null;
    }

    /**
     * Retrieve the name of the glossary from a term's identity.
     *
     * @param identity of the term
     * @return only the name of the glossary
     */
    public static String getGlossaryNameFromIdentity(String identity) {
        if (identity != null && identity.contains(glossaryDelimiter)) {
            return identity.substring(identity.indexOf(glossaryDelimiter) + glossaryDelimiter.length());
        }
        return null;
    }

    /**
     * Build up details about the term on the provided row.
     *
     * @param glossaryCache cache of glossaries keyed by glossary name
     * @param categoryCache cache of categories keyed by category identity
     * @param row a row of data from the spreadsheet, as a map from column name to value
     * @param delim delimiter used in cells that can contain multiple values
     * @return the term enrichment details for that row
     */
    public static TermEnrichmentDetails getFromRow(
            GlossaryCache glossaryCache, CategoryCache categoryCache, Map<String, String> row, String delim) {
        TermEnrichmentDetailsBuilder<?, ?> builder = getFromRow(TermEnrichmentDetails.builder(), row, delim);
        if (getMissingFields(row, REQUIRED).isEmpty()) {
            Asset glossary = glossaryCache.get(row.get(COL_GLOSSARY));
            if (glossary != null) {
                String termName = row.get(COL_TERM_NAME);
                List<String> categoryPaths = getMultiValuedList(row.get(COL_CATEGORIES), delim);
                List<Asset> categories = new ArrayList<>();
                for (String categoryPath : categoryPaths) {
                    String categoryIdentity = CategoryEnrichmentDetails.getIdentity(categoryPath, glossary);
                    Asset category = categoryCache.get(categoryIdentity);
                    if (category != null) {
                        categories.add(category);
                    } else {
                        log.warn(
                                "Unable to find category {} for term {} — skipping this categorization.",
                                categoryIdentity,
                                termName);
                    }
                }
                builder = builder.glossary(glossary)
                        .name(termName)
                        .userDescription(row.get(COL_USER_DESCRIPTION))
                        .readme(row.get(COL_README))
                        .customMetadataValues(getCustomMetadataValuesFromRow(row, delim))
                        .categories(categories)
                        .relatedTerms(getMultiValuedList(row.get(COL_T_RELATED), delim))
                        .recommendedTerms(getMultiValuedList(row.get(COL_T_RECOMMENDED), delim))
                        .synonyms(getMultiValuedList(row.get(COL_T_SYNONYMS), delim))
                        .antonyms(getMultiValuedList(row.get(COL_T_ANTONYMS), delim))
                        .translatedTerms(getMultiValuedList(row.get(COL_T_TRANSLATED), delim))
                        .validValuesFor(getMultiValuedList(row.get(COL_T_VALID_VALUES), delim))
                        .classifies(getMultiValuedList(row.get(COL_T_CLASSIFIES), delim));
                return builder.stub(false).build();
            } else {
                log.warn("Unknown glossary {} — skipping term: {}", row.get(COL_GLOSSARY), row.get(COL_TERM_NAME));
            }
        }
        return null;
    }

    /**
     * Create terms in bulk, if they do not exist, or update them if they do (idempotent).
     *
     * @param terms the set of terms to ensure exist
     * @param batchSize maximum number of terms to create per batch
     * @param replaceAtlanTags if true, the Atlan tags in the spreadsheet will overwrite all existing Atlan tags on the asset; otherwise they will only be appended
     * @param replaceCM if true, the custom metadata in the spreadsheet will overwrite all custom metadata on the asset; otherwise only the attributes with values will be updated
     * @param updateOnly if true, only attempt to update existing assets, otherwise allow assets to be created as well
     * @return a cache of the terms
     */
    public static TermCache upsert(
            Map<String, TermEnrichmentDetails> terms,
            int batchSize,
            boolean replaceAtlanTags,
            boolean replaceCM,
            boolean updateOnly) {
        TermCache termIdentityToResult = new TermCache();
        Map<String, List<String>> toTag = new HashMap<>();
        Map<String, Map<String, CustomMetadataAttributes>> cmToUpdate = new HashMap<>();
        Map<String, String> readmes = new HashMap<>();
        Map<String, TermEnrichmentDetails> termToTerm = new HashMap<>();

        long localCount = 0;
        long totalResults = terms.size();
        for (TermEnrichmentDetails details : terms.values()) {
            Asset glossary = details.getGlossary();
            if (glossary != null) {
                String termName = details.getName();
                GlossaryTerm.GlossaryTermBuilder<?, ?> builder = null;
                try {
                    GlossaryTerm found =
                            GlossaryTerm.findByNameFast(termName, glossary.getQualifiedName(), List.of("anchor"));
                    builder = found.trimToRequired().guid(found.getGuid());
                } catch (NotFoundException e) {
                    if (updateOnly) {
                        log.warn("Unable to find existing term — skipping: {}@{}", termName, glossary.getName());
                    } else {
                        builder = GlossaryTerm.creator(termName, glossary.getGuid());
                    }
                } catch (AtlanException e) {
                    log.error("Unable to even search for the term: {}", details.getIdentity(), e);
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
                    for (Asset category : details.getCategories()) {
                        builder = builder.category(GlossaryCategory.refByGuid(category.getGuid()));
                    }
                    if (details.getCustomMetadataValues() != null) {
                        builder = builder.customMetadataSets(details.getCustomMetadataValues());
                    }
                    if (details.getAtlanTags() != null) {
                        List<String> clsNames = details.getAtlanTags();
                        for (String clsName : clsNames) {
                            builder = builder.atlanTag(AtlanTag.of(clsName));
                        }
                    }
                    GlossaryTerm term = builder.build();
                    // Create the term now, as we need to resolve GUIDs and qualifiedNames
                    // before we can take next actions
                    try {
                        AssetMutationResponse response = replaceCM
                                ? term.saveReplacingCM(replaceAtlanTags)
                                : term.saveMergingCM(replaceAtlanTags);
                        if (response != null) {
                            localCount++;
                            log.info(
                                    " ... processed {}/{} ({}%)",
                                    localCount, totalResults, Math.round(((double) localCount / totalResults) * 100));
                            List<Asset> created = response.getCreatedAssets();
                            if (created != null) {
                                for (Asset one : created) {
                                    if (one.getName().equals(termName)) {
                                        termIdentityToResult.put(
                                                details.getIdentity(),
                                                term.toBuilder()
                                                        .qualifiedName(one.getQualifiedName())
                                                        .guid(one.getGuid())
                                                        .build());
                                    }
                                }
                            }
                            List<Asset> updated = response.getUpdatedAssets();
                            if (updated != null) {
                                for (Asset one : updated) {
                                    if (one.getName().equals(termName)) {
                                        termIdentityToResult.put(
                                                details.getIdentity(),
                                                term.toBuilder()
                                                        .qualifiedName(one.getQualifiedName())
                                                        .guid(one.getGuid())
                                                        .build());
                                    }
                                }
                            }
                        }
                        if (!termIdentityToResult.containsKey(details.getIdentity())) {
                            // If it was a no-op because nothing changed, pass-through the term we already found
                            termIdentityToResult.put(details.getIdentity(), term);
                        }
                    } catch (AtlanException e) {
                        log.error("Unable to upsert term: {}", details.getIdentity(), e);
                    }
                    if (!replaceAtlanTags && !details.getAtlanTags().isEmpty()) {
                        // Note that the qualifiedName is only resolved after the asset is
                        // created (or updated) above
                        Asset resolved = termIdentityToResult.get(details.getIdentity());
                        if (resolved != null) {
                            toTag.put(resolved.getQualifiedName(), details.getAtlanTags());
                        }
                    }
                    if (!replaceCM && !details.getCustomMetadataValues().isEmpty()) {
                        // Note that the GUID is only resolved after the asset is
                        // created (or updated) above
                        Asset resolved = termIdentityToResult.get(details.getIdentity());
                        if (resolved != null) {
                            cmToUpdate.put(resolved.getGuid(), details.getCustomMetadataValues());
                        }
                    }
                    String readmeContents = details.getReadme();
                    if (readmeContents != null && !readmeContents.isEmpty()) {
                        readmes.put(details.getIdentity(), readmeContents);
                    }
                    if (details.hasTermToTermRelationships()) {
                        termToTerm.put(details.getIdentity(), details);
                    }
                }
            }
        }

        // If we did not replace the Atlan tags, they must be added in a second pass, after the asset exists
        if (!replaceAtlanTags) {
            appendAtlanTags(toTag, GlossaryTerm.TYPE_NAME);
        }

        // If we did not replace custom metadata, it must be selectively updated one-by-one
        if (!replaceCM) {
            selectivelyUpdateCustomMetadata(cmToUpdate);
        }

        // Then go through and create any the READMEs linked to these assets...
        try {
            AssetBatch readmeBatch = new AssetBatch(Atlan.getDefaultClient(), Readme.TYPE_NAME, batchSize);
            for (Map.Entry<String, String> entry : readmes.entrySet()) {
                String termIdentity = entry.getKey();
                String readmeContent = entry.getValue();
                Asset term = termIdentityToResult.get(termIdentity);
                if (term != null) {
                    Readme readme = Readme.creator(term, readmeContent).build();
                    readmeBatch.add(readme);
                } else {
                    log.error("Unable to find term GUID for {} — cannot add README.", termIdentity);
                }
            }
            readmeBatch.flush();
        } catch (AtlanException e) {
            log.error("Unable to bulk-upsert READMEs for terms.", e);
        }

        // And finally go through and create any term-to-term relationships
        try {
            AssetBatch termToTermBatch =
                    new AssetBatch(Atlan.getDefaultClient(), "term-to-term relationship", batchSize);
            for (Map.Entry<String, TermEnrichmentDetails> entry : termToTerm.entrySet()) {
                String identity = entry.getKey();
                TermEnrichmentDetails t2tDetails = entry.getValue();
                Asset asset = termIdentityToResult.get(identity);
                if (asset instanceof GlossaryTerm) {
                    GlossaryTerm term = (GlossaryTerm) asset;
                    try {
                        GlossaryTerm.GlossaryTermBuilder<?, ?> toUpdate = term.trimToRequired();
                        for (String t2tRelated : t2tDetails.getRelatedTerms()) {
                            Asset related = termIdentityToResult.get(t2tRelated);
                            if (related != null) {
                                toUpdate = toUpdate.seeAlsoOne(GlossaryTerm.refByGuid(related.getGuid()));
                            } else {
                                log.warn("Unable to find related term: {}", t2tRelated);
                            }
                        }
                        for (String t2tRecommended : t2tDetails.getRecommendedTerms()) {
                            Asset recommended = termIdentityToResult.get(t2tRecommended);
                            if (recommended != null) {
                                toUpdate = toUpdate.preferredTerm(GlossaryTerm.refByGuid(recommended.getGuid()));
                            } else {
                                log.warn("Unable to find recommended term: {}", t2tRecommended);
                            }
                        }
                        for (String t2tSynonym : t2tDetails.getSynonyms()) {
                            Asset synonym = termIdentityToResult.get(t2tSynonym);
                            if (synonym != null) {
                                toUpdate = toUpdate.synonym(GlossaryTerm.refByGuid(synonym.getGuid()));
                            } else {
                                log.warn("Unable to find synonym: {}", t2tSynonym);
                            }
                        }
                        for (String t2tAntonym : t2tDetails.getAntonyms()) {
                            Asset antonym = termIdentityToResult.get(t2tAntonym);
                            if (antonym != null) {
                                toUpdate = toUpdate.antonym(GlossaryTerm.refByGuid(antonym.getGuid()));
                            } else {
                                log.warn("Unable to find antonym: {}", t2tAntonym);
                            }
                        }
                        for (String t2tTranslated : t2tDetails.getTranslatedTerms()) {
                            Asset translated = termIdentityToResult.get(t2tTranslated);
                            if (translated != null) {
                                toUpdate = toUpdate.translatedTerm(GlossaryTerm.refByGuid(translated.getGuid()));
                            } else {
                                log.warn("Unable to find translated term: {}", t2tTranslated);
                            }
                        }
                        for (String t2tValidFor : t2tDetails.getValidValuesFor()) {
                            Asset valid = termIdentityToResult.get(t2tValidFor);
                            if (valid != null) {
                                toUpdate = toUpdate.validValueFor(GlossaryTerm.refByGuid(valid.getGuid()));
                            } else {
                                log.warn("Unable to find related valid values for: {}", t2tValidFor);
                            }
                        }
                        for (String t2tClassifies : t2tDetails.getClassifies()) {
                            Asset classifies = termIdentityToResult.get(t2tClassifies);
                            if (classifies != null) {
                                toUpdate = toUpdate.classify(GlossaryTerm.refByGuid(classifies.getGuid()));
                            } else {
                                log.warn("Unable to find related classifies: {}", t2tClassifies);
                            }
                        }
                        termToTermBatch.add(toUpdate.build());
                    } catch (InvalidRequestException e) {
                        log.error(
                                "Missing key information to be able to set term-to-term relationships for: {}",
                                identity,
                                e);
                    }
                }
            }
            termToTermBatch.flush();
        } catch (AtlanException e) {
            log.error("Unable to bulk-upsert term-to-term relationships.", e);
        }

        return termIdentityToResult;
    }
}
