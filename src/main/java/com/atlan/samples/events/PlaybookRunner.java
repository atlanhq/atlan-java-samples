/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.events;

import static com.atlan.util.QueryFactory.CompoundQuery;
import static com.atlan.util.QueryFactory.have;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.atlan.Atlan;
import com.atlan.AtlanClient;
import com.atlan.events.AtlanEventHandler;
import com.atlan.exception.AtlanException;
import com.atlan.model.assets.Asset;
import com.atlan.model.enums.CertificateStatus;
import com.atlan.model.enums.KeywordFields;
import com.atlan.model.enums.PlaybookActionOperator;
import com.atlan.model.enums.PlaybookActionType;
import com.atlan.model.search.IndexSearchDSL;
import com.atlan.model.search.IndexSearchRequest;
import com.atlan.model.search.IndexSearchResponse;
import com.atlan.model.workflow.*;
import com.atlan.net.HttpClient;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

/**
 * An experiment to dynamically run through playbooks and apply them to any of the assets
 * flowing through events.
 */
@Slf4j
public class PlaybookRunner implements AtlanEventHandler {

    private static final List<String> MUTABLE_ATTRS = List.of(
            "description",
            "userDescription",
            "certificateStatus",
            "ownerUsers",
            "ownerGroups",
            "meanings",
            "classifications");

    /** Singleton for reuse */
    private static final PlaybookRunner INSTANCE = createInstance();

    private static PlaybookRunner createInstance() {
        return new PlaybookRunner();
    }

    public static PlaybookRunner getInstance() {
        return INSTANCE;
    }

    // Note: can just reuse the default validatePrerequisites

    /** {@inheritDoc} */
    @Override
    public Asset getCurrentState(AtlanClient client, Asset fromEvent, Logger log) throws AtlanException {
        return Asset.get(client, fromEvent.getGuid(), true);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<Asset> calculateChanges(Asset original, Logger log) throws AtlanException {

        long start = System.currentTimeMillis();

        // Prep assets for later comparison
        log.info("Picked up event for: {}", original.getQualifiedName());
        Asset.AssetBuilder<?, ?> full = (Asset.AssetBuilder<?, ?>) original.toBuilder();
        Asset.AssetBuilder<?, ?> trimmed = original.trimToRequired();

        // Retrieve the playbooks
        // TODO: cache them...
        Map<String, List<PlaybookRule>> playbooks = fetchPlaybooks();

        long elapsed = System.currentTimeMillis() - start;

        // Ensure we wait a little bit for Elastic to become consistent before attempting
        // our queries
        if (elapsed < 2000) {
            try {
                int wait = elapsed < 1000 ? 3 : 2;
                Thread.sleep(HttpClient.waitTime(wait).toMillis());
            } catch (InterruptedException e) {
                log.warn("Consistency delay was interrupted, results from here may vary...", e);
            }
        }

        // Iterate through the playbooks
        for (Map.Entry<String, List<PlaybookRule>> entry : playbooks.entrySet()) {
            String playbookName = entry.getKey();
            List<PlaybookRule> rules = entry.getValue();
            // Iterate through the rules in each playbook
            for (PlaybookRule rule : rules) {
                IndexSearchRequest filter = rule.getConfig().getQuery();
                Query query = filter.getDsl().getQuery();
                // Add the asset in the event to the search criteria of the rule,
                // to confirm there is a match (that we should apply the associated actions)
                IndexSearchRequest match = IndexSearchRequest.builder(IndexSearchDSL.builder(CompoundQuery.builder()
                                        .must(query)
                                        .must(have(KeywordFields.GUID).eq(original.getGuid()))
                                        .build()
                                        ._toQuery())
                                .build())
                        .build();
                Asset asset = null;
                IndexSearchResponse response = match.search();
                if (response != null
                        && response.getAssets() != null
                        && !response.getAssets().isEmpty()) {
                    asset = response.getAssets().get(0);
                }
                if (asset == null) {
                    // If there is no match, skip any actions for that rule
                    log.info(
                            "Asset {} did not match playbook \"{}\"'s criteria (rule: {}) - skipping its actions.",
                            original.getGuid(),
                            playbookName,
                            rule.getName());
                } else {
                    // If there is a match, run the actions against the asset
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
        return hasChanges(original, full.build(), log) ? Set.of(trimmed.build()) : Collections.emptySet();
    }

    // Note: can reuse default hasChanges
    // Note: can reuse default upsertChanges

    /**
     * Fetch the playbooks that exist in Atlan.
     *
     * @return a map from playbook name to its list of rules
     * @throws AtlanException on any API communication issue
     */
    private Map<String, List<PlaybookRule>> fetchPlaybooks() throws AtlanException {
        Map<String, List<PlaybookRule>> map = new LinkedHashMap<>();
        WorkflowSearchResponse response = Atlan.getDefaultClient().playbooks.list(50);
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
                            rules = Atlan.getDefaultClient().readValue(value, new TypeReference<>() {});
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
                full.certificateStatus(CertificateStatus.fromValue((String) value));
                trimmed.certificateStatus(CertificateStatus.fromValue((String) value));
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
