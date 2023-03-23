/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.atlan.Atlan;
import com.atlan.exception.AtlanException;
import com.atlan.model.assets.Asset;
import com.atlan.model.enums.KeywordFields;
import com.atlan.model.events.AtlanEvent;
import com.atlan.model.events.AtlanEventPayload;
import com.atlan.model.search.IndexSearchDSL;
import com.atlan.model.search.IndexSearchRequest;
import com.atlan.model.search.IndexSearchResponse;
import com.atlan.serde.Serde;
import com.atlan.util.QueryFactory;
import io.numaproj.numaflow.function.Datum;
import io.numaproj.numaflow.function.Message;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for event handlers.
 */
@Slf4j
public abstract class AbstractEventHandler {

    public static final String FAILURE = "failure";
    public static final String SUCCESS = "success";

    // Set up Atlan connectivity through environment variables
    static {
        Atlan.setBaseUrl(System.getenv("ATLAN_BASE_URL"));
        Atlan.setApiToken(System.getenv("ATLAN_API_KEY"));
    }

    /**
     * Translate the Numaflow message into an Atlan event object.
     * @param data the Numaflow message
     * @return an Atlan event object representation of the message
     */
    static AtlanEvent getAtlanEvent(Datum data) {
        try {
            return Serde.mapper.readValue(data.getValue(), AtlanEvent.class);
        } catch (IOException e) {
            log.error("Unable to deserialize event: {}", new String(data.getValue(), StandardCharsets.UTF_8), e);
        }
        return null;
    }

    /**
     * Retrieve the full asset from Atlan, to ensure we have the latest information about it.
     * Note: this will be slower than getCurrentViewOfAsset, but does not rely on the eventual
     * consistency of the search index so will have the absolute latest information about an
     * asset.
     *
     * @param event containing details about an asset
     * @return the current information about the asset in Atlan, in its entirety
     * @throws AtlanException on any issues communicating with the API
     */
    static Asset getCurrentFullAsset(AtlanEvent event) throws AtlanException {
        AtlanEventPayload payload = event.getPayload();
        if (payload != null && payload.getAsset() != null) {
            return Asset.retrieveFull(payload.getAsset().getGuid());
        }
        return null;
    }

    /**
     * Retrieve a limited set of information about the asset in Atlan,
     * as up-to-date as is available in the search index, to ensure we have
     * reasonably up-to-date information about it.
     * Note: this will be faster than getCurrentFullAsset, but relies on the eventual
     * consistency of the search index so may not have the absolute latest information about
     * an asset.
     *
     * @param event containing details about an asset
     * @param limitedToAttributes the limited set of attributes to retrieve about the asset
     * @param includeMeanings if true, include any assigned terms
     * @param includeClassifications if true, include any assigned classifications
     * @return the current information about the asset in Atlan, limited to what was requested
     * @throws AtlanException on any issues communicating with the API
     */
    static Asset getCurrentViewOfAsset(
            AtlanEvent event,
            Collection<String> limitedToAttributes,
            boolean includeMeanings,
            boolean includeClassifications)
            throws AtlanException {
        AtlanEventPayload payload = event.getPayload();
        if (payload != null && payload.getAsset() != null) {
            Asset asset = payload.getAsset();
            IndexSearchRequest request = IndexSearchRequest.builder()
                    .dsl(IndexSearchDSL.builder()
                            .query(QueryFactory.CompoundQuery.builder()
                                    .must(QueryFactory.beActive())
                                    .must(QueryFactory.beOfType(asset.getTypeName()))
                                    .must(QueryFactory.have(KeywordFields.QUALIFIED_NAME)
                                            .eq(asset.getQualifiedName()))
                                    .build()
                                    ._toQuery())
                            .build())
                    .excludeClassifications(!includeClassifications)
                    .excludeMeanings(!includeMeanings)
                    .attributes(limitedToAttributes == null ? Collections.emptySet() : limitedToAttributes)
                    .build();
            IndexSearchResponse response = request.search();
            if (response != null) {
                if (response.getAssets().size() > 1) {
                    log.warn(
                            "Multiple assets of type {} found with the same qualifiedName {} — returning only the first.",
                            asset.getTypeName(),
                            asset.getQualifiedName());
                }
                if (!response.getAssets().isEmpty()) {
                    return response.getAssets().get(0);
                }
            }
        }
        return null;
    }

    /**
     * Route the message as failed.
     * @param data the Numaflow message
     * @return a message array indicating the message failed to be processed
     */
    static Message[] failed(Datum data) {
        log.info("Routing to: {}", FAILURE);
        return new Message[] {Message.to(FAILURE, data.getValue())};
    }

    /**
     * Route the message as succeeded.
     * @param data the Numaflow message
     * @return a message array indicating the message was successfully processed
     */
    static Message[] succeeded(Datum data) {
        log.info("Routing to: {}", SUCCESS);
        return new Message[] {Message.to(SUCCESS, data.getValue())};
    }
}
