/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.atlan.Atlan;
import com.atlan.model.events.AtlanEvent;
import com.atlan.serde.Serde;
import io.numaproj.numaflow.function.Datum;
import io.numaproj.numaflow.function.FunctionServer;
import io.numaproj.numaflow.function.Message;
import io.numaproj.numaflow.function.map.MapFunc;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lombok.extern.slf4j.Slf4j;

/**
 * Very basic example of logging a deserialized event and then passing it on to all
 * subsequent vertices, unchanged.
 */
@Slf4j
public class EventLogger {

    private static Message[] process(String key, Datum data) {
        try {
            AtlanEvent event = Serde.mapper.readValue(data.getValue(), AtlanEvent.class);
            log.info("Event received: {}", event.getPayload());
        } catch (IOException e) {
            log.error("Unable to deserialize event: {}", new String(data.getValue(), StandardCharsets.UTF_8), e);
        }
        return new Message[]{Message.toAll(data.getValue())};
    }

    public static void main(String[] args) throws IOException {
        Atlan.setBaseUrl(System.getenv("ATLAN_BASE_URL"));
        Atlan.setApiToken(System.getenv("ATLAN_API_KEY"));
        new FunctionServer().registerMapper(new MapFunc(EventLogger::process)).start();
    }
}
