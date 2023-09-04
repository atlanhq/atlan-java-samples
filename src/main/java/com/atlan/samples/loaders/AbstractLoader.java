/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders;

import com.atlan.Atlan;
import com.atlan.cache.ReflectionCache;
import com.atlan.exception.AtlanException;
import com.atlan.model.assets.Asset;
import com.atlan.model.core.AtlanTag;
import com.atlan.model.enums.AtlanEnum;
import com.atlan.model.structs.AtlanStruct;
import com.atlan.serde.Serde;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;

@Slf4j
public abstract class AbstractLoader {

    private int _batchSize = 50;
    private String _delimiter = "|";
    private Region _region = null;
    private String _bucket = null;
    private String _filename = null;
    private boolean _updateOnly = false;

    /**
     * Extract the configuration parameters to use from the lambda payload (or equivalent).
     *
     * @param event configuration payload from the lambda function (or equivalent)
     */
    protected void parseParametersFromEvent(Map<String, String> event) {
        if (event != null) {
            log.debug(" ... event: {}", event);
            String batchSize = event.getOrDefault("BATCH_SIZE", "50");
            try {
                _batchSize = Integer.parseInt(batchSize);
            } catch (NumberFormatException e) {
                log.warn(
                        "Unable to determine a number from the BATCH_SIZE value of '{}', falling back to a default of 50.",
                        batchSize);
                _batchSize = 50;
            }
            String updateOnly = event.getOrDefault("UPDATE_ONLY", "false");
            _updateOnly = updateOnly.toUpperCase(Locale.ROOT).equals("TRUE");
            _delimiter = event.getOrDefault("DELIMITER", "|");
            String region = event.getOrDefault("REGION", "ap-south-1");
            _region = Region.of(region);
            _bucket = event.getOrDefault("BUCKET", null);
            _filename = event.getOrDefault("FILENAME", "atlan-documentation-template.xlsx");
            Atlan.setBaseUrl(event.getOrDefault("ATLAN_BASE_URL", null));
            Atlan.setApiToken(event.getOrDefault("ATLAN_API_KEY", null));
            String maxRetries = event.getOrDefault("MAX_RETRIES", "20");
            int _maxRetries = 20;
            try {
                _maxRetries = Integer.parseInt(maxRetries);
            } catch (NumberFormatException e) {
                log.warn(
                        "Unable to determine a number from the MAX_RETRIES value of '{}', falling back to a default of 20.",
                        maxRetries);
            }
            Atlan.setMaxNetworkRetries(_maxRetries);
        }
    }

    /**
     * Retrieve a mapping of the assets that need to be retagged.
     * The method will first look up the provided assets to determine only the missing Atlan tags
     * that need to be appended (rather than attempting to blindly append all Atlan tags).
     *
     * @param assetMap mapping of assets to consider, keyed by qualifiedName with the value a list of Atlan tag names to add the asset to
     * @param typeName of all the assets
     */
    protected void appendAtlanTags(Map<String, List<String>> assetMap, String typeName) {
        Map<String, List<String>> toRetag = new HashMap<>();
        if (!assetMap.isEmpty()) {
            for (Map.Entry<String, List<String>> details : assetMap.entrySet()) {
                String qn = details.getKey();
                List<String> atlanTags = new ArrayList<>(details.getValue());
                try {
                    Asset column = Asset.get(Atlan.getDefaultClient(), typeName, qn, false);
                    Set<AtlanTag> existing = column.getAtlanTags();
                    List<String> toRemove = new ArrayList<>();
                    for (AtlanTag one : existing) {
                        if (atlanTags.contains(one.getTypeName())) {
                            toRemove.add(one.getTypeName());
                        }
                    }
                    atlanTags.removeAll(toRemove);
                    if (!atlanTags.isEmpty()) {
                        toRetag.put(qn, atlanTags);
                    }
                } catch (AtlanException e) {
                    log.error("Unable to find {} {} — cannot retag it.", typeName, qn, e);
                }
            }
        }
        if (!toRetag.isEmpty()) {
            log.info("... tagging {} {}s:", toRetag.size(), typeName);
            for (Map.Entry<String, List<String>> details : toRetag.entrySet()) {
                String qn = details.getKey();
                List<String> atlanTags = details.getValue();
                try {
                    log.info("...... tagging: {}", qn);
                    Atlan.getDefaultClient().assets.addAtlanTags(typeName, qn, atlanTags);
                } catch (AtlanException e) {
                    log.error("Unable to tag {} {} with: {}", typeName, qn, atlanTags, e);
                }
            }
        }
    }

    /**
     * Retrieve a list of embedded values from a single string, itself separated by the provided delimiter.
     *
     * @param value to decompose into a collection of values
     * @param delimiter used to separate each item in the value
     * @return a list of all values embedded in the provided value
     */
    protected static List<String> getEmbeddedList(String value, String delimiter) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(value.split(Pattern.quote(delimiter)));
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T extends AtlanEnum> T getEnum(String value, Class<T> clazz) {
        if (value != null && !value.isEmpty()) {
            try {
                return (T) clazz.getMethod("fromValue", String.class).invoke(null, value);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                log.warn("Unable to translate {} to a {}.", value, clazz.getName(), e);
            }
        }
        return null;
    }

    protected Object deserializeValueFromCSV(String value, Method setter) throws IOException {
        Class<?> paramClass = ReflectionCache.getParameterOfMethod(setter);
        Class<?> innerClass = null;
        String fieldName = setter.getName();
        if (Collection.class.isAssignableFrom(paramClass) || Map.class.isAssignableFrom(paramClass)) {
            Type paramType = ReflectionCache.getParameterizedTypeOfMethod(setter);
            innerClass = ReflectionCache.getClassOfParameterizedType(paramType);
        }
        return deserializeValueFromCSV(value, paramClass, innerClass, fieldName);
    }

    private Object deserializeValueFromCSV(String value, Class<?> type, Class<?> innerType, String fieldName)
            throws IOException {
        if (value == null || value.isEmpty()) {
            return null;
        } else if (String.class.isAssignableFrom(type)) {
            return value;
        } else if (Boolean.class.isAssignableFrom(type)) {
            return Boolean.parseBoolean(value);
        } else if (Integer.class.isAssignableFrom(type)) {
            return Integer.parseInt(value);
        } else if (Long.class.isAssignableFrom(type)) {
            return Long.parseLong(value);
        } else if (Double.class.isAssignableFrom(type)) {
            return Double.parseDouble(value);
        } else if (Collection.class.isAssignableFrom(type)) {
            String[] values = value.split(Pattern.quote(getDelimiter()));
            List<Object> results = new ArrayList<>();
            if (AtlanTag.class.isAssignableFrom(innerType)) {
                for (String tag : values) {
                    // TODO: parameters to decide whether to propagate these or not?
                    results.add(AtlanTag.of(tag));
                }
            } else if (Asset.class.isAssignableFrom(innerType)) {
                for (String asset : values) {
                    results.add(deserializeAssetRefFromCSV(asset));
                }
            } else if (String.class.isAssignableFrom(innerType)) {
                results.addAll(Arrays.asList(values));
            } else if (AtlanStruct.class.isAssignableFrom(innerType)) {
                for (String struct : values) {
                    results.add(deserializeStructToCSV(struct, innerType));
                }
            } else {
                throw new IOException("Unhandled collection of values: " + innerType);
            }
            if (type == Collection.class || type == List.class) {
                return results;
            } else if (type == Set.class || type == SortedSet.class) {
                return new TreeSet<>(results);
            } else {
                throw new IOException("Unable to deserialize CSV list to Java class: " + type);
            }
            /*} else if (Map.class.isAssignableFrom(type)) {
            Map<?, ?> map = new HashMap<>();
            String[] entries = value.split(Pattern.quote(getDelimiter()));
            for (String entry : entries) {
                String[] tokens = entry.split(Pattern.quote("="));
                String key = tokens[0];
                String mapValue = tokens[1];
                map.put(key, deserializeValueFromCSV(mapValue, ???, ???, fieldName));
            }
            return map;*/
        } else if (Asset.class.isAssignableFrom(type)) {
            return deserializeAssetRefFromCSV(value);
        } else if (AtlanEnum.class.isAssignableFrom(type)) {
            return deserializeEnumFromCSV(value, type);
        } else if (AtlanStruct.class.isAssignableFrom(type)) {
            return deserializeStructToCSV(value, type);
        } else {
            // For everything else, just turn it directly into a string
            throw new IOException("Unhandled data type for " + fieldName + ": " + type);
        }
    }

    protected Object deserializeAssetRefFromCSV(String assetRef) throws IOException {
        String[] tokens = assetRef.split(Pattern.quote("@"));
        String typeName = tokens[0];
        try {
            Class<?> assetClass = Serde.getAssetClassForType(typeName);
            Method method = assetClass.getMethod("refByQualifiedName", String.class, String.class);
            return method.invoke(null, typeName, tokens[1]);
        } catch (ClassNotFoundException e) {
            throw new IOException(
                    "No class " + typeName + " found — unable to translate to an asset reference: " + assetRef, e);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IOException("Unable to translate to an asset reference: " + assetRef, e);
        }
    }

    protected Object deserializeStructToCSV(String struct, Class<?> structClass) throws IOException {
        // TODO: figure out how to deserialize a "toString" representation...
        throw new IOException("Structs are currently unhandled in the loader.");
    }

    protected Object deserializeEnumFromCSV(String enumValue, Class<?> enumClass) throws IOException {
        try {
            Method method = enumClass.getMethod("fromValue", String.class);
            return method.invoke(null, enumValue);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IOException("Unable to translate to an enumerated value: " + enumValue, e);
        }
    }

    public void setFilename(String _filename) {
        this._filename = _filename;
    }

    public String getFilename() {
        return _filename;
    }

    public String getBucket() {
        return _bucket;
    }

    public Region getRegion() {
        return _region;
    }

    public int getBatchSize() {
        return _batchSize;
    }

    public String getDelimiter() {
        return _delimiter;
    }

    public boolean isUpdateOnly() {
        return _updateOnly;
    }
}
