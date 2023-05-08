/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.exception.AtlanException;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.Asset;
import com.atlan.model.assets.Column;
import com.atlan.samples.loaders.*;
import com.atlan.util.AssetBatch;
import java.util.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for capturing the full details provided about a column.
 */
@Slf4j
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class ColumnDetails extends AssetDetails {

    public static final String COL_COLUMN = "COLUMN NAME";
    public static final String COL_COLUMN_TYPE = "COLUMN DATA TYPE";
    public static final String COL_COLUMN_PK = "PRIMARY KEY?";
    public static final String COL_COLUMN_FK = "FOREIGN KEY?";

    private static final List<String> REQUIRED = List.of(
            ConnectionDetails.COL_CONNECTOR,
            ConnectionDetails.COL_CONNECTION,
            DatabaseDetails.COL_DB,
            SchemaDetails.COL_SCHEMA,
            ContainerDetails.COL_CONTAINER,
            ContainerDetails.COL_CONTAINER_TYPE,
            COL_COLUMN);

    @ToString.Include
    private int index;

    @ToString.Include
    private String name;

    @ToString.Include
    private String parentType;

    @ToString.Include
    private String parentQualifiedName;

    @ToString.Include
    private String rawType;

    @ToString.Include
    private String mappedType;

    @ToString.Include
    private Boolean primaryKey;

    @ToString.Include
    private Boolean foreignKey;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentity() {
        return parentQualifiedName + "/" + name;
    }

    /**
     * Build up details about the column on the provided row.
     *
     * @param connectionCache a cache of connections that have first been resolved across the spreadsheet
     * @param row a row of data from the spreadsheet, as a map from column name to value
     * @param delim delimiter used in cells that can contain multiple values
     * @param index the order (position) of the column in its parent container (based on its order in the spreadsheet)
     * @return the column details for that row
     */
    public static ColumnDetails getFromRow(
            Map<ConnectionDetails, String> connectionCache, Map<String, String> row, String delim, int index) {
        ColumnDetailsBuilder<?, ?> builder = getFromRow(ColumnDetails.builder(), row, delim);
        if (getMissingFields(row, REQUIRED).isEmpty()) {
            String containerQualifiedName = ContainerDetails.getQualifiedName(connectionCache, row);
            builder = builder.index(index)
                    .name(row.get(COL_COLUMN))
                    .parentQualifiedName(containerQualifiedName)
                    .parentType(row.get(ContainerDetails.COL_CONTAINER_TYPE))
                    .primaryKey(getBoolean(row.get(COL_COLUMN_PK)))
                    .foreignKey(getBoolean(row.get(COL_COLUMN_FK)));
            String rawType = row.get(COL_COLUMN_TYPE);
            String rawTypeOnly = DataTypeMapper.getTypeOnly(rawType);
            String mappedType = DataTypeMapper.getMappedType(rawTypeOnly);
            if (rawType.length() > 0) {
                builder = builder.rawType(rawType);
            }
            if (mappedType != null && mappedType.length() > 0) {
                builder = builder.mappedType(mappedType);
            }
            return builder.stub(false).build();
        }
        return null;
    }

    /**
     * Create columns in bulk, if they do not exist, or update them if they do (idempotent).
     *
     * @param columns the set of columns to ensure exist
     * @param batchSize maximum number of columns to create per batch
     * @param updateOnly if true, only attempt to update existing assets, otherwise allow assets to be created as well
     * @return details of all parent containers in which assets were created or updated
     */
    public static Set<ContainerDetails> upsert(Map<String, ColumnDetails> columns, int batchSize, boolean updateOnly) {
        Set<ContainerDetails> parents = new HashSet<>();
        AssetBatch batch = new AssetBatch(Column.TYPE_NAME, batchSize);
        Map<String, List<String>> toClassify = new HashMap<>();

        for (ColumnDetails details : columns.values()) {
            String parentQualifiedName = details.getParentQualifiedName();
            String parentType = details.getParentType();
            String columnName = details.getName();
            parents.add(ContainerDetails.getHeader(parentQualifiedName, parentType));
            if (updateOnly) {
                String qualifiedName = Column.generateQualifiedName(columnName, parentQualifiedName);
                try {
                    Asset.retrieveMinimal(Column.TYPE_NAME, qualifiedName);
                    Column toUpdate = Column.updater(qualifiedName, columnName)
                            .description(details.getDescription())
                            .certificateStatus(details.getCertificate())
                            .certificateStatusMessage(details.getCertificateStatusMessage())
                            .announcementType(details.getAnnouncementType())
                            .announcementTitle(details.getAnnouncementTitle())
                            .announcementMessage(details.getAnnouncementMessage())
                            .ownerUsers(details.getOwnerUsers())
                            .ownerGroups(details.getOwnerGroups())
                            .isPrimary(details.getPrimaryKey())
                            .isForeign(details.getForeignKey())
                            .build();
                    if (!details.getClassifications().isEmpty()) {
                        toClassify.put(toUpdate.getQualifiedName(), details.getClassifications());
                    }
                    batch.add(toUpdate);
                } catch (NotFoundException e) {
                    log.warn("Unable to find existing column — skipping: {}", qualifiedName, e);
                } catch (AtlanException e) {
                    log.error("Unable to lookup whether column exists or not.", e);
                }
            } else {
                String mappedType = details.getMappedType();
                String rawType = details.getRawType();
                Column.ColumnBuilder<?, ?> builder = Column.creator(
                                columnName, parentType, parentQualifiedName, details.getIndex())
                        .description(details.getDescription())
                        .certificateStatus(details.getCertificate())
                        .certificateStatusMessage(details.getCertificateStatusMessage())
                        .announcementType(details.getAnnouncementType())
                        .announcementTitle(details.getAnnouncementTitle())
                        .announcementMessage(details.getAnnouncementMessage())
                        .ownerUsers(details.getOwnerUsers())
                        .ownerGroups(details.getOwnerGroups())
                        .dataType(mappedType)
                        .isPrimary(details.getPrimaryKey())
                        .isForeign(details.getForeignKey());
                Long maxLength = DataTypeMapper.getMaxLength(rawType);
                Integer precision = DataTypeMapper.getPrecision(rawType);
                Double scale = DataTypeMapper.getScale(rawType);
                if (maxLength != null) {
                    builder = builder.maxLength(maxLength);
                }
                if (precision != null) {
                    builder = builder.precision(precision);
                }
                if (scale != null) {
                    builder = builder.numericScale(scale);
                }
                Column column = builder.build();
                if (!details.getClassifications().isEmpty()) {
                    toClassify.put(column.getQualifiedName(), details.getClassifications());
                }
                batch.add(column);
            }
        }
        // And don't forget to flush out any that remain
        batch.flush();

        // Classifications must be added in a second pass, after the asset exists
        appendClassifications(toClassify, Column.TYPE_NAME);

        return parents;
    }
}
