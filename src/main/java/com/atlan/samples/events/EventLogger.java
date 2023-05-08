/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import io.numaproj.numaflow.function.FunctionServer;
import io.numaproj.numaflow.function.interfaces.Datum;
import io.numaproj.numaflow.function.types.MessageList;
import lombok.extern.slf4j.Slf4j;

/**
 * Very basic example of logging a deserialized event and then passing it on to all
 * subsequent vertexes, unchanged.
 */
@Slf4j
public class EventLogger extends AbstractEventHandler {

    public MessageList processMessage(String[] keys, Datum data) {
        log.info("Event received: {}", getAtlanEvent(data));
        return forward(data);
    }

    public static void main(String[] args) throws Exception {
        new FunctionServer().registerMapHandler(new EventLogger()).start();
    }
}
