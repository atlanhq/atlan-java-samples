/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders.caches;

import com.atlan.model.assets.Asset;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for lazy-loading a cache of assets based on some human-constructable identity.
 */
public abstract class AssetCache {

    private final Map<String, Asset> cache = new HashMap<>();

    /**
     * Retrieve an asset from the cache, lazily-loading it on any cache misses.
     *
     * @param identity of the asset to retrieve
     * @return the asset with the specified identity
     */
    public Asset get(String identity) {
        if (!cache.containsKey(identity)) {
            cache.put(identity, lookupAsset(identity));
        }
        return cache.get(identity);
    }

    /**
     * Add an asset to the cache.
     *
     * @param identity of the asset to add to the cache
     * @param asset the asset to add to the cache
     */
    public void put(String identity, Asset asset) {
        cache.put(identity, asset);
    }

    /**
     * Indicates whether the cache already contains an asset with a given identity.
     *
     * @param identity of the asset to check for presence in the cache
     * @return true if this identity is already in the cache, false otherwise
     */
    public boolean containsKey(String identity) {
        return cache.containsKey(identity);
    }

    /**
     * Actually go to Atlan and find the asset with the provided identity.
     *
     * @param identity of the asset to lookup
     * @return the asset, from Atlan itself
     */
    protected abstract Asset lookupAsset(String identity);
}
