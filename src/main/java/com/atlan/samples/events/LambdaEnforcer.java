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
 * An example to automatically revert any asset that is marked VERIFIED if it does not
 * at least have: a description, at least one owner, and lineage.
 */
@Slf4j
public class LambdaEnforcer extends AbstractLambdaHandler {
    /**
     * {@inheritDoc}
     */
    @Override
    public void processEvent(AtlanEvent event, Context context) throws IOException {
        if (VerificationEnforcer.validatePrerequisites(event)) {
            try {
                VerificationEnforcer.enforceVerification(event.getPayload().getAsset(), log);
            } catch (AtlanException e) {
                throw new IOException(
                        "Unable to update the asset's certificate: "
                                + event.getPayload().getAsset().getQualifiedName(),
                        e);
            }
        } else {
            log.warn("There was no asset with a verification to enforce.");
        }
    }
}
