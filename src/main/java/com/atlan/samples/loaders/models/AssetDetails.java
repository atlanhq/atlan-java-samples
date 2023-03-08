/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.api.EntityUniqueAttributesEndpoint;
import com.atlan.exception.AtlanException;
import com.atlan.model.assets.Asset;
import com.atlan.model.core.Classification;
import com.atlan.model.core.CustomMetadataAttributes;
import com.atlan.model.enums.AtlanAnnouncementType;
import com.atlan.model.enums.AtlanCertificateStatus;
import java.util.*;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for capturing the full details provided about an asset.
 */
@Slf4j
@Getter
@SuperBuilder
@EqualsAndHashCode
@ToString(onlyExplicitlyIncluded = true)
public abstract class AssetDetails {
    private static final String COL_DESCRIPTION = "DESCRIPTION";
    private static final String COL_CERTIFICATE = "CERTIFICATE";
    private static final String COL_CERT_MESSAGE = "CERTIFICATE MESSAGE";
    private static final String COL_ANNOUNCEMENT = "ANNOUNCEMENT";
    private static final String COL_ANN_TITLE = "ANNOUNCEMENT TITLE";
    private static final String COL_ANN_MESSAGE = "ANNOUNCEMENT MESSAGE";
    private static final String COL_OWN_USERS = "OWNER USERS";
    private static final String COL_OWN_GROUPS = "OWNER GROUPS";
    private static final String COL_CLASSIFICATIONS = "CLASSIFICATIONS";

    @ToString.Include
    private String description;

    @ToString.Include
    private AtlanCertificateStatus certificate;

    @ToString.Include
    private String certificateStatusMessage;

    @ToString.Include
    private AtlanAnnouncementType announcementType;

    @ToString.Include
    private String announcementTitle;

    @ToString.Include
    private String announcementMessage;

    @ToString.Include
    @Builder.Default
    private List<String> ownerUsers = Collections.emptyList();

    @ToString.Include
    @Builder.Default
    private List<String> ownerGroups = Collections.emptyList();

    @ToString.Include
    @Builder.Default
    private List<String> classifications = Collections.emptyList();

    @ToString.Include
    @Builder.Default
    private boolean stub = true;

    /**
     * Retrieve the unique identity of this asset for deduplication across rows.
     *
     * @return String giving the unique identity of the asset
     */
    public abstract String getIdentity();

    /**
     * Build up details about the asset on the provided row.
     *
     * @param builder into which to append the details from the row
     * @param row a row of data from the spreadsheet, as a map from column name to value
     * @param delim delimiter used in cells that can contain multiple values
     * @return the asset details for that row
     */
    protected static <T extends AssetDetailsBuilder<?, ?>> T getFromRow(
            T builder, Map<String, String> row, String delim) {
        builder.description(row.get(COL_DESCRIPTION))
                .certificateStatusMessage(row.get(COL_CERT_MESSAGE))
                .announcementTitle(row.get(COL_ANN_TITLE))
                .announcementMessage(row.get(COL_ANN_MESSAGE))
                .ownerUsers(getMultiValuedList(row.get(COL_OWN_USERS), delim))
                .ownerGroups(getMultiValuedList(row.get(COL_OWN_GROUPS), delim))
                .classifications(getMultiValuedList(row.get(COL_CLASSIFICATIONS), delim));

        String certificate = row.get(COL_CERTIFICATE);
        String announcement = row.get(COL_ANNOUNCEMENT);

        if (certificate.length() > 0) {
            builder.certificate(AtlanCertificateStatus.fromValue(certificate));
        }
        if (announcement.length() > 0) {
            builder.announcementType(AtlanAnnouncementType.fromValue(announcement));
        }
        return builder;
    }

    /**
     * Translate the provided cell value into a boolean. Any case-insensitive: `X`, `Y`, `YES`, or `TRUE`
     * are truthful values, and any other values (including a blank) are false.
     *
     * @param candidate to translate to a boolean
     * @return the boolean equivalent
     */
    protected static boolean getBoolean(String candidate) {
        if (candidate == null || candidate.length() == 0) {
            return false;
        } else {
            String upper = candidate.toUpperCase();
            return upper.equals("X") || upper.equals("Y") || upper.equals("YES") || upper.equals("TRUE");
        }
    }

    /**
     * Retrieve a list of multiple values from the provided cell contents.
     *
     * @param candidate contents of a cell that could contain multiple values
     * @param delim delimiter between values
     * @return a list of the contents of the cell, divided into single values, or an empty list
     */
    protected static List<String> getMultiValuedList(String candidate, String delim) {
        if (candidate != null && candidate.length() > 0) {
            return Arrays.asList(candidate.split(Pattern.quote(delim)));
        }
        return Collections.emptyList();
    }

    /**
     * Validates whether the provided row of data has all the fields required for this kind of asset.
     *
     * @param row of data
     * @param requiredFields collection of column names that are required for this kind of asset
     * @return a list of the required fields that are missing (or an empty list if all required fields are present)
     */
    protected static List<String> getMissingFields(Map<String, String> row, Collection<String> requiredFields) {
        List<String> missingFields = new ArrayList<>();
        for (String columnName : requiredFields) {
            String candidate = row.get(columnName);
            if (candidate == null || candidate.length() == 0) {
                missingFields.add(columnName);
            }
        }
        return missingFields;
    }

    /**
     * Validates whether the provided row of data has any of the other fields populated, indicating
     * this row is for some other kind of asset.
     *
     * @param row of data
     * @param requiredEmptyFields collection of column names that are required to be empty for this kind of asset
     * @return a list of the required empty fields that are in fact populated (or an empty list if all required empty fields are indeed empty)
     */
    protected static List<String> getRequiredEmptyFields(
            Map<String, String> row, Collection<String> requiredEmptyFields) {
        List<String> nonEmptyFields = new ArrayList<>();
        for (String columnName : requiredEmptyFields) {
            String candidate = row.get(columnName);
            if (candidate != null && candidate.length() > 0) {
                nonEmptyFields.add(columnName);
            }
        }
        return nonEmptyFields;
    }

    /**
     * Retrieve a mapping of the assets that need to be reclassified.
     * The method will first look up the provided assets to determine only the missing classifications
     * that need to be appended (rather than attempting to blindly append all classifications).
     *
     * @param assetMap mapping of assets to consider, keyed by qualifiedName with the value a list of classification names to add the asset to
     * @param typeName of all the assets
     */
    protected static void appendClassifications(Map<String, List<String>> assetMap, String typeName) {
        Map<String, List<String>> toReclassify = new HashMap<>();
        if (!assetMap.isEmpty()) {
            for (Map.Entry<String, List<String>> details : assetMap.entrySet()) {
                String qn = details.getKey();
                List<String> classifications = new ArrayList<>(details.getValue());
                try {
                    Asset column = Asset.retrieveMinimal(typeName, qn);
                    Set<Classification> existing = column.getClassifications();
                    List<String> toRemove = new ArrayList<>();
                    for (Classification one : existing) {
                        if (classifications.contains(one.getTypeName())) {
                            toRemove.add(one.getTypeName());
                        }
                    }
                    classifications.removeAll(toRemove);
                    if (!classifications.isEmpty()) {
                        toReclassify.put(qn, classifications);
                    }
                } catch (AtlanException e) {
                    log.error("Unable to find {} {} — cannot reclassify it.", typeName, qn, e);
                }
            }
        }
        if (!toReclassify.isEmpty()) {
            log.info("... classifying {} {}s:", toReclassify.size(), typeName);
            for (Map.Entry<String, List<String>> details : toReclassify.entrySet()) {
                String qn = details.getKey();
                List<String> classifications = details.getValue();
                try {
                    log.info("...... classifying: {}", qn);
                    EntityUniqueAttributesEndpoint.addClassifications(typeName, qn, classifications);
                } catch (AtlanException e) {
                    log.error("Unable to classify {} {} with: {}", typeName, qn, classifications, e);
                }
            }
        }
    }

    /**
     * Selectively update the custom metadata for the provided assets. Only the custom metadata attributes that have been
     * provided will be updated, while all other custom metadata attributes will be left as-is on existing assets.
     *
     * @param guidMap mapping of assets keyed by GUID with the value a map keyed by custom metadata structure name to populated attributes
     */
    protected static void selectivelyUpdateCustomMetadata(Map<String, Map<String, CustomMetadataAttributes>> guidMap) {
        if (guidMap != null) {
            for (Map.Entry<String, Map<String, CustomMetadataAttributes>> outer : guidMap.entrySet()) {
                String guid = outer.getKey();
                Map<String, CustomMetadataAttributes> attrMap = outer.getValue();
                for (Map.Entry<String, CustomMetadataAttributes> inner : attrMap.entrySet()) {
                    String cmName = inner.getKey();
                    CustomMetadataAttributes cma = inner.getValue();
                    if (cmName != null && cma != null && !cma.isEmpty()) {
                        try {
                            log.info("... selectively updating custom metadata {} on asset {}", cmName, guid);
                            Asset.updateCustomMetadataAttributes(guid, cmName, cma);
                        } catch (AtlanException e) {
                            log.error("Unable to update custom metadata {} on {} with: {}", cmName, guid, cma, e);
                        }
                    }
                }
            }
        }
    }
}
