/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.atlan.events.AbstractNumaflowHandler;
import com.atlan.exception.AtlanException;
import com.atlan.model.events.AtlanEvent;
import io.numaproj.numaflow.function.FunctionServer;
import io.numaproj.numaflow.function.interfaces.Datum;
import io.numaproj.numaflow.function.types.MessageList;
import lombok.extern.slf4j.Slf4j;

/**
 * An example to automatically revert any asset that is marked VERIFIED if it does not
 * at least have: a description, at least one owner, and lineage.
 */
@Slf4j
public class NumaflowEnforcer extends AbstractNumaflowHandler {
    /**
     * {@inheritDoc}
     */
    @Override
    public MessageList processEvent(AtlanEvent event, String[] keys, Datum data) {
        if (!VerificationEnforcer.validatePrerequisites(event)) {
            return failed(keys, data);
        }
        try {
            boolean changed =
                    VerificationEnforcer.enforceVerification(event.getPayload().getAsset(), log);
            if (changed) {
                return succeeded(keys, data);
            } else {
                return drop();
            }
        } catch (AtlanException e) {
            log.error(
                    "Unable to update the asset's certificate: {}",
                    event.getPayload().getAsset().getQualifiedName(),
                    e);
            return failed(keys, data);
        }
    }

    /**
     * Register the event processing function.
     *
     * @param args (unused)
     * @throws Exception on any errors starting the event processor
     */
    public static void main(String[] args) throws Exception {
        new FunctionServer().registerMapHandler(new NumaflowEnforcer()).start();
    }
}
