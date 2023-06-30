/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import io.numaproj.numaflow.function.FunctionServer;
import lombok.extern.slf4j.Slf4j;

/**
 * An experiment to dynamically run through (a subset of) playbook actions and apply them to any
 * of the assets flowing through events.
 */
@Slf4j
public class NumaflowPlaybookRunner extends AbstractNumaflowHandler {
    /**
     * Default constructor - pass handler up to superclass.
     */
    public NumaflowPlaybookRunner() {
        super(PlaybookRunner.getInstance());
    }

    /**
     * Register the event processing function.
     *
     * @param args (unused)
     * @throws Exception on any errors starting the event processor
     */
    public static void main(String[] args) throws Exception {
        new FunctionServer().registerMapHandler(new NumaflowPlaybookRunner()).start();
    }
}
