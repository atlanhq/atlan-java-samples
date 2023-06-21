/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.amazonaws.services.lambda.runtime.Context;
import com.atlan.events.AbstractLambdaHandler;
import com.atlan.exception.AtlanException;
import com.atlan.model.events.AtlanEvent;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * An example to calculate a Data as a Product (DaaP) completeness score based on
 * the level of enrichment of an asset.
 */
@Slf4j
public class LambdaScorer extends AbstractLambdaHandler implements DaapScoreCalculator {
    /**
     * {@inheritDoc}
     */
    @Override
    public void processEvent(AtlanEvent event, Context context) throws IOException {
        if (DaapScoreCalculator.validatePrerequisites(event, log)) {
            try {
                DaapScoreCalculator.calculateScore(event.getPayload().getAsset(), log);
            } catch (AtlanException e) {
                throw new IOException(
                        "Unable to update DaaP completeness score for: "
                                + event.getPayload().getAsset().getQualifiedName(),
                        e);
            }
        } else {
            log.warn("Custom metadata failed to be created, or there was no asset to score.");
        }
    }
}
