/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.atlan.samples.loaders.caches.CategoryCache;
import com.atlan.samples.loaders.caches.GlossaryCache;
import com.atlan.samples.loaders.caches.TermCache;
import com.atlan.samples.loaders.models.AssetEnrichmentDetails;
import com.atlan.samples.loaders.models.CategoryEnrichmentDetails;
import com.atlan.samples.loaders.models.GlossaryEnrichmentDetails;
import com.atlan.samples.loaders.models.TermEnrichmentDetails;
import com.atlan.samples.readers.ExcelReader;
import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EnrichmentLoader extends AbstractLoader implements RequestHandler<Map<String, String>, String> {

    private static final String GLOSSARY_ENRICHMENT = "Glossary enrichment";
    private static final String CATEGORY_ENRICHMENT = "Category enrichment";
    private static final String TERM_ENRICHMENT = "Term enrichment";
    private static final String ASSET_ENRICHMENT = "Asset enrichment";

    private static boolean REPLACE_CLASSIFICATIONS = false;
    private static boolean REPLACE_CUSTOM_METADATA = false;

    public static void main(String[] args) {
        EnrichmentLoader el = new EnrichmentLoader();
        Map<String, String> event = new HashMap<>(System.getenv());
        if (!event.containsKey("DELIMITER")) {
            event.put("DELIMITER", ",");
        }
        if (!event.containsKey("FILENAME")) {
            event.put("FILENAME", "atlan-enrichment.xlsx");
        }
        el.handleRequest(event, null);
    }

    @Override
    protected void parseParametersFromEvent(Map<String, String> event) {
        super.parseParametersFromEvent(event);
        if (event != null) {
            String replaceClassifications = event.getOrDefault("REPLACE_CLASSIFICATIONS", "false");
            REPLACE_CLASSIFICATIONS =
                    replaceClassifications.toUpperCase(Locale.ROOT).equals("TRUE");
            String replaceCM = event.getOrDefault("REPLACE_CUSTOM_METADATA", "false");
            REPLACE_CUSTOM_METADATA = replaceCM.toUpperCase(Locale.ROOT).equals("TRUE");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleRequest(Map<String, String> event, Context context) {

        try {

            log.info("Retrieving configuration and context...");
            if (context != null && context.getClientContext() != null) {
                log.debug(
                        " ... client environment: {}",
                        context.getClientContext().getEnvironment());
                log.debug(" ... client custom: {}", context.getClientContext().getCustom());
            }
            parseParametersFromEvent(event);

            log.info("Loading enrichment details from: {}", getFilename());

            ExcelReader xlsx = new ExcelReader(getFilename());

            // 1. Create glossaries for each row in the Glossary enrichment sheet
            GlossaryCache glossaryCache;
            try {
                List<Map<String, String>> glossaryData = xlsx.getRowsFromSheet(GLOSSARY_ENRICHMENT, 0);
                log.info("Processing sheet {}, with {} rows.", GLOSSARY_ENRICHMENT, glossaryData.size());
                Map<String, GlossaryEnrichmentDetails> glossaries = new LinkedHashMap<>();
                for (Map<String, String> row : glossaryData) {
                    GlossaryEnrichmentDetails details = GlossaryEnrichmentDetails.getFromRow(row, getDelimiter());
                    if (details != null) {
                        // Only overwrite any details about the glossary if none previously existed
                        String identity = details.getIdentity();
                        GlossaryEnrichmentDetails existing = glossaries.get(identity);
                        if (existing == null || existing.isStub()) {
                            glossaries.put(identity, details);
                        }
                    }
                }
                glossaryCache = GlossaryEnrichmentDetails.upsert(
                        glossaries, getBatchSize(), REPLACE_CLASSIFICATIONS, REPLACE_CUSTOM_METADATA, isUpdateOnly());
            } catch (IOException e) {
                log.warn("Could not find sheet: {} — skipping.", GLOSSARY_ENRICHMENT);
                glossaryCache = new GlossaryCache();
            }

            // 2. Create categories for each row in the Category enrichment sheet
            CategoryCache categoryCache = new CategoryCache();
            try {
                List<Map<String, String>> categoryData = xlsx.getRowsFromSheet(CATEGORY_ENRICHMENT, 0);
                log.info("Processing sheet {}, with {} rows.", CATEGORY_ENRICHMENT, categoryData.size());
                Map<String, CategoryEnrichmentDetails> categories = new LinkedHashMap<>();
                for (Map<String, String> row : categoryData) {
                    CategoryEnrichmentDetails details =
                            CategoryEnrichmentDetails.getFromRow(glossaryCache, row, getDelimiter());
                    if (details != null) {
                        // Only overwrite any details about the category if none previously existed
                        String identity = details.getIdentity();
                        CategoryEnrichmentDetails existing = categories.get(identity);
                        if (existing == null || existing.isStub()) {
                            categories.put(identity, details);
                        }
                    }
                }
                CategoryEnrichmentDetails.upsert(
                        categoryCache,
                        categories,
                        getBatchSize(),
                        1,
                        REPLACE_CLASSIFICATIONS,
                        REPLACE_CUSTOM_METADATA,
                        isUpdateOnly());
            } catch (IOException e) {
                log.warn("Could not find sheet: {} — skipping", CATEGORY_ENRICHMENT);
            }

            // 3. Create terms for each row in the Term enrichment sheet
            TermCache termCache;
            try {
                List<Map<String, String>> termData = xlsx.getRowsFromSheet(TERM_ENRICHMENT, 0);
                log.info("Processing sheet {}, with {} rows.", TERM_ENRICHMENT, termData.size());
                Map<String, TermEnrichmentDetails> terms = new LinkedHashMap<>();
                for (Map<String, String> row : termData) {
                    TermEnrichmentDetails details =
                            TermEnrichmentDetails.getFromRow(glossaryCache, categoryCache, row, getDelimiter());
                    if (details != null) {
                        // Only overwrite any details about the term if none previously existed
                        String identity = details.getIdentity();
                        TermEnrichmentDetails existing = terms.get(identity);
                        if (existing == null || existing.isStub()) {
                            terms.put(identity, details);
                        }
                    }
                }
                termCache = TermEnrichmentDetails.upsert(
                        terms, getBatchSize(), REPLACE_CLASSIFICATIONS, REPLACE_CUSTOM_METADATA, isUpdateOnly());
            } catch (IOException e) {
                log.warn("Could not find sheet: {} — skipping", TERM_ENRICHMENT);
                termCache = new TermCache();
            }

            // 4. Create assets for each row in the Asset enrichment sheet
            try {
                List<Map<String, String>> assetData = xlsx.getRowsFromSheet(ASSET_ENRICHMENT, 0);
                log.info("Processing sheet {}, with {} rows.", ASSET_ENRICHMENT, assetData.size());
                Map<String, AssetEnrichmentDetails> assets = new LinkedHashMap<>();
                for (Map<String, String> row : assetData) {
                    AssetEnrichmentDetails details = AssetEnrichmentDetails.getFromRow(termCache, row, getDelimiter());
                    if (details != null) {
                        // Only overwrite any details about the term if none previously existed
                        String identity = details.getIdentity();
                        AssetEnrichmentDetails existing = assets.get(identity);
                        if (existing == null || existing.isStub()) {
                            assets.put(identity, details);
                        }
                    }
                }
                AssetEnrichmentDetails.upsert(
                        assets, getBatchSize(), REPLACE_CLASSIFICATIONS, REPLACE_CUSTOM_METADATA, isUpdateOnly());
            } catch (IOException e) {
                log.warn("Could not find sheet: {} — skipping.", ASSET_ENRICHMENT);
            }

        } catch (IOException e) {
            log.error("Failed to read Excel file from: {}", getFilename(), e);
            System.exit(1);
        }

        return getFilename();
    }
}
