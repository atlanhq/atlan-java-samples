/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import io.numaproj.numaflow.function.FunctionServer;
import lombok.extern.slf4j.Slf4j;

/**
 * An example to automatically revert any asset that is marked VERIFIED if it does not
 * at least have: a description, at least one owner, and lineage.
 */
@Slf4j
public class NumaflowEnforcer extends AbstractNumaflowHandler {
    /**
     * Default constructor - pass handler up to superclass.
     */
    public NumaflowEnforcer() {
        super(VerificationEnforcer.getInstance());
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
