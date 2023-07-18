/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2022 Atlan Pte. Ltd. */
package com.probable.guacamole.model.assets;

import com.atlan.Atlan;
import com.atlan.exception.AtlanException;
import com.atlan.exception.ErrorCode;
import com.atlan.exception.InvalidRequestException;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.*;
import com.atlan.model.enums.AtlanAnnouncementType;
import com.atlan.model.enums.CertificateStatus;
import com.atlan.model.relations.UniqueAttributes;
import com.atlan.model.structs.ColumnValueFrequencyMap;
import com.atlan.model.structs.Histogram;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import javax.annotation.processing.Generated;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Specialized form of a column specific to Guacamole.
 */
@Generated(value = "com.probable.guacamole.generators.POJOGenerator")
@Getter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Slf4j
@SuppressWarnings("cast")
public class GuacamoleColumn extends Asset
        implements IGuacamoleColumn, IColumn, ISQL, ICatalog, IAsset, IReferenceable {
    private static final long serialVersionUID = 2L;

    public static final String TYPE_NAME = "GuacamoleColumn";

    /** Fixed typeName for GuacamoleColumns. */
    @Getter(onMethod_ = {@Override})
    @Builder.Default
    String typeName = TYPE_NAME;

    /** TBC */
    @Attribute
    Double columnAverage;

    /** TBC */
    @Attribute
    Double columnAverageLength;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<IDbtModelColumn> columnDbtModelColumns;

    /** TBC */
    @Attribute
    Integer columnDepthLevel;

    /** TBC */
    @Attribute
    Integer columnDistinctValuesCount;

    /** TBC */
    @Attribute
    Long columnDistinctValuesCountLong;

    /** TBC */
    @Attribute
    Integer columnDuplicateValuesCount;

    /** TBC */
    @Attribute
    Long columnDuplicateValuesCountLong;

    /** TBC */
    @Attribute
    @Singular("addColumnHistogram")
    List<Histogram> columnHistogram;

    /** TBC */
    @Attribute
    Double columnMax;

    /** TBC */
    @Attribute
    Integer columnMaximumStringLength;

    /** TBC */
    @Attribute
    @Singular("addColumnMax")
    SortedSet<String> columnMaxs;

    /** TBC */
    @Attribute
    Double columnMean;

    /** TBC */
    @Attribute
    Double columnMedian;

    /** TBC */
    @Attribute
    Double columnMin;

    /** TBC */
    @Attribute
    Integer columnMinimumStringLength;

    /** TBC */
    @Attribute
    @Singular("addColumnMin")
    SortedSet<String> columnMins;

    /** TBC */
    @Attribute
    Integer columnMissingValuesCount;

    /** TBC */
    @Attribute
    Long columnMissingValuesCountLong;

    /** TBC */
    @Attribute
    Double columnMissingValuesPercentage;

    /** TBC */
    @Attribute
    Double columnStandardDeviation;

    /** TBC */
    @Attribute
    Double columnSum;

    /** TBC */
    @Attribute
    @Singular
    List<ColumnValueFrequencyMap> columnTopValues;

    @Attribute
    @Singular
    SortedSet<IDbtTest> dbtTests;

    /** TBC */
    @Attribute
    Integer columnUniqueValuesCount;

    /** TBC */
    @Attribute
    Long columnUniqueValuesCountLong;

    /** TBC */
    @Attribute
    Double columnUniquenessPercentage;

    /** TBC */
    @Attribute
    Double columnVariance;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<IMetric> dataQualityMetricDimensions;

    /** TBC */
    @Attribute
    String dataType;

    /** TBC */
    @Attribute
    String databaseName;

    /** TBC */
    @Attribute
    String databaseQualifiedName;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<IDbtMetric> dbtMetrics;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<IDbtModelColumn> dbtModelColumns;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<IDbtModel> dbtModels;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<IDbtSource> dbtSources;

    /** TBC */
    @Attribute
    String defaultValue;

    /** TBC */
    @Attribute
    IColumn foreignKeyFrom;

    /** TBC */
    @Attribute
    @Singular("addForeignKeyTo")
    SortedSet<IColumn> foreignKeyTo;

    /** Time (epoch) when this column was imagined, in milliseconds. */
    @Attribute
    Long guacamoleConceptualized;

    /** Specialized table that contains this specialized column. */
    @Attribute
    IGuacamoleTable guacamoleTable;

    /** Maximum size of a Guacamole column. */
    @Attribute
    Long guacamoleWidth;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<ILineageProcess> inputToProcesses;

    /** TBC */
    @Attribute
    Boolean isClustered;

    /** TBC */
    @Attribute
    Boolean isDist;

    /** TBC */
    @Attribute
    Boolean isForeign;

    /** TBC */
    @Attribute
    Boolean isIndexed;

    /** TBC */
    @Attribute
    Boolean isNullable;

    /** TBC */
    @Attribute
    Boolean isPartition;

    /** TBC */
    @Attribute
    Boolean isPinned;

    /** TBC */
    @Attribute
    Boolean isPrimary;

    /** TBC */
    @Attribute
    Boolean isProfiled;

    /** TBC */
    @Attribute
    Boolean isSort;

    /** TBC */
    @Attribute
    Long lastProfiledAt;

    /** TBC */
    @Attribute
    @JsonProperty("materialisedView")
    IMaterializedView materializedView;

    /** TBC */
    @Attribute
    Long maxLength;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<IMetric> metricTimestamps;

    /** TBC */
    @Attribute
    Integer nestedColumnCount;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<IColumn> nestedColumns;

    /** TBC */
    @Attribute
    Double numericScale;

    /** TBC */
    @Attribute
    Integer order;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<ILineageProcess> outputFromProcesses;

    /** TBC */
    @Attribute
    IColumn parentColumn;

    /** TBC */
    @Attribute
    String parentColumnName;

    /** TBC */
    @Attribute
    String parentColumnQualifiedName;

    /** TBC */
    @Attribute
    Integer partitionOrder;

    /** TBC */
    @Attribute
    Long pinnedAt;

    /** TBC */
    @Attribute
    String pinnedBy;

    /** TBC */
    @Attribute
    Integer precision;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<IAtlanQuery> queries;

    /** TBC */
    @Attribute
    Long queryCount;

    /** TBC */
    @Attribute
    Long queryCountUpdatedAt;

    /** TBC */
    @Attribute
    Long queryUserCount;

    /** TBC */
    @Attribute
    @Singular("putQueryUserMap")
    Map<String, Long> queryUserMap;

    /** TBC */
    @Attribute
    String rawDataTypeDefinition;

    /** TBC */
    @Attribute
    String schemaName;

    /** TBC */
    @Attribute
    String schemaQualifiedName;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<IDbtSource> sqlDBTSources;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<IDbtModel> sqlDbtModels;

    /** TBC */
    @Attribute
    String subDataType;

    /** TBC */
    @Attribute
    ITable table;

    /** TBC */
    @Attribute
    String tableName;

    /** TBC */
    @Attribute
    ITablePartition tablePartition;

    /** TBC */
    @Attribute
    String tableQualifiedName;

    /** TBC */
    @Attribute
    @Singular
    Map<String, String> validations;

    /** TBC */
    @Attribute
    IView view;

    /** TBC */
    @Attribute
    String viewName;

    /** TBC */
    @Attribute
    String viewQualifiedName;

    /**
     * Reference to a GuacamoleColumn by GUID.
     *
     * @param guid the GUID of the GuacamoleColumn to reference
     * @return reference to a GuacamoleColumn that can be used for defining a relationship to a GuacamoleColumn
     */
    public static GuacamoleColumn refByGuid(String guid) {
        return GuacamoleColumn.builder().guid(guid).build();
    }

    /**
     * Reference to a GuacamoleColumn by qualifiedName.
     *
     * @param qualifiedName the qualifiedName of the GuacamoleColumn to reference
     * @return reference to a GuacamoleColumn that can be used for defining a relationship to a GuacamoleColumn
     */
    public static GuacamoleColumn refByQualifiedName(String qualifiedName) {
        return GuacamoleColumn.builder()
                .uniqueAttributes(
                        UniqueAttributes.builder().qualifiedName(qualifiedName).build())
                .build();
    }

    /**
     * Retrieves a GuacamoleColumn by its GUID, complete with all of its relationships.
     *
     * @param guid of the GuacamoleColumn to retrieve
     * @return the requested full GuacamoleColumn, complete with all of its relationships
     * @throws AtlanException on any error during the API invocation, such as the {@link NotFoundException} if the GuacamoleColumn does not exist or the provided GUID is not a GuacamoleColumn
     */
    public static GuacamoleColumn retrieveByGuid(String guid) throws AtlanException {
        Asset asset = Asset.retrieveFull(guid);
        if (asset == null) {
            throw new NotFoundException(ErrorCode.ASSET_NOT_FOUND_BY_GUID, guid);
        } else if (asset instanceof GuacamoleColumn) {
            return (GuacamoleColumn) asset;
        } else {
            throw new NotFoundException(ErrorCode.ASSET_NOT_TYPE_REQUESTED, guid, "GuacamoleColumn");
        }
    }

    /**
     * Retrieves a GuacamoleColumn by its qualifiedName, complete with all of its relationships.
     *
     * @param qualifiedName of the GuacamoleColumn to retrieve
     * @return the requested full GuacamoleColumn, complete with all of its relationships
     * @throws AtlanException on any error during the API invocation, such as the {@link NotFoundException} if the GuacamoleColumn does not exist
     */
    public static GuacamoleColumn retrieveByQualifiedName(String qualifiedName) throws AtlanException {
        Asset asset = Asset.retrieveFull(Atlan.getDefaultClient(), TYPE_NAME, qualifiedName);
        if (asset instanceof GuacamoleColumn) {
            return (GuacamoleColumn) asset;
        } else {
            throw new NotFoundException(ErrorCode.ASSET_NOT_FOUND_BY_QN, qualifiedName, "GuacamoleColumn");
        }
    }

    /**
     * Restore the archived (soft-deleted) GuacamoleColumn to active.
     *
     * @param qualifiedName for the GuacamoleColumn
     * @return true if the GuacamoleColumn is now active, and false otherwise
     * @throws AtlanException on any API problems
     */
    public static boolean restore(String qualifiedName) throws AtlanException {
        return Asset.restore(Atlan.getDefaultClient(), TYPE_NAME, qualifiedName);
    }

    /**
     * Builds the minimal object necessary to update a GuacamoleColumn.
     *
     * @param qualifiedName of the GuacamoleColumn
     * @param name of the GuacamoleColumn
     * @return the minimal request necessary to update the GuacamoleColumn, as a builder
     */
    public static GuacamoleColumnBuilder<?, ?> updater(String qualifiedName, String name) {
        return GuacamoleColumn.builder().qualifiedName(qualifiedName).name(name);
    }

    /**
     * Builds the minimal object necessary to apply an update to a GuacamoleColumn, from a potentially
     * more-complete GuacamoleColumn object.
     *
     * @return the minimal object necessary to update the GuacamoleColumn, as a builder
     * @throws InvalidRequestException if any of the minimal set of required properties for GuacamoleColumn are not found in the initial object
     */
    @Override
    public GuacamoleColumnBuilder<?, ?> trimToRequired() throws InvalidRequestException {
        List<String> missing = new ArrayList<>();
        if (this.getQualifiedName() == null || this.getQualifiedName().length() == 0) {
            missing.add("qualifiedName");
        }
        if (this.getName() == null || this.getName().length() == 0) {
            missing.add("name");
        }
        if (!missing.isEmpty()) {
            throw new InvalidRequestException(
                    ErrorCode.MISSING_REQUIRED_UPDATE_PARAM, "GuacamoleColumn", String.join(",", missing));
        }
        return updater(this.getQualifiedName(), this.getName());
    }

    /**
     * Remove the system description from a GuacamoleColumn.
     *
     * @param qualifiedName of the GuacamoleColumn
     * @param name of the GuacamoleColumn
     * @return the updated GuacamoleColumn, or null if the removal failed
     * @throws AtlanException on any API problems
     */
    public static GuacamoleColumn removeDescription(String qualifiedName, String name) throws AtlanException {
        return (GuacamoleColumn) Asset.removeDescription(Atlan.getDefaultClient(), updater(qualifiedName, name));
    }

    /**
     * Remove the user's description from a GuacamoleColumn.
     *
     * @param qualifiedName of the GuacamoleColumn
     * @param name of the GuacamoleColumn
     * @return the updated GuacamoleColumn, or null if the removal failed
     * @throws AtlanException on any API problems
     */
    public static GuacamoleColumn removeUserDescription(String qualifiedName, String name) throws AtlanException {
        return (GuacamoleColumn) Asset.removeUserDescription(Atlan.getDefaultClient(), updater(qualifiedName, name));
    }

    /**
     * Remove the owners from a GuacamoleColumn.
     *
     * @param qualifiedName of the GuacamoleColumn
     * @param name of the GuacamoleColumn
     * @return the updated GuacamoleColumn, or null if the removal failed
     * @throws AtlanException on any API problems
     */
    public static GuacamoleColumn removeOwners(String qualifiedName, String name) throws AtlanException {
        return (GuacamoleColumn) Asset.removeOwners(Atlan.getDefaultClient(), updater(qualifiedName, name));
    }

    /**
     * Update the certificate on a GuacamoleColumn.
     *
     * @param qualifiedName of the GuacamoleColumn
     * @param certificate to use
     * @param message (optional) message, or null if no message
     * @return the updated GuacamoleColumn, or null if the update failed
     * @throws AtlanException on any API problems
     */
    public static GuacamoleColumn updateCertificate(String qualifiedName, CertificateStatus certificate, String message)
            throws AtlanException {
        return (GuacamoleColumn) Asset.updateCertificate(
                Atlan.getDefaultClient(), builder(), TYPE_NAME, qualifiedName, certificate, message);
    }

    /**
     * Remove the certificate from a GuacamoleColumn.
     *
     * @param qualifiedName of the GuacamoleColumn
     * @param name of the GuacamoleColumn
     * @return the updated GuacamoleColumn, or null if the removal failed
     * @throws AtlanException on any API problems
     */
    public static GuacamoleColumn removeCertificate(String qualifiedName, String name) throws AtlanException {
        return (GuacamoleColumn) Asset.removeCertificate(Atlan.getDefaultClient(), updater(qualifiedName, name));
    }

    /**
     * Update the announcement on a GuacamoleColumn.
     *
     * @param qualifiedName of the GuacamoleColumn
     * @param type type of announcement to set
     * @param title (optional) title of the announcement to set (or null for no title)
     * @param message (optional) message of the announcement to set (or null for no message)
     * @return the result of the update, or null if the update failed
     * @throws AtlanException on any API problems
     */
    public static GuacamoleColumn updateAnnouncement(
            String qualifiedName, AtlanAnnouncementType type, String title, String message) throws AtlanException {
        return (GuacamoleColumn) Asset.updateAnnouncement(
                Atlan.getDefaultClient(), builder(), TYPE_NAME, qualifiedName, type, title, message);
    }

    /**
     * Remove the announcement from a GuacamoleColumn.
     *
     * @param qualifiedName of the GuacamoleColumn
     * @param name of the GuacamoleColumn
     * @return the updated GuacamoleColumn, or null if the removal failed
     * @throws AtlanException on any API problems
     */
    public static GuacamoleColumn removeAnnouncement(String qualifiedName, String name) throws AtlanException {
        return (GuacamoleColumn) Asset.removeAnnouncement(Atlan.getDefaultClient(), updater(qualifiedName, name));
    }

    /**
     * Replace the terms linked to the GuacamoleColumn.
     *
     * @param qualifiedName for the GuacamoleColumn
     * @param name human-readable name of the GuacamoleColumn
     * @param terms the list of terms to replace on the GuacamoleColumn, or null to remove all terms from the GuacamoleColumn
     * @return the GuacamoleColumn that was updated (note that it will NOT contain details of the replaced terms)
     * @throws AtlanException on any API problems
     */
    public static GuacamoleColumn replaceTerms(String qualifiedName, String name, List<IGlossaryTerm> terms)
            throws AtlanException {
        return (GuacamoleColumn) Asset.replaceTerms(Atlan.getDefaultClient(), updater(qualifiedName, name), terms);
    }

    /**
     * Link additional terms to the GuacamoleColumn, without replacing existing terms linked to the GuacamoleColumn.
     * Note: this operation must make two API calls — one to retrieve the GuacamoleColumn's existing terms,
     * and a second to append the new terms.
     *
     * @param qualifiedName for the GuacamoleColumn
     * @param terms the list of terms to append to the GuacamoleColumn
     * @return the GuacamoleColumn that was updated  (note that it will NOT contain details of the appended terms)
     * @throws AtlanException on any API problems
     */
    public static GuacamoleColumn appendTerms(String qualifiedName, List<IGlossaryTerm> terms) throws AtlanException {
        return (GuacamoleColumn) Asset.appendTerms(Atlan.getDefaultClient(), TYPE_NAME, qualifiedName, terms);
    }

    /**
     * Remove terms from a GuacamoleColumn, without replacing all existing terms linked to the GuacamoleColumn.
     * Note: this operation must make two API calls — one to retrieve the GuacamoleColumn's existing terms,
     * and a second to remove the provided terms.
     *
     * @param qualifiedName for the GuacamoleColumn
     * @param terms the list of terms to remove from the GuacamoleColumn, which must be referenced by GUID
     * @return the GuacamoleColumn that was updated (note that it will NOT contain details of the resulting terms)
     * @throws AtlanException on any API problems
     */
    public static GuacamoleColumn removeTerms(String qualifiedName, List<IGlossaryTerm> terms) throws AtlanException {
        return (GuacamoleColumn) Asset.removeTerms(Atlan.getDefaultClient(), TYPE_NAME, qualifiedName, terms);
    }

    /**
     * Add Atlan tags to a GuacamoleColumn, without replacing existing Atlan tags linked to the GuacamoleColumn.
     * Note: this operation must make two API calls — one to retrieve the GuacamoleColumn's existing Atlan tags,
     * and a second to append the new Atlan tags.
     *
     * @param qualifiedName of the GuacamoleColumn
     * @param atlanTagNames human-readable names of the Atlan tags to add
     * @throws AtlanException on any API problems
     * @return the updated GuacamoleColumn
     */
    public static GuacamoleColumn appendAtlanTags(String qualifiedName, List<String> atlanTagNames)
            throws AtlanException {
        return (GuacamoleColumn)
                Asset.appendAtlanTags(Atlan.getDefaultClient(), TYPE_NAME, qualifiedName, atlanTagNames);
    }

    /**
     * Add Atlan tags to a GuacamoleColumn, without replacing existing Atlan tags linked to the GuacamoleColumn.
     * Note: this operation must make two API calls — one to retrieve the GuacamoleColumn's existing Atlan tags,
     * and a second to append the new Atlan tags.
     *
     * @param qualifiedName of the GuacamoleColumn
     * @param atlanTagNames human-readable names of the Atlan tags to add
     * @param propagate whether to propagate the Atlan tag (true) or not (false)
     * @param removePropagationsOnDelete whether to remove the propagated Atlan tags when the Atlan tag is removed from this asset (true) or not (false)
     * @param restrictLineagePropagation whether to avoid propagating through lineage (true) or do propagate through lineage (false)
     * @throws AtlanException on any API problems
     * @return the updated GuacamoleColumn
     */
    public static GuacamoleColumn appendAtlanTags(
            String qualifiedName,
            List<String> atlanTagNames,
            boolean propagate,
            boolean removePropagationsOnDelete,
            boolean restrictLineagePropagation)
            throws AtlanException {
        return (GuacamoleColumn) Asset.appendAtlanTags(
                Atlan.getDefaultClient(),
                TYPE_NAME,
                qualifiedName,
                atlanTagNames,
                propagate,
                removePropagationsOnDelete,
                restrictLineagePropagation);
    }

    /**
     * Add Atlan tags to a GuacamoleColumn.
     *
     * @param qualifiedName of the GuacamoleColumn
     * @param atlanTagNames human-readable names of the Atlan tags to add
     * @throws AtlanException on any API problems, or if any of the Atlan tags already exist on the GuacamoleColumn
     * @deprecated see {@link #appendAtlanTags(String, List)} instead
     */
    @Deprecated
    public static void addAtlanTags(String qualifiedName, List<String> atlanTagNames) throws AtlanException {
        Asset.addAtlanTags(Atlan.getDefaultClient(), TYPE_NAME, qualifiedName, atlanTagNames);
    }

    /**
     * Add Atlan tags to a GuacamoleColumn.
     *
     * @param qualifiedName of the GuacamoleColumn
     * @param atlanTagNames human-readable names of the Atlan tags to add
     * @param propagate whether to propagate the Atlan tag (true) or not (false)
     * @param removePropagationsOnDelete whether to remove the propagated Atlan tags when the Atlan tag is removed from this asset (true) or not (false)
     * @param restrictLineagePropagation whether to avoid propagating through lineage (true) or do propagate through lineage (false)
     * @throws AtlanException on any API problems, or if any of the Atlan tags already exist on the GuacamoleColumn
     * @deprecated see {@link #appendAtlanTags(String, List, boolean, boolean, boolean)} instead
     */
    @Deprecated
    public static void addAtlanTags(
            String qualifiedName,
            List<String> atlanTagNames,
            boolean propagate,
            boolean removePropagationsOnDelete,
            boolean restrictLineagePropagation)
            throws AtlanException {
        Asset.addAtlanTags(
                Atlan.getDefaultClient(),
                TYPE_NAME,
                qualifiedName,
                atlanTagNames,
                propagate,
                removePropagationsOnDelete,
                restrictLineagePropagation);
    }

    /**
     * Remove an Atlan tag from a GuacamoleColumn.
     *
     * @param qualifiedName of the GuacamoleColumn
     * @param atlanTagName human-readable name of the Atlan tag to remove
     * @throws AtlanException on any API problems, or if the Atlan tag does not exist on the GuacamoleColumn
     */
    public static void removeAtlanTag(String qualifiedName, String atlanTagName) throws AtlanException {
        Asset.removeAtlanTag(Atlan.getDefaultClient(), TYPE_NAME, qualifiedName, atlanTagName);
    }
}
