/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.models;

import com.atlan.model.assets.ADLSAccount;
import com.atlan.samples.loaders.AssetBatch;
import java.util.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Utility class for capturing the full details provided about a database.
 */
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class AccountDetails extends AssetDetails {
    public static final String COL_ACCOUNT = "ACCOUNT NAME";

    private static final List<String> REQUIRED =
            List.of(ConnectionDetails.COL_CONNECTOR, ConnectionDetails.COL_CONNECTION, COL_ACCOUNT);
    private static final List<String> REQUIRED_EMPTY =
            List.of(BucketDetails.COL_BUCKET_NAME, ObjectDetails.COL_OBJECT_NAME);

    @ToString.Include
    private String connectionQualifiedName;

    @ToString.Include
    private String name;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentity() {
        return connectionQualifiedName + "/" + name;
    }

    /**
     * Construct an account's qualifiedName from the row of data and cache of connections.
     *
     * @param connectionCache cache of connections
     * @param row of data
     * @return the qualifiedName for the account on that row of data
     */
    public static String getQualifiedName(Map<ConnectionDetails, String> connectionCache, Map<String, String> row) {
        String connectionQN = ConnectionDetails.getQualifiedName(connectionCache, row);
        if (connectionQN != null) {
            String accountName = row.get(COL_ACCOUNT);
            if (accountName != null) {
                return connectionQN + "/" + accountName;
            }
        }
        return null;
    }

    /**
     * Build up details about the account on the provided row.
     *
     * @param connectionCache a cache of connections that have first been resolved across the spreadsheet
     * @param row a row of data from the spreadsheet, as a map from column name to value
     * @param delim delimiter used in cells that can contain multiple values
     * @return the account details for that row
     */
    public static AccountDetails getFromRow(
            Map<ConnectionDetails, String> connectionCache, Map<String, String> row, String delim) {
        if (getMissingFields(row, REQUIRED).isEmpty()) {
            String connectionQualifiedName = ConnectionDetails.getQualifiedName(connectionCache, row);
            if (getRequiredEmptyFields(row, REQUIRED_EMPTY).isEmpty()) {
                return getFromRow(AccountDetails.builder(), row, delim)
                        .connectionQualifiedName(connectionQualifiedName)
                        .name(row.get(COL_ACCOUNT))
                        .build();
            } else {
                return AccountDetails.builder()
                        .connectionQualifiedName(connectionQualifiedName)
                        .name(row.get(COL_ACCOUNT))
                        .build();
            }
        }
        return null;
    }

    /**
     * Create accounts in bulk, if they do not exist, or update them if they do (idempotent).
     *
     * @param accounts the set of accounts to ensure exist
     * @param batchSize maximum number of accounts to create per batch
     */
    public static void upsert(Map<String, AccountDetails> accounts, int batchSize) {
        AssetBatch batch = new AssetBatch(ADLSAccount.TYPE_NAME, batchSize);
        Map<String, List<String>> toClassify = new HashMap<>();

        for (AccountDetails details : accounts.values()) {
            String connectionQualifiedName = details.getConnectionQualifiedName();
            String accountName = details.getName();
            ADLSAccount account = ADLSAccount.creator(accountName, connectionQualifiedName)
                    .description(details.getDescription())
                    .certificateStatus(details.getCertificate())
                    .certificateStatusMessage(details.getCertificateStatusMessage())
                    .announcementType(details.getAnnouncementType())
                    .announcementTitle(details.getAnnouncementTitle())
                    .announcementMessage(details.getAnnouncementMessage())
                    .ownerUsers(details.getOwnerUsers())
                    .ownerGroups(details.getOwnerGroups())
                    .build();
            if (!details.getClassifications().isEmpty()) {
                toClassify.put(account.getQualifiedName(), details.getClassifications());
            }
            batch.add(account);
        }
        // And don't forget to flush out any that remain
        batch.flush();

        // Classifications must be added in a second pass, after the asset exists
        appendClassifications(toClassify, ADLSAccount.TYPE_NAME);
    }
}
