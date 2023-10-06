/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import lombok.extern.slf4j.Slf4j;

/**
 * An example to calculate a Data as a Product (DaaP) completeness score based on
 * the level of enrichment of an asset.
 */
@Slf4j
public class LambdaScorer extends AbstractLambdaHandler {
    /**
     * Default constructor - pass handler up to superclass.
     */
    public LambdaScorer() {
        super(DaapScoreCalculator.getInstance());
    }
}
