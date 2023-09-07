/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.reporters;

import com.atlan.Atlan;
import com.atlan.cache.ReflectionCache;
import com.atlan.model.assets.*;
import com.atlan.model.core.AtlanTag;
import com.atlan.model.enums.AtlanEnum;
import com.atlan.model.fields.AtlanField;
import com.atlan.model.fields.CustomMetadataField;
import com.atlan.model.structs.AtlanStruct;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;

@Slf4j
public abstract class AbstractReporter {

    public static final double BYTES_IN_GB = 1073741824.0;
    public static final String TIMESTAMP_FORMAT = "uuuuMMdd-HHmmss-SSS";

    private int _batchSize = 50;
    private String _delimiter = "|";
    private Region _region = null;
    private String _bucket = null;
    private String _filename = null;

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
            _delimiter = event.getOrDefault("DELIMITER", "|");
            String region = event.getOrDefault("REGION", "ap-south-1");
            _region = Region.of(region);
            _bucket = event.getOrDefault("BUCKET", null);
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

    protected void setFilenameWithPrefix(Map<String, String> event, String prefix) {
        setFilenameWithPrefix(event, prefix, "xlsx");
    }

    protected void setFilenameWithPrefix(Map<String, String> event, String prefix, String extension) {
        String timestamp = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));
        String filename = event.getOrDefault("FILE_PREFIX", prefix);
        if (filename.endsWith("." + extension)) {
            filename = filename.substring(0, filename.lastIndexOf("." + extension));
        }
        filename += "-" + timestamp + "." + extension;
        setFilename(filename);
    }

    protected static String getFormattedDateTime(Long ts) {
        if (ts != null && ts > 0L) {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else {
            return "";
        }
    }

    protected static String getAssetLink(String guid) {
        return Atlan.getBaseUrl() + "/assets/" + guid + "/overview";
    }

    protected static String getDescription(Asset asset) {
        String description = asset.getUserDescription();
        if (description == null || description.isEmpty()) {
            description = asset.getDescription();
        }
        return description == null ? "" : description;
    }

    protected String getUserOwners(Asset asset) {
        return getDelimitedList(asset.getOwnerUsers());
    }

    protected String getGroupOwners(Asset asset) {
        return getDelimitedList(asset.getOwnerGroups());
    }

    protected static int getCount(Collection<?> collection) {
        if (collection == null) {
            return 0;
        } else {
            return collection.size();
        }
    }

    protected static String getValue(AtlanEnum e) {
        if (e == null) {
            return "";
        } else {
            return e.getValue();
        }
    }

    protected static String protectFromNull(Object o) {
        if (o == null) {
            return "";
        } else {
            return o.toString();
        }
    }

    protected static String getREADME(Asset asset) {
        IReadme readme = asset.getReadme();
        if (readme != null) {
            String content = readme.getDescription();
            if (content != null && !content.isEmpty()) {
                return content;
            }
        }
        return "";
    }

    /**
     * Retrieve a comma-separated list of all Atlan tags assigned to the provided asset,
     * whether directly or through propagation.
     *
     * @param asset for which to find Atlan tags
     * @return comma-separated list of the Atlan tags
     */
    protected String getAtlanTags(Asset asset) {
        Set<AtlanTag> atlanTags = asset.getAtlanTags();
        if (atlanTags != null) {
            Set<String> atlanTagNames =
                    atlanTags.stream().map(AtlanTag::getTypeName).collect(Collectors.toSet());
            return getDelimitedList(atlanTagNames);
        }
        return "";
    }

    /**
     * Retrieve a comma-separated list of only the directly-assigned (not propagated) Atlan tags
     * to the provided asset.
     *
     * @param asset for which to find direct Atlan tags
     * @return comma-separated list of the direct Atlan tags
     */
    protected String getDirectAtlanTags(Asset asset) {
        Set<String> atlanTagNames = new TreeSet<>();
        if (asset != null) {
            Set<AtlanTag> atlanTags = asset.getAtlanTags();
            if (atlanTags != null) {
                for (AtlanTag classification : atlanTags) {
                    String classifiedEntity = classification.getEntityGuid();
                    if (asset.getGuid().equals(classifiedEntity)) {
                        atlanTagNames.add(classification.getTypeName());
                    }
                }
            }
        }
        return getDelimitedList(atlanTagNames);
    }

    /**
     * Retrieve a list of multiple values as a single string, separated by the provided delimiter.
     *
     * @param items to combine into a single string
     * @return a single string of all items separated by the delimiter
     */
    protected String getDelimitedList(Collection<String> items) {
        if (items == null) {
            return "";
        } else {
            return String.join(getDelimiter(), items);
        }
    }

    /**
     * Translates the provided value for the provided field on the provided asset into a string
     * representation that can be encoded into a single cell of Excel / CSV.
     *
     * @param from the asset from which to read the value
     * @param field the attribute rom which to read the value
     * @return the string-encoded form that can be placed in a single cell
     */
    protected String getStringValueForField(Asset from, AtlanField field) {
        try {
            // Start by retrieving the value from the asset, then figure out how to serialize it
            Object value;
            if (field instanceof CustomMetadataField) {
                CustomMetadataField cmf = (CustomMetadataField) field;
                value = from.getCustomMetadata(cmf.getSetName(), cmf.getAttributeName());
            } else {
                String deserializedName =
                        ReflectionCache.getDeserializedName(from.getClass(), field.getAtlanFieldName());
                value = ReflectionCache.getValue(from, deserializedName);
            }
            return serializeValueToCSV(from.getGuid(), value);
        } catch (IOException e) {
            log.error(
                    "Unable to retrieve attribute {} on: {}::{}",
                    field.getAtlanFieldName(),
                    from.getTypeName(),
                    from.getQualifiedName(),
                    e);
        }
        // If we fall through, return an empty string
        return "";
    }

    /**
     * Serialize the provided value into a form that can be stored in a single cell of CSV.
     *
     * @param fromGuid the GUID of the asset being serialized (needed to determine direct vs propagated Atlan tags)
     * @param value the value to be serialized
     * @return a String representation of the value that can be stored in a single cell of CSV
     */
    @SuppressWarnings("unchecked")
    protected String serializeValueToCSV(String fromGuid, Object value) {
        // Then figure out how to CSV-string serialize it
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Collection) {
            Optional<?> element1 = ((Collection<?>) value).stream().findFirst();
            if (element1.isPresent()) {
                Object first = element1.get();
                if (first instanceof AtlanTag) {
                    Collection<AtlanTag> tags = (Collection<AtlanTag>) value;
                    List<String> directTags = new ArrayList<>();
                    for (AtlanTag tag : tags) {
                        if (fromGuid.equals(tag.getEntityGuid())) {
                            directTags.add(tag.getTypeName());
                        }
                    }
                    return getDelimitedList(directTags);
                } else if (first instanceof Asset) {
                    return getDelimitedList(((Collection<Asset>) value)
                            .stream().map(this::serializeAssetRefToCSV).collect(Collectors.toList()));
                } else if (first instanceof String) {
                    return getDelimitedList((Collection<String>) value);
                } else if (first instanceof AtlanStruct) {
                    return getDelimitedList(((Collection<AtlanStruct>) value)
                            .stream().map(this::serializeStructToCSV).collect(Collectors.toList()));
                } else {
                    log.warn("Unhandled collection of values: {}", first.getClass());
                }
            }
            return "";
        } else if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            List<String> values = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                values.add(entry.getKey() + "=" + serializeValueToCSV(fromGuid, entry.getValue()));
            }
            return getDelimitedList(values);
        } else if (value instanceof Asset) {
            return serializeAssetRefToCSV((Asset) value);
        } else if (value instanceof AtlanEnum) {
            return ((AtlanEnum) value).getValue();
        } else if (value instanceof AtlanStruct) {
            return serializeStructToCSV((AtlanStruct) value);
        } else if (value != null) {
            // For everything else, just turn it directly into a string
            return value.toString();
        }
        // If we fall through, return an empty string
        return "";
    }

    /**
     * Serialize the provided asset reference into a form that can be stored in a single cell of CSV.
     *
     * @param asset the related asset to be serialized
     * @return a String representation of the related asset
     */
    protected String serializeAssetRefToCSV(Asset asset) {
        String qualifiedName = asset.getQualifiedName();
        if ((qualifiedName == null || qualifiedName.isEmpty()) && asset.getUniqueAttributes() != null) {
            qualifiedName = asset.getUniqueAttributes().getQualifiedName();
        }
        return asset.getTypeName() + "@" + qualifiedName;
    }

    /**
     * Serialize the provided struct into a form that can be stored in a single cell of CSV.
     * Note: this is not yet implemented!
     *
     * @param struct the struct instance to be serialized
     * @return a String representation of the struct instance
     */
    protected String serializeStructToCSV(AtlanStruct struct) {
        // TODO: probably some format that's better than this...
        return struct.toString();
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
}
