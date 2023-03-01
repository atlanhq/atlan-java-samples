/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.exception.AtlanException;
import com.atlan.exception.InvalidRequestException;
import com.atlan.exception.NotFoundException;
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
public class TermEnrichmentDetails extends AssetDetails {

    public static final String COL_GLOSSARY = "GLOSSARY NAME";
    public static final String COL_TERM_NAME = "TERM NAME*";
    public static final String COL_USER_DESCRIPTION = "USER DESCRIPTION";
    public static final String COL_README = "README";
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
     * Build up details about the term on the provided row.
     *
     * @param glossaryCache cache of glossaries keyed by glossary name
     * @param categoryCache cache of categories keyed by category identity
     * @param row a row of data from the spreadsheet, as a map from column name to value
     * @param delim delimiter used in cells that can contain multiple values
     * @return the term enrichment details for that row
     */
    public static TermEnrichmentDetails getFromRow(
            Map<String, Asset> glossaryCache, Map<String, Asset> categoryCache, Map<String, String> row, String delim) {
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
                        .categories(categories)
                        .relatedTerms(getMultiValuedList(row.get(COL_T_RELATED), delim))
                        .recommendedTerms(getMultiValuedList(row.get(COL_T_RECOMMENDED), delim))
                        .synonyms(getMultiValuedList(row.get(COL_T_SYNONYMS), delim))
                        .antonyms(getMultiValuedList(row.get(COL_T_ANTONYMS), delim))
                        .translatedTerms(getMultiValuedList(row.get(COL_T_TRANSLATED), delim))
                        .validValuesFor(getMultiValuedList(row.get(COL_T_VALID_VALUES), delim))
                        .classifies(getMultiValuedList(row.get(COL_T_CLASSIFIES), delim));
                return builder.stub(false).build();
            }
        }
        return null;
    }

    /**
     * Create terms in bulk, if they do not exist, or update them if they do (idempotent).
     *
     * @param terms the set of terms to ensure exist
     * @param batchSize maximum number of terms to create per batch
     * @return a cache of the terms
     */
    public static Map<String, Asset> upsert(Map<String, TermEnrichmentDetails> terms, int batchSize) {
        Map<String, Asset> termIdentityToResult = new HashMap<>();
        Map<String, List<String>> toClassify = new HashMap<>();
        Map<String, String> readmes = new HashMap<>();
        Map<String, TermEnrichmentDetails> termToTerm = new HashMap<>();

        for (TermEnrichmentDetails details : terms.values()) {
            Asset glossary = details.getGlossary();
            if (glossary != null) {
                String termName = details.getName();
                GlossaryTerm.GlossaryTermBuilder<?, ?> builder = null;
                try {
                    GlossaryTerm found = GlossaryTerm.findByNameFast(termName, glossary.getQualifiedName(), List.of("anchor"));
                    builder = found.trimToRequired().guid(found.getGuid());
                } catch (NotFoundException e) {
                    builder = GlossaryTerm.creator(termName, glossary.getGuid(), glossary.getQualifiedName());
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
                    GlossaryTerm term = builder.build();
                    // Create the term now, as we need to resolve GUIDs and qualifiedNames
                    // before we can take next actions
                    try {
                        AssetMutationResponse response = term.upsert();
                        if (response != null) {
                            List<Asset> created = response.getCreatedAssets();
                            if (created != null) {
                                for (Asset one : created) {
                                    if (one.getName().equals(termName)) {
                                        termIdentityToResult.put(details.getIdentity(), one);
                                    }
                                }
                            }
                            List<Asset> updated = response.getUpdatedAssets();
                            if (created != null) {
                                for (Asset one : updated) {
                                    if (one.getName().equals(termName)) {
                                        termIdentityToResult.put(details.getIdentity(), one);
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
                    if (!details.getClassifications().isEmpty()) {
                        // Note that the qualifiedName is only resolved after the asset is
                        // created (or updated) above
                        Asset resolved = termIdentityToResult.get(details.getIdentity());
                        if (resolved != null) {
                            toClassify.put(resolved.getQualifiedName(), details.getClassifications());
                        }
                    }
                    String readmeContents = details.getReadme();
                    if (readmeContents != null && readmeContents.length() > 0) {
                        readmes.put(details.getIdentity(), readmeContents);
                    }
                    if (details.hasTermToTermRelationships()) {
                        termToTerm.put(details.getIdentity(), details);
                    }
                }
            }
        }

        // Classifications must be added in a second pass, after the asset exists
        appendClassifications(toClassify, GlossaryTerm.TYPE_NAME);

        // Then go through and create any the READMEs linked to these assets...
        AssetBatch readmeBatch = new AssetBatch(Readme.TYPE_NAME, batchSize);
        for (Map.Entry<String, String> entry : readmes.entrySet()) {
            String termIdentity = entry.getKey();
            String readmeContent = entry.getValue();
            Asset term = termIdentityToResult.get(termIdentity);
            if (term != null) {
                Readme readme =
                        Readme.creator(term, term.getName(), readmeContent).build();
                readmeBatch.add(readme);
            } else {
                log.error("Unable to find term GUID for {} — cannot add README.", termIdentity);
            }
        }
        readmeBatch.flush();

        // And finally go through and create any term-to-term relationships
        AssetBatch termToTermBatch = new AssetBatch(GlossaryTerm.TYPE_NAME, batchSize);
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
                            log.warn("Unable to find synonym: {}", synonym);
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

        return termIdentityToResult;
    }
}
