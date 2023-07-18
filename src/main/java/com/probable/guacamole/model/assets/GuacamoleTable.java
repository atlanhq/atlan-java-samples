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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.probable.guacamole.model.enums.GuacamoleTemperature;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import javax.annotation.processing.Generated;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Specialized form of a table specific to Guacamole.
 */
@Generated(value = "com.probable.guacamole.generators.POJOGenerator")
@Getter
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Slf4j
@SuppressWarnings("cast")
public class GuacamoleTable extends Asset implements IGuacamoleTable, ITable, ISQL, ICatalog, IAsset, IReferenceable {
    private static final long serialVersionUID = 2L;

    public static final String TYPE_NAME = "GuacamoleTable";

    /** Fixed typeName for GuacamoleTables. */
    @Getter(onMethod_ = {@Override})
    @Builder.Default
    String typeName = TYPE_NAME;

    /** TBC */
    @Attribute
    String alias;

    /** TBC */
    @Attribute
    Long columnCount;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<IColumn> columns;

    /** TBC */
    @Attribute
    String databaseName;

    @Attribute
    @Singular
    SortedSet<IDbtTest> dbtTests;

    /** TBC */
    @Attribute
    String databaseQualifiedName;

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
    @Singular
    SortedSet<ITable> dimensions;

    /** TBC */
    @Attribute
    String externalLocation;

    /** TBC */
    @Attribute
    String externalLocationFormat;

    /** TBC */
    @Attribute
    String externalLocationRegion;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<ITable> facts;

    /** Whether this table is currently archived (true) or not (false). */
    @Attribute
    Boolean guacamoleArchived;

    /** Specialized columns contained within this specialized table. */
    @Attribute
    @Singular
    SortedSet<IGuacamoleColumn> guacamoleColumns;

    /** Consolidated quantification metric spanning number of columns, rows, and sparsity of population. */
    @Attribute
    Long guacamoleSize;

    /** Rough measure of the IOPS allocated to the table's processing. */
    @Attribute
    GuacamoleTemperature guacamoleTemperature;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<ILineageProcess> inputToProcesses;

    /** TBC */
    @Attribute
    Boolean isPartitioned;

    /** TBC */
    @Attribute
    Boolean isProfiled;

    /** TBC */
    @Attribute
    Boolean isQueryPreview;

    /** TBC */
    @Attribute
    Boolean isTemporary;

    /** TBC */
    @Attribute
    Long lastProfiledAt;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<ILineageProcess> outputFromProcesses;

    /** TBC */
    @Attribute
    Long partitionCount;

    /** TBC */
    @Attribute
    String partitionList;

    /** TBC */
    @Attribute
    String partitionStrategy;

    /** TBC */
    @Attribute
    @Singular
    SortedSet<ITablePartition> partitions;

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
    @Singular("putQueryPreviewConfig")
    Map<String, String> queryPreviewConfig;

    /** TBC */
    @Attribute
    Long queryUserCount;

    /** TBC */
    @Attribute
    @Singular("putQueryUserMap")
    Map<String, Long> queryUserMap;

    /** TBC */
    @Attribute
    Long rowCount;

    /** TBC */
    @Attribute
    @JsonProperty("atlanSchema")
    ISchema schema;

    /** TBC */
    @Attribute
    String schemaName;

    /** TBC */
    @Attribute
    String schemaQualifiedName;

    /** TBC */
    @Attribute
    Long sizeBytes;

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
    String tableName;

    /** TBC */
    @Attribute
    String tableQualifiedName;

    /** TBC */
    @Attribute
    String viewName;

    /** TBC */
    @Attribute
    String viewQualifiedName;

    /**
     * Reference to a GuacamoleTable by GUID.
     *
     * @param guid the GUID of the GuacamoleTable to reference
     * @return reference to a GuacamoleTable that can be used for defining a relationship to a GuacamoleTable
     */
    public static GuacamoleTable refByGuid(String guid) {
        return GuacamoleTable.builder().guid(guid).build();
    }

    /**
     * Reference to a GuacamoleTable by qualifiedName.
     *
     * @param qualifiedName the qualifiedName of the GuacamoleTable to reference
     * @return reference to a GuacamoleTable that can be used for defining a relationship to a GuacamoleTable
     */
    public static GuacamoleTable refByQualifiedName(String qualifiedName) {
        return GuacamoleTable.builder()
                .uniqueAttributes(
                        UniqueAttributes.builder().qualifiedName(qualifiedName).build())
                .build();
    }

    /**
     * Retrieves a GuacamoleTable by its GUID, complete with all of its relationships.
     *
     * @param guid of the GuacamoleTable to retrieve
     * @return the requested full GuacamoleTable, complete with all of its relationships
     * @throws AtlanException on any error during the API invocation, such as the {@link NotFoundException} if the GuacamoleTable does not exist or the provided GUID is not a GuacamoleTable
     */
    public static GuacamoleTable retrieveByGuid(String guid) throws AtlanException {
        Asset asset = Asset.retrieveFull(guid);
        if (asset == null) {
            throw new NotFoundException(ErrorCode.ASSET_NOT_FOUND_BY_GUID, guid);
        } else if (asset instanceof GuacamoleTable) {
            return (GuacamoleTable) asset;
        } else {
            throw new NotFoundException(ErrorCode.ASSET_NOT_TYPE_REQUESTED, guid, "GuacamoleTable");
        }
    }

    /**
     * Retrieves a GuacamoleTable by its qualifiedName, complete with all of its relationships.
     *
     * @param qualifiedName of the GuacamoleTable to retrieve
     * @return the requested full GuacamoleTable, complete with all of its relationships
     * @throws AtlanException on any error during the API invocation, such as the {@link NotFoundException} if the GuacamoleTable does not exist
     */
    public static GuacamoleTable retrieveByQualifiedName(String qualifiedName) throws AtlanException {
        Asset asset = Asset.retrieveFull(Atlan.getDefaultClient(), TYPE_NAME, qualifiedName);
        if (asset instanceof GuacamoleTable) {
            return (GuacamoleTable) asset;
        } else {
            throw new NotFoundException(ErrorCode.ASSET_NOT_FOUND_BY_QN, qualifiedName, "GuacamoleTable");
        }
    }

    /**
     * Restore the archived (soft-deleted) GuacamoleTable to active.
     *
     * @param qualifiedName for the GuacamoleTable
     * @return true if the GuacamoleTable is now active, and false otherwise
     * @throws AtlanException on any API problems
     */
    public static boolean restore(String qualifiedName) throws AtlanException {
        return Asset.restore(Atlan.getDefaultClient(), TYPE_NAME, qualifiedName);
    }

    /**
     * Builds the minimal object necessary to update a GuacamoleTable.
     *
     * @param qualifiedName of the GuacamoleTable
     * @param name of the GuacamoleTable
     * @return the minimal request necessary to update the GuacamoleTable, as a builder
     */
    public static GuacamoleTableBuilder<?, ?> updater(String qualifiedName, String name) {
        return GuacamoleTable.builder().qualifiedName(qualifiedName).name(name);
    }

    /**
     * Builds the minimal object necessary to apply an update to a GuacamoleTable, from a potentially
     * more-complete GuacamoleTable object.
     *
     * @return the minimal object necessary to update the GuacamoleTable, as a builder
     * @throws InvalidRequestException if any of the minimal set of required properties for GuacamoleTable are not found in the initial object
     */
    @Override
    public GuacamoleTableBuilder<?, ?> trimToRequired() throws InvalidRequestException {
        List<String> missing = new ArrayList<>();
        if (this.getQualifiedName() == null || this.getQualifiedName().length() == 0) {
            missing.add("qualifiedName");
        }
        if (this.getName() == null || this.getName().length() == 0) {
            missing.add("name");
        }
        if (!missing.isEmpty()) {
            throw new InvalidRequestException(
                    ErrorCode.MISSING_REQUIRED_UPDATE_PARAM, "GuacamoleTable", String.join(",", missing));
        }
        return updater(this.getQualifiedName(), this.getName());
    }

    /**
     * Remove the system description from a GuacamoleTable.
     *
     * @param qualifiedName of the GuacamoleTable
     * @param name of the GuacamoleTable
     * @return the updated GuacamoleTable, or null if the removal failed
     * @throws AtlanException on any API problems
     */
    public static GuacamoleTable removeDescription(String qualifiedName, String name) throws AtlanException {
        return (GuacamoleTable) Asset.removeDescription(Atlan.getDefaultClient(), updater(qualifiedName, name));
    }

    /**
     * Remove the user's description from a GuacamoleTable.
     *
     * @param qualifiedName of the GuacamoleTable
     * @param name of the GuacamoleTable
     * @return the updated GuacamoleTable, or null if the removal failed
     * @throws AtlanException on any API problems
     */
    public static GuacamoleTable removeUserDescription(String qualifiedName, String name) throws AtlanException {
        return (GuacamoleTable) Asset.removeUserDescription(Atlan.getDefaultClient(), updater(qualifiedName, name));
    }

    /**
     * Remove the owners from a GuacamoleTable.
     *
     * @param qualifiedName of the GuacamoleTable
     * @param name of the GuacamoleTable
     * @return the updated GuacamoleTable, or null if the removal failed
     * @throws AtlanException on any API problems
     */
    public static GuacamoleTable removeOwners(String qualifiedName, String name) throws AtlanException {
        return (GuacamoleTable) Asset.removeOwners(Atlan.getDefaultClient(), updater(qualifiedName, name));
    }

    /**
     * Update the certificate on a GuacamoleTable.
     *
     * @param qualifiedName of the GuacamoleTable
     * @param certificate to use
     * @param message (optional) message, or null if no message
     * @return the updated GuacamoleTable, or null if the update failed
     * @throws AtlanException on any API problems
     */
    public static GuacamoleTable updateCertificate(String qualifiedName, CertificateStatus certificate, String message)
            throws AtlanException {
        return (GuacamoleTable) Asset.updateCertificate(
                Atlan.getDefaultClient(), builder(), TYPE_NAME, qualifiedName, certificate, message);
    }

    /**
     * Remove the certificate from a GuacamoleTable.
     *
     * @param qualifiedName of the GuacamoleTable
     * @param name of the GuacamoleTable
     * @return the updated GuacamoleTable, or null if the removal failed
     * @throws AtlanException on any API problems
     */
    public static GuacamoleTable removeCertificate(String qualifiedName, String name) throws AtlanException {
        return (GuacamoleTable) Asset.removeCertificate(Atlan.getDefaultClient(), updater(qualifiedName, name));
    }

    /**
     * Update the announcement on a GuacamoleTable.
     *
     * @param qualifiedName of the GuacamoleTable
     * @param type type of announcement to set
     * @param title (optional) title of the announcement to set (or null for no title)
     * @param message (optional) message of the announcement to set (or null for no message)
     * @return the result of the update, or null if the update failed
     * @throws AtlanException on any API problems
     */
    public static GuacamoleTable updateAnnouncement(
            String qualifiedName, AtlanAnnouncementType type, String title, String message) throws AtlanException {
        return (GuacamoleTable) Asset.updateAnnouncement(
                Atlan.getDefaultClient(), builder(), TYPE_NAME, qualifiedName, type, title, message);
    }

    /**
     * Remove the announcement from a GuacamoleTable.
     *
     * @param qualifiedName of the GuacamoleTable
     * @param name of the GuacamoleTable
     * @return the updated GuacamoleTable, or null if the removal failed
     * @throws AtlanException on any API problems
     */
    public static GuacamoleTable removeAnnouncement(String qualifiedName, String name) throws AtlanException {
        return (GuacamoleTable) Asset.removeAnnouncement(Atlan.getDefaultClient(), updater(qualifiedName, name));
    }

    /**
     * Replace the terms linked to the GuacamoleTable.
     *
     * @param qualifiedName for the GuacamoleTable
     * @param name human-readable name of the GuacamoleTable
     * @param terms the list of terms to replace on the GuacamoleTable, or null to remove all terms from the GuacamoleTable
     * @return the GuacamoleTable that was updated (note that it will NOT contain details of the replaced terms)
     * @throws AtlanException on any API problems
     */
    public static GuacamoleTable replaceTerms(String qualifiedName, String name, List<IGlossaryTerm> terms)
            throws AtlanException {
        return (GuacamoleTable) Asset.replaceTerms(Atlan.getDefaultClient(), updater(qualifiedName, name), terms);
    }

    /**
     * Link additional terms to the GuacamoleTable, without replacing existing terms linked to the GuacamoleTable.
     * Note: this operation must make two API calls — one to retrieve the GuacamoleTable's existing terms,
     * and a second to append the new terms.
     *
     * @param qualifiedName for the GuacamoleTable
     * @param terms the list of terms to append to the GuacamoleTable
     * @return the GuacamoleTable that was updated  (note that it will NOT contain details of the appended terms)
     * @throws AtlanException on any API problems
     */
    public static GuacamoleTable appendTerms(String qualifiedName, List<IGlossaryTerm> terms) throws AtlanException {
        return (GuacamoleTable) Asset.appendTerms(Atlan.getDefaultClient(), TYPE_NAME, qualifiedName, terms);
    }

    /**
     * Remove terms from a GuacamoleTable, without replacing all existing terms linked to the GuacamoleTable.
     * Note: this operation must make two API calls — one to retrieve the GuacamoleTable's existing terms,
     * and a second to remove the provided terms.
     *
     * @param qualifiedName for the GuacamoleTable
     * @param terms the list of terms to remove from the GuacamoleTable, which must be referenced by GUID
     * @return the GuacamoleTable that was updated (note that it will NOT contain details of the resulting terms)
     * @throws AtlanException on any API problems
     */
    public static GuacamoleTable removeTerms(String qualifiedName, List<IGlossaryTerm> terms) throws AtlanException {
        return (GuacamoleTable) Asset.removeTerms(Atlan.getDefaultClient(), TYPE_NAME, qualifiedName, terms);
    }

    /**
     * Add Atlan tags to a GuacamoleTable, without replacing existing Atlan tags linked to the GuacamoleTable.
     * Note: this operation must make two API calls — one to retrieve the GuacamoleTable's existing Atlan tags,
     * and a second to append the new Atlan tags.
     *
     * @param qualifiedName of the GuacamoleTable
     * @param atlanTagNames human-readable names of the Atlan tags to add
     * @throws AtlanException on any API problems
     * @return the updated GuacamoleTable
     */
    public static GuacamoleTable appendAtlanTags(String qualifiedName, List<String> atlanTagNames)
            throws AtlanException {
        return (GuacamoleTable)
                Asset.appendAtlanTags(Atlan.getDefaultClient(), TYPE_NAME, qualifiedName, atlanTagNames);
    }

    /**
     * Add Atlan tags to a GuacamoleTable, without replacing existing Atlan tags linked to the GuacamoleTable.
     * Note: this operation must make two API calls — one to retrieve the GuacamoleTable's existing Atlan tags,
     * and a second to append the new Atlan tags.
     *
     * @param qualifiedName of the GuacamoleTable
     * @param atlanTagNames human-readable names of the Atlan tags to add
     * @param propagate whether to propagate the Atlan tag (true) or not (false)
     * @param removePropagationsOnDelete whether to remove the propagated Atlan tags when the Atlan tag is removed from this asset (true) or not (false)
     * @param restrictLineagePropagation whether to avoid propagating through lineage (true) or do propagate through lineage (false)
     * @throws AtlanException on any API problems
     * @return the updated GuacamoleTable
     */
    public static GuacamoleTable appendAtlanTags(
            String qualifiedName,
            List<String> atlanTagNames,
            boolean propagate,
            boolean removePropagationsOnDelete,
            boolean restrictLineagePropagation)
            throws AtlanException {
        return (GuacamoleTable) Asset.appendAtlanTags(
                Atlan.getDefaultClient(),
                TYPE_NAME,
                qualifiedName,
                atlanTagNames,
                propagate,
                removePropagationsOnDelete,
                restrictLineagePropagation);
    }

    /**
     * Add Atlan tags to a GuacamoleTable.
     *
     * @param qualifiedName of the GuacamoleTable
     * @param atlanTagNames human-readable names of the Atlan tags to add
     * @throws AtlanException on any API problems, or if any of the Atlan tags already exist on the GuacamoleTable
     * @deprecated see {@link #appendAtlanTags(String, List)} instead
     */
    @Deprecated
    public static void addAtlanTags(String qualifiedName, List<String> atlanTagNames) throws AtlanException {
        Asset.addAtlanTags(Atlan.getDefaultClient(), TYPE_NAME, qualifiedName, atlanTagNames);
    }

    /**
     * Add Atlan tags to a GuacamoleTable.
     *
     * @param qualifiedName of the GuacamoleTable
     * @param atlanTagNames human-readable names of the Atlan tags to add
     * @param propagate whether to propagate the Atlan tag (true) or not (false)
     * @param removePropagationsOnDelete whether to remove the propagated Atlan tags when the Atlan tag is removed from this asset (true) or not (false)
     * @param restrictLineagePropagation whether to avoid propagating through lineage (true) or do propagate through lineage (false)
     * @throws AtlanException on any API problems, or if any of the Atlan tags already exist on the GuacamoleTable
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
     * Remove an Atlan tag from a GuacamoleTable.
     *
     * @param qualifiedName of the GuacamoleTable
     * @param atlanTagName human-readable name of the Atlan tag to remove
     * @throws AtlanException on any API problems, or if the Atlan tag does not exist on the GuacamoleTable
     */
    public static void removeAtlanTag(String qualifiedName, String atlanTagName) throws AtlanException {
        Asset.removeAtlanTag(Atlan.getDefaultClient(), TYPE_NAME, qualifiedName, atlanTagName);
    }
}
