/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.amazonaws.services.lambda.runtime.Context;
import com.atlan.events.AbstractLambdaHandler;
import com.atlan.model.events.AtlanEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LambdaLogger extends AbstractLambdaHandler {
    @Override
    public void processEvent(AtlanEvent event, Context context) {
        log.info("Atlan event payload: {}", event.getPayload().toJson());
    }
}
