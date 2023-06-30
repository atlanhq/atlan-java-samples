/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Utility class to convert data types for columns.
 */
public class DataTypeMapper {

    /** Types that appear to be spurious values in the data — we will skip these entirely. */
    private static final Set<String> spuriousValues = Set.of("DATA TYPE", "DATA_TYPE", "HIERARCHYID", "NULL");

    /** Mappings from one type to another. */
    private static final Map<String, String> typeMap = Map.ofEntries(Map.entry("NVARCHAR", "VARCHAR"));

    /**
     * Retrieve the mapped data type for the provided type, or null if there is no mapped type (for spurious types).
     *
     * @param type to map
     * @return the mapped data type
     */
    public static String getMappedType(String type) {
        if (spuriousValues.contains(type)) {
            // If it's in our list of spurious values, return null
            return null;
        } else {
            // Otherwise returned the mapped type (if any), and finally fallback to
            // returning the received type itself
            return typeMap.getOrDefault(type, type);
        }
    }

    /**
     * Retrieve only the type name from the provided SQL type string.
     * Note: this will also uppercase the typename for case-insensitive comparison purposes.
     *
     * @param sqlType from which to retrieve the type name
     * @return the type name (alone), or null if none could be found
     */
    public static String getTypeOnly(String sqlType) {
        if (sqlType != null) {
            if (sqlType.contains("(")) {
                return sqlType.substring(0, sqlType.indexOf("(")).trim().toUpperCase(Locale.ROOT);
            } else {
                return sqlType.toUpperCase(Locale.ROOT);
            }
        }
        return null;
    }

    /**
     * Retrieve the maximum length defined by the provided SQL type string.
     *
     * @param sqlType from which to retrieve the maximum length
     * @return the maximum length, or null if none could be found
     */
    public static Long getMaxLength(String sqlType) {
        if (sqlType != null) {
            if (sqlType.contains("(") && !sqlType.contains(",")) {
                String length = sqlType.substring(sqlType.indexOf("(") + 1, sqlType.indexOf(")"))
                        .trim();
                // TODO: should probably make this more general by catching a format exception...
                if (!length.equals("MAX")) {
                    return Long.parseLong(length);
                }
            }
        }
        return null;
    }

    /**
     * Retrieve the precision defined by the provided SQL type string.
     *
     * @param sqlType from which to retrieve the precision
     * @return the precision, or null if none could be found
     */
    public static Integer getPrecision(String sqlType) {
        if (sqlType != null) {
            if (sqlType.contains("(") && sqlType.contains(",")) {
                String precision = sqlType.substring(sqlType.indexOf("(") + 1, sqlType.indexOf(","))
                        .trim();
                return Integer.parseInt(precision);
            }
        }
        return null;
    }

    /**
     * Retrieve the scale defined by the provided SQL type string.
     *
     * @param sqlType from which to retrieve the scale
     * @return the scale, or null if none could be found
     */
    public static Double getScale(String sqlType) {
        if (sqlType != null) {
            if (sqlType.contains("(") && sqlType.contains(",")) {
                String scale = sqlType.substring(sqlType.indexOf(",") + 1, sqlType.indexOf(")"))
                        .trim();
                return Double.parseDouble(scale);
            }
        }
        return null;
    }
}
