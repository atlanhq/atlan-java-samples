/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.reporters;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.atlan.Atlan;
import com.atlan.exception.AtlanException;
import com.atlan.model.admin.AtlanGroup;
import com.atlan.model.admin.AtlanUser;
import com.atlan.model.admin.GroupResponse;
import com.atlan.samples.writers.ExcelWriter;
import com.atlan.samples.writers.S3Writer;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
public class UserReporter extends AbstractReporter implements RequestHandler<Map<String, String>, String> {

    private static final Map<String, String> USER_REPORT_HEADERS = createUserDetailsReportHeaders();

    private static final Set<String> autoSizeSheets = new HashSet<>();

    private static final Comparator<String> stringComparator = Comparator.nullsFirst(String::compareTo);

    public static void main(String[] args) {
        UserReporter ur = new UserReporter();
        Map<String, String> event = new HashMap<>(System.getenv());
        if (!event.containsKey("DELIMITER")) {
            event.put("DELIMITER", "|");
        }
        ur.handleRequest(event, null);
    }

    @Override
    protected void parseParametersFromEvent(Map<String, String> event) {
        super.parseParametersFromEvent(event);
        if (event != null) {
            setFilenameWithPrefix(event, "user-details-report");
        }
    }

    @Override
    public String handleRequest(Map<String, String> event, Context context) {

        try {
            parseParametersFromEvent(event);

            log.info("Retrieving user details for tenant: {}", Atlan.getBaseUrlSafe());
            List<AtlanUser> users;
            users = AtlanUser.retrieveAll();
            users.sort(Comparator.comparing(AtlanUser::getFirstName, stringComparator)
                    .thenComparing(AtlanUser::getLastName, stringComparator));

            log.info("Creating Excel file (in-memory)...");
            if (context != null && context.getClientContext() != null) {
                log.debug(
                        " ... client environment: {}",
                        context.getClientContext().getEnvironment());
                log.debug(" ... client custom: {}", context.getClientContext().getCustom());
            }

            // Adding content in the excel report
            ExcelWriter xlsx = new ExcelWriter();
            Sheet userinfo = xlsx.createSheet("User Details");
            autoSizeSheets.add("User Details");
            xlsx.addHeader(userinfo, USER_REPORT_HEADERS);
            addUserDetailRecords(xlsx, userinfo, users);

            // If a bucket was provided, we'll write out to S3
            if (getBucket() != null) {
                log.info("Putting file: {} into S3", getFilename());
                S3Client s3Client = S3Client.builder().region(getRegion()).build();
                S3Writer s3 = new S3Writer(s3Client);
                s3.putExcelFile(xlsx.asByteArray(), getBucket(), getFilename());
            } else {
                // Otherwise we'll write out to a file (locally)
                log.info("Writing report to file: {}", getFilename());
                xlsx.create(getFilename(), autoSizeSheets);
            }
        } catch (AtlanException e) {
            log.error("Failed to retrieve asset details from: {}", Atlan.getBaseUrlSafe(), e);
            System.exit(1);
        } catch (IOException e) {
            log.error("Failed to write Excel file to: {}", getFilename(), e);
            System.exit(1);
        }

        return getFilename();
    }

    static Map<String, String> createUserDetailsReportHeaders() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("GUID", "Unique identifier for the user.");
        map.put("Username", "Unique username for the user.");
        map.put("First Name", "First name of the user.");
        map.put("Last Name", "Last name of the user.");
        map.put("Email", "Email address of the user.");
        map.put("Verified", "When true, the user's email has been verified.");
        map.put(
                "Enabled",
                "When true, the user is enabled and allowed to log in to Atlan. When false, the user will be prevented from logging in to Atlan.");
        map.put("Role", "User roles, including login roles and custom roles emerging from persona associations.");
        map.put("Logins", "Number of successful logins by the user account.");
        map.put("Last Login", "Timestamp of the last successful login for the user.");
        map.put("Groups", "Atlan Groups the user is part of.");
        map.put("Personas", "Personas the user is part, directly and by virtue of group membership.");
        return map;
    }

    void addUserDetailRecords(ExcelWriter xlsx, Sheet sheet, List<AtlanUser> users) throws AtlanException {

        // iterate on user list and write rows in the report
        for (AtlanUser user : users) {
            if (user != null) {

                // convert user's last login timestamp to local timezone
                Long userLastLoginTime = user.getLastLoginTime();
                String reportLastLoginTime = "";
                if (userLastLoginTime != null && userLastLoginTime > 0) {
                    LocalDateTime ldt =
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(userLastLoginTime), ZoneOffset.UTC);
                    reportLastLoginTime = ldt.format(DateTimeFormatter.ISO_DATE_TIME);
                }

                // format user roles
                List<String> roles = user.getRoles();
                roles.remove("default-roles-default");
                String reportRoles = String.join("|", roles).replace("$", "");

                // gather details of user groups
                List<String> groupNames = Collections.emptyList();
                try {
                    GroupResponse groupResponse = user.fetchGroups();
                    if (groupResponse != null) {
                        List<AtlanGroup> groupList = groupResponse.getRecords();
                        if (groupList != null) {
                            groupNames =
                                    groupList.stream().map(AtlanGroup::getAlias).collect(Collectors.toList());
                        }
                    }
                } catch (AtlanException e) {
                    log.warn("Failed to retrieve group information for user {}.", user.getUsername(), e);
                }
                String reportGroupNames = String.join("|", groupNames);

                // gather details of user personas
                List<String> personaNames = Collections.emptyList();
                try {
                    SortedSet<AtlanUser.Persona> userPersonas = user.getPersonas();
                    if (userPersonas != null) {
                        personaNames = userPersonas.stream()
                                .map(AtlanUser.Persona::getDisplayName)
                                .collect(Collectors.toList());
                    }
                } catch (Exception e) {
                    log.warn("Failed to retrieve persona details for user {}.", user.getUsername(), e);
                }
                String reportPersonaNames = String.join("|", personaNames);

                // write row
                xlsx.appendRow(
                        sheet,
                        List.of(
                                ExcelWriter.DataCell.of(user.getId()),
                                ExcelWriter.DataCell.of(user.getUsername()),
                                ExcelWriter.DataCell.of(user.getFirstName()),
                                ExcelWriter.DataCell.of(user.getLastName()),
                                ExcelWriter.DataCell.of(user.getEmail()),
                                ExcelWriter.DataCell.of(user.getEmailVerified()),
                                ExcelWriter.DataCell.of(user.getEnabled()),
                                ExcelWriter.DataCell.of(reportRoles),
                                ExcelWriter.DataCell.of(user.getLoginEvents().size()),
                                ExcelWriter.DataCell.of(reportLastLoginTime),
                                ExcelWriter.DataCell.of(reportGroupNames),
                                ExcelWriter.DataCell.of(reportPersonaNames)));
            }
        }
    }
}
