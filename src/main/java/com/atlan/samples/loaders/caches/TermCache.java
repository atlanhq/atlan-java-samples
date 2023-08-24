/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.caches;

import com.atlan.exception.AtlanException;
import com.atlan.model.assets.Asset;
import com.atlan.model.assets.GlossaryTerm;
import com.atlan.samples.loaders.models.TermEnrichmentDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TermCache extends AssetCache {

    /** {@inheritDoc} */
    protected Asset lookupAsset(String identity) {
        String termName = TermEnrichmentDetails.getNameFromIdentity(identity);
        String glossaryName = TermEnrichmentDetails.getGlossaryNameFromIdentity(identity);
        try {
            return GlossaryTerm.findByName(termName, glossaryName);
        } catch (AtlanException e) {
            log.error("Unable to lookup or find term: {}", identity, e);
        }
        return null;
    }
}
