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
import com.atlan.model.enums.KeywordFields;
import com.atlan.model.search.*;
import com.atlan.samples.writers.ExcelWriter;
import com.atlan.samples.writers.S3Writer;
import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
public class EnrichmentReporter extends AbstractReporter implements RequestHandler<Map<String, String>, String> {

    private static final LinkedHashMap<String, String> ASSET_ENRICHMENT = createAssetEnrichmentHeader();
    private static final LinkedHashMap<String, String> GLOSSARY_ENRICHMENT = createGlossaryEnrichmentHeader();
    private static final LinkedHashMap<String, String> CATEGORY_ENRICHMENT = createCategoryEnrichmentHeader();
    private static final LinkedHashMap<String, String> TERM_ENRICHMENT = createTermEnrichmentHeader();
    private static final Set<String> autoSizeSheets = new HashSet<>();

    private enum FilterType {
        BY_GROUP,
        BY_CLASSIFICATION,
        BY_PREFIX
    }

    private static FilterType FILTER_TYPE = null;
    private static List<String> CLASSIFICATION_LIST = null;
    private static String PREFIX = null;
    private static boolean INCLUDE_FIELD_LEVEL = false;
    private static boolean DIRECT_CLASSIFICATIONS_ONLY = false;

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
            if (filterBy.toUpperCase().equals("GROUP")) {
                FILTER_TYPE = FilterType.BY_GROUP;
            } else if (filterBy.toUpperCase().equals("CLASSIFICATION")) {
                FILTER_TYPE = FilterType.BY_CLASSIFICATION;
            } else {
                FILTER_TYPE = FilterType.BY_PREFIX;
            }
            String classification = event.getOrDefault("CLASSIFICATION", null);
            if (classification != null && classification.length() > 0) {
                CLASSIFICATION_LIST = List.of(classification);
            }
            String prefix = event.getOrDefault("PREFIX", null);
            if (prefix != null && prefix.length() > 0) {
                PREFIX = prefix;
            }
            String includeFieldLevel = event.getOrDefault("INCLUDE_FIELD_LEVEL", "false");
            INCLUDE_FIELD_LEVEL = includeFieldLevel.toUpperCase().equals("TRUE");
            String directClassificationsOnly = event.getOrDefault("DIRECT_CLASSIFICATIONS_ONLY", "false");
            DIRECT_CLASSIFICATIONS_ONLY =
                    directClassificationsOnly.toUpperCase().equals("TRUE");
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
            log.error("Failed to retrieve asset details from: {}", Atlan.getBaseUrlSafe(), e);
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
        IndexSearchRequest request = IndexSearchRequest.builder()
                .dsl(IndexSearchDSL.builder()
                        .from(0)
                        .size(getBatchSize())
                        .query(query)
                        .sortOption(Sort.by(KeywordFields.NAME))
                        .build())
                .attributes(ENRICHMENT_ATTRIBUTES)
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
        IndexSearchRequest request = IndexSearchRequest.builder()
                .dsl(IndexSearchDSL.builder()
                        .from(0)
                        .size(getBatchSize())
                        .query(query)
                        .sortOption(Sort.by(KeywordFields.NAME))
                        .build())
                .attributes(ENRICHMENT_ATTRIBUTES)
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
        } else if (FILTER_TYPE == FilterType.BY_CLASSIFICATION) {
            builder = builder.must(beClassifiedByAtLeastOneOf(CLASSIFICATION_LIST));
        } else if (FILTER_TYPE == FilterType.BY_PREFIX) {
            builder = builder.must(have(KeywordFields.QUALIFIED_NAME).startingWith(PREFIX));
        }
        Query query = builder.build()._toQuery();
        IndexSearchRequest request = IndexSearchRequest.builder()
                .dsl(IndexSearchDSL.builder()
                        .from(0)
                        .size(getBatchSize())
                        .query(query)
                        .sortOption(Sort.by(KeywordFields.GUID, SortOrder.Asc))
                        .build())
                .attributes(ENRICHMENT_ATTRIBUTES)
                .attributes(childRelationships)
                .relationAttribute("description")
                .relationAttribute("userDescription")
                .build();
        log.info("Retrieving first {} asset details from: {}", getBatchSize(), Atlan.getBaseUrlSafe());
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
                    xlsx.appendRow(
                            sheet,
                            List.of(
                                    DataCell.of(result.getConnectorType()),
                                    DataCell.of(result.getQualifiedName()),
                                    DataCell.of(result.getTypeName()),
                                    DataCell.of(result.getName()),
                                    DataCell.of(result.getDescription()),
                                    DataCell.of(result.getUserDescription()),
                                    DataCell.of(getUserOwners(result, getDelimiter())),
                                    DataCell.of(getGroupOwners(result, getDelimiter())),
                                    DataCell.of(result.getCertificateStatus()),
                                    DataCell.of(result.getCertificateStatusMessage()),
                                    DataCell.of(result.getCertificateUpdatedBy()),
                                    DataCell.of(getFormattedDateTime(result.getCertificateUpdatedAt())),
                                    DataCell.of(result.getAnnouncementType()),
                                    DataCell.of(result.getAnnouncementTitle()),
                                    DataCell.of(result.getAnnouncementMessage()),
                                    DataCell.of(result.getAnnouncementUpdatedBy()),
                                    DataCell.of(getFormattedDateTime(result.getAnnouncementUpdatedAt())),
                                    DataCell.of(result.getCreatedBy()),
                                    DataCell.of(getFormattedDateTime(result.getCreateTime())),
                                    DataCell.of(result.getUpdatedBy()),
                                    DataCell.of(getFormattedDateTime(result.getUpdateTime())),
                                    DataCell.of(getREADME(result)),
                                    DataCell.of(getTerms(result.getAssignedTerms(), termGuidToDetails)),
                                    DataCell.of(getCount(result.getLinks())),
                                    DataCell.of(
                                            DIRECT_CLASSIFICATIONS_ONLY
                                                    ? getDirectClassifications(result, getDelimiter())
                                                    : getClassifications(result, getDelimiter())),
                                    DataCell.of(childAssets.size()),
                                    DataCell.of(descriptionCounts),
                                    DataCell.of(getAssetLink(guid))));
                    processed.put(guid, result.getQualifiedName());
                }
            }
            log.info(" retrieving next {} asset details from: {}", getBatchSize(), Atlan.getBaseUrlSafe());
            response = response.getNextPage();
            results = response.getAssets();
        }
    }

    void getGlossaries(ExcelWriter xlsx, Sheet sheet) throws AtlanException {
        for (Glossary glossary : glossaryGuidToDetails.values()) {
            xlsx.appendRow(
                    sheet,
                    List.of(
                            DataCell.of(glossary.getName()),
                            DataCell.of(glossary.getDescription()),
                            DataCell.of(glossary.getUserDescription()),
                            DataCell.of(getUserOwners(glossary, getDelimiter())),
                            DataCell.of(getGroupOwners(glossary, getDelimiter())),
                            DataCell.of(glossary.getCertificateStatus()),
                            DataCell.of(glossary.getCertificateStatusMessage()),
                            DataCell.of(glossary.getCertificateUpdatedBy()),
                            DataCell.of(getFormattedDateTime(glossary.getCertificateUpdatedAt())),
                            DataCell.of(glossary.getAnnouncementType()),
                            DataCell.of(glossary.getAnnouncementTitle()),
                            DataCell.of(glossary.getAnnouncementMessage()),
                            DataCell.of(glossary.getAnnouncementUpdatedBy()),
                            DataCell.of(getFormattedDateTime(glossary.getAnnouncementUpdatedAt())),
                            DataCell.of(glossary.getCreatedBy()),
                            DataCell.of(getFormattedDateTime(glossary.getCreateTime())),
                            DataCell.of(glossary.getUpdatedBy()),
                            DataCell.of(getFormattedDateTime(glossary.getUpdateTime())),
                            DataCell.of(getREADME(glossary)),
                            DataCell.of(getCount(glossary.getLinks())),
                            DataCell.of(getAssetLink(glossary.getGuid()))));
        }
    }

    void findCategories(ExcelWriter xlsx, Sheet sheet) throws AtlanException {
        log.info("Finding categories...");
        Query query = CompoundQuery.builder()
                .must(beActive())
                .must(beOfType(GlossaryCategory.TYPE_NAME))
                .build()
                ._toQuery();
        IndexSearchRequest request = IndexSearchRequest.builder()
                .dsl(IndexSearchDSL.builder()
                        .from(0)
                        .size(getBatchSize())
                        .query(query)
                        .sortOption(Sort.by(KeywordFields.NAME))
                        .build())
                .attributes(ENRICHMENT_ATTRIBUTES)
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
            xlsx.appendRow(
                    sheet,
                    List.of(
                            DataCell.of(glossary == null ? "" : glossary.getName()),
                            DataCell.of(categoryPath),
                            DataCell.of(category.getDescription()),
                            DataCell.of(category.getUserDescription()),
                            DataCell.of(getUserOwners(category, getDelimiter())),
                            DataCell.of(getGroupOwners(category, getDelimiter())),
                            DataCell.of(category.getCertificateStatus()),
                            DataCell.of(category.getCertificateStatusMessage()),
                            DataCell.of(category.getCertificateUpdatedBy()),
                            DataCell.of(getFormattedDateTime(category.getCertificateUpdatedAt())),
                            DataCell.of(category.getAnnouncementType()),
                            DataCell.of(category.getAnnouncementTitle()),
                            DataCell.of(category.getAnnouncementMessage()),
                            DataCell.of(category.getAnnouncementUpdatedBy()),
                            DataCell.of(getFormattedDateTime(category.getAnnouncementUpdatedAt())),
                            DataCell.of(category.getCreatedBy()),
                            DataCell.of(getFormattedDateTime(category.getCreateTime())),
                            DataCell.of(category.getUpdatedBy()),
                            DataCell.of(getFormattedDateTime(category.getUpdateTime())),
                            DataCell.of(getREADME(category)),
                            DataCell.of(getCount(category.getLinks())),
                            DataCell.of(getAssetLink(category.getGuid()))));
        }
    }

    static String getCategoryPath(GlossaryCategory category, Map<String, GlossaryCategory> guidMap) {
        GlossaryCategory parent = category.getParentCategory();
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
            xlsx.appendRow(
                    sheet,
                    List.of(
                            DataCell.of(glossary == null ? "" : glossary.getName()),
                            DataCell.of(term.getName()),
                            DataCell.of(term.getDescription()),
                            DataCell.of(term.getUserDescription()),
                            DataCell.of(getCategories(term)),
                            DataCell.of(getUserOwners(term, getDelimiter())),
                            DataCell.of(getGroupOwners(term, getDelimiter())),
                            DataCell.of(term.getCertificateStatus()),
                            DataCell.of(
                                    DIRECT_CLASSIFICATIONS_ONLY
                                            ? getDirectClassifications(term, getDelimiter())
                                            : getClassifications(term, getDelimiter())),
                            DataCell.of(term.getCertificateStatusMessage()),
                            DataCell.of(term.getCertificateUpdatedBy()),
                            DataCell.of(getFormattedDateTime(term.getCertificateUpdatedAt())),
                            DataCell.of(term.getAnnouncementType()),
                            DataCell.of(term.getAnnouncementTitle()),
                            DataCell.of(term.getAnnouncementMessage()),
                            DataCell.of(term.getAnnouncementUpdatedBy()),
                            DataCell.of(getFormattedDateTime(term.getAnnouncementUpdatedAt())),
                            DataCell.of(term.getCreatedBy()),
                            DataCell.of(getFormattedDateTime(term.getCreateTime())),
                            DataCell.of(term.getUpdatedBy()),
                            DataCell.of(getFormattedDateTime(term.getUpdateTime())),
                            DataCell.of(getREADME(term)),
                            DataCell.of(getTerms(term.getSeeAlso(), termGuidToDetails)),
                            DataCell.of(getTerms(term.getPreferredTerms(), termGuidToDetails)),
                            DataCell.of(getTerms(term.getSynonyms(), termGuidToDetails)),
                            DataCell.of(getTerms(term.getAntonyms(), termGuidToDetails)),
                            DataCell.of(getTerms(term.getTranslatedTerms(), termGuidToDetails)),
                            DataCell.of(getTerms(term.getValidValuesFor(), termGuidToDetails)),
                            DataCell.of(getTerms(term.getClassifies(), termGuidToDetails)),
                            DataCell.of(getCount(term.getLinks())),
                            DataCell.of(getAssetLink(term.getGuid()))));
        }
    }

    String getCategories(GlossaryTerm term) {
        Set<GlossaryCategory> categories = term.getCategories();
        List<String> categoryPaths = new ArrayList<>(categories.size());
        for (GlossaryCategory category : categories) {
            String path = categoryGuidToPath.getOrDefault(category.getGuid(), null);
            if (path != null) {
                categoryPaths.add(path);
            }
        }
        return getDelimitedList(categoryPaths, getDelimiter());
    }

    String getTerms(Set<GlossaryTerm> terms, Map<String, GlossaryTerm> guidMap) {
        List<String> qualifiedTerms = new ArrayList<>(terms.size());
        for (GlossaryTerm term : terms) {
            GlossaryTerm related = guidMap.getOrDefault(term.getGuid(), null);
            if (related != null) {
                Glossary glossary =
                        glossaryGuidToDetails.get(related.getAnchor().getGuid());
                qualifiedTerms.add(related.getName() + "@" + (glossary == null ? "" : glossary.getName()));
            }
        }
        return getDelimitedList(qualifiedTerms, getDelimiter());
    }

    /**
     * Retrieve the child assets from the provided asset.
     *
     * @param asset from which to retrieve the child assets
     * @return the list of child assets
     */
    static List<Asset> getChildAssets(Asset asset) {
        List<Asset> childAssets;
        String assetType = asset.getTypeName();
        switch (assetType) {
            case Table.TYPE_NAME:
                childAssets = new ArrayList<>(((Table) asset).getColumns());
                break;
            case View.TYPE_NAME:
                childAssets = new ArrayList<>(((View) asset).getColumns());
                break;
            case MaterializedView.TYPE_NAME:
                childAssets = new ArrayList<>(((MaterializedView) asset).getColumns());
                break;
            case LookerDashboard.TYPE_NAME:
                LookerDashboard dashboard = (LookerDashboard) asset;
                childAssets = new ArrayList<>(dashboard.getTiles());
                childAssets.addAll(dashboard.getLooks());
                break;
            case LookerModel.TYPE_NAME:
                LookerModel model = (LookerModel) asset;
                childAssets = new ArrayList<>(model.getExplores());
                childAssets.addAll(model.getFields());
                childAssets.addAll(model.getQueries());
                break;
            case LookerProject.TYPE_NAME:
                LookerProject project = (LookerProject) asset;
                childAssets = new ArrayList<>(project.getModels());
                childAssets.addAll(project.getExplores());
                childAssets.addAll(project.getFields());
                childAssets.addAll(project.getViews());
                break;
            case LookerExplore.TYPE_NAME:
                childAssets = new ArrayList<>(((LookerExplore) asset).getFields());
                break;
            case LookerQuery.TYPE_NAME:
                LookerQuery query = (LookerQuery) asset;
                childAssets = new ArrayList<>(query.getTiles());
                childAssets.addAll(query.getLooks());
                break;
            case LookerView.TYPE_NAME:
                childAssets = new ArrayList<>(((LookerView) asset).getFields());
                break;
            case MetabaseCollection.TYPE_NAME:
                MetabaseCollection collection = (MetabaseCollection) asset;
                childAssets = new ArrayList<>(collection.getMetabaseDashboards());
                childAssets.addAll(collection.getMetabaseQuestions());
                break;
            case MetabaseDashboard.TYPE_NAME:
                childAssets = new ArrayList<>(((MetabaseDashboard) asset).getMetabaseQuestions());
                break;
            case MetabaseQuestion.TYPE_NAME:
                childAssets = new ArrayList<>(((MetabaseQuestion) asset).getMetabaseDashboards());
                break;
            case ModeWorkspace.TYPE_NAME:
                childAssets = new ArrayList<>(((ModeWorkspace) asset).getModeCollections());
                break;
            case ModeCollection.TYPE_NAME:
                childAssets = new ArrayList<>(((ModeCollection) asset).getModeReports());
                break;
            case ModeQuery.TYPE_NAME:
                childAssets = new ArrayList<>(((ModeQuery) asset).getModeCharts());
                break;
            case ModeReport.TYPE_NAME:
                ModeReport modeReport = (ModeReport) asset;
                childAssets = new ArrayList<>(modeReport.getModeCollections());
                childAssets.addAll(modeReport.getModeQueries());
                break;
            case PowerBIWorkspace.TYPE_NAME:
                PowerBIWorkspace workspace = (PowerBIWorkspace) asset;
                childAssets = new ArrayList<>(workspace.getReports());
                childAssets.addAll(workspace.getDatasets());
                childAssets.addAll(workspace.getDashboards());
                childAssets.addAll(workspace.getDataflows());
                break;
            case PowerBIDashboard.TYPE_NAME:
                childAssets = new ArrayList<>(((PowerBIDashboard) asset).getTiles());
                break;
            case PowerBIDataflow.TYPE_NAME:
                childAssets = new ArrayList<>(((PowerBIDataflow) asset).getDatasets());
                break;
            case PowerBIDataset.TYPE_NAME:
                PowerBIDataset dataset = (PowerBIDataset) asset;
                childAssets = new ArrayList<>(dataset.getReports());
                childAssets.addAll(dataset.getTiles());
                childAssets.addAll(dataset.getTables());
                childAssets.addAll(dataset.getDatasources());
                childAssets.addAll(dataset.getDataflows());
                break;
            case PowerBIDatasource.TYPE_NAME:
                childAssets = new ArrayList<>(((PowerBIDatasource) asset).getDatasets());
                break;
            case PowerBIReport.TYPE_NAME:
                PowerBIReport pbiReport = (PowerBIReport) asset;
                childAssets = new ArrayList<>(pbiReport.getTiles());
                childAssets.addAll(pbiReport.getPages());
                break;
            case PowerBITable.TYPE_NAME:
                PowerBITable pbiTable = (PowerBITable) asset;
                childAssets = new ArrayList<>(pbiTable.getMeasures());
                childAssets.addAll(pbiTable.getColumns());
                break;
            case PresetWorkspace.TYPE_NAME:
                childAssets = new ArrayList<>(((PresetWorkspace) asset).getPresetDashboards());
                break;
            case PresetDashboard.TYPE_NAME:
                PresetDashboard presetDashboard = (PresetDashboard) asset;
                childAssets = new ArrayList<>(presetDashboard.getPresetDatasets());
                childAssets.addAll(presetDashboard.getPresetCharts());
                break;
            case ADLSContainer.TYPE_NAME:
                childAssets = new ArrayList<>(((ADLSContainer) asset).getAdlsObjects());
                break;
            case GCSBucket.TYPE_NAME:
                childAssets = new ArrayList<>(((GCSBucket) asset).getGcsObjects());
                break;
            case S3Bucket.TYPE_NAME:
                childAssets = new ArrayList<>(((S3Bucket) asset).getObjects());
                break;
            case SalesforceOrganization.TYPE_NAME:
                SalesforceOrganization org = (SalesforceOrganization) asset;
                childAssets = new ArrayList<>(org.getReports());
                childAssets.addAll(org.getObjects());
                childAssets.addAll(org.getDashboards());
                break;
            case SalesforceDashboard.TYPE_NAME:
                childAssets = new ArrayList<>(((SalesforceDashboard) asset).getReports());
                break;
            case SalesforceReport.TYPE_NAME:
                childAssets = new ArrayList<>(((SalesforceReport) asset).getDashboards());
                break;
            case SalesforceObject.TYPE_NAME:
                SalesforceObject object = (SalesforceObject) asset;
                childAssets = new ArrayList<>(object.getLookupFields());
                childAssets.addAll(object.getFields());
                break;
            case TableauSite.TYPE_NAME:
                childAssets = new ArrayList<>(((TableauSite) asset).getProjects());
                break;
            case TableauProject.TYPE_NAME:
                TableauProject tableauProject = (TableauProject) asset;
                childAssets = new ArrayList<>(tableauProject.getWorkbooks());
                childAssets.addAll(tableauProject.getDatasources());
                childAssets.addAll(tableauProject.getFlows());
                break;
            case TableauWorkbook.TYPE_NAME:
                TableauWorkbook tableauWorkbook = (TableauWorkbook) asset;
                childAssets = new ArrayList<>(tableauWorkbook.getWorksheets());
                childAssets.addAll(tableauWorkbook.getDatasources());
                childAssets.addAll(tableauWorkbook.getDashboards());
                break;
            case TableauWorksheet.TYPE_NAME:
                TableauWorksheet worksheet = (TableauWorksheet) asset;
                childAssets = new ArrayList<>(worksheet.getDatasourceFields());
                childAssets.addAll(worksheet.getCalculatedFields());
                childAssets.addAll(worksheet.getDashboards());
                break;
            case TableauCalculatedField.TYPE_NAME:
                childAssets = new ArrayList<>(((TableauCalculatedField) asset).getWorksheets());
                break;
            case TableauDashboard.TYPE_NAME:
                childAssets = new ArrayList<>(((TableauDashboard) asset).getWorksheets());
                break;
            case TableauDatasource.TYPE_NAME:
                childAssets = new ArrayList<>(((TableauDatasource) asset).getFields());
                break;
            case TableauDatasourceField.TYPE_NAME:
                childAssets = new ArrayList<>(((TableauDatasourceField) asset).getWorksheets());
                break;
            case DataStudioAsset.TYPE_NAME:
            default:
                childAssets = Collections.emptyList();
                break;
        }
        return childAssets;
    }

    static LinkedHashMap<String, String> createAssetEnrichmentHeader() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("Connector", "Type of the data source");
        map.put("Qualified name", "Unique name of the asset");
        map.put("Type", "Type of asset");
        map.put("Name", "Name of the asset");
        map.put("Description", "Explanation of the asset");
        map.put("User Description", "Explanation of the asset, as provided by a user in the UI");
        map.put("User Owners", "Comma-separated list of the usernames who are owners of this asset");
        map.put("Group Owners", "Comma-separated list of the group names who are owners of this asset");
        map.put("Certification", "Certificate associated with this asset, one of: Verified, Draft, Deprecated");
        map.put("Certification Message", "Message associated with the certificate (if any)");
        map.put("Certification Updated By", "User who last updated the certificate");
        map.put("Certification Updated At", "Date and time when the certificate was last updated");
        map.put("Announcement Type", "Type of announcement associated with this asset");
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
                "Classifications",
                "Comma-separated list of the names of classifications applied to the asset, if any (blank means no classifications)");
        map.put("Children", "Count of children of this asset (for example, columns in a table)");
        map.put(
                "Children with descriptions",
                "Count of children of this asset with a description present, whether system-provided or user-provided");
        map.put("Link", "Link to the detailed asset within Atlan");
        return map;
    }

    static LinkedHashMap<String, String> createGlossaryEnrichmentHeader() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("Glossary Name", "Name of the glossary");
        map.put("Description", "Explanation of the glossary's contained terminology");
        map.put("User Description", "Explanation of the glossary's meaning, as provided by a user in the UI");
        map.put("User Owners", "Comma-separated list of the usernames who are owners of this glossary");
        map.put("Group Owners", "Comma-separated list of the group names who are owners of this glossary");
        map.put("Certification", "Certificate associated with this glossary, one of: Verified, Draft, Deprecated");
        map.put("Certification Message", "Message associated with the certificate (if any)");
        map.put("Certification Updated By", "User who last updated the certificate");
        map.put("Certification Updated At", "Date and time when the certificate was last updated");
        map.put("Announcement Type", "Type of announcement associated with this glossary");
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
        return map;
    }

    static LinkedHashMap<String, String> createCategoryEnrichmentHeader() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("Glossary Name", "Name of the glossary in which the category exists");
        map.put("Category Path", "Path of the category, separated by '@'");
        map.put("Description", "Explanation of the category's meaning");
        map.put("User Description", "Explanation of the category's meaning, as provided by a user in the UI");
        map.put("User Owners", "Comma-separated list of the usernames who are owners of this term");
        map.put("Group Owners", "Comma-separated list of the group names who are owners of this term");
        map.put("Certification", "Certificate associated with this term, one of: Verified, Draft, Deprecated");
        map.put("Certification Message", "Message associated with the certificate (if any)");
        map.put("Certification Updated By", "User who last updated the certificate");
        map.put("Certification Updated At", "Date and time when the certificate was last updated");
        map.put("Announcement Type", "Type of announcement associated with this category");
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
        return map;
    }

    static LinkedHashMap<String, String> createTermEnrichmentHeader() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("Glossary Name", "Name of the glossary in which the term exists");
        map.put("Term Name*", "Name of the term, which cannot include '@'");
        map.put("Description", "Explanation of the term's meaning");
        map.put("User Description", "Explanation of the term's meaning, as provided by a user in the UI");
        map.put("Categories", "Comma-separated list of categories the term is organized within");
        map.put("User Owners", "Comma-separated list of the usernames who are owners of this term");
        map.put("Group Owners", "Comma-separated list of the group names who are owners of this term");
        map.put("Certification", "Certificate associated with this term, one of: Verified, Draft, Deprecated");
        map.put("Classifications", "Comma-separated list of the classifications associated with this term");
        map.put("Certification Message", "Message associated with the certificate (if any)");
        map.put("Certification Updated By", "User who last updated the certificate");
        map.put("Certification Updated At", "Date and time when the certificate was last updated");
        map.put("Announcement Type", "Type of announcement associated with this term");
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
        return map;
    }
}
