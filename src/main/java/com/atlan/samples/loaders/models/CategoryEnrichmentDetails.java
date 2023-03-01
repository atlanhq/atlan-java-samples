/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.exception.AtlanException;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.Asset;
import com.atlan.model.assets.GlossaryCategory;
import com.atlan.model.assets.Readme;
import com.atlan.model.core.AssetMutationResponse;
import com.atlan.samples.loaders.*;
import java.util.*;
import java.util.regex.Pattern;
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
public class CategoryEnrichmentDetails extends AssetDetails {

    public static final String COL_GLOSSARY = "GLOSSARY NAME";
    public static final String COL_CATEGORY_PATH = "CATEGORY PATH";
    public static final String COL_USER_DESCRIPTION = "USER DESCRIPTION";
    public static final String COL_README = "README";

    private static final List<String> REQUIRED = List.of(COL_GLOSSARY, COL_CATEGORY_PATH);

    private static final String categoryGlossaryDelimiter = "@@@";

    @ToString.Include
    private Asset glossary;

    @ToString.Include
    private String categoryPath;

    @ToString.Include
    private String userDescription;

    @ToString.Include
    private String readme;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentity() {
        return getIdentity(categoryPath, glossary);
    }

    /**
     * Construct the unique identity for the provided category path in a given glossary.
     *
     * @param categoryPath path of the category (@-delimited)
     * @param glossary glossary in which the category is expected to exist
     * @return the unique identity (not qualifiedName) of the category
     */
    public static String getIdentity(String categoryPath, Asset glossary) {
        return categoryPath + categoryGlossaryDelimiter + glossary.getName();
    }

    /**
     * Build up details about the category on the provided row.
     *
     * @param glossaryCache cache of glossaries keyed by glossary name
     * @param row a row of data from the spreadsheet, as a map from column name to value
     * @param delim delimiter used in cells that can contain multiple values
     * @return the category enrichment details for that row
     */
    public static CategoryEnrichmentDetails getFromRow(
            Map<String, Asset> glossaryCache, Map<String, String> row, String delim) {
        CategoryEnrichmentDetailsBuilder<?, ?> builder = getFromRow(CategoryEnrichmentDetails.builder(), row, delim);
        if (getMissingFields(row, REQUIRED).isEmpty()) {
            Asset glossary = glossaryCache.get(row.get(COL_GLOSSARY));
            if (glossary != null) {
                builder = builder.glossary(glossary)
                        .categoryPath(row.get(COL_CATEGORY_PATH))
                        .userDescription(row.get(COL_USER_DESCRIPTION))
                        .readme(row.get(COL_README));
                return builder.stub(false).build();
            }
        }
        return null;
    }

    /**
     * Create categories in bulk, if they do not exist, or update them if they do (idempotent).
     *
     * @param categoryCache a cache of categories keyed by category identity
     * @param categories the set of categories to ensure exist
     * @param batchSize maximum number of categories to create per batch
     * @param level of categories to create
     */
    public static void upsert(
            Map<String, Asset> categoryCache,
            Map<String, CategoryEnrichmentDetails> categories,
            int batchSize,
            int level) {
        Map<String, String> readmes = new HashMap<>();
        Map<String, CategoryEnrichmentDetails> leftovers = new LinkedHashMap<>();

        // Note that we need to do this in multiple passes, so parent categories are always
        // created before children, and we won't really be able to batch them due to
        // the hierarchical nature of the categories...
        for (CategoryEnrichmentDetails details : categories.values()) {
            Asset glossary = details.getGlossary();
            String categoryPath = details.getCategoryPath();
            String[] tokens = categoryPath.split(Pattern.quote("@"));
            if (tokens.length == level) {
                String categoryName = tokens[tokens.length - 1];
                GlossaryCategory.GlossaryCategoryBuilder<?, ?> builder = null;
                if (!categoryCache.containsKey(details.getIdentity())) {
                    try {
                        GlossaryCategory found = GlossaryCategory.findByNameFast(
                                categoryName, glossary.getQualifiedName(), List.of("anchor"));
                        builder = found.trimToRequired().guid(found.getGuid());
                    } catch (NotFoundException e) {
                        builder =
                                GlossaryCategory.creator(categoryName, glossary.getGuid(), glossary.getQualifiedName());
                    } catch (AtlanException e) {
                        log.error("Unable to even search for the category: {}", details.getIdentity(), e);
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
                    if (tokens.length > 1) {
                        // If there are multiple tokens, there is a hierarchy to construct
                        String parentPath = categoryPath.substring(0, categoryPath.lastIndexOf("@"));
                        String parentName;
                        if (parentPath.contains("@")) {
                            parentName = parentPath.substring(parentPath.lastIndexOf("@") + 1);
                        } else {
                            parentName = parentPath;
                        }
                        String parentIdentity = getIdentity(parentPath, glossary);
                        Asset parent = categoryCache.get(parentIdentity);
                        if (parent != null) {
                            builder = builder.parentCategory(GlossaryCategory.refByGuid(parent.getGuid()));
                        } else {
                            log.error(
                                    "Parent category {} in glossary {} not defined in spreadsheet — cannot create {} as a child of this category.",
                                    parentName,
                                    glossary.getName(),
                                    categoryName);
                        }
                    }
                    GlossaryCategory category = builder.build();
                    try {
                        // TODO: matching on name alone has very minor risk of a collision as it is not strictly unique
                        AssetMutationResponse response = category.upsert();
                        if (response != null) {
                            List<Asset> created = response.getCreatedAssets();
                            if (created != null) {
                                for (Asset one : created) {
                                    if (one.getName().equals(categoryName)) {
                                        categoryCache.put(details.getIdentity(), one);
                                    }
                                }
                            }
                            List<Asset> updated = response.getUpdatedAssets();
                            if (created != null) {
                                for (Asset one : updated) {
                                    if (one.getName().equals(categoryName)) {
                                        categoryCache.put(details.getIdentity(), one);
                                    }
                                }
                            }
                        }
                        if (!categoryCache.containsKey(details.getIdentity())) {
                            // If it was a no-op because nothing changed, pass-through the category we already found
                            categoryCache.put(details.getIdentity(), category);
                        }
                    } catch (AtlanException e) {
                        log.error("Unable to upsert category: {}", details.getIdentity(), e);
                    }
                    String readmeContents = details.getReadme();
                    if (readmeContents != null && readmeContents.length() > 0) {
                        readmes.put(details.getIdentity(), readmeContents);
                    }
                }
            } else {
                leftovers.put(details.getIdentity(), details);
            }
        }

        // Then go through and create any the READMEs linked to these assets...
        AssetBatch readmeBatch = new AssetBatch(Readme.TYPE_NAME, batchSize);
        for (Map.Entry<String, String> entry : readmes.entrySet()) {
            String categoryIdentity = entry.getKey();
            String readmeContent = entry.getValue();
            Asset category = categoryCache.get(categoryIdentity);
            if (category != null) {
                Readme readme = Readme.creator(category, category.getName(), readmeContent)
                        .build();
                readmeBatch.add(readme);
            } else {
                log.error("Unable to find category GUID for {} — cannot add README.", categoryIdentity);
            }
        }
        readmeBatch.flush();

        // And then recurse on the leftovers...
        if (!leftovers.isEmpty()) {
            upsert(categoryCache, leftovers, batchSize, level + 1);
        }
    }
}
