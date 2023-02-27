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
public class SlackDiscussionReporter extends AbstractReporter implements RequestHandler<Map<String, String>, String> {

    private static final LinkedHashMap<String, String> SLACK_DISCUSSIONS = createSlackDiscussionHeader();

    private static final Set<String> autoSizeSheets = new HashSet<>();

    private static final HashMap<String, Long> assetToSlackDiscussions = new HashMap<>();
    private static final HashMap<String, Asset> guidToLinkedAsset = new HashMap<>();

    public static void main(String[] args) {
        SlackDiscussionReporter sdr = new SlackDiscussionReporter();
        sdr.handleRequest(System.getenv(), null);
    }

    @Override
    protected void parseParametersFromEvent(Map<String, String> event) {
        super.parseParametersFromEvent(event);
        if (event != null) {
            setFilenameWithPrefix(event, "slack-discussion-report");
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

            Sheet assets = xlsx.createSheet("Slack discussions");
            autoSizeSheets.add("Slack discussions");
            xlsx.addHeader(assets, SLACK_DISCUSSIONS);
            getSlackDiscussions(xlsx, assets);

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

    void getSlackDiscussions(ExcelWriter xlsx, Sheet sheet) throws AtlanException {
        // TODO: Ideally filter the links retrieved to only those for slack.com, but this does not
        //  appear possible with the index on the 'link' attribute
        Query query = CompoundQuery.builder()
                .must(beActive())
                .must(beOfType(Link.TYPE_NAME))
                .build()
                ._toQuery();
        IndexSearchRequest request = IndexSearchRequest.builder()
                .dsl(IndexSearchDSL.builder()
                        .from(0)
                        .size(getBatchSize())
                        .query(query)
                        .sortOption(Sort.by(KeywordFields.GUID, SortOrder.Asc))
                        .build())
                .attribute("asset")
                .attribute("link")
                .attribute("reference")
                .relationAttribute("qualifiedName")
                .relationAttribute("typeName")
                .relationAttribute("name")
                .build();
        log.info("Retrieving first {} link details from: {}", getBatchSize(), Atlan.getBaseUrlSafe());
        IndexSearchResponse response = request.search();
        List<Asset> results = response.getAssets();
        while (results != null) {
            for (Asset result : results) {
                if (result instanceof Link) {
                    Link link = (Link) result;
                    if (link.getAsset() != null) {
                        String assetGuid = link.getAsset().getGuid();
                        String url = link.getLink();
                        if (url.contains("slack.com")) {
                            if (!assetToSlackDiscussions.containsKey(assetGuid)) {
                                assetToSlackDiscussions.put(assetGuid, 0L);
                            }
                            assetToSlackDiscussions.put(assetGuid, assetToSlackDiscussions.get(assetGuid) + 1);
                            guidToLinkedAsset.put(assetGuid, link.getAsset());
                        }
                    }
                }
            }
            log.info(" retrieving next {} link details from: {}", getBatchSize(), Atlan.getBaseUrlSafe());
            response = response.getNextPage();
            results = response.getAssets();
        }
        for (Map.Entry<String, Long> entry : assetToSlackDiscussions.entrySet()) {
            String assetGuid = entry.getKey();
            Long linkCount = entry.getValue();
            Asset asset = guidToLinkedAsset.get(assetGuid);
            if (asset != null) {
                xlsx.appendRow(
                        sheet,
                        List.of(
                                DataCell.of(asset.getUniqueAttributes().getQualifiedName()),
                                DataCell.of(asset.getTypeName()),
                                DataCell.of(asset.getName()),
                                DataCell.of(linkCount),
                                DataCell.of(getAssetLink(assetGuid))));
            }
        }
    }

    static LinkedHashMap<String, String> createSlackDiscussionHeader() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("Qualified name", "Unique name of the asset");
        map.put("Type", "Type of asset");
        map.put("Name", "Name of the asset");
        map.put("Slack", "Number of discussions through Slack linked to this asset");
        map.put("Link", "Link to the detailed asset within Atlan");
        return map;
    }
}
