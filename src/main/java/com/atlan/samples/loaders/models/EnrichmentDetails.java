/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.Atlan;
import com.atlan.exception.AtlanException;
import com.atlan.model.core.CustomMetadataAttributes;
import com.atlan.model.typedefs.AttributeDef;
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
public abstract class EnrichmentDetails extends AssetDetails {

    private static final String CM_DELIMITER = "|";

    public static final String COL_USER_DESCRIPTION = "USER DESCRIPTION";
    public static final String COL_README = "README";

    @ToString.Include
    private String userDescription;

    @ToString.Include
    private String readme;

    @ToString.Include
    private Map<String, CustomMetadataAttributes> customMetadataValues;

    /**
     * Retrieve all custom metadata values specified on the row.
     *
     * @param row of data
     * @param delimiter used to separate multiple values in a single cell
     * @return a map keyed by custom metadata structure name to attribute names and values
     */
    protected static Map<String, CustomMetadataAttributes> getCustomMetadataValuesFromRow(
            Map<String, String> row, String delimiter) {
        Map<String, CustomMetadataAttributes> cmMap = new HashMap<>();
        try {
            Map<String, List<AttributeDef>> customAttrDefs =
                    Atlan.getDefaultClient().getCustomMetadataCache().getAllCustomAttributes();
            for (String colName : row.keySet()) {
                if (colName != null && colName.contains(CM_DELIMITER)) {
                    String[] tokens = colName.split(Pattern.quote(CM_DELIMITER));
                    String cmName = tokens[0];
                    String cmAttrName = tokens[1];
                    List<AttributeDef> attrDefs = customAttrDefs.getOrDefault(cmName, Collections.emptyList());
                    AttributeDef attrDef = null;
                    for (AttributeDef candidate : attrDefs) {
                        if (candidate.getDisplayName().equals(cmAttrName)) {
                            attrDef = candidate;
                            break;
                        }
                    }
                    if (attrDef != null) {
                        String value = row.get(colName);
                        if (value != null && value.length() > 0) {
                            if (!cmMap.containsKey(cmName)) {
                                cmMap.put(
                                        cmName,
                                        CustomMetadataAttributes.builder().build());
                            }
                            String typeName = attrDef.getTypeName();
                            Object toSet;
                            switch (typeName) {
                                case "boolean":
                                    toSet = getBoolean(value);
                                    break;
                                    // TODO: would probably be nice to translate dates into epochs
                                default:
                                    toSet = value;
                                    break;
                            }
                            if (attrDef.getOptions().getMultiValueSelect()) {
                                toSet = Arrays.asList(value.split(Pattern.quote(delimiter)));
                            }
                            CustomMetadataAttributes cma = cmMap.get(cmName).toBuilder()
                                    .attribute(cmAttrName, toSet)
                                    .build();
                            cmMap.put(cmName, cma);
                        }
                    }
                }
            }
        } catch (AtlanException e) {
            log.error("Unable to retrieve custom metadata attribute definitions.", e);
        }
        return cmMap;
    }
}
