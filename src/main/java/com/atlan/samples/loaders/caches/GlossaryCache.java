/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.caches;

import com.atlan.exception.AtlanException;
import com.atlan.model.assets.Asset;
import com.atlan.model.assets.Glossary;
import com.atlan.samples.loaders.models.GlossaryEnrichmentDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GlossaryCache extends AssetCache {

    /** {@inheritDoc} */
    protected Asset lookupAsset(String identity) {
        String glossaryName = GlossaryEnrichmentDetails.getNameFromIdentity(identity);
        try {
            Glossary glossary = Glossary.findByName(glossaryName, null);
            if (glossary != null) {
                return Glossary.refByGuid(glossary.getGuid());
            }
        } catch (AtlanException e) {
            log.error("Unable to lookup or find glossary: {}", identity, e);
        }
        return null;
    }
}
