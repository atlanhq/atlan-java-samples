<!-- SPDX-License-Identifier: CC-BY-4.0 -->
<!-- Copyright 2023 Atlan Pte. Ltd. -->

[![Build](https://github.com/atlanhq/atlan-java-samples/workflows/Merge/badge.svg)](https://github.com/atlanhq/atlan-java-samples/actions/workflows/merge.yml?query=workflow%3AMerge)
[![JavaDocs](https://img.shields.io/badge/javadocs-passing-success)](https://atlanhq.github.io/atlan-java-samples/)
[![Development](https://img.shields.io/nexus/s/com.atlan/atlan-java-samples?label=development&server=https%3A%2F%2Fs01.oss.sonatype.org)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/atlan/atlan-java-samples/)
<!--[![CodeQL](https://github.com/atlanhq/atlan-java-samples/workflows/CodeQL/badge.svg)](https://github.com/atlanhq/atlan-java-samples/actions/workflows/codeql-analysis.yml) -->

# Atlan Java Samples

This repository houses samples of using the [Atlan Java SDK](https://developer.atlan.com/sdks/java/).

Currently, these cover:

- [Loaders](https://developer.atlan.com/samples/loaders) — import metadata into Atlan, both net-new and by enriching existing assets
- [Reporters](https://developer.atlan.com/samples/reporters) — extract or report on metadata in Atlan

These are based on common functionality to read and write from Excel files (based on [Apache POI](https://poi.apache.org/)),
and to integrate with AWS S3 to enable them to run as Lambda functions.

----
License: [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/),
Copyright 2023 Atlan Pte. Ltd.
