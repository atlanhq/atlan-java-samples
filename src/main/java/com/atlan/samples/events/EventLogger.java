/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import io.numaproj.numaflow.function.Datum;
import io.numaproj.numaflow.function.FunctionServer;
import io.numaproj.numaflow.function.Message;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * Very basic example of logging a deserialized event and then passing it on to all
 * subsequent vertexes, unchanged.
 */
@Slf4j
public class EventLogger extends AbstractEventHandler {

    public Message[] processMessage(String[] keys, Datum data) {
        log.info("Event received: {}", getAtlanEvent(data));
        return new Message[] {Message.toAll(data.getValue())};
    }

    public static void main(String[] args) throws IOException {
        new FunctionServer().registerMapHandler(new EventLogger()).start();
    }
}
