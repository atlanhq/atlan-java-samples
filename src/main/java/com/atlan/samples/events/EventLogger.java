/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.atlan.events.AtlanEventHandler;
import com.atlan.model.assets.Asset;
import com.atlan.model.events.AtlanEvent;
import java.util.Collection;
import java.util.Set;
import org.slf4j.Logger;

/**
 * An example to only log events that were received, taking no actual action.
 */
public class EventLogger implements AtlanEventHandler {

    /** Singleton for reuse */
    private static final EventLogger INSTANCE = createInstance();

    private static EventLogger createInstance() {
        return new EventLogger();
    }

    public static EventLogger getInstance() {
        return INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public boolean validatePrerequisites(AtlanEvent event, Logger log) {
        // Do nothing, just log the event
        log.info("Atlan event payload: {}", event.getPayload().toJson());
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Asset getCurrentState(Asset fromEvent, Logger log) {
        // Do nothing, just pass-through the asset as-received
        return fromEvent;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<Asset> calculateChanges(Asset asset, Logger log) {
        // Do nothing, just pass-through the asset unchanged
        return Set.of(asset);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasChanges(Asset current, Asset modified, Logger log) {
        // Do nothing, just allow the message to be forwarded along as a no-op
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void upsertChanges(Collection<Asset> changedAssets, Logger log) {
        // Do nothing
    }
}
