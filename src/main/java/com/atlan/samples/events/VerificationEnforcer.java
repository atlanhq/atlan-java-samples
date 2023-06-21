/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.atlan.events.AtlanEventHandler;
import com.atlan.exception.AtlanException;
import com.atlan.model.assets.Asset;
import com.atlan.model.enums.CertificateStatus;
import com.atlan.model.events.AtlanEvent;
import java.util.List;
import org.slf4j.Logger;

/**
 * An example to automatically revert any asset that is marked VERIFIED if it does not
 * at least have: a description, at least one owner, and lineage.
 */
public interface VerificationEnforcer {

    List<String> REQUIRED_ATTRS = List.of(
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

    String ENFORCEMENT_MESSAGE = "To be verified, an asset must have a description, at least one owner, and lineage.";

    /**
     * Validate the necessary inputs for enforcement are present in the payload.
     *
     * @param event the event from Atlan
     * @return true only if there is an asset in the event
     */
    static boolean validatePrerequisites(AtlanEvent event) {
        return event != null && event.getPayload() != null && event.getPayload().getAsset() != null;
    }

    /**
     * Check if the asset is verified and can remain verified, and if not revert its status to draft.
     *
     * @param fromEvent the asset to check verification against
     * @param log for logging information
     * @return true if a verification change was required and applied, false if there was no change to apply
     * @throws AtlanException on any issues retrieving the asset or enforcing its verification
     */
    static boolean enforceVerification(Asset fromEvent, Logger log) throws AtlanException {

        // Retrieve the current details about the asset from Atlan
        // (in case the processing of this event was delayed or a later retry and
        // the asset has since changed in Atlan — don't want to calculate based on
        // stale information)
        Asset asset = AtlanEventHandler.getCurrentViewOfAsset(fromEvent, REQUIRED_ATTRS, false, false);

        // We only need to consider enforcement if the asset is currently verified
        if (asset.getCertificateStatus() == CertificateStatus.VERIFIED) {
            if (!AtlanEventHandler.hasDescription(asset)
                    || !AtlanEventHandler.hasOwner(asset)
                    || !AtlanEventHandler.hasLineage(asset)) {
                Asset toUpdate = asset.trimToRequired()
                        .certificateStatus(CertificateStatus.DRAFT)
                        .certificateStatusMessage(ENFORCEMENT_MESSAGE)
                        .build();
                toUpdate.upsert();
                log.info("Enforced verification reversal on: {}", asset.getQualifiedName());
                return true;
            }
            log.info(
                    "Asset has all required information present to be verified, no enforcement required: {}",
                    asset.getQualifiedName());
        } else {
            log.info("Asset is no longer verified, no enforcement action to consider: {}", asset.getQualifiedName());
        }
        return false;
    }
}
