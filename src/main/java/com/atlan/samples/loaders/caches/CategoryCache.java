/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.caches;

import com.atlan.exception.AtlanException;
import com.atlan.model.assets.Asset;
import com.atlan.model.assets.GlossaryCategory;
import com.atlan.samples.loaders.models.CategoryEnrichmentDetails;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CategoryCache extends AssetCache {

    /** {@inheritDoc} */
    protected Asset lookupAsset(String identity) {
        String categoryPath = CategoryEnrichmentDetails.getPathFromIdentity(identity);
        String categoryName = CategoryEnrichmentDetails.getNameFromPath(categoryPath);
        String glossaryName = CategoryEnrichmentDetails.getGlossaryNameFromIdentity(identity);
        try {
            // TODO: technically there can be multiple categories with the same name in a
            //  given glossary, so we really need a findByPath() method instead
            return GlossaryCategory.findByName(categoryName, glossaryName, null);
        } catch (AtlanException e) {
            log.error("Unable to lookup or find category: {}", identity, e);
        }
        return null;
    }
}
