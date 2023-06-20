/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.probable.guacamole.model.enums;

import com.atlan.model.enums.AtlanSearchableField;
import javax.annotation.processing.Generated;
import lombok.Getter;

@Generated(value = "com.probable.guacamole.generators.POJOGenerator")
public enum StemmedFields implements AtlanSearchableField {
    /** TBC */
    DATA_STUDIO_ASSET_TITLE("dataStudioAssetTitle.stemmed"),
    /** TBC */
    NAME("name.stemmed"),
    /** TBC */
    PRESET_DASHBOARD_CHANGED_BY_NAME("presetDashboardChangedByName.stemmed"),
    /** TBC */
    PRESET_DATASET_DATASOURCE_NAME("presetDatasetDatasourceName.stemmed"),
    ;

    @Getter(onMethod_ = {@Override})
    private final String indexedFieldName;

    StemmedFields(String indexedFieldName) {
        this.indexedFieldName = indexedFieldName;
    }
}
