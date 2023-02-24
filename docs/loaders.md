<!-- SPDX-License-Identifier: CC-BY-4.0 -->
<!-- Copyright 2023 Atlan Pte. Ltd. -->

# Atlan Java Samples — Loaders

This package provides examples to ingest metadata into Atlan, each in an idempotent manner.

Each relies on an input spreadsheet that has certain columns pre-defined. The name of these columns is the critical piece to use, their order in the spreadsheet is irrelevant.

In all cases, these columns names must be the _second_ row of the spreadsheet. The first row is ignored, and can therefore be used to give any groupings or instructions to users you want _above_ the fixed set of column names.

Each of the loaders shares a general set of columns as follows, which apply to all assets in Atlan:

- Description: a basic explanation of that asset (usually 1-sentence)
- Certificate: a certificate for the asset (`VERIFIED`, `DRAFT` or `DEPRECATED`)
- Certificate Message: an optional message to associate with the certificate
- Announcement: an announcement for the asset (`information`, `warning` or `issue`)
- Announcement Title: a subject line to associate with the announcement
- Announcement Message: a detailed message to associate with the announcement
- Owner Users: a pipe-delimited list of Atlan usernames who should be owners for this asset
- Owner Groups: a pipe-delimited list of Atlan group names who should be owners for this asset
- Classifications: a pipe-delimited list of the human-readable names of any classifications that should be added to this asset

Note: any classifications listed must already exist in Atlan, and will always be appended to any existing classifications on the asset (rather than replacing existing classifications on the asset)

Providing values for these columns is optional in all cases — none of these are mandatory for creating or updating an asset in Atlan.

Finally, any cell that expects a boolean value can be provided any of the following, case-insensitive, to mean `true`:

- `X`
- `Y`
- `YES`
- `TRUE`

## `TabularAssetLoader`

Updates (or creates, if it does not exist) metadata about tabular assets: databases, schemas, tables, and columns.

Expects a sheet in the Excel workbook named `Tablular Assets`, with the following header columns (row 2):

- Connector: name of the connector (must be a valid Atlan connector type)
- Connection Name: name of the connection
- Database Name: name of the database
- Schema Name: name of the schema
- Container Name: name of the container (table, view, etc) object
- Container Type: type of the container (Table, View or MaterialisedView)
- Column Name: name of the column
- Column Data Type: a SQL-style data type — for example, VARCHAR(30) or DECIMAL(3,5)
- Primary Key?: if true, indicates this column is a primary key (blank means false)
- Foreign Key?: if true, indicates this column is a foreign key (blank means false)

Special interpretation of the common columns:

- Owner Users: when a row defines a connection, these will be set as the connection admins.
- Owner Groups: when a row defines a connection, these will be set as connection admins. Furthermore, if the group name starts with `$` then it will be interpreted as a role (for example `$admin` is for "All Admins").

Note: if no owners at all are listed in the spreadsheet, `$admin` ("All Admins") will be used as the default for connection admins.

## `ObjectStoreAssetLoader`

Updates (or creates, if it does not exist) metadata about object store assets: accounts, buckets, and objects.

Expects a sheet in the Excel workbook named `Object Store Assets`, with the following header columns (row 2):

- Connector: name of the connector (must be a valid Atlan connector type)
- Connection Name: name of the connection
- Account Name: name of the ADLS account (only used for ADLS assets)
- Bucket Name: name of the bucket (or container, for ADLS)
- Bucket ARN: unique Amazon Resource Number (ARN), required for an AWS bucket
- Object Name: name of the object
- Object ARN: unique Amazon Resource Number (ARN), required for an AWS object
- Object Path: (optional) the path to the object, used as the `objectKey`
- Object Size: (optional) the size of the object, in bytes
- Content Type: (optional) the content type of the object, for example `text/csv`

Special interpretation of the common columns:

- Owner Users: when a row defines a connection, these will be set as the connection admins.
- Owner Groups: when a row defines a connection, these will be set as connection admins. Furthermore, if the group name starts with `$` then it will be interpreted as a role (for example `$admin` is for "All Admins").

Note: if no owners at all are listed in the spreadsheet, `$admin` ("All Admins") will be used as the default for connection admins.

## `LineageLoader`

Updates (or creates, if they do not exist) lineage: processes that can have one or more inputs and produce one or more outputs.

Expects a sheet in the Excel workbook named `Lineage`, with the following header columns (row 2):

- Connector (s): name of the connector for the source asset (must be a valid Atlan connector type)
- Connection (s): name of the connection for the source asset
- Source Asset Type: type of the source asset
- Source Asset: partial or fully-qualified name of the source asset. If you provide a full `qualifiedName` (for example, copied and pasted from Atlan) then the `Connector (s)` and `Connection (s)` columns are ignored. If you provide a partial `qualifiedName` (for example, `db/schema/table`) the connection portion of the qualified name will be looked up using the `Connector (s)` and `Connection (s)` details.
- Orchestrator: name of the software tool or system that orchestrated the process to be represented in lineage. Each unique value in this column will result in an `API` connector in Atlan, in which all lineage processes for that orchestrator will be contained.
- Process ID: unique name of the process that should be represented in lineage. All rows that share the combination of `Orchestrator` and `Process ID` will be combined into a single lineage process (with potentially multiple sources and targets).
- Target Asset: partial or fully-qualified name of the target asset. If you provide a full `qualifiedName` (for example, copied and pasted from Atlan) then the `Connector (t)` and `Connection (t)` columns are ignored. If you provide a partial `qualifiedName` (for example, `db/schema/table`) the connection portion of the qualified name will be looked up using the `Connector (t)` and `Connection (t)` details.
- SQL / Code: (optional) any SQL or other code you want to use to describe in technical detail what occurred within the integration process
- Process URL: (optional) a URL to the integration process itself. This could be to the details of the process running in the orchestrator tool, to a GitHub repository holding the code for this integration process, or to any other arbitrary URL describing the process. 

Note:

- The lineage loader will only create connections and process assets for the orchestrators — the source and target assets (and their connections) must already exist. (If you need these to be created, use the loaders above to first create the source and target assets.)
- The common columns in this sheet (description, certificate, announcement and so on) will all be used to describe the lineage process itself — not the source or target asset(s).

----
License: [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/),
Copyright 2023 Atlan Pte. Ltd.
