/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import static com.atlan.util.QueryFactory.*;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.atlan.api.PlaybooksEndpoint;
import com.atlan.exception.AtlanException;
import com.atlan.model.assets.*;
import com.atlan.model.core.AssetMutationResponse;
import com.atlan.model.enums.AtlanCertificateStatus;
import com.atlan.model.enums.KeywordFields;
import com.atlan.model.enums.PlaybookActionOperator;
import com.atlan.model.enums.PlaybookActionType;
import com.atlan.model.events.AtlanEvent;
import com.atlan.model.search.IndexSearchDSL;
import com.atlan.model.search.IndexSearchRequest;
import com.atlan.model.search.IndexSearchResponse;
import com.atlan.model.workflow.*;
import com.atlan.serde.Serde;
import com.fasterxml.jackson.core.type.TypeReference;
import io.numaproj.numaflow.function.Datum;
import io.numaproj.numaflow.function.FunctionServer;
import io.numaproj.numaflow.function.MessageList;
import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

/**
 * An experiment to dynamically run through playbooks and apply them to any of the assets
 * flowing through events.
 */
@Slf4j
public class PlaybookRunner extends AbstractEventHandler {

    private static final List<String> MUTABLE_ATTRS = List.of(
            "description",
            "userDescription",
            "certificateStatus",
            "ownerUsers",
            "ownerGroups",
            "meanings",
            "classifications");

    /**
     * Logic to apply to each event we receive.
     *
     * @param keys unique key of the event
     * @param data details of the event (including its payload)
     * @return an array of messages that can be passed to further vertexes in the pipeline
     */
    public MessageList processMessage(String[] keys, Datum data) {

        // 1. Ensure there's an Atlan event payload present
        AtlanEvent event = getAtlanEvent(data);
        if (event == null) {
            return failed(keys, data);
        }

        // 2. Retrieve the current view of the asset
        Asset original;
        try {
            original = getCurrentFullAsset(event);
        } catch (AtlanException e) {
            log.error("Unable to find the asset in Atlan: {}", event.getPayload(), e);
            return failed(keys, data);
        }
        if (original == null) {
            log.error(
                    "No current view of asset found (deleted or not yet available in search index): {}",
                    event.getPayload().getAsset());
            return failed(keys, data);
        }
        Asset.AssetBuilder<?, ?> full = (Asset.AssetBuilder<?, ?>) original.toBuilder();
        Asset.AssetBuilder<?, ?> trimmed;
        try {
            trimmed = original.trimToRequired();
        } catch (AtlanException e) {
            log.error("Unable to produce update-able version of the asset: {}", original.getQualifiedName(), e);
            return failed(keys, data);
        }

        // 3. Retrieve the playbooks
        // TODO: cache them...
        Map<String, List<PlaybookRule>> playbooks;
        try {
            playbooks = fetchPlaybooks();
        } catch (AtlanException e) {
            log.error("Unable to retrieve playbooks from Atlan.", e);
            return failed(keys, data);
        }

        // 4. Iterate through the playbooks
        for (Map.Entry<String, List<PlaybookRule>> entry : playbooks.entrySet()) {
            String playbookName = entry.getKey();
            List<PlaybookRule> rules = entry.getValue();
            // 5. Iterate through the rules in each playbook
            for (PlaybookRule rule : rules) {
                IndexSearchRequest filter = rule.getConfig().getQuery();
                Query query = filter.getDsl().getQuery();
                // 6. Add the asset in the event to the search criteria of the rule
                //  to confirm there is a match (that we should apply the associated actions)
                IndexSearchRequest match = IndexSearchRequest.builder()
                        .dsl(IndexSearchDSL.builder()
                                .query(CompoundQuery.builder()
                                        .must(query)
                                        .must(have(KeywordFields.GUID).eq(original.getGuid()))
                                        .build()
                                        ._toQuery())
                                .build())
                        .build();
                Asset asset = null;
                try {
                    IndexSearchResponse response = match.search();
                    if (response != null
                            && response.getAssets() != null
                            && !response.getAssets().isEmpty()) {
                        asset = response.getAssets().get(0);
                    }
                } catch (AtlanException e) {
                    log.error("Unable to search for asset {}, sending to a retry.", original.getGuid());
                    return failed(keys, data);
                }
                if (asset == null) {
                    // If there is no match, skip any actions for that rule
                    log.info(
                            "Asset {} did not match playbook \"{}\"'s criteria (rule: {}) - skipping its actions.",
                            original.getGuid(),
                            playbookName,
                            rule.getName());
                } else {
                    // 6. If there is a match, run the actions against the asset
                    List<PlaybookAction> actions = rule.getActions();
                    for (PlaybookAction action : actions) {
                        PlaybookActionType type = action.getType();
                        if (type == PlaybookActionType.METADATA_UPDATE) {
                            log.info(
                                    "Applying actions from \"{}::{}\" to: {}",
                                    playbookName,
                                    rule.getName(),
                                    original.getQualifiedName());
                            applyMetadataUpdate(full, trimmed, action.getActionsSchema());
                        } else {
                            // TODO: handle all playbook actions
                            log.warn(
                                    "Unhandled playbook action type {} in playbook \"{}\" (rule: {}) - skipping.",
                                    type,
                                    playbookName,
                                    rule.getName());
                        }
                    }
                }
            }
        }

        // 7. If the asset is unchanged after the actions, drop it; otherwise
        //  upsert the changed asset
        Asset mutated = full.build();
        if (original.equals(mutated)) {
            log.info("No change in asset: {}", original.getQualifiedName());
            return drop();
        } else {
            try {
                log.info("Updating changed asset: {}", original.getQualifiedName());
                AssetMutationResponse response = trimmed.build().upsert();
                if (response != null
                        && response.getUpdatedAssets() != null
                        && !response.getUpdatedAssets().isEmpty()) {
                    boolean updated = false;
                    for (Asset candidate : response.getUpdatedAssets()) {
                        if (mutated.getGuid().equals(candidate.getGuid())) {
                            updated = true;
                            break;
                        }
                    }
                    if (updated) {
                        return succeeded(keys, data);
                    }
                }
                log.error("Failed to update the asset: {}", mutated.getQualifiedName());
                return failed(keys, data);
            } catch (AtlanException e) {
                log.error("Unable to update asset: {}", mutated.getQualifiedName(), e);
                return failed(keys, data);
            }
        }
    }

    /**
     * Register the event processing function.
     *
     * @param args (unused)
     * @throws IOException on any errors starting the event processor
     */
    public static void main(String[] args) throws IOException {
        new FunctionServer().registerMapHandler(new PlaybookRunner()).start();
    }

    /**
     * Fetch the playbooks that exist in Atlan.
     *
     * @return a map from playbook name to its list of rules
     * @throws AtlanException on any API communication issue
     */
    private Map<String, List<PlaybookRule>> fetchPlaybooks() throws AtlanException {
        Map<String, List<PlaybookRule>> map = new LinkedHashMap<>();
        WorkflowSearchResponse response = PlaybooksEndpoint.list(50);
        if (response != null && response.getHits() != null) {
            List<WorkflowSearchResult> hits = response.getHits().getHits();
            for (WorkflowSearchResult hit : hits) {
                WorkflowSearchResultDetail detail = hit.get_source();
                WorkflowSpec spec = detail.getSpec();
                WorkflowMetadata metadata = spec.getWorkflowMetadata();
                Map<String, String> annotations = metadata.getAnnotations();
                String playbookName = annotations.get("workflows.argoproj.io/atlanName");
                List<NameValuePair> parameters = spec.getTemplates()
                        .get(0)
                        .getDag()
                        .getTasks()
                        .get(0)
                        .getArguments()
                        .getParameters();
                List<PlaybookRule> rules = null;
                for (NameValuePair parameter : parameters) {
                    if (parameter.getName().equals("rules")) {
                        String value = (String) parameter.getValue();
                        try {
                            rules = Serde.mapper.readValue(value, new TypeReference<>() {});
                        } catch (IOException e) {
                            log.error("Unable to parse rules for playbook \"{}\" - skipping...", playbookName);
                        }
                    }
                }
                if (rules != null) {
                    map.put(playbookName, rules);
                }
            }
        }
        return map;
    }

    /**
     * Apply the metadata updates defined in by the playbook action.
     *
     * @param full complete asset against which to apply the changes (for later idempotency comparison)
     * @param trimmed asset containing only updates made by playbook actions (for limiting other changes)
     * @param schema defining the changes to apply
     */
    private void applyMetadataUpdate(
            Asset.AssetBuilder<?, ?> full, Asset.AssetBuilder<?, ?> trimmed, PlaybookActionSchema schema) {
        String operand = schema.getOperand();
        PlaybookActionOperator operator = schema.getOperator();
        Object value = schema.getValue();
        switch (operand) {
            case "certificateStatus":
                full.certificateStatus(AtlanCertificateStatus.fromValue((String) value));
                trimmed.certificateStatus(AtlanCertificateStatus.fromValue((String) value));
                break;
            case "description":
                full.description((String) value);
                trimmed.description((String) value);
                break;
            case "userDescription":
                full.userDescription((String) value);
                trimmed.userDescription((String) value);
                break;
            case "owners":
                changeOwners(full, trimmed, operator, value);
                break;
            default:
                log.error("Unhandled attribute {} - skipping.", operand);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void changeOwners(
            Asset.AssetBuilder<?, ?> full,
            Asset.AssetBuilder<?, ?> trimmed,
            PlaybookActionOperator operator,
            Object value) {
        if (value instanceof Map) {
            Map<String, List<String>> owners = (Map<String, List<String>>) value;
            switch (operator) {
                case ADD:
                    if (owners.containsKey("ownerUsers")) {
                        full.ownerUsers(owners.get("ownerUsers"));
                        trimmed.ownerUsers(owners.get("ownerUsers"));
                    }
                    if (owners.containsKey("ownerGroups")) {
                        full.ownerGroups(owners.get("ownerGroups"));
                        trimmed.ownerGroups(owners.get("ownerGroups"));
                    }
                    break;
                case REMOVE:
                    Asset current = full.build();
                    if (owners.containsKey("ownerUsers")) {
                        full.clearOwnerUsers();
                        trimmed.clearOwnerUsers();
                        List<String> usersToRemove = owners.get("ownerUsers");
                        List<String> usersToKeep = new ArrayList<>();
                        for (String user : current.getOwnerUsers()) {
                            if (!usersToRemove.contains(user)) {
                                usersToKeep.add(user);
                            }
                        }
                        if (!usersToKeep.isEmpty()) {
                            full.ownerUsers(usersToKeep);
                            trimmed.ownerUsers(usersToKeep);
                        } else {
                            full.nullField("ownerUsers");
                            trimmed.nullField("ownerUsers");
                        }
                    }
                    if (owners.containsKey("ownerGroups")) {
                        full.clearOwnerGroups();
                        trimmed.clearOwnerGroups();
                        List<String> groupsToRemove = owners.get("ownerGroups");
                        List<String> groupsToKeep = new ArrayList<>();
                        for (String group : current.getOwnerGroups()) {
                            if (!groupsToRemove.contains(group)) {
                                groupsToKeep.add(group);
                            }
                        }
                        if (!groupsToKeep.isEmpty()) {
                            full.ownerGroups(groupsToKeep);
                            trimmed.ownerGroups(groupsToKeep);
                        } else {
                            full.nullField("ownerGroups");
                            trimmed.nullField("ownerGroups");
                        }
                    }
                    break;
                case REPLACE:
                    full.clearOwnerGroups().clearOwnerUsers();
                    trimmed.clearOwnerGroups().clearOwnerUsers();
                    if (owners.containsKey("ownerUsers")) {
                        full.ownerUsers(owners.get("ownerUsers"));
                        trimmed.ownerUsers(owners.get("ownerUsers"));
                    } else {
                        full.nullField("ownerUsers");
                        trimmed.nullField("ownerUsers");
                    }
                    if (owners.containsKey("ownerGroups")) {
                        full.ownerGroups(owners.get("ownerGroups"));
                        trimmed.ownerGroups(owners.get("ownerGroups"));
                    } else {
                        full.nullField("ownerGroups");
                        trimmed.nullField("ownerGroups");
                    }
                    break;
                default:
                    log.error("Unknown operation for owners - skipping: {}", operator);
                    break;
            }
        }
    }
}
