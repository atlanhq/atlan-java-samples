/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.caches;

import com.atlan.exception.AtlanException;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.Asset;
import com.atlan.model.assets.Glossary;
import com.atlan.model.assets.GlossaryCategory;
import com.atlan.model.assets.IGlossaryCategory;
import com.atlan.samples.loaders.models.CategoryEnrichmentDetails;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CategoryCache extends AssetCache {

    /** {@inheritDoc} */
    protected Asset lookupAsset(String identity) {
        String glossaryName = CategoryEnrichmentDetails.getGlossaryNameFromIdentity(identity);
        try {
            // Since there can be multiple categories with the same name, and building up an
            // entire category path is a recursive operation, it'll likely be best if we just
            // bulk-load the entire hierarchy from a glossary for caching purposes
            Asset foundCategory = null;
            Glossary glossary = Glossary.findByName(glossaryName, List.of("name"));
            Glossary.CategoryHierarchy hierarchy = glossary.getHierarchy(List.of("anchor"));
            for (IGlossaryCategory category : hierarchy.breadthFirst()) {
                String categoryPath = getCategoryPath(hierarchy, category);
                String categoryId = CategoryEnrichmentDetails.getIdentity(categoryPath, glossary);
                put(categoryId, (Asset) category);
                if (categoryId.equals(identity)) {
                    foundCategory = (Asset) category;
                }
            }
            return foundCategory;
        } catch (NotFoundException e) {
            log.info("No existing categories found in glossary: {}", glossaryName);
        } catch (AtlanException e) {
            log.error("Unable to lookup or find category: {}", identity, e);
        }
        return null;
    }

    private String getCategoryPath(Glossary.CategoryHierarchy hierarchy, IGlossaryCategory category) {
        String parentPath = null;
        if (category.getParentCategory() != null) {
            GlossaryCategory parent =
                    hierarchy.getCategory(category.getParentCategory().getGuid());
            parentPath = getCategoryPath(hierarchy, parent);
        }
        return parentPath == null ? category.getName() : parentPath + "@" + category.getName();
    }
}
