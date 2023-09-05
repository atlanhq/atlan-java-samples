/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.readers;

import com.atlan.model.assets.Asset;
import java.util.List;

public interface AssetGenerator {
    /**
     * Interface to generate an asset from the values of a row.
     *
     * @param row the row of values from which to build the asset
     * @param header list of field names in the same order as columns in the CSV
     * @param typeIdx numeric index within the columns of the typeName field
     * @param qnIdx numeric index within the columns of the qualifiedName field
     * @return the asset built from the values on the row
     */
    Asset buildFromRow(List<String> row, List<String> header, int typeIdx, int qnIdx);
}
