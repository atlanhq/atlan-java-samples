/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import io.numaproj.numaflow.function.FunctionServer;
import lombok.extern.slf4j.Slf4j;

/**
 * Very basic example of logging a deserialized event and then passing it on to all
 * subsequent vertexes, unchanged.
 */
@Slf4j
public class NumaflowLogger extends AbstractNumaflowHandler {
    /**
     * Default constructor - pass handler up to superclass.
     */
    public NumaflowLogger() {
        super(EventLogger.getInstance());
    }

    /**
     * Register the event processing function.
     *
     * @param args (unused)
     * @throws Exception on any errors starting the event processor
     */
    public static void main(String[] args) throws Exception {
        new FunctionServer().registerMapHandler(new NumaflowLogger()).start();
    }
}
