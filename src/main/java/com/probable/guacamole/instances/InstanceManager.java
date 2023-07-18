/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.probable.guacamole.instances;

import static com.atlan.util.QueryFactory.*;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.atlan.Atlan;
import com.atlan.exception.AtlanException;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.Connection;
import com.atlan.model.core.AssetMutationResponse;
import com.atlan.model.enums.AtlanConnectorType;
import com.atlan.model.enums.AtlanDeleteType;
import com.atlan.model.enums.CertificateStatus;
import com.atlan.model.search.IndexSearchRequest;
import com.atlan.model.search.IndexSearchResponse;
import com.atlan.util.AssetBatch;
import com.probable.guacamole.AtlanRunner;
import com.probable.guacamole.model.assets.GuacamoleColumn;
import com.probable.guacamole.model.assets.GuacamoleTable;
import com.probable.guacamole.model.enums.GuacamoleTemperature;
import com.probable.guacamole.model.enums.KeywordFields;
import com.probable.guacamole.model.enums.NumericFields;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InstanceManager extends AtlanRunner {

    private static Connection connection = null;

    public static void main(String[] args) {
        InstanceManager im = new InstanceManager();
        im.createConnection();
        im.createEntities();
        im.readEntities();
        im.updateEntities();
        im.searchEntities();
        im.purgeEntities();
    }

    void createConnection() {
        try {
            List<Connection> results = Connection.findByName(SERVICE_TYPE, AtlanConnectorType.MONGODB, null);
            if (results.size() > 0) {
                connection = results.get(0);
                log.info("Connection already exists, reusing it: {}", connection.getQualifiedName());
            }
        } catch (NotFoundException err) {
            try {
                Connection toCreate = Connection.creator(
                                SERVICE_TYPE,
                                AtlanConnectorType.MONGODB,
                                List.of(Atlan.getDefaultClient().getRoleCache().getIdForName("$admin")),
                                null,
                                null)
                        .build();
                AssetMutationResponse response = toCreate.save().block();
                connection = (Connection) response.getCreatedAssets().get(0);
            } catch (AtlanException e) {
                log.error("Unable to create a new connection.", e);
            }
        } catch (AtlanException e) {
            log.error("Unable to search for existing connection.", e);
        }
    }

    void createEntities() {
        GuacamoleTable table = GuacamoleTable.builder()
                .connectionQualifiedName(connection.getQualifiedName())
                .connectorType(AtlanConnectorType.MONGODB)
                .name("table")
                .qualifiedName(connection.getQualifiedName() + "/table")
                .guacamoleTemperature(GuacamoleTemperature.HOT)
                .guacamoleArchived(false)
                .guacamoleSize(123L)
                .build();
        try {
            AssetMutationResponse response = table.save();
            log.info("Created table entity: {}", response);
            table = (GuacamoleTable) response.getCreatedAssets().get(0);
        } catch (AtlanException e) {
            log.error("Failed to create new guacamole table.", e);
        }
        try {
            AssetBatch batch = new AssetBatch(Atlan.getDefaultClient(), GuacamoleColumn.TYPE_NAME, 20);
            GuacamoleColumn child1 = GuacamoleColumn.builder()
                    .connectionQualifiedName(connection.getQualifiedName())
                    .connectorType(AtlanConnectorType.MONGODB)
                    .name("column1")
                    .qualifiedName(table.getQualifiedName() + "/column1")
                    .guacamoleTable(GuacamoleTable.refByGuid(table.getGuid()))
                    .guacamoleConceptualized(123456789L)
                    .guacamoleWidth(100L)
                    .build();
            batch.add(child1);
            GuacamoleColumn child2 = GuacamoleColumn.builder()
                    .connectionQualifiedName(connection.getQualifiedName())
                    .connectorType(AtlanConnectorType.MONGODB)
                    .name("column2")
                    .qualifiedName(table.getQualifiedName() + "/column2")
                    .guacamoleTable(GuacamoleTable.refByGuid(table.getGuid()))
                    .guacamoleConceptualized(1234567890L)
                    .guacamoleWidth(200L)
                    .build();
            batch.add(child2);
            AssetMutationResponse response = batch.flush();
            log.info("Created child entities: {}", response);
        } catch (AtlanException e) {
            log.error("Unable to bulk-upsert Guacamole columns.", e);
        }
    }

    void readEntities() {
        final String parentQN = connection.getQualifiedName() + "/table";
        final String child1QN = parentQN + "/column1";
        final String child2QN = parentQN + "/column2";
        try {
            GuacamoleTable table = GuacamoleTable.retrieveByQualifiedName(parentQN);
            assert table.getQualifiedName().equals(parentQN);
            assert table.getGuacamoleColumns().size() == 2;
            String tableGuid = table.getGuid();
            GuacamoleColumn one = GuacamoleColumn.retrieveByQualifiedName(child1QN);
            assert one.getQualifiedName().equals(child1QN);
            assert one.getGuacamoleTable().getGuid().equals(tableGuid);
            GuacamoleColumn two = GuacamoleColumn.retrieveByQualifiedName(child2QN);
            assert two.getQualifiedName().equals(child2QN);
            assert two.getGuacamoleTable().getGuid().equals(tableGuid);
        } catch (AtlanException e) {
            log.error("Unable to read entities.", e);
        }
    }

    void updateEntities() {
        GuacamoleTable toUpdate = GuacamoleTable.updater(connection.getQualifiedName() + "/table", "table")
                .description("Now with a description!")
                .certificateStatus(CertificateStatus.DRAFT)
                .build();
        try {
            AssetMutationResponse response = toUpdate.save();
            log.info("Updated parent: {}", response);
        } catch (AtlanException e) {
            log.error("Unable to update entity.", e);
        }
    }

    void searchEntities() {
        Query query = CompoundQuery.builder()
                .must(beActive())
                .must(beOneOfTypes(List.of(GuacamoleTable.TYPE_NAME, GuacamoleColumn.TYPE_NAME)))
                .build()
                ._toQuery();

        IndexSearchRequest request = IndexSearchRequest.builder(query).build();

        try {
            IndexSearchResponse response = request.search();
            log.info("Found results: {}", response);
            assert response.getApproximateCount() == 3;
            assert response.getAssets().size() == 3;
        } catch (AtlanException e) {
            log.error("Unable to search.", e);
        }

        query = CompoundQuery.builder()
                .must(beActive())
                .must(beOfType(GuacamoleColumn.TYPE_NAME))
                .must(have(KeywordFields.NAME).eq("column1"))
                .build()
                ._toQuery();

        request = IndexSearchRequest.builder(query).build();

        try {
            IndexSearchResponse response = request.search();
            log.info("Found results: {}", response);
            assert response.getApproximateCount() == 1;
            assert response.getAssets().size() == 1;
        } catch (AtlanException e) {
            log.error("Unable to search.", e);
        }

        query = CompoundQuery.builder()
                .must(beActive())
                .must(beOfType(GuacamoleColumn.TYPE_NAME))
                .must(have(NumericFields.GUACAMOLE_WIDTH).gt(150L))
                .build()
                ._toQuery();

        request = IndexSearchRequest.builder(query).build();

        try {
            IndexSearchResponse response = request.search();
            log.info("Found results: {}", response);
            assert response.getApproximateCount() == 1;
            assert response.getAssets().size() == 1;
        } catch (AtlanException e) {
            log.error("Unable to search.", e);
        }

        query = CompoundQuery.builder()
                .must(beActive())
                .must(beOfType(GuacamoleTable.TYPE_NAME))
                .must(have(KeywordFields.DESCRIPTION).startingWith("Now"))
                .build()
                ._toQuery();

        request = IndexSearchRequest.builder(query).build();

        try {
            IndexSearchResponse response = request.search();
            log.info("Found results: {}", response);
            assert response.getApproximateCount() == 1;
            assert response.getAssets().size() == 1;
        } catch (AtlanException e) {
            log.error("Unable to search.", e);
        }
    }

    void purgeEntities() {
        final String parentQN = connection.getQualifiedName() + "/table";
        final String child1QN = parentQN + "/column1";
        final String child2QN = parentQN + "/column2";
        try {
            GuacamoleTable parent = GuacamoleTable.retrieveByQualifiedName(parentQN);
            GuacamoleColumn one = GuacamoleColumn.retrieveByQualifiedName(child1QN);
            GuacamoleColumn two = GuacamoleColumn.retrieveByQualifiedName(child2QN);
            Atlan.getDefaultClient()
                    .assets()
                    .delete(List.of(parent.getGuid(), one.getGuid(), two.getGuid()), AtlanDeleteType.PURGE);
            log.info("Entities purged.");
        } catch (AtlanException e) {
            log.error("Unable to purge entities.", e);
        }
    }
}
