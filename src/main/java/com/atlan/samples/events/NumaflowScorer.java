/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import io.numaproj.numaflow.function.FunctionServer;
import lombok.extern.slf4j.Slf4j;

/**
 * An example to calculate a Data as a Product (DaaP) completeness score based on
 * the level of enrichment of an asset.
 */
@Slf4j
public class NumaflowScorer extends AbstractNumaflowHandler {
    /**
     * Default constructor - pass handler up to superclass.
     */
    public NumaflowScorer() {
        super(DaapScoreCalculator.getInstance());
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
