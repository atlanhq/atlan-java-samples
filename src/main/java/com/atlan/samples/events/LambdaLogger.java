/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.atlan.events.AbstractLambdaHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LambdaLogger extends AbstractLambdaHandler {
    /**
     * Default constructor - pass handler up to superclass.
     */
    public LambdaLogger() {
        super(EventLogger.getInstance());
    }
}
