/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.reporters;

import static com.atlan.samples.writers.ExcelWriter.DataCell;
import static com.atlan.util.QueryFactory.*;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.atlan.Atlan;
import com.atlan.exception.AtlanException;
import com.atlan.model.assets.*;
import com.atlan.model.core.CustomMetadataAttributes;
import com.atlan.model.enums.KeywordFields;
import com.atlan.model.search.*;
import com.atlan.model.typedefs.AttributeDef;
import com.atlan.samples.writers.ExcelWriter;
import com.atlan.samples.writers.S3Writer;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
public class EnrichmentReporter extends AbstractReporter implements RequestHandler<Map<String, String>, String> {

    private static final String CM_DELIMITER = "|";

    private static Map<String, List<String>> CM_ATTRIBUTE_ORDER;
    private static Map<String, String> CM_ATTRIBUTE_HEADERS;
    private static Set<String> CM_ATTRIBUTES_FOR_SEARCH;
    private static Set<String> autoSizeSheets = new HashSet<>();

    private enum FilterType {
        BY_GROUP,
        BY_ATLAN_TAG,
        BY_PREFIX
    }

    private static FilterType FILTER_TYPE = null;
    private static List<String> ATLAN_TAG_LIST = null;
    private static String PREFIX = null;
    private static boolean INCLUDE_FIELD_LEVEL = false;
    private static boolean DIRECT_ATLAN_TAG_ONLY = false;

    private static final Map<String, String> categoryGuidToPath = new HashMap<>();
    private static final Map<String, Glossary> glossaryGuidToDetails = new HashMap<>();
    private static final Map<String, GlossaryTerm> termGuidToDetails = new HashMap<>();
    private static final Map<String, String> processed = new HashMap<>();

    static final List<String> ENRICHMENT_ATTRIBUTES = List.of(
            "name",
            "description",
            "userDescription",
            "ownerUsers",
            "ownerGroups",
            "certificateStatus",
            "certificateStatusMessage",
            "certificateUpdatedBy",
            "certificateUpdatedAt",
            "announcementType",
            "announcementTitle",
            "announcementMessage",
            "announcementUpdatedBy",
            "announcementUpdatedAt",
            "createdBy",
            "createTime",
            "updatedBy",
            "updateTime",
            "readme",
            "classificationNames",
            "links",
            "connectorName",
            "meanings");

    private static final List<String> assetTypes = List.of(
            Table.TYPE_NAME,
            View.TYPE_NAME,
            MaterializedView.TYPE_NAME,
            LookerDashboard.TYPE_NAME,
            LookerModel.TYPE_NAME,
            LookerProject.TYPE_NAME,
            LookerQuery.TYPE_NAME,
            LookerExplore.TYPE_NAME,
            LookerView.TYPE_NAME,
            MetabaseCollection.TYPE_NAME,
            MetabaseDashboard.TYPE_NAME,
            MetabaseQuestion.TYPE_NAME,
            ModeWorkspace.TYPE_NAME,
            ModeCollection.TYPE_NAME,
            ModeQuery.TYPE_NAME,
            ModeReport.TYPE_NAME,
            PowerBIWorkspace.TYPE_NAME,
            PowerBIDashboard.TYPE_NAME,
            PowerBIDataflow.TYPE_NAME,
            PowerBIDataset.TYPE_NAME,
            PowerBIDatasource.TYPE_NAME,
            PowerBIReport.TYPE_NAME,
            PowerBITable.TYPE_NAME,
            PresetWorkspace.TYPE_NAME,
            PresetDashboard.TYPE_NAME,
            DataStudioAsset.TYPE_NAME,
            ADLSAccount.TYPE_NAME,
            ADLSContainer.TYPE_NAME,
            ADLSObject.TYPE_NAME,
            GCSBucket.TYPE_NAME,
            GCSObject.TYPE_NAME,
            S3Bucket.TYPE_NAME,
            S3Object.TYPE_NAME,
            SalesforceOrganization.TYPE_NAME,
            SalesforceDashboard.TYPE_NAME,
            SalesforceReport.TYPE_NAME,
            SalesforceObject.TYPE_NAME,
            TableauSite.TYPE_NAME,
            TableauProject.TYPE_NAME,
            TableauWorkbook.TYPE_NAME,
            TableauWorksheet.TYPE_NAME,
            TableauCalculatedField.TYPE_NAME,
            TableauDashboard.TYPE_NAME,
            TableauDatasource.TYPE_NAME,
            TableauDatasourceField.TYPE_NAME);
    private static final List<String> childRelationships = List.of(
            "columns",
            "looks",
            "tiles",
            "fields",
            "explores",
            "queries",
            "models",
            "views",
            "metabaseDashboards",
            "metabaseQuestions",
            "modeCollections",
            "modeReports",
            "modeCharts",
            "modeCollections",
            "modeQueries",
            "dashboards",
            "datasets",
            "reports",
            "tables",
            "datasources",
            "dataflows",
            "pages",
            "measures",
            "presetDashboards",
            "presetDatasets",
            "presetCharts",
            "adlsObjects",
            "gcsObjects",
            "objects",
            "lookupFields",
            "projects",
            "workbooks",
            "flows",
            "worksheets",
            "datasourceFields",
            "calculatedFields");

    static final List<String> RELATION_ATTRIBUTES = List.of("name", "description");

    public static void main(String[] args) {
        EnrichmentReporter er = new EnrichmentReporter();
        Map<String, String> event = new HashMap<>(System.getenv());
        if (!event.containsKey("FILTER_BY")) {
            event.put("FILTER_BY", "GROUP");
        }
        if (!event.containsKey("DELIMITER")) {
            event.put("DELIMITER", ",");
        }
        er.handleRequest(event, null);
    }

    @Override
    protected void parseParametersFromEvent(Map<String, String> event) {
        super.parseParametersFromEvent(event);
        if (event != null) {
            setFilenameWithPrefix(event, "enrichment-report");
            String filterBy = event.getOrDefault("FILTER_BY", "GROUP");
            if (filterBy.toUpperCase(Locale.ROOT).equals("GROUP")) {
                FILTER_TYPE = FilterType.BY_GROUP;
            } else if (filterBy.toUpperCase(Locale.ROOT).equals("ATLAN_TAG")) {
                FILTER_TYPE = FilterType.BY_ATLAN_TAG;
            } else {
                FILTER_TYPE = FilterType.BY_PREFIX;
            }
            String atlanTag = event.getOrDefault("ATLAN_TAG", null);
            if (atlanTag != null && atlanTag.length() > 0) {
                ATLAN_TAG_LIST = List.of(atlanTag);
            }
            String prefix = event.getOrDefault("PREFIX", null);
            if (prefix != null && prefix.length() > 0) {
                PREFIX = prefix;
            }
            String includeFieldLevel = event.getOrDefault("INCLUDE_FIELD_LEVEL", "false");
            INCLUDE_FIELD_LEVEL = includeFieldLevel.toUpperCase(Locale.ROOT).equals("TRUE");
            String directAtlanTagsOnly = event.getOrDefault("DIRECT_ATLAN_TAGS_ONLY", "false");
            DIRECT_ATLAN_TAG_ONLY = directAtlanTagsOnly.toUpperCase(Locale.ROOT).equals("TRUE");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleRequest(Map<String, String> event, Context context) {

        try {

            log.info("Creating Excel file (in-memory)...");
            if (context != null && context.getClientContext() != null) {
                log.debug(
                        " ... client environment: {}",
                        context.getClientContext().getEnvironment());
                log.debug(" ... client custom: {}", context.getClientContext().getCustom());
            }

            parseParametersFromEvent(event);

            // Can only cache custom metadata-related heading information after we have access to
            // an environment
            getOrderedCustomMetadata();
            Map<String, String> ASSET_ENRICHMENT = createAssetEnrichmentHeader();
            Map<String, String> GLOSSARY_ENRICHMENT = createGlossaryEnrichmentHeader();
            Map<String, String> CATEGORY_ENRICHMENT = createCategoryEnrichmentHeader();
            Map<String, String> TERM_ENRICHMENT = createTermEnrichmentHeader();

            ExcelWriter xlsx = new ExcelWriter();

            // Before anything else, cache the glossaries and terms (for x-ref purposes)
            cacheGlossaries();
            cacheTerms();

            Sheet assets = xlsx.createSheet("Asset enrichment");
            autoSizeSheets.add("Asset enrichment");
            xlsx.addHeader(assets, ASSET_ENRICHMENT);
            getAssets(xlsx, assets);

            Sheet glossaries = xlsx.createSheet("Glossary enrichment");
            autoSizeSheets.add("Glossary enrichment");
            xlsx.addHeader(glossaries, GLOSSARY_ENRICHMENT);
            getGlossaries(xlsx, glossaries);

            Sheet categories = xlsx.createSheet("Category enrichment");
            autoSizeSheets.add("Category enrichment");
            xlsx.addHeader(categories, CATEGORY_ENRICHMENT);
            findCategories(xlsx, categories);

            Sheet terms = xlsx.createSheet("Term enrichment");
            autoSizeSheets.add("Term enrichment");
            xlsx.addHeader(terms, TERM_ENRICHMENT);
            getTerms(xlsx, terms);

            // If a bucket was provided, we'll write out to S3
            if (getBucket() != null) {
                S3Client s3Client = S3Client.builder().region(getRegion()).build();
                S3Writer s3 = new S3Writer(s3Client);
                s3.putExcelFile(xlsx.asByteArray(), getBucket(), getFilename());
            } else {
                // Otherwise we'll write out to a file (locally)
                log.info("Writing report to file: {}", getFilename());
                xlsx.create(getFilename(), autoSizeSheets);
            }

        } catch (AtlanException e) {
            log.error("Failed to retrieve asset details from: {}", Atlan.getBaseUrl(), e);
            System.exit(1);
        } catch (IOException e) {
            log.error("Failed to write Excel file to: {}", getFilename(), e);
            System.exit(1);
        }

        return getFilename();
    }

    void cacheGlossaries() throws AtlanException {
        log.info("Finding glossaries...");
        Query query = CompoundQuery.builder()
                .must(beActive())
                .must(beOfType(Glossary.TYPE_NAME))
                .build()
                ._toQuery();
        IndexSearchRequest request = IndexSearchRequest.builder(IndexSearchDSL.builder(query)
                        .size(getBatchSize())
                        .sortOption(Sort.by(KeywordFields.NAME))
                        .build())
                .attributes(ENRICHMENT_ATTRIBUTES)
                .attributes(CM_ATTRIBUTES_FOR_SEARCH)
                .relationAttributes(RELATION_ATTRIBUTES)
                .build();
        IndexSearchResponse response = request.search();
        List<Asset> results = response.getAssets();
        while (results != null) {
            for (Asset result : results) {
                if (result instanceof Glossary) {
                    glossaryGuidToDetails.put(result.getGuid(), (Glossary) result);
                }
            }
            response = response.getNextPage();
            results = response.getAssets();
        }
    }

    void cacheTerms() throws AtlanException {
        log.info("Finding terms...");
        Query query = CompoundQuery.builder()
                .must(beActive())
                .must(beOfType(GlossaryTerm.TYPE_NAME))
                .build()
                ._toQuery();
        IndexSearchRequest request = IndexSearchRequest.builder(IndexSearchDSL.builder(query)
                        .size(getBatchSize())
                        .sortOption(Sort.by(KeywordFields.NAME))
                        .build())
                .attributes(ENRICHMENT_ATTRIBUTES)
                .attributes(CM_ATTRIBUTES_FOR_SEARCH)
                .attribute("anchor")
                .attribute("categories")
                .attribute("seeAlso")
                .attribute("preferredTerms")
                .attribute("synonyms")
                .attribute("antonyms")
                .attribute("translatedTerms")
                .attribute("validValuesFor")
                .attribute("classifies")
                .relationAttributes(RELATION_ATTRIBUTES)
                .build();
        IndexSearchResponse response = request.search();
        List<Asset> results = response.getAssets();
        while (results != null) {
            for (Asset result : results) {
                if (result instanceof GlossaryTerm) {
                    termGuidToDetails.put(result.getGuid(), (GlossaryTerm) result);
                }
            }
            response = response.getNextPage();
            results = response.getAssets();
        }
    }

    void getAssets(ExcelWriter xlsx, Sheet sheet) throws AtlanException {
        CompoundQuery.CompoundQueryBuilder builder = CompoundQuery.builder().must(beActive());
        if (!INCLUDE_FIELD_LEVEL) {
            builder = builder.must(beOneOfTypes(assetTypes));
        }
        if (FILTER_TYPE == FilterType.BY_GROUP) {
            builder = builder.must(have(KeywordFields.OWNER_GROUPS).present());
        } else if (FILTER_TYPE == FilterType.BY_ATLAN_TAG) {
            builder = builder.must(beTaggedByAtLeastOneOf(ATLAN_TAG_LIST));
        } else if (FILTER_TYPE == FilterType.BY_PREFIX) {
            builder = builder.must(have(KeywordFields.QUALIFIED_NAME).startingWith(PREFIX));
        }
        Query query = builder.build()._toQuery();
        IndexSearchRequest request = IndexSearchRequest.builder(IndexSearchDSL.builder(query)
                        .size(getBatchSize())
                        .sortOption(Sort.by(KeywordFields.GUID, SortOrder.Asc))
                        .build())
                .attributes(ENRICHMENT_ATTRIBUTES)
                .attributes(childRelationships)
                .attributes(CM_ATTRIBUTES_FOR_SEARCH)
                .relationAttribute("description")
                .relationAttribute("userDescription")
                .build();
        log.info("Retrieving first {} asset details from: {}", getBatchSize(), Atlan.getBaseUrl());
        IndexSearchResponse response = request.search();
        List<Asset> results = response.getAssets();
        while (results != null) {
            for (Asset result : results) {
                String guid = result.getGuid();
                if (!processed.containsKey(guid)) {
                    List<Asset> childAssets = getChildAssets(result);
                    long descriptionCounts = 0;
                    for (Asset child : childAssets) {
                        String childDesc = getDescription(child);
                        descriptionCounts += childDesc.length() > 0 ? 1 : 0;
                    }
                    List<DataCell> row = new ArrayList<>();
                    row.add(DataCell.of(result.getConnectorType()));
                    row.add(DataCell.of(result.getQualifiedName()));
                    row.add(DataCell.of(result.getTypeName()));
                    row.add(DataCell.of(result.getName()));
                    row.add(DataCell.of(result.getDescription()));
                    row.add(DataCell.of(result.getUserDescription()));
                    row.add(DataCell.of(getUserOwners(result, getDelimiter())));
                    row.add(DataCell.of(getGroupOwners(result, getDelimiter())));
                    row.add(DataCell.of(result.getCertificateStatus()));
                    row.add(DataCell.of(result.getCertificateStatusMessage()));
                    row.add(DataCell.of(result.getCertificateUpdatedBy()));
                    row.add(DataCell.of(getFormattedDateTime(result.getCertificateUpdatedAt())));
                    row.add(DataCell.of(result.getAnnouncementType()));
                    row.add(DataCell.of(result.getAnnouncementTitle()));
                    row.add(DataCell.of(result.getAnnouncementMessage()));
                    row.add(DataCell.of(result.getAnnouncementUpdatedBy()));
                    row.add(DataCell.of(getFormattedDateTime(result.getAnnouncementUpdatedAt())));
                    row.add(DataCell.of(result.getCreatedBy()));
                    row.add(DataCell.of(getFormattedDateTime(result.getCreateTime())));
                    row.add(DataCell.of(result.getUpdatedBy()));
                    row.add(DataCell.of(getFormattedDateTime(result.getUpdateTime())));
                    row.add(DataCell.of(getREADME(result)));
                    row.add(DataCell.of(getTerms(result.getAssignedTerms(), termGuidToDetails)));
                    row.add(DataCell.of(getCount(result.getLinks())));
                    row.add(DataCell.of(
                            DIRECT_ATLAN_TAG_ONLY
                                    ? getDirectAtlanTags(result, getDelimiter())
                                    : getAtlanTags(result, getDelimiter())));
                    row.add(DataCell.of(childAssets.size()));
                    row.add(DataCell.of(descriptionCounts));
                    row.add(DataCell.of(getAssetLink(guid)));
                    addCustomMetadata(row, result);
                    xlsx.appendRow(sheet, row);
                    processed.put(guid, result.getQualifiedName());
                }
            }
            log.info(" retrieving next {} asset details from: {}", getBatchSize(), Atlan.getBaseUrl());
            response = response.getNextPage();
            results = response.getAssets();
        }
    }

    void getGlossaries(ExcelWriter xlsx, Sheet sheet) throws AtlanException {
        for (Glossary glossary : glossaryGuidToDetails.values()) {
            List<DataCell> row = new ArrayList<>();
            row.add(DataCell.of(glossary.getName()));
            row.add(DataCell.of(glossary.getDescription()));
            row.add(DataCell.of(glossary.getUserDescription()));
            row.add(DataCell.of(getUserOwners(glossary, getDelimiter())));
            row.add(DataCell.of(getGroupOwners(glossary, getDelimiter())));
            row.add(DataCell.of(glossary.getCertificateStatus()));
            row.add(DataCell.of(glossary.getCertificateStatusMessage()));
            row.add(DataCell.of(glossary.getCertificateUpdatedBy()));
            row.add(DataCell.of(getFormattedDateTime(glossary.getCertificateUpdatedAt())));
            row.add(DataCell.of(glossary.getAnnouncementType()));
            row.add(DataCell.of(glossary.getAnnouncementTitle()));
            row.add(DataCell.of(glossary.getAnnouncementMessage()));
            row.add(DataCell.of(glossary.getAnnouncementUpdatedBy()));
            row.add(DataCell.of(getFormattedDateTime(glossary.getAnnouncementUpdatedAt())));
            row.add(DataCell.of(glossary.getCreatedBy()));
            row.add(DataCell.of(getFormattedDateTime(glossary.getCreateTime())));
            row.add(DataCell.of(glossary.getUpdatedBy()));
            row.add(DataCell.of(getFormattedDateTime(glossary.getUpdateTime())));
            row.add(DataCell.of(getREADME(glossary)));
            row.add(DataCell.of(getCount(glossary.getLinks())));
            row.add(DataCell.of(getAssetLink(glossary.getGuid())));
            addCustomMetadata(row, glossary);
            xlsx.appendRow(sheet, row);
        }
    }

    void findCategories(ExcelWriter xlsx, Sheet sheet) throws AtlanException {
        log.info("Finding categories...");
        Query query = CompoundQuery.builder()
                .must(beActive())
                .must(beOfType(GlossaryCategory.TYPE_NAME))
                .build()
                ._toQuery();
        IndexSearchRequest request = IndexSearchRequest.builder(IndexSearchDSL.builder(query)
                        .size(getBatchSize())
                        .sortOption(Sort.by(KeywordFields.NAME))
                        .build())
                .attributes(ENRICHMENT_ATTRIBUTES)
                .attributes(CM_ATTRIBUTES_FOR_SEARCH)
                .attribute("anchor")
                .attribute("parentCategory")
                .relationAttributes(RELATION_ATTRIBUTES)
                .build();
        IndexSearchResponse response = request.search();
        List<Asset> results = response.getAssets();
        Map<String, GlossaryCategory> categoryGuidToDetails = new HashMap<>();
        while (results != null) {
            for (Asset result : results) {
                if (result instanceof GlossaryCategory) {
                    categoryGuidToDetails.put(result.getGuid(), (GlossaryCategory) result);
                }
            }
            response = response.getNextPage();
            results = response.getAssets();
        }
        for (GlossaryCategory category : categoryGuidToDetails.values()) {
            String categoryPath = getCategoryPath(category, categoryGuidToDetails);
            categoryGuidToPath.put(category.getGuid(), categoryPath);
            Glossary glossary = glossaryGuidToDetails.get(category.getAnchor().getGuid());
            List<DataCell> row = new ArrayList<>();
            row.add(DataCell.of(glossary == null ? "" : glossary.getName()));
            row.add(DataCell.of(categoryPath));
            row.add(DataCell.of(category.getDescription()));
            row.add(DataCell.of(category.getUserDescription()));
            row.add(DataCell.of(getUserOwners(category, getDelimiter())));
            row.add(DataCell.of(getGroupOwners(category, getDelimiter())));
            row.add(DataCell.of(category.getCertificateStatus()));
            row.add(DataCell.of(category.getCertificateStatusMessage()));
            row.add(DataCell.of(category.getCertificateUpdatedBy()));
            row.add(DataCell.of(getFormattedDateTime(category.getCertificateUpdatedAt())));
            row.add(DataCell.of(category.getAnnouncementType()));
            row.add(DataCell.of(category.getAnnouncementTitle()));
            row.add(DataCell.of(category.getAnnouncementMessage()));
            row.add(DataCell.of(category.getAnnouncementUpdatedBy()));
            row.add(DataCell.of(getFormattedDateTime(category.getAnnouncementUpdatedAt())));
            row.add(DataCell.of(category.getCreatedBy()));
            row.add(DataCell.of(getFormattedDateTime(category.getCreateTime())));
            row.add(DataCell.of(category.getUpdatedBy()));
            row.add(DataCell.of(getFormattedDateTime(category.getUpdateTime())));
            row.add(DataCell.of(getREADME(category)));
            row.add(DataCell.of(getCount(category.getLinks())));
            row.add(DataCell.of(getAssetLink(category.getGuid())));
            addCustomMetadata(row, category);
            xlsx.appendRow(sheet, row);
        }
    }

    static String getCategoryPath(GlossaryCategory category, Map<String, GlossaryCategory> guidMap) {
        IGlossaryCategory parent = category.getParentCategory();
        if (parent == null || parent.getGuid() == null) {
            return category.getName();
        } else {
            // Retrieve the full parent object from the map before recursing
            return getCategoryPath(guidMap.get(parent.getGuid()), guidMap) + "@" + category.getName();
        }
    }

    void getTerms(ExcelWriter xlsx, Sheet sheet) throws AtlanException {
        for (GlossaryTerm term : termGuidToDetails.values()) {
            Glossary glossary = glossaryGuidToDetails.get(term.getAnchor().getGuid());
            List<DataCell> row = new ArrayList<>();
            row.add(DataCell.of(glossary == null ? "" : glossary.getName()));
            row.add(DataCell.of(term.getName()));
            row.add(DataCell.of(term.getDescription()));
            row.add(DataCell.of(term.getUserDescription()));
            row.add(DataCell.of(getCategories(term)));
            row.add(DataCell.of(getUserOwners(term, getDelimiter())));
            row.add(DataCell.of(getGroupOwners(term, getDelimiter())));
            row.add(DataCell.of(term.getCertificateStatus()));
            row.add(DataCell.of(
                    DIRECT_ATLAN_TAG_ONLY
                            ? getDirectAtlanTags(term, getDelimiter())
                            : getAtlanTags(term, getDelimiter())));
            row.add(DataCell.of(term.getCertificateStatusMessage()));
            row.add(DataCell.of(term.getCertificateUpdatedBy()));
            row.add(DataCell.of(getFormattedDateTime(term.getCertificateUpdatedAt())));
            row.add(DataCell.of(term.getAnnouncementType()));
            row.add(DataCell.of(term.getAnnouncementTitle()));
            row.add(DataCell.of(term.getAnnouncementMessage()));
            row.add(DataCell.of(term.getAnnouncementUpdatedBy()));
            row.add(DataCell.of(getFormattedDateTime(term.getAnnouncementUpdatedAt())));
            row.add(DataCell.of(term.getCreatedBy()));
            row.add(DataCell.of(getFormattedDateTime(term.getCreateTime())));
            row.add(DataCell.of(term.getUpdatedBy()));
            row.add(DataCell.of(getFormattedDateTime(term.getUpdateTime())));
            row.add(DataCell.of(getREADME(term)));
            row.add(DataCell.of(getTerms(term.getSeeAlso(), termGuidToDetails)));
            row.add(DataCell.of(getTerms(term.getPreferredTerms(), termGuidToDetails)));
            row.add(DataCell.of(getTerms(term.getSynonyms(), termGuidToDetails)));
            row.add(DataCell.of(getTerms(term.getAntonyms(), termGuidToDetails)));
            row.add(DataCell.of(getTerms(term.getTranslatedTerms(), termGuidToDetails)));
            row.add(DataCell.of(getTerms(term.getValidValuesFor(), termGuidToDetails)));
            row.add(DataCell.of(getTerms(term.getClassifies(), termGuidToDetails)));
            row.add(DataCell.of(getCount(term.getLinks())));
            row.add(DataCell.of(getAssetLink(term.getGuid())));
            addCustomMetadata(row, term);
            xlsx.appendRow(sheet, row);
        }
    }

    String getCategories(GlossaryTerm term) {
        Set<IGlossaryCategory> categories = term.getCategories();
        List<String> categoryPaths = new ArrayList<>(categories.size());
        for (IGlossaryCategory category : categories) {
            String path = categoryGuidToPath.getOrDefault(category.getGuid(), null);
            if (path != null) {
                categoryPaths.add(path);
            }
        }
        return getDelimitedList(categoryPaths, getDelimiter());
    }

    String getTerms(Set<IGlossaryTerm> terms, Map<String, GlossaryTerm> guidMap) {
        List<String> qualifiedTerms = new ArrayList<>(terms.size());
        for (IGlossaryTerm term : terms) {
            GlossaryTerm related = guidMap.getOrDefault(term.getGuid(), null);
            if (related != null) {
                Glossary glossary =
                        glossaryGuidToDetails.get(related.getAnchor().getGuid());
                qualifiedTerms.add(related.getName() + "@" + (glossary == null ? "" : glossary.getName()));
            }
        }
        return getDelimitedList(qualifiedTerms, getDelimiter());
    }

    @SuppressWarnings("unchecked")
    private void addCustomMetadata(List<DataCell> row, Asset result) {
        Map<String, CustomMetadataAttributes> map = result.getCustomMetadataSets();
        if (map != null) {
            for (Map.Entry<String, List<String>> entry : CM_ATTRIBUTE_ORDER.entrySet()) {
                String cmName = entry.getKey();
                List<String> attrOrder = entry.getValue();
                CustomMetadataAttributes attrs = map.get(cmName);
                if (attrs != null) {
                    Map<String, Object> active = attrs.getAttributes();
                    for (String attrName : attrOrder) {
                        Object value = active.get(attrName);
                        if (value == null) {
                            row.add(DataCell.of(""));
                        } else if (value instanceof Collection) {
                            row.add(DataCell.of(getDelimitedList((Collection<String>) value, getDelimiter())));
                        } else if (value instanceof Boolean) {
                            row.add(DataCell.of((Boolean) value));
                        } else if (value instanceof Long) {
                            row.add(DataCell.of((Long) value));
                        } else if (value instanceof Double) {
                            row.add(DataCell.of((Double) value));
                        } else {
                            row.add(DataCell.of(value.toString()));
                        }
                    }
                } else {
                    // Fill in the blanks so that we retain positioning
                    for (int i = 0; i < attrOrder.size(); i++) {
                        row.add(DataCell.of(""));
                    }
                }
            }
        }
    }

    /**
     * Retrieve the child assets from the provided asset.
     *
     * @param asset from which to retrieve the child assets
     * @return the list of child assets
     */
    static List<Asset> getChildAssets(Asset asset) {
        List<Asset> childAssets = new ArrayList<>();
        String assetType = asset.getTypeName();
        switch (assetType) {
            case Table.TYPE_NAME:
                for (IColumn column : ((Table) asset).getColumns()) {
                    childAssets.add((Asset) column);
                }
                break;
            case View.TYPE_NAME:
                for (IColumn column : ((View) asset).getColumns()) {
                    childAssets.add((Asset) column);
                }
                break;
            case MaterializedView.TYPE_NAME:
                for (IColumn column : ((MaterializedView) asset).getColumns()) {
                    childAssets.add((Asset) column);
                }
                break;
            case LookerDashboard.TYPE_NAME:
                LookerDashboard dashboard = (LookerDashboard) asset;
                for (ILookerTile tile : dashboard.getTiles()) {
                    childAssets.add((Asset) tile);
                }
                for (ILookerLook look : dashboard.getLooks()) {
                    childAssets.add((Asset) look);
                }
                break;
            case LookerModel.TYPE_NAME:
                LookerModel model = (LookerModel) asset;
                for (ILookerExplore explore : model.getExplores()) {
                    childAssets.add((Asset) explore);
                }
                for (ILookerField field : model.getFields()) {
                    childAssets.add((Asset) field);
                }
                for (ILookerQuery query : model.getQueries()) {
                    childAssets.add((Asset) query);
                }
                break;
            case LookerProject.TYPE_NAME:
                LookerProject project = (LookerProject) asset;
                for (ILookerModel lModel : project.getModels()) {
                    childAssets.add((Asset) lModel);
                }
                for (ILookerExplore explore : project.getExplores()) {
                    childAssets.add((Asset) explore);
                }
                for (ILookerField field : project.getFields()) {
                    childAssets.add((Asset) field);
                }
                for (ILookerView view : project.getViews()) {
                    childAssets.add((Asset) view);
                }
                break;
            case LookerExplore.TYPE_NAME:
                for (ILookerField field : ((LookerExplore) asset).getFields()) {
                    childAssets.add((Asset) field);
                }
                break;
            case LookerQuery.TYPE_NAME:
                LookerQuery query = (LookerQuery) asset;
                for (ILookerTile tile : query.getTiles()) {
                    childAssets.add((Asset) tile);
                }
                for (ILookerLook look : query.getLooks()) {
                    childAssets.add((Asset) look);
                }
                break;
            case LookerView.TYPE_NAME:
                for (ILookerField field : ((LookerView) asset).getFields()) {
                    childAssets.add((Asset) field);
                }
                break;
            case MetabaseCollection.TYPE_NAME:
                MetabaseCollection collection = (MetabaseCollection) asset;
                for (IMetabaseDashboard mDashboard : collection.getMetabaseDashboards()) {
                    childAssets.add((Asset) mDashboard);
                }
                for (IMetabaseQuestion question : collection.getMetabaseQuestions()) {
                    childAssets.add((Asset) question);
                }
                break;
            case MetabaseDashboard.TYPE_NAME:
                for (IMetabaseQuestion question : ((MetabaseDashboard) asset).getMetabaseQuestions()) {
                    childAssets.add((Asset) question);
                }
                break;
            case MetabaseQuestion.TYPE_NAME:
                for (IMetabaseDashboard mDashboard : ((MetabaseQuestion) asset).getMetabaseDashboards()) {
                    childAssets.add((Asset) mDashboard);
                }
                break;
            case ModeWorkspace.TYPE_NAME:
                for (IModeCollection mCollection : ((ModeWorkspace) asset).getModeCollections()) {
                    childAssets.add((Asset) mCollection);
                }
                break;
            case ModeCollection.TYPE_NAME:
                for (IModeReport report : ((ModeCollection) asset).getModeReports()) {
                    childAssets.add((Asset) report);
                }
                break;
            case ModeQuery.TYPE_NAME:
                for (IModeChart chart : ((ModeQuery) asset).getModeCharts()) {
                    childAssets.add((Asset) chart);
                }
                break;
            case ModeReport.TYPE_NAME:
                ModeReport modeReport = (ModeReport) asset;
                for (IModeCollection mCollection : modeReport.getModeCollections()) {
                    childAssets.add((Asset) mCollection);
                }
                for (IModeQuery mQuery : modeReport.getModeQueries()) {
                    childAssets.add((Asset) mQuery);
                }
                break;
            case PowerBIWorkspace.TYPE_NAME:
                PowerBIWorkspace workspace = (PowerBIWorkspace) asset;
                for (IPowerBIReport report : workspace.getReports()) {
                    childAssets.add((Asset) report);
                }
                for (IPowerBIDataset dataset : workspace.getDatasets()) {
                    childAssets.add((Asset) dataset);
                }
                for (IPowerBIDashboard pDashboard : workspace.getDashboards()) {
                    childAssets.add((Asset) pDashboard);
                }
                for (IPowerBIDataflow dataflow : workspace.getDataflows()) {
                    childAssets.add((Asset) dataflow);
                }
                break;
            case PowerBIDashboard.TYPE_NAME:
                for (IPowerBITile tile : ((PowerBIDashboard) asset).getTiles()) {
                    childAssets.add((Asset) tile);
                }
                break;
            case PowerBIDataflow.TYPE_NAME:
                for (IPowerBIDataset dataset : ((PowerBIDataflow) asset).getDatasets()) {
                    childAssets.add((Asset) dataset);
                }
                break;
            case PowerBIDataset.TYPE_NAME:
                PowerBIDataset dataset = (PowerBIDataset) asset;
                for (IPowerBIReport report : dataset.getReports()) {
                    childAssets.add((Asset) report);
                }
                for (IPowerBITile tile : dataset.getTiles()) {
                    childAssets.add((Asset) tile);
                }
                for (IPowerBITable table : dataset.getTables()) {
                    childAssets.add((Asset) table);
                }
                for (IPowerBIDatasource datasource : dataset.getDatasources()) {
                    childAssets.add((Asset) datasource);
                }
                for (IPowerBIDataflow dataflow : dataset.getDataflows()) {
                    childAssets.add((Asset) dataflow);
                }
                break;
            case PowerBIDatasource.TYPE_NAME:
                for (IPowerBIDataset pDataset : ((PowerBIDatasource) asset).getDatasets()) {
                    childAssets.add((Asset) pDataset);
                }
                break;
            case PowerBIReport.TYPE_NAME:
                PowerBIReport pbiReport = (PowerBIReport) asset;
                for (IPowerBITile tile : pbiReport.getTiles()) {
                    childAssets.add((Asset) tile);
                }
                for (IPowerBIPage page : pbiReport.getPages()) {
                    childAssets.add((Asset) page);
                }
                break;
            case PowerBITable.TYPE_NAME:
                PowerBITable pbiTable = (PowerBITable) asset;
                for (IPowerBIMeasure measure : pbiTable.getMeasures()) {
                    childAssets.add((Asset) measure);
                }
                for (IPowerBIColumn column : pbiTable.getColumns()) {
                    childAssets.add((Asset) column);
                }
                break;
            case PresetWorkspace.TYPE_NAME:
                for (IPresetDashboard pDashboard : ((PresetWorkspace) asset).getPresetDashboards()) {
                    childAssets.add((Asset) pDashboard);
                }
                break;
            case PresetDashboard.TYPE_NAME:
                PresetDashboard presetDashboard = (PresetDashboard) asset;
                for (IPresetDataset pDataset : presetDashboard.getPresetDatasets()) {
                    childAssets.add((Asset) pDataset);
                }
                for (IPresetChart chart : presetDashboard.getPresetCharts()) {
                    childAssets.add((Asset) chart);
                }
                break;
            case ADLSContainer.TYPE_NAME:
                for (IADLSObject object : ((ADLSContainer) asset).getAdlsObjects()) {
                    childAssets.add((Asset) object);
                }
                break;
            case GCSBucket.TYPE_NAME:
                for (IGCSObject object : ((GCSBucket) asset).getGcsObjects()) {
                    childAssets.add((Asset) object);
                }
                break;
            case S3Bucket.TYPE_NAME:
                for (IS3Object object : ((S3Bucket) asset).getObjects()) {
                    childAssets.add((Asset) object);
                }
                break;
            case SalesforceOrganization.TYPE_NAME:
                SalesforceOrganization org = (SalesforceOrganization) asset;
                for (ISalesforceReport report : org.getReports()) {
                    childAssets.add((Asset) report);
                }
                for (ISalesforceObject object : org.getObjects()) {
                    childAssets.add((Asset) object);
                }
                for (ISalesforceDashboard sDashboard : org.getDashboards()) {
                    childAssets.add((Asset) sDashboard);
                }
                break;
            case SalesforceDashboard.TYPE_NAME:
                for (ISalesforceReport report : ((SalesforceDashboard) asset).getReports()) {
                    childAssets.add((Asset) report);
                }
                break;
            case SalesforceReport.TYPE_NAME:
                for (ISalesforceDashboard sDashboard : ((SalesforceReport) asset).getDashboards()) {
                    childAssets.add((Asset) sDashboard);
                }
                break;
            case SalesforceObject.TYPE_NAME:
                SalesforceObject object = (SalesforceObject) asset;
                for (ISalesforceField field : object.getLookupFields()) {
                    childAssets.add((Asset) field);
                }
                for (ISalesforceField field : object.getFields()) {
                    childAssets.add((Asset) field);
                }
                break;
            case TableauSite.TYPE_NAME:
                for (ITableauProject tProject : ((TableauSite) asset).getProjects()) {
                    childAssets.add((Asset) tProject);
                }
                break;
            case TableauProject.TYPE_NAME:
                TableauProject tableauProject = (TableauProject) asset;
                for (ITableauWorkbook workbook : tableauProject.getWorkbooks()) {
                    childAssets.add((Asset) workbook);
                }
                for (ITableauDatasource datasource : tableauProject.getDatasources()) {
                    childAssets.add((Asset) datasource);
                }
                for (ITableauFlow flow : tableauProject.getFlows()) {
                    childAssets.add((Asset) flow);
                }
                break;
            case TableauWorkbook.TYPE_NAME:
                TableauWorkbook tableauWorkbook = (TableauWorkbook) asset;
                for (ITableauWorksheet worksheet : tableauWorkbook.getWorksheets()) {
                    childAssets.add((Asset) worksheet);
                }
                for (ITableauDatasource datasource : tableauWorkbook.getDatasources()) {
                    childAssets.add((Asset) datasource);
                }
                for (ITableauDashboard tDashboard : tableauWorkbook.getDashboards()) {
                    childAssets.add((Asset) tDashboard);
                }
                break;
            case TableauWorksheet.TYPE_NAME:
                TableauWorksheet worksheet = (TableauWorksheet) asset;
                for (ITableauDatasourceField field : worksheet.getDatasourceFields()) {
                    childAssets.add((Asset) field);
                }
                for (ITableauCalculatedField field : worksheet.getCalculatedFields()) {
                    childAssets.add((Asset) field);
                }
                for (ITableauDashboard tDashboard : worksheet.getDashboards()) {
                    childAssets.add((Asset) tDashboard);
                }
                break;
            case TableauCalculatedField.TYPE_NAME:
                for (ITableauWorksheet tWorksheet : ((TableauCalculatedField) asset).getWorksheets()) {
                    childAssets.add((Asset) tWorksheet);
                }
                break;
            case TableauDashboard.TYPE_NAME:
                for (ITableauWorksheet tWorksheet : ((TableauDashboard) asset).getWorksheets()) {
                    childAssets.add((Asset) tWorksheet);
                }
                break;
            case TableauDatasource.TYPE_NAME:
                for (ITableauField field : ((TableauDatasource) asset).getFields()) {
                    childAssets.add((Asset) field);
                }
                break;
            case TableauDatasourceField.TYPE_NAME:
                for (ITableauWorksheet tWorksheet : ((TableauDatasourceField) asset).getWorksheets()) {
                    childAssets.add((Asset) tWorksheet);
                }
                break;
            case DataStudioAsset.TYPE_NAME:
            default:
                childAssets = Collections.emptyList();
                break;
        }
        return childAssets;
    }

    static void getOrderedCustomMetadata() {
        CM_ATTRIBUTE_ORDER = new LinkedHashMap<>();
        CM_ATTRIBUTE_HEADERS = new LinkedHashMap<>();
        CM_ATTRIBUTES_FOR_SEARCH = new HashSet<>();
        try {
            Map<String, List<AttributeDef>> allAttrs =
                    Atlan.getDefaultClient().getCustomMetadataCache().getAllCustomAttributes();
            List<String> sortedNames = allAttrs.keySet().stream().sorted().collect(Collectors.toList());
            for (String cmName : sortedNames) {
                CM_ATTRIBUTES_FOR_SEARCH.addAll(
                        Atlan.getDefaultClient().getCustomMetadataCache().getAttributesForSearchResults(cmName));
                List<AttributeDef> attrs = allAttrs.get(cmName);
                List<String> attrNames = new ArrayList<>();
                for (AttributeDef attr : attrs) {
                    String attrName = attr.getDisplayName();
                    attrNames.add(attrName);
                    boolean multiValued = attr.getOptions().getMultiValueSelect() != null
                            && attr.getOptions().getMultiValueSelect();
                    if (multiValued) {
                        CM_ATTRIBUTE_HEADERS.put(
                                cmName + CM_DELIMITER + attrName, "Comma-separated list of " + attr.getDescription());
                    } else {
                        CM_ATTRIBUTE_HEADERS.put(cmName + CM_DELIMITER + attrName, attr.getDescription());
                    }
                }
                CM_ATTRIBUTE_ORDER.put(cmName, attrNames);
            }
        } catch (AtlanException e) {
            log.error("Unable to retrieve custom metadata definitions.", e);
        }
    }

    static Map<String, String> createAssetEnrichmentHeader() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Connector", "Type of the data source");
        map.put("Qualified name", "Unique name of the asset");
        map.put("Type", "Type of asset");
        map.put("Name", "Name of the asset");
        map.put("Description", "Explanation of the asset");
        map.put("User Description", "Explanation of the asset, as provided by a user in the UI");
        map.put("Owner Users", "Comma-separated list of the usernames who are owners of this asset");
        map.put("Owner Groups", "Comma-separated list of the group names who are owners of this asset");
        map.put("Certificate", "Certificate associated with this asset, one of: Verified, Draft, Deprecated");
        map.put("Certificate Message", "Message associated with the certificate (if any)");
        map.put("Certificate Updated By", "User who last updated the certificate");
        map.put("Certificate Updated At", "Date and time when the certificate was last updated");
        map.put("Announcement", "Type of announcement associated with this asset");
        map.put("Announcement Title", "Title of the announcement");
        map.put("Announcement Message", "Message associated with the announcement (if any)");
        map.put("Announcement Updated By", "User who last updated the announcement");
        map.put("Announcement Updated At", "Date and time when the announcement was last updated");
        map.put("Created By", "User who created this asset");
        map.put("Created At", "Date and time when this asset was created");
        map.put("Updated By", "User who last updated this asset");
        map.put("Updated At", "Date and time when the asset was last updated");
        map.put("README", "README contents for this asset (as HTML)");
        map.put("Assigned Terms", "Terms that have been linked to the asset");
        map.put("Resources", "Count of resources (links) associated with the asset");
        map.put(
                "Atlan Tags",
                "Comma-separated list of the names of Atlan tags applied to the asset, if any (blank means no Atlan tags)");
        map.put("Children", "Count of children of this asset (for example, columns in a table)");
        map.put(
                "Children with descriptions",
                "Count of children of this asset with a description present, whether system-provided or user-provided");
        map.put("Link", "Link to the detailed asset within Atlan");
        map.putAll(CM_ATTRIBUTE_HEADERS);
        return map;
    }

    static Map<String, String> createGlossaryEnrichmentHeader() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Glossary Name", "Name of the glossary");
        map.put("Description", "Explanation of the glossary's contained terminology");
        map.put("User Description", "Explanation of the glossary's meaning, as provided by a user in the UI");
        map.put("Owner Users", "Comma-separated list of the usernames who are owners of this glossary");
        map.put("Owner Groups", "Comma-separated list of the group names who are owners of this glossary");
        map.put("Certificate", "Certificate associated with this glossary, one of: Verified, Draft, Deprecated");
        map.put("Certificate Message", "Message associated with the certificate (if any)");
        map.put("Certificate Updated By", "User who last updated the certificate");
        map.put("Certificate Updated At", "Date and time when the certificate was last updated");
        map.put("Announcement", "Type of announcement associated with this glossary");
        map.put("Announcement Title", "Title of the announcement");
        map.put("Announcement Message", "Message associated with the announcement (if any)");
        map.put("Announcement Updated By", "User who last updated the announcement");
        map.put("Announcement Updated At", "Date and time when the announcement was last updated");
        map.put("Created By", "User who created this glossary");
        map.put("Created At", "Date and time when this glossary was created");
        map.put("Updated By", "User who last updated this glossary");
        map.put("Updated At", "Date and time when the glossary was last updated");
        map.put("README", "README contents for this glossary (as HTML)");
        map.put("Resources", "Count of resources (links) associated with the glossary");
        map.put("Link", "Link to the detailed glossary within Atlan");
        map.putAll(CM_ATTRIBUTE_HEADERS);
        return map;
    }

    static Map<String, String> createCategoryEnrichmentHeader() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Glossary Name", "Name of the glossary in which the category exists");
        map.put("Category Path", "Path of the category, separated by '@'");
        map.put("Description", "Explanation of the category's meaning");
        map.put("User Description", "Explanation of the category's meaning, as provided by a user in the UI");
        map.put("Owner Users", "Comma-separated list of the usernames who are owners of this term");
        map.put("Owner Groups", "Comma-separated list of the group names who are owners of this term");
        map.put("Certificate", "Certificate associated with this term, one of: Verified, Draft, Deprecated");
        map.put("Certificate Message", "Message associated with the certificate (if any)");
        map.put("Certificate Updated By", "User who last updated the certificate");
        map.put("Certificate Updated At", "Date and time when the certificate was last updated");
        map.put("Announcement", "Type of announcement associated with this category");
        map.put("Announcement Title", "Title of the announcement");
        map.put("Announcement Message", "Message associated with the announcement (if any)");
        map.put("Announcement Updated By", "User who last updated the announcement");
        map.put("Announcement Updated At", "Date and time when the announcement was last updated");
        map.put("Created By", "User who created this category");
        map.put("Created At", "Date and time when this category was created");
        map.put("Updated By", "User who last updated this category");
        map.put("Updated At", "Date and time when the category was last updated");
        map.put("README", "README contents for this category (as HTML)");
        map.put("Resources", "Count of resources (links) associated with the category");
        map.put("Link", "Link to the detailed category within Atlan");
        map.putAll(CM_ATTRIBUTE_HEADERS);
        return map;
    }

    static Map<String, String> createTermEnrichmentHeader() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Glossary Name", "Name of the glossary in which the term exists");
        map.put("Term Name*", "Name of the term, which cannot include '@'");
        map.put("Description", "Explanation of the term's meaning");
        map.put("User Description", "Explanation of the term's meaning, as provided by a user in the UI");
        map.put("Categories", "Comma-separated list of categories the term is organized within");
        map.put("Owner Users", "Comma-separated list of the usernames who are owners of this term");
        map.put("Owner Groups", "Comma-separated list of the group names who are owners of this term");
        map.put("Certificate", "Certificate associated with this term, one of: Verified, Draft, Deprecated");
        map.put("Atlan Tags", "Comma-separated list of the Atlan tags associated with this term");
        map.put("Certificate Message", "Message associated with the certificate (if any)");
        map.put("Certificate Updated By", "User who last updated the certificate");
        map.put("Certificate Updated At", "Date and time when the certificate was last updated");
        map.put("Announcement", "Type of announcement associated with this term");
        map.put("Announcement Title", "Title of the announcement");
        map.put("Announcement Message", "Message associated with the announcement (if any)");
        map.put("Announcement Updated By", "User who last updated the announcement");
        map.put("Announcement Updated At", "Date and time when the announcement was last updated");
        map.put("Created By", "User who created this term");
        map.put("Created At", "Date and time when this term was created");
        map.put("Updated By", "User who last updated this term");
        map.put("Updated At", "Date and time when the term was last updated");
        map.put("README", "README contents for this term (as HTML)");
        map.put("Related Terms", "Comma-separated list of this term's related terms");
        map.put("Recommended Terms", "Comma-separated list of this term's recommended terms");
        map.put("Synonyms", "Comma-separated list of this term's synonyms");
        map.put("Antonyms", "Comma-separated list of this term's antonyms");
        map.put("Translated Terms", "Comma-separated list of this term's translated terms");
        map.put("Valid Values For", "Comma-separated list of the terms this term is a valid value for");
        map.put("Classifies", "Comma-separated list of the terms this term classifies");
        map.put("Resources", "Count of resources (links) associated with the term");
        map.put("Link", "Link to the detailed term within Atlan");
        map.putAll(CM_ATTRIBUTE_HEADERS);
        return map;
    }
}
