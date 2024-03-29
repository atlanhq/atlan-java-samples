/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.atlan.cache.ReflectionCache;
import com.atlan.model.assets.Asset;
import com.atlan.model.core.CustomMetadataAttributes;
import com.atlan.model.fields.AtlanField;
import com.atlan.samples.readers.AssetGenerator;
import com.atlan.samples.readers.CSVReader;
import com.atlan.serde.Serde;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AssetLoader extends AbstractLoader implements RequestHandler<Map<String, String>, String> {

    /**
     * Produce a list of all the attributes that should be overwritten for each asset,
     * even when their value is empty in the input CSV file.
     *
     * @return list of attributes to overwrite for each asset
     */
    public abstract List<AtlanField> getAttributesToOverwrite();

    /**
     * Produces a function that will translate all the values from a row of CSV
     * into an asset that can be saved into Atlan.
     *
     * @return an asset generator that translates from row of CSV to asset
     */
    public AssetGenerator getRowToAssetTranslator() {
        return (row, header, typeIdx, qnIdx) -> {
            String typeName = row.get(typeIdx);
            String qualifiedName = row.get(qnIdx);
            if (typeName != null && !typeName.isEmpty() && qualifiedName != null && !qualifiedName.isEmpty()) {
                try {
                    Class<?> assetClass = Serde.getAssetClassForType(typeName);
                    Method method = assetClass.getMethod("_internal");
                    Asset.AssetBuilder<?, ?> builder = (Asset.AssetBuilder<?, ?>) method.invoke(null);
                    Map<String, CustomMetadataAttributes.CustomMetadataAttributesBuilder<?, ?>> customMetadataMap =
                            new HashMap<>();
                    for (int i = 0; i < header.size(); i++) {
                        String fieldName = header.get(i);
                        if (fieldName != null && !fieldName.isEmpty()) {
                            String rValue = row.get(i);
                            if (fieldName.contains("::")) {
                                // Custom metadata field...
                                String[] tokens = fieldName.split(Pattern.quote("::"));
                                String setName = tokens[0];
                                String attrName = tokens[1];
                                if (!customMetadataMap.containsKey(setName)) {
                                    customMetadataMap.put(setName, CustomMetadataAttributes.builder());
                                }
                                Object value = deserializeCMValueFromCSV(rValue);
                                customMetadataMap.get(setName).attribute(attrName, value);
                            } else {
                                // "Normal" field...
                                String deserializedFieldName =
                                        ReflectionCache.getDeserializedName(assetClass, fieldName);
                                try {
                                    Method setter =
                                            ReflectionCache.getSetter(builder.getClass(), deserializedFieldName);
                                    if (setter != null) {
                                        Object value = deserializeValueFromCSV(rValue, setter);
                                        if (value != null) {
                                            ReflectionCache.setValue(builder, deserializedFieldName, value);
                                        }
                                    }
                                } catch (IOException e) {
                                    log.error("Unable to deserialize a value from the CSV.", e);
                                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                                    log.error(
                                            "Unable to set value for {} on: {}::{}",
                                            deserializedFieldName,
                                            typeName,
                                            qualifiedName,
                                            e);
                                }
                            }
                        }
                    }
                    if (!customMetadataMap.isEmpty()) {
                        for (Map.Entry<String, CustomMetadataAttributes.CustomMetadataAttributesBuilder<?, ?>> entry :
                                customMetadataMap.entrySet()) {
                            builder.customMetadata(
                                    entry.getKey(), entry.getValue().build());
                        }
                    }
                    Asset candidate = builder.build();
                    for (AtlanField field : getAttributesToOverwrite()) {
                        try {
                            Method getter = ReflectionCache.getGetter(
                                    Serde.getAssetClassForType(candidate.getTypeName()), field.getAtlanFieldName());
                            // TODO: double-check this works for empty lists, too?
                            if (getter.invoke(candidate) == null) {
                                builder.nullField(field.getAtlanFieldName());
                            }
                        } catch (ClassNotFoundException e) {
                            log.error(
                                    "Unknown type {} — cannot clear {}.",
                                    candidate.getTypeName(),
                                    field.getAtlanFieldName(),
                                    e);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            log.error(
                                    "Unable to clear {} on: {}::{}",
                                    field.getAtlanFieldName(),
                                    candidate.getTypeName(),
                                    candidate.getQualifiedName(),
                                    e);
                        }
                    }
                    return builder.build();
                } catch (ClassNotFoundException
                        | NoSuchMethodException
                        | InvocationTargetException
                        | IllegalAccessException e) {
                    log.error("Unable to dynamically retrieve asset for typeName {}, skipping...", typeName, e);
                }
            }
            // No asset can be parsed if there is no typeName or qualifiedName
            log.warn(
                    "Unable to parse any asset when either (or both) typeName ({}) and qualifiedName ({}) are empty.",
                    typeName,
                    qualifiedName);
            return null;
        };
    }

    /** {@inheritDoc} */
    @Override
    public String handleRequest(Map<String, String> event, Context context) {
        log.info("Retrieving configuration and context...");
        if (context != null && context.getClientContext() != null) {
            log.debug(" ... client environment: {}", context.getClientContext().getEnvironment());
            log.debug(" ... client custom: {}", context.getClientContext().getCustom());
        }
        parseParametersFromEvent(event);
        log.info("Loading assets from: {}", getFilename());
        try (CSVReader csv = new CSVReader(getFilename())) {
            long start = System.currentTimeMillis();
            csv.streamRows(getRowToAssetTranslator(), getBatchSize());
            long finish = System.currentTimeMillis();
            log.info("Total time taken: {} ms", finish - start);
        } catch (IOException e) {
            log.error("Unable to read from file: {}", getFilename(), e);
        }
        return getFilename();
    }

    /**
     * Translates environment variables into a pseudo-event, to have a common
     * method ({@link #handleRequest(Map, Context)}) across both local workstation runtimes
     * and AWS Lambda execution.
     *
     * @return a map of environment variables as if they are event context from AWS
     */
    public static Map<String, String> prepEvent() {
        Map<String, String> event = new HashMap<>(System.getenv());
        if (!event.containsKey("DELIMITER")) {
            event.put("DELIMITER", "|");
        }
        if (!event.containsKey("FILENAME")) {
            event.put("FILENAME", "asset-reporter.csv");
        }
        return event;
    }
}
