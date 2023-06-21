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
 * An example to calculate a Data as a Product (DaaP) completeness score based on
 * the level of enrichment of an asset.
 */
@Slf4j
public class NumaflowScorer extends AbstractNumaflowHandler implements DaapScoreCalculator {
    /**
     * {@inheritDoc}
     */
    @Override
    public MessageList processEvent(AtlanEvent event, String[] keys, Datum data) {
        if (!DaapScoreCalculator.validatePrerequisites(event, log)) {
            return failed(keys, data);
        }
        try {
            boolean changed =
                    DaapScoreCalculator.calculateScore(event.getPayload().getAsset(), log);
            if (changed) {
                return succeeded(keys, data);
            } else {
                return drop();
            }
        } catch (AtlanException e) {
            log.error(
                    "Unable to update DaaP completeness score for: {}",
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
        new FunctionServer().registerMapHandler(new NumaflowScorer()).start();
    }
}
