<!-- SPDX-License-Identifier: CC-BY-4.0 -->
<!-- Copyright 2023 Atlan Pte. Ltd. -->

# Atlan Java Samples

This repository houses samples of using the [Atlan Java SDK](https://developer.atlan.com/sdks/java/).

Currently, these cover:

- [Loaders](src/main/java/com/atlan/samples/loaders) — import metadata into Atlan, both net-new and by enriching existing assets
- [Reporters](src/main/java/com/atlan/samples/reporters) — extract or report on metadata in Atlan

These are based on common functionality to read and write from Excel files (based on [Apache POI](https://poi.apache.org/)),
and to integrate with AWS S3 to enable them to run as Lambda functions.

----
License: [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/),
Copyright 2023 Atlan Pte. Ltd.
