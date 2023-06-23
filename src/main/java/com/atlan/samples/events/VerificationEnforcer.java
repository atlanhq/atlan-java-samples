/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import com.atlan.events.AtlanEventHandler;
import com.atlan.exception.AtlanException;
import com.atlan.exception.ErrorCode;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.Asset;
import com.atlan.model.enums.CertificateStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;

/**
 * An example to automatically revert any asset that is marked VERIFIED if it does not
 * at least have: a description, at least one owner, and lineage.
 */
public class VerificationEnforcer implements AtlanEventHandler {

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

    /** Singleton for reuse */
    private static final VerificationEnforcer INSTANCE = createInstance();

    private static VerificationEnforcer createInstance() {
        return new VerificationEnforcer();
    }

    public static VerificationEnforcer getInstance() {
        return INSTANCE;
    }

    // Note: we can just re-use the default validatePrerequisites

    /** {@inheritDoc} */
    @Override
    public Asset getCurrentState(Asset fromEvent, Logger log) throws AtlanException {
        Asset asset = AtlanEventHandler.getCurrentViewOfAsset(fromEvent, REQUIRED_ATTRS, false, false);
        if (asset == null) {
            throw new NotFoundException(
                    ErrorCode.ASSET_NOT_FOUND_BY_QN, fromEvent.getQualifiedName(), fromEvent.getTypeName());
        }
        return asset;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<Asset> calculateChanges(Asset asset, Logger log) throws AtlanException {
        // We only need to consider enforcement if the asset is currently verified
        if (asset.getCertificateStatus() == CertificateStatus.VERIFIED) {
            if (!AtlanEventHandler.hasDescription(asset)
                    || !AtlanEventHandler.hasOwner(asset)
                    || !AtlanEventHandler.hasLineage(asset)) {
                return Set.of(asset.trimToRequired()
                        .certificateStatus(CertificateStatus.DRAFT)
                        .certificateStatusMessage(ENFORCEMENT_MESSAGE)
                        .build());
            } else {
                log.info(
                        "Asset has all required information present to be verified, no enforcement required: {}",
                        asset.getQualifiedName());
            }
        } else {
            log.info("Asset is no longer verified, no enforcement action to consider: {}", asset.getQualifiedName());
        }
        return Collections.emptySet();
    }

    // Note: we can just re-use the default hasChanges
    // Note: we can just re-use the default upsertChanges
}
