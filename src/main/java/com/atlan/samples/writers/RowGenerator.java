/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.writers;

import com.atlan.model.assets.Asset;

public interface RowGenerator {
    /**
     * Interface to generate an iterable set of values for a row.
     *
     * @param asset the asset from which to generate the values
     * @return the values, as an iterable set of strings
     */
    Iterable<String> valuesFromAsset(Asset asset);
}
