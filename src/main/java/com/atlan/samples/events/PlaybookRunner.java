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
        Asset fromEvent = getAssetFromEvent(data);
        if (fromEvent == null) {
            return failed(keys, data);
        }

        // 2. Retrieve the current view of the asset
        Asset original;
        try {
            original = getCurrentViewOfAsset(fromEvent, MUTABLE_ATTRS, true, true);
        } catch (AtlanException e) {
            log.error("Unable to find the asset in Atlan: {}", fromEvent.getQualifiedName(), e);
            return failed(keys, data);
        }
        if (original == null) {
            log.error("No current view of asset found (deleted or not yet available in search index): {}", fromEvent);
            return failed(keys, data);
        }
        Asset.AssetBuilder<?, ?> builder = (Asset.AssetBuilder<?, ?>) original.toBuilder();

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
                    // If there is no match, fail the asset (to be picked up by a retry vertex)
                    log.warn(
                            "Asset {} did not match playbook {}'s criteria, sending to a retry.",
                            original.getGuid(),
                            playbookName);
                    return failed(keys, data);
                }
                // 6. If there is a match, run the actions against the asset
                List<PlaybookAction> actions = rule.getActions();
                for (PlaybookAction action : actions) {
                    PlaybookActionType type = action.getType();
                    if (type == PlaybookActionType.METADATA_UPDATE) {
                        applyMetadataUpdate(builder, action.getActionsSchema());
                    } else {
                        // TODO: handle all playbook actions
                        log.error("Unhandled playbook action type {} in playbook {} — skipping.", type, playbookName);
                    }
                }
            }
        }

        // 7. If the asset is unchanged after the actions, drop it; otherwise
        //  upsert the changed asset
        Asset mutated = builder.build();
        if (original.equals(mutated)) {
            log.info("No change in asset: {}", original.getQualifiedName());
            return drop();
        } else {
            try {
                AssetMutationResponse response = mutated.upsert();
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
                            log.error("Unable to parse rules for playbook {} — skipping...", playbookName);
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
     * @param builder against which to apply the changes
     * @param schema defining the changes to apply
     */
    private void applyMetadataUpdate(Asset.AssetBuilder<?, ?> builder, PlaybookActionSchema schema) {
        String operand = schema.getOperand();
        PlaybookActionOperator operator = schema.getOperator();
        Object value = schema.getValue();
        switch (operand) {
            case "certificateStatus":
                builder.certificateStatus(AtlanCertificateStatus.fromValue((String) value));
                break;
            case "description":
                builder.description((String) value);
                break;
            case "userDescription":
                builder.userDescription((String) value);
                break;
            case "owners":
                changeOwners(builder, operator, value);
                break;
            default:
                log.error("Unhandled attribute {} — skipping.", operand);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void changeOwners(Asset.AssetBuilder<?, ?> builder, PlaybookActionOperator operator, Object value) {
        if (value instanceof Map) {
            Map<String, List<String>> owners = (Map<String, List<String>>) value;
            switch (operator) {
                case ADD:
                    if (owners.containsKey("ownerUsers")) {
                        builder.ownerUsers(owners.get("ownerUsers"));
                    }
                    if (owners.containsKey("ownerGroups")) {
                        builder.ownerGroups(owners.get("ownerGroups"));
                    }
                    break;
                case REMOVE:
                    Asset current = builder.build();
                    if (owners.containsKey("ownerUsers")) {
                        builder.clearOwnerUsers();
                        List<String> usersToRemove = owners.get("ownerUsers");
                        List<String> usersToKeep = new ArrayList<>();
                        for (String user : current.getOwnerUsers()) {
                            if (!usersToRemove.contains(user)) {
                                usersToKeep.add(user);
                            }
                        }
                        if (!usersToKeep.isEmpty()) {
                            builder.ownerUsers(usersToKeep);
                        } else {
                            builder.nullField("ownerUsers");
                        }
                    }
                    if (owners.containsKey("ownerGroups")) {
                        builder.clearOwnerGroups();
                        List<String> groupsToRemove = owners.get("ownerGroups");
                        List<String> groupsToKeep = new ArrayList<>();
                        for (String group : current.getOwnerGroups()) {
                            if (!groupsToRemove.contains(group)) {
                                groupsToKeep.add(group);
                            }
                        }
                        if (!groupsToKeep.isEmpty()) {
                            builder.ownerGroups(groupsToKeep);
                        } else {
                            builder.nullField("ownerGroups");
                        }
                    }
                    break;
                case REPLACE:
                    builder.clearOwnerGroups().clearOwnerUsers();
                    if (owners.containsKey("ownerUsers")) {
                        builder.ownerUsers(owners.get("ownerUsers"));
                    } else {
                        builder.nullField("ownerUsers");
                    }
                    if (owners.containsKey("ownerGroups")) {
                        builder.ownerGroups(owners.get("ownerGroups"));
                    } else {
                        builder.nullField("ownerGroups");
                    }
                    break;
                default:
                    log.error("Unknown operation for owners — skipping: {}", operator);
                    break;
            }
        }
    }
}
