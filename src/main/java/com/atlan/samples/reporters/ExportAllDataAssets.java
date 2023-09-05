/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.reporters;

import com.atlan.Atlan;
import com.atlan.model.assets.*;
import com.atlan.model.fields.AtlanField;
import com.atlan.model.search.FluentSearch;
import java.util.List;

public class ExportAllDataAssets extends AssetReporter {

    /** {@inheritDoc} */
    @Override
    public FluentSearch.FluentSearchBuilder<?, ?> getAssetsToExtract() {
        return Atlan.getDefaultClient()
                .assets
                .select()
                .where(Asset.QUALIFIED_NAME.startsWith("default"))
                .whereNot(FluentSearch.superTypes(List.of(IAccessControl.TYPE_NAME, INamespace.TYPE_NAME)))
                .whereNot(FluentSearch.assetTypes(
                        List.of(AuthPolicy.TYPE_NAME, Procedure.TYPE_NAME, AtlanQuery.TYPE_NAME)));
    }

    /** {@inheritDoc} */
    @Override
    public List<AtlanField> getAttributesToExtract() {
        /*try {
            // Example: including custom metadata fields (of course, add this 'rating' variable to the list
            // below to include it in the set of attributes to extract.
            CustomMetadataField rating = new CustomMetadataField(Atlan.getDefaultClient(), "QD", "Rating");
        } catch (AtlanException e) {
            e.printStackTrace();
        }*/
        return List.of(
                Asset.NAME,
                Asset.DESCRIPTION,
                Asset.USER_DESCRIPTION,
                Asset.OWNER_USERS,
                Asset.OWNER_GROUPS,
                Asset.CERTIFICATE_STATUS,
                Asset.CERTIFICATE_STATUS_MESSAGE,
                Asset.CERTIFICATE_UPDATED_BY,
                Asset.CERTIFICATE_UPDATED_AT,
                Asset.ANNOUNCEMENT_TYPE,
                Asset.ANNOUNCEMENT_TITLE,
                Asset.ANNOUNCEMENT_MESSAGE,
                Asset.ANNOUNCEMENT_UPDATED_BY,
                Asset.ANNOUNCEMENT_UPDATED_AT,
                Asset.CREATED_BY,
                Asset.CREATE_TIME,
                Asset.UPDATED_BY,
                Asset.UPDATE_TIME,
                Asset.ASSIGNED_TERMS,
                IAWS.AWS_ARN); // Note that for any type that has extra mandatory fields, those must be included
        // TODO: need to add idempotency for these... Asset.ATLAN_TAGS);
    }

    public static void main(String[] args) {
        ExportAllDataAssets eada = new ExportAllDataAssets();
        eada.handleRequest(prepEvent(), null);
    }
}
