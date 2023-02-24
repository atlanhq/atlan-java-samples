<!-- SPDX-License-Identifier: CC-BY-4.0 -->
<!-- Copyright 2023 Atlan Pte. Ltd. -->

# Atlan Java Samples — Reporters

This package provides examples to extract metadata from Atlan.

These can be run:

- Through an AWS Lambda function, which can be scheduled to run periodically from within Atlan. Each run will produce a timestamped Excel file into an S3 bucket of your choice.
- Locally on your workstation. Each run will produce a timestamped Excel file in the directory where you run the code on your workstation.

(Detailed instructions for running the reporters in either mode are listed below the description of each reporter.)

## `EnrichmentReporter`

Extracts metadata about assets and their enrichment, producing an Excel workbook consisting of 4 worksheets:

- `Asset enrichment` contains details about all non-glossary assets that fit the filter criteria (see below)
- `Glossary enrichment` contains details about each of the glossaries
- `Category enrichment` contains details about each of the categories, across all glossaries
- `Term enrichment` contains details about each term, across all glossaries

To filter the assets to include in the `Asset enrichment` worksheet, you must choose one of the following criteria:

- `GROUP` will include all assets that have at least one group owner defined
- `CLASSIFICATION` will include all assets that are assigned a specified classification

Field-level assets (like database columns) will only be counted rather than individually listed in the output.

To use this reporter as an AWS Lambda function, you can configure the following options (default values are shown, in case you leave any out):

```json
{
  "FILTER_BY": "GROUP",
  "CLASSIFICATION": "(name of classification to filter by)",
  "BATCH_SIZE": "50",
  "FILE_PREFIX": "enrichment-report",
  "REGION": "ap-south-1",
  "BUCKET": ""
}
```

- `FILTER_BY` defines how you want to filter the non-glossary assets. `GROUP` will only report on assets that have a group owner defined, while `CLASSIFICATION` will only report on assets with a specific classification.
- `CLASSIFICATION` defines which classification you want to report on (only assets with this classification will be included in the report). Note that this only has any effect when `FILTER_BY` is set to `CLASSIFICATION`.
- `BATCH_SIZE` defines how many records the API calls attempt to retrieve on each request.
- `FILE_PREFIX` defines what the Excel file's name will start with.
- `REGION` defines the AWS region of the S3 bucket where you want to write the Excel file output.
- `BUCKET` defines the S3 bucket where you want the reporter to store the Excel file output. (Note that there is no default value for this, so this **must** be sent for the utility to run via a Lambda function and produce consumable output — if blank (default) the reporter produces a local Excel file.)

You can configure the reporters to run via a Lambda function in AWS. This approach will allow you to schedule
the reporter to run on whatever frequency you prefer using the AWS Lambda Trigger workflow in Atlan.

When run in this mode, the reporter will write the output Excel files into an S3 bucket of your choice.

## Configuring the reporters to run through AWS Lambda

### Build the package

To build the package for deployment into a Lambda function, from a cloned copy of this repository run:

```shell
./gradlew buildZip
```

This will create a `atlan-java-samples-1.0-SNAPSHOT.zip` file under the `build/distributions` directory.

### Create Lambda function

Create a new Lambda function (from scratch) in your AWS account, with the following:

- Runtime of **Java 11 (Corretto)**
- Upload the `atlan-java-samples-1.0-SNAPSHOT.zip` file as the **Code source** for the function
- Change the *Handler* for the **Runtime settings** to the class of the report you want the Lambda to produce:
    - `com.atlan.samples.reporters.EnrichmentReporter` for the enrichment report
- Change the *Configuration* of the function as follows:
    - Increase the **Timeout** to at least 1 minute
    - Add S3 permissions to allow reading and writing to the S3 bucket where you want to store the Excel output
    - Add a policy that allows either an IAM role or user to use the `lambda:InvokeFunction` action on the function
    - Add an environment variable called `ATLAN_API_KEY` to the function, and set its value to your Atlan API token
    - Add an environment variable called `ATLAN_BASE_URL` to the function, and set its value to your Atlan tenant URL (for example, `https://my.atlan.com`)

### Create S3 bucket

Ensure the S3 bucket where you want the results to be stored already exists, before running the Lambda function.
(The reporter will only try to write to an existing bucket, it will **not** attempt to create that bucket if it does
not exist.)

### Create Atlan AWS Lambda Trigger

When [setting up the Lambda trigger in Atlan](https://ask.atlan.com/hc/en-us/articles/6755270200977),
provide:

- the IAM credentials you gave permission to invoke the Lambda function
- the ARN of the Lambda function
- use `$LATEST` as the qualifier for the function

And for the payload, see the specific configuration options listed for each different reporter.

## Configuring the reporters to run locally

You can also run the reporters locally on any machine with network access to your Atlan tenant.

When run in this mode, the reporters will write the output Excel files locally on the machine running
the reporter.

### API token and URL

By default, the reporter looks at environment variables for the API token and URL to run against:

- `ATLAN_API_KEY` for the API token
- `ATLAN_BASE_URL` for the URL of the Atlan tenant

### File to write

The Excel spreadsheet will be created in the directory where the reporter is run, and will
have a name of the form `<FILE_PREFIX>-yyyyMMdd-HHmmss-SSS.xlsx`, where the `FILE_PREFIX` will
default to a specific name by report type (if unspecified as an environment variable).

### Logging

By default, only messages at `INFO` and above will be logged (to the console). You can change this level
by modifying the `main/resources/logback.xml` to give:

- more detail (`DEBUG` will log every API request and response, including their full payloads)
- less detail (`WARN` will only print warnings and errors, `ERROR` only errors).

----
License: [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/),
Copyright 2023 Atlan Pte. Ltd.
