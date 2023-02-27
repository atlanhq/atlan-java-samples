package com.atlan.samples.readers;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;

/**
 * Utility class for parsing and reading the contents of an OpenAPI spec file, using the Swagger parser.
 */
public class SpecReader {

    private final OpenAPI spec;
    private final boolean hasInfo;
    private final boolean hasContact;
    private final boolean hasLicense;
    private final boolean hasExternalDocs;

    /**
     * Construct a new OpenAPI object.
     *
     * @param url location of the OpenAPI spec
     */
    public SpecReader(String url) {
        spec = new OpenAPIV3Parser().read(url);
        hasInfo = spec.getInfo() != null;
        hasContact = hasInfo && spec.getInfo().getContact() != null;
        hasLicense = hasInfo && spec.getInfo().getLicense() != null;
        hasExternalDocs = spec.getExternalDocs() != null;
    }

    /**
     * Retrieve the version of OpenAPI for the spec.
     *
     * @return OpenAPI version
     */
    public String getOpenAPIVersion() {
        return spec.getOpenapi();
    }

    /**
     * Retrieve the title of the OpenAPI spec.
     *
     * @return the title of the spec
     */
    public String getTitle() {
        return hasInfo ? spec.getInfo().getTitle() : null;
    }

    /**
     * Retrieve the description of the OpenAPI spec.
     *
     * @return the description of the spec
     */
    public String getDescription() {
        return hasInfo ? spec.getInfo().getDescription() : null;
    }

    /**
     * Retrieve the terms of service URL of the OpenAPI spec.
     *
     * @return the terms of service URL
     */
    public String getTermsOfServiceURL() {
        return hasInfo ? spec.getInfo().getTermsOfService() : null;
    }

    /**
     * Retrieve the contact email address for the OpenAPI spec.
     *
     * @return the contact email address
     */
    public String getContactEmail() {
        return hasContact ? spec.getInfo().getContact().getEmail() : null;
    }

    /**
     * Retrieve the contact name for the OpenAPI spec.
     *
     * @return the contact name
     */
    public String getContactName() {
        return hasContact ? spec.getInfo().getContact().getName() : null;
    }

    /**
     * Retrieve the contact URL for the OpenAPI spec.
     *
     * @return the contact URL
     */
    public String getContactURL() {
        return hasContact ? spec.getInfo().getContact().getUrl() : null;
    }

    /**
     * Retrieve the license name for the OpenAPI spec.
     *
     * @return the license name
     */
    public String getLicenseName() {
        return hasLicense ? spec.getInfo().getLicense().getName() : null;
    }

    /**
     * Retrieve the license URL for the OpenAPI spec.
     *
     * @return the license URL
     */
    public String getLicenseURL() {
        return hasLicense ? spec.getInfo().getLicense().getUrl() : null;
    }

    /**
     * Retrieve the version of the OpenAPI spec.
     *
     * @return the version number
     */
    public String getVersion() {
        return hasInfo ? spec.getInfo().getVersion() : null;
    }

    /**
     * Retrieve the URL of external docs defined for the OpenAPI spec.
     *
     * @return URL for the external docs
     */
    public String getExternalDocsURL() {
        return hasExternalDocs ? spec.getExternalDocs().getUrl() : null;
    }

    /**
     * Retrieve the description of the external docs defined for the OpenAPI spec.
     *
     * @return description of the external docs
     */
    public String getExternalDocsDescription() {
        return hasExternalDocs ? spec.getExternalDocs().getDescription() : null;
    }

    /**
     * Retrieve the paths defined within the spec.
     *
     * @return the paths defined in the spec
     */
    public Paths getPaths() {
        return spec.getPaths();
    }

}
