/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.atlan.exception.AtlanException;
import com.atlan.samples.loaders.models.*;
import com.atlan.samples.readers.ExcelReader;
import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LineageLoader extends AbstractLoader implements RequestHandler<Map<String, String>, String> {

    private static final String SHEET_NAME = "Lineage";

    public static void main(String[] args) {
        LineageLoader ll = new LineageLoader();
        ll.handleRequest(System.getenv(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleRequest(Map<String, String> event, Context context) {

        try {

            log.info("Retrieving configuration and context...");
            if (context != null && context.getClientContext() != null) {
                log.debug(
                        " ... client environment: {}",
                        context.getClientContext().getEnvironment());
                log.debug(" ... client custom: {}", context.getClientContext().getCustom());
            }
            parseParametersFromEvent(event);

            log.info("Loading lineage from: {}::{}", getFilename(), SHEET_NAME);

            ExcelReader xlsx = new ExcelReader(getFilename());
            List<Map<String, String>> data = xlsx.getRowsFromSheet(SHEET_NAME, 1);

            // Fastest way will be to load in batches by level of the asset hierarchy,
            // even though this means multiple passes over the data (it's all in-memory)

            // 1. Upsert connections for each unique orchestrator name
            Map<String, ConnectionDetails> connections = new LinkedHashMap<>();
            for (Map<String, String> row : data) {
                ConnectionDetails details = LineageDetails.getOrchestratorFromRow(row);
                if (details != null) {
                    // Only overwrite any details about the connection if none previously existed
                    String identity = details.getIdentity();
                    ConnectionDetails existing = connections.get(identity);
                    if (existing == null || existing.isStub()) {
                        connections.put(identity, details);
                    }
                }
            }
            Map<ConnectionDetails, String> connectionCache = ConnectionDetails.upsert(connections, getBatchSize());

            // 2. Add existing source and target connections into the cache
            for (Map<String, String> row : data) {
                ConnectionDetails source = LineageDetails.getSourceConnectionFromRow(row);
                if (source != null) {
                    try {
                        ConnectionDetails.findAndCache(connectionCache, source.getName(), source.getType());
                    } catch (AtlanException e) {
                        log.error("Unable to find source connection: {}", source, e);
                    }
                }
                ConnectionDetails target = LineageDetails.getTargetConnectionFromRow(row);
                if (target != null) {
                    try {
                        ConnectionDetails.findAndCache(connectionCache, target.getName(), target.getType());
                    } catch (AtlanException e) {
                        log.error("Unable to find target connection: {}", target, e);
                    }
                }
            }

            // 3. Build up map of unique processes, with all their input and output details,
            //    and then bulk-upsert them
            Map<String, Set<LineageDetails>> lineage = new LinkedHashMap<>();
            for (Map<String, String> row : data) {
                LineageDetails details = LineageDetails.getFromRow(connectionCache, row, getDelimiter());
                if (details != null) {
                    String processQN = details.getProcessConnectionQualifiedName() + "/" + details.getProcessId();
                    if (!lineage.containsKey(processQN)) {
                        lineage.put(processQN, new LinkedHashSet<>());
                    }
                    lineage.get(processQN).add(details);
                }
            }
            LineageDetails.upsert(lineage, getBatchSize());

        } catch (IOException e) {
            log.error("Failed to read Excel file from: {}", getFilename(), e);
            System.exit(1);
        }

        return getFilename();
    }
}
