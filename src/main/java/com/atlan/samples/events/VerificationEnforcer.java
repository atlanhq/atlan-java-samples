/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.atlan.exception.AtlanException;
import com.atlan.model.assets.Asset;
import com.atlan.model.enums.AtlanCertificateStatus;
import io.numaproj.numaflow.function.Datum;
import io.numaproj.numaflow.function.FunctionServer;
import io.numaproj.numaflow.function.Message;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * An example to automatically revert any asset that is marked VERIFIED if it does not
 * at least have: a description, at least one owner, and lineage.
 */
@Slf4j
public class VerificationEnforcer extends AbstractEventHandler {

    private static final List<String> REQUIRED_ATTRS = List.of(
            "description",
            "userDescription",
            "ownerUsers",
            "ownerGroups",
            "__hasLineage",
            "inputToProcesses",
            "outputFromProcesses",
            "certificateStatus",
            "awsArn", // required attribute for AWS objects
            "anchor"); // required attribute for terms and categories

    private static final String ENFORCEMENT_MESSAGE =
            "To be verified, an asset must have a description, at least one owner, and lineage.";

    /**
     * Logic to apply to each event we receive.
     *
     * @param keys unique key of the event
     * @param data details of the event (including its payload)
     * @return an array of messages that can be passed to further vertexes in the pipeline
     */
    public Message[] processMessage(String[] keys, Datum data) {

        // 1. Ensure there's an Atlan event payload present
        Asset fromEvent = getAssetFromEvent(data);
        if (fromEvent == null) {
            return failed(data);
        }

        // 2. Retrieve the current details about the asset from Atlan
        //    (in case the processing of this event was delayed or a later retry and
        //    the asset has since changed in Atlan — don't want to calculate based on
        //    stale information)
        Asset asset;
        try {
            asset = getCurrentViewOfAsset(fromEvent, REQUIRED_ATTRS, false, false);
        } catch (AtlanException e) {
            log.error("Unable to find the asset in Atlan: {}", fromEvent.getQualifiedName(), e);
            return failed(data);
        }
        if (asset == null) {
            log.error("No current view of asset found (deleted or not yet available in search index): {}", fromEvent);
            return failed(data);
        }

        // 3. We only need to consider enforcement if the asset is currently verified
        if (asset.getCertificateStatus() == AtlanCertificateStatus.VERIFIED) {
            if (!hasDescription(asset) || !hasOwner(asset) || !hasLineage(asset)) {
                try {
                    Asset toUpdate = asset.trimToRequired()
                            .certificateStatus(AtlanCertificateStatus.DRAFT)
                            .certificateStatusMessage(ENFORCEMENT_MESSAGE)
                            .build();
                    toUpdate.upsert();
                    log.info("Enforced verification reversal on: {}", asset.getQualifiedName());
                    return succeeded(data);
                } catch (AtlanException e) {
                    log.error("Unable to update the asset's certificate: {}", asset.getQualifiedName(), e);
                    return failed(data);
                }
            }
            log.info(
                    "Asset has all required information present to be verified, no enforcement required: {}",
                    asset.getQualifiedName());
        } else {
            log.info("Asset is no longer verified, no enforcement action to consider: {}", asset.getQualifiedName());
        }
        return drop();
    }

    /**
     * Register the event processing function.
     *
     * @param args (unused)
     * @throws IOException on any errors starting the event processor
     */
    public static void main(String[] args) throws IOException {
        new FunctionServer().registerMapHandler(new VerificationEnforcer()).start();
    }
}
