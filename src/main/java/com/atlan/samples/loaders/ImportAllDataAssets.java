/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders;

import com.atlan.model.assets.Asset;
import com.atlan.model.fields.AtlanField;
import java.util.List;

public class ImportAllDataAssets extends AssetLoader {

    /** {@inheritDoc} */
    @Override
    public List<AtlanField> getAttributesToOverwrite() {
        return List.of(Asset.CERTIFICATE_STATUS);
    }

    public static void main(String[] args) {
        ImportAllDataAssets iada = new ImportAllDataAssets();
        iada.handleRequest(prepEvent(), null);
    }
}
