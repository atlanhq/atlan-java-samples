/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.reporters;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.atlan.exception.AtlanException;
import com.atlan.model.assets.Asset;
import com.atlan.model.fields.AtlanField;
import com.atlan.model.fields.CustomMetadataField;
import com.atlan.model.search.FluentSearch;
import com.atlan.samples.writers.CSVWriter;
import com.atlan.samples.writers.RowGenerator;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AssetReporter extends AbstractReporter implements RequestHandler<Map<String, String>, String> {

    /**
     * Produce a query that will retrieve all the assets you want to extract.
     *
     * @param event context passed through the Lambda invocation event (or environment variables)
     * @return a fluent search with all necessary conditions for selecting the set of assets to extract
     */
    public abstract FluentSearch.FluentSearchBuilder<?, ?> getAssetsToExtract(Map<String, String> event);

    /**
     * Produce a list of all the attributes that should be extracted for each asset,
     * in the order they should be produced for each row of the output. The qualifiedName and
     * type of every asset will automatically be included as the first two columns (as they are
     * required), so you do not need to specify these.
     *
     * @param event context passed through the Lambda invocation event (or environment variables)
     * @return list of attributes to include for each asset
     */
    public abstract List<AtlanField> getAttributesToExtract(Map<String, String> event);

    /**
     * Produce a list of all the attributes that should be extracted for each related asset
     * that is retrieved through the search. The qualifiedName and type of every related asset
     * will automatically be included, so you do not need to specify these.
     * (You only need to override this class if you want any other related attributes.)
     *
     * @param event context passed through the Lambda invocation event (or environment variables)
     * @return list of attributes to include for each related asset
     */
    public List<AtlanField> getRelatedAttributesToExtract(Map<String, String> event) {
        return Collections.emptyList();
    }

    /**
     * Produces a function that will translate all the attributes from each asset
     * to string-encoded results that can be placed into the CSV.
     *
     * @param event context passed through the Lambda invocation event (or environment variables)
     * @return a row generator that translates from asset to a row of strings for the CSV
     */
    public RowGenerator getAssetToValueTranslator(Map<String, String> event) {
        return (a) -> {
            List<String> values = new ArrayList<>();
            values.add(getStringValueForField(a, Asset.QUALIFIED_NAME));
            values.add(getStringValueForField(a, Asset.TYPE_NAME));
            for (AtlanField field : getAttributesToExtract(event)) {
                if (!field.equals(Asset.QUALIFIED_NAME) && !field.equals(Asset.TYPE_NAME)) {
                    values.add(getStringValueForField(a, field));
                }
            }
            return values;
        };
    }

    /** {@inheritDoc} */
    @Override
    public String handleRequest(Map<String, String> event, Context context) {

        if (context != null && context.getClientContext() != null) {
            log.debug(" ... client environment: {}", context.getClientContext().getEnvironment());
            log.debug(" ... client custom: {}", context.getClientContext().getCustom());
        }

        parseParametersFromEvent(event);

        FluentSearch.FluentSearchBuilder<?, ?> assets = getAssetsToExtract(event)
                .pageSize(getBatchSize())
                .includesOnResults(getAttributesToExtract(event))
                .includeOnRelations(Asset.QUALIFIED_NAME)
                .includesOnRelations(getRelatedAttributesToExtract(event));

        try (CSVWriter csv = new CSVWriter(getFilename())) {
            List<String> headerNames = Stream.of(Asset.QUALIFIED_NAME, Asset.TYPE_NAME)
                    .map(AtlanField::getAtlanFieldName)
                    .collect(Collectors.toList());
            headerNames.addAll(getAttributesToExtract(event).stream()
                    .map(AssetReporter::getHeaderForField)
                    .collect(Collectors.toList()));
            csv.writeHeader(headerNames);
            long start = System.currentTimeMillis();
            csv.streamAssets(assets.stream(true), getAssetToValueTranslator(event), assets.count(), getBatchSize());
            long finish = System.currentTimeMillis();
            log.info("Total time taken: {} ms", finish - start);
        } catch (AtlanException e) {
            log.error("Unable to stream assets.", e);
        } catch (IOException e) {
            log.error("Unable to create or write to file: {}", getFilename());
        }

        return getFilename();
    }

    private static String getHeaderForField(AtlanField field) {
        if (field instanceof CustomMetadataField) {
            // For custom metadata, translate the header to human-readable names
            CustomMetadataField cm = (CustomMetadataField) field;
            return cm.getSetName() + "::" + cm.getAttributeName();
        } else {
            return field.getAtlanFieldName();
        }
    }

    /**
     * Translates environment variables into a pseudo-event, to have a common
     * method ({@link #handleRequest(Map, Context)}) across both local workstation runtimes
     * and AWS Lambda execution.
     *
     * @return a map of environment variables as if they are event context from AWS
     */
    public static Map<String, String> envVarsAsEvent() {
        Map<String, String> event = new HashMap<>(System.getenv());
        if (!event.containsKey("DELIMITER")) {
            event.put("DELIMITER", "|");
        }
        return event;
    }

    @Override
    protected void parseParametersFromEvent(Map<String, String> event) {
        super.parseParametersFromEvent(event);
        if (event != null) {
            // Note that this only sets a default prefix of 'asset-report', only
            // if there is no FILE_PREFIX defined in the event itself
            setFilenameWithPrefix(event, "asset-report", "csv");
        }
    }
}
