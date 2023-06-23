/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.atlan.events.AbstractLambdaHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * An example to automatically revert any asset that is marked VERIFIED if it does not
 * at least have: a description, at least one owner, and lineage.
 */
@Slf4j
public class LambdaEnforcer extends AbstractLambdaHandler {
    /**
     * Default constructor - pass handler up to superclass.
     */
    public LambdaEnforcer() {
        super(VerificationEnforcer.getInstance());
    }
}
