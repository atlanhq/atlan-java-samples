/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.probable.guacamole.model.enums;

import com.atlan.model.enums.AtlanSearchableField;
import javax.annotation.processing.Generated;
import lombok.Getter;

@Generated(value = "com.probable.guacamole.generators.POJOGenerator")
public enum RankFields implements AtlanSearchableField {
    /** TBC */
    POPULARITY_SCORE("popularityScore.rank_feature"),
    /** TBC */
    VIEW_SCORE("viewScore.rank_feature"),
    ;

    @Getter(onMethod_ = {@Override})
    private final String indexedFieldName;

    RankFields(String indexedFieldName) {
        this.indexedFieldName = indexedFieldName;
    }
}
