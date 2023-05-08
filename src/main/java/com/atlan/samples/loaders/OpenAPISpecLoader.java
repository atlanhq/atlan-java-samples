/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.loaders;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.atlan.cache.RoleCache;
import com.atlan.exception.AtlanException;
import com.atlan.exception.NotFoundException;
import com.atlan.model.assets.*;
import com.atlan.model.assets.Asset;
import com.atlan.model.core.AssetMutationResponse;
import com.atlan.model.enums.AtlanConnectorType;
import com.atlan.samples.readers.OpenAPISpecReader;
import com.atlan.util.AssetBatch;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads API assets into Atlan from an OpenAPI spec.
 */
@Slf4j
public class OpenAPISpecLoader extends AbstractLoader implements RequestHandler<Map<String, String>, String> {

    private String _specUrl = null;
    private String _apiName = null;

    public static void main(String[] args) {
        OpenAPISpecLoader sl = new OpenAPISpecLoader();
        sl.handleRequest(System.getenv(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parseParametersFromEvent(Map<String, String> event) {
        super.parseParametersFromEvent(event);
        if (event != null) {
            String specUrl = event.getOrDefault("API_SPEC_URL", null);
            if (specUrl != null) {
                _specUrl = specUrl;
            }
            String apiName = event.getOrDefault("API_NAME", null);
            if (apiName != null) {
                _apiName = apiName;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String handleRequest(Map<String, String> event, Context context) {

        log.info("Retrieving configuration and context...");
        if (context != null && context.getClientContext() != null) {
            log.debug(" ... client environment: {}", context.getClientContext().getEnvironment());
            log.debug(" ... client custom: {}", context.getClientContext().getCustom());
        }
        parseParametersFromEvent(event);

        if (_apiName == null || _specUrl == null) {
            log.error(
                    "Missing key inputs — must have both API_SPEC_URL ({}) and API_NAME ({}) defined.",
                    _apiName,
                    _specUrl);
            System.exit(1);
        }

        log.info("Loading definitions from: {}", _specUrl);

        OpenAPISpecReader parser = new OpenAPISpecReader(_specUrl);

        String connectionQualifiedName = null;
        log.info("Looking for existing API connection named: {}", _apiName);
        try {
            List<Connection> found = Connection.findByName(_apiName, AtlanConnectorType.API, null);
            if (found.size() > 1) {
                log.warn("... found multiple API connections with name {}", _apiName);
            }
            connectionQualifiedName = found.get(0).getQualifiedName();
            log.info("... re-using: {} ({})", _apiName, connectionQualifiedName);
        } catch (NotFoundException e) {
            try {
                log.info("... none found, creating a new API connection...");
                Connection connectionToCreate = Connection.creator(
                                _apiName, AtlanConnectorType.API, List.of(RoleCache.getIdForName("$admin")), null, null)
                        .build();
                AssetMutationResponse response = connectionToCreate.upsert();
                if (response != null && response.getCreatedAssets().size() == 1) {
                    connectionQualifiedName = response.getCreatedAssets().get(0).getQualifiedName();
                    log.info("... created connection: {}", connectionQualifiedName);
                }
            } catch (AtlanException create) {
                log.error("Unable to create a connection for the API.", create);
                System.exit(1);
            }
        } catch (AtlanException find) {
            log.error("Unable to even attempt to find an existing connection for the API.", find);
            System.exit(1);
        }

        if (connectionQualifiedName == null) {
            log.error("Unable to find an existing or create a new connection for the API.");
            System.exit(1);
        }

        // Retrieve the connection, to ensure async access is resolved
        try {
            Asset.retrieveMinimal(Connection.TYPE_NAME, connectionQualifiedName);
        } catch (AtlanException e) {
            log.error("Unable to retrieve the connection for the API.", e);
            System.exit(1);
        }

        String specQualifiedName = null;
        try {
            APISpec specToCreate = APISpec.creator(parser.getTitle(), connectionQualifiedName)
                    .sourceURL(_specUrl)
                    .apiSpecType(parser.getOpenAPIVersion())
                    .description(parser.getDescription())
                    .apiSpecTermsOfServiceURL(parser.getTermsOfServiceURL())
                    .apiSpecContactEmail(parser.getContactEmail())
                    .apiSpecContactName(parser.getContactName())
                    .apiSpecContactURL(parser.getContactURL())
                    .apiSpecLicenseName(parser.getLicenseName())
                    .apiSpecLicenseURL(parser.getLicenseURL())
                    .apiSpecVersion(parser.getVersion())
                    .apiExternalDoc("url", parser.getExternalDocsURL())
                    .apiExternalDoc("description", parser.getExternalDocsDescription())
                    .build();
            log.info("Upserting APISpec: {}", specToCreate.getQualifiedName());
            AssetMutationResponse response = specToCreate.upsert();
            if (response != null) {
                if (response.getCreatedAssets().size() == 1) {
                    specQualifiedName = response.getCreatedAssets().get(0).getQualifiedName();
                    log.info("... created APISpec: {}", specQualifiedName);
                } else if (response.getUpdatedAssets().size() == 1) {
                    specQualifiedName = response.getUpdatedAssets().get(0).getQualifiedName();
                    log.info("... updated APISpec: {}", specQualifiedName);
                } else {
                    specQualifiedName = specToCreate.getQualifiedName();
                    log.info("... reusing existing APISpec: {}", specQualifiedName);
                }
            }
        } catch (AtlanException e) {
            log.error("Unable to upsert APISpec.", e);
            System.exit(1);
        }

        AssetBatch batch = new AssetBatch(APIPath.TYPE_NAME, getBatchSize());
        Paths apiPaths = parser.getPaths();
        if (apiPaths != null && !apiPaths.isEmpty()) {
            log.info("Creating an APIPath for each path defined within the spec: {}", apiPaths.size());
            for (Map.Entry<String, PathItem> apiPath : apiPaths.entrySet()) {
                String pathUrl = apiPath.getKey();
                PathItem pathDetails = apiPath.getValue();
                List<String> operations = new ArrayList<>();
                StringBuilder desc = new StringBuilder();
                desc.append("| Method | Summary |\n|---|---|\n");
                Operation get = pathDetails.getGet();
                if (get != null) {
                    operations.add("GET");
                    desc.append("| `GET` | ").append(get.getSummary()).append(" |\n");
                }
                Operation post = pathDetails.getPost();
                if (post != null) {
                    operations.add("POST");
                    desc.append("| `POST` | ").append(post.getSummary()).append(" |\n");
                }
                Operation put = pathDetails.getPut();
                if (put != null) {
                    operations.add("PUT");
                    desc.append("| `PUT` | ").append(put.getSummary()).append(" |\n");
                }
                Operation patch = pathDetails.getPatch();
                if (patch != null) {
                    operations.add("PATCH");
                    desc.append("| `PATCH` | ").append(patch.getSummary()).append(" |\n");
                }
                Operation delete = pathDetails.getDelete();
                if (delete != null) {
                    operations.add("DELETE");
                    desc.append("| `DELETE` | ").append(delete.getSummary()).append(" |\n");
                }
                APIPath path = APIPath.creator(pathUrl, specQualifiedName)
                        .description(desc.toString())
                        .apiPathRawURI(pathUrl)
                        .apiPathSummary(pathDetails.getSummary())
                        .apiPathAvailableOperations(operations)
                        .apiPathIsTemplated(pathUrl.contains("{") && pathUrl.contains("}"))
                        .build();
                logResult(batch.add(path));
            }
            logResult(batch.flush());
        }
        return _specUrl;
    }

    private void logResult(AssetMutationResponse response) {
        if (response != null) {
            List<Asset> createdList = response.getCreatedAssets();
            for (Asset created : createdList) {
                log.info("...... created: {}: {}", created.getTypeName(), created.getQualifiedName());
            }
            List<Asset> updatedList = response.getUpdatedAssets();
            for (Asset updated : updatedList) {
                log.info("...... updated: {}: {}", updated.getTypeName(), updated.getQualifiedName());
            }
        }
    }
}
