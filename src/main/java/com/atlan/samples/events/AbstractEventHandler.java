/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.atlan.Atlan;
import com.atlan.exception.AtlanException;
import com.atlan.model.assets.Asset;
import com.atlan.model.assets.Catalog;
import com.atlan.model.assets.LineageProcess;
import com.atlan.model.enums.KeywordFields;
import com.atlan.model.events.AtlanEvent;
import com.atlan.model.events.AtlanEventPayload;
import com.atlan.model.search.IndexSearchDSL;
import com.atlan.model.search.IndexSearchRequest;
import com.atlan.model.search.IndexSearchResponse;
import com.atlan.serde.Serde;
import com.atlan.util.QueryFactory;
import io.numaproj.numaflow.function.handlers.MapHandler;
import io.numaproj.numaflow.function.interfaces.Datum;
import io.numaproj.numaflow.function.types.Message;
import io.numaproj.numaflow.function.types.MessageList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for event handlers.
 */
@Slf4j
public abstract class AbstractEventHandler extends MapHandler {

    public static final String FAILURE = "failure";
    public static final String SUCCESS = "success";

    // Set up Atlan connectivity through environment variables
    static {
        Atlan.setBaseUrl(System.getenv("ATLAN_BASE_URL"));
        Atlan.setApiToken(System.getenv("ATLAN_API_KEY"));
    }

    /**
     * Translate the Numaflow message into an Atlan event object.
     *
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
     * Translate the Numaflow message directly into the Atlan asset nested in its payload.
     *
     * @param data the Numaflow message
     * @return the nested asset object in the event, or null if there is none
     */
    static Asset getAssetFromEvent(Datum data) {
        AtlanEvent event = getAtlanEvent(data);
        if (event == null || event.getPayload() == null || event.getPayload().getAsset() == null) {
            log.error("No payload found in event: {}", data);
            return null;
        }
        return event.getPayload().getAsset();
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
            return getCurrentViewOfAsset(
                    payload.getAsset(), limitedToAttributes, includeMeanings, includeClassifications);
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
     * @param fromEvent details of the asset in the event
     * @param limitedToAttributes the limited set of attributes to retrieve about the asset
     * @param includeMeanings if true, include any assigned terms
     * @param includeClassifications if true, include any assigned classifications
     * @return the current information about the asset in Atlan, limited to what was requested
     * @throws AtlanException on any issues communicating with the API
     */
    static Asset getCurrentViewOfAsset(
            Asset fromEvent,
            Collection<String> limitedToAttributes,
            boolean includeMeanings,
            boolean includeClassifications)
            throws AtlanException {
        IndexSearchRequest request = IndexSearchRequest.builder()
                .dsl(IndexSearchDSL.builder()
                        .query(QueryFactory.CompoundQuery.builder()
                                .must(QueryFactory.beActive())
                                .must(QueryFactory.beOfType(fromEvent.getTypeName()))
                                .must(QueryFactory.have(KeywordFields.QUALIFIED_NAME)
                                        .eq(fromEvent.getQualifiedName()))
                                .build()
                                ._toQuery())
                        .build())
                .excludeClassifications(!includeClassifications)
                .excludeMeanings(!includeMeanings)
                .attributes(limitedToAttributes == null ? Collections.emptySet() : limitedToAttributes)
                // Include attributes that are mandatory for updates, for some asset types
                .attribute("anchor")
                .attribute("awsArn")
                .relationAttribute("guid")
                .build();
        IndexSearchResponse response = request.search();
        if (response != null && response.getAssets() != null) {
            if (response.getAssets().size() > 1) {
                log.warn(
                        "Multiple assets of type {} found with the same qualifiedName {} — returning only the first.",
                        fromEvent.getTypeName(),
                        fromEvent.getQualifiedName());
            }
            if (!response.getAssets().isEmpty()) {
                return response.getAssets().get(0);
            }
        }
        return null;
    }

    /**
     * Check if the asset has either a user-provided or system-provided description.
     *
     * @param asset to check for the presence of a description
     * @return true if there is either a user-provided or system-provided description
     */
    static boolean hasDescription(Asset asset) {
        String description = asset.getUserDescription();
        if (description == null || description.length() == 0) {
            description = asset.getDescription();
        }
        return description != null && description.length() > 0;
    }

    /**
     * Check if the asset has any individual or group owners.
     *
     * @param asset to check for the presence of an owner
     * @return true if there is at least one individual or group owner
     */
    static boolean hasOwner(Asset asset) {
        Set<String> ownerUsers = asset.getOwnerUsers();
        Set<String> ownerGroups = asset.getOwnerGroups();
        return (ownerUsers != null && !ownerUsers.isEmpty()) || (ownerGroups != null && !ownerGroups.isEmpty());
    }

    /**
     * Check if the asset has any assigned terms.
     *
     * @param asset to check for the presence of an assigned term
     * @return true if there is at least one assigned term
     */
    static boolean hasAssignedTerms(Asset asset) {
        return (asset.getAssignedTerms() != null && !asset.getAssignedTerms().isEmpty())
                || (asset.getMeanings() != null && !asset.getMeanings().isEmpty());
    }

    /**
     * Check if the asset has any classifications.
     *
     * @param asset to check for the presence of a classification
     * @return true if there is at least one assigned classification
     */
    static boolean hasClassifications(Asset asset) {
        return asset.getClassifications() != null && !asset.getClassifications().isEmpty();
    }

    /**
     * Check if the asset has any lineage.
     *
     * @param asset to check for the presence of lineage
     * @return true if the asset is input to or output from at least one process
     */
    static boolean hasLineage(Asset asset) {
        if (asset instanceof Catalog) {
            // If possible, look directly on inputs and outputs rather than the __hasLineage flag
            Catalog details = (Catalog) asset;
            Set<LineageProcess> downstream = details.getInputToProcesses();
            Set<LineageProcess> upstream = details.getOutputFromProcesses();
            return (downstream != null && !downstream.isEmpty()) || (upstream != null && !upstream.isEmpty());
        } else {
            return asset.getHasLineage();
        }
    }

    /**
     * Route the message as failed.
     *
     * @param keys the Numaflow keys for the message
     * @param data the Numaflow message
     * @return a message list indicating the message failed to be processed
     */
    static MessageList failed(String keys[], Datum data) {
        log.info("Routing to: {}", FAILURE);
        return MessageList.newBuilder()
                .addMessage(new Message(data.getValue(), keys, new String[] {FAILURE}))
                .build();
    }

    /**
     * Route the message as succeeded.
     *
     * @param keys the Numaflow keys for the message
     * @param data the Numaflow message
     * @return a message list indicating the message was successfully processed
     */
    static MessageList succeeded(String keys[], Datum data) {
        log.info("Routing to: {}", SUCCESS);
        return MessageList.newBuilder()
                .addMessage(new Message(data.getValue(), keys, new String[] {SUCCESS}))
                .build();
    }

    /**
     * Route the message forward, as-is.
     *
     * @param data the Numaflow message
     * @return a message list indicating the message should be forwarded as-is
     */
    static MessageList forward(Datum data) {
        return MessageList.newBuilder().addMessage(new Message(data.getValue())).build();
    }

    /**
     * Drop the message. Mostly this should be used when receiving an event that is
     * the result of this handler taking an action on a previous event.
     * (Without this, we could have an infinite loop of that action being applied
     * over and over again.)
     *
     * @return a message list indicating the message can be safely ignored
     */
    static MessageList drop() {
        return MessageList.newBuilder().addMessage(Message.toDrop()).build();
    }
}
