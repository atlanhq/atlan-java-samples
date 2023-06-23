/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.atlan.events.AbstractLambdaHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * An experiment to dynamically run through (a subset of) playbook actions and apply them to any
 * of the assets flowing through events.
 */
@Slf4j
public class LambdaPlaybookRunner extends AbstractLambdaHandler {
    /**
     * Default constructor - pass handler up to superclass.
     */
    public LambdaPlaybookRunner() {
        super(PlaybookRunner.getInstance());
    }
}
