/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.readers;

import com.atlan.Atlan;
import com.atlan.exception.AtlanException;
import com.atlan.model.assets.Asset;
import com.atlan.util.AssetBatch;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for reading from CSV files, using FastCSV.
 */
@Slf4j
public class CSVReader implements Closeable {

    private final CsvReader reader;
    private final List<String> header;
    private final long totalRowCount;
    private final int typeIdx;
    private final int qualifiedNameIdx;

    /**
     * Construct a new CSV file reader, using a comma (as the C in CSV implies)
     * as the field separator.
     *
     * @param path location and filename of the CSV file to read
     * @throws IOException on any errors accessing or reading from the file
     */
    public CSVReader(String path) throws IOException {
        this(path, ',');
    }

    /**
     * Construct a new CSV file reader, using a specific field separator character.
     *
     * @param path location and filename of the CSV file to read
     * @throws IOException on any errors accessing or reading from the file
     */
    public CSVReader(String path, char fieldSeparator) throws IOException {
        Path inputFile = Paths.get(path);
        CsvReader.CsvReaderBuilder builder = CsvReader.builder()
                .fieldSeparator(fieldSeparator)
                .quoteCharacter('"')
                .skipEmptyRows(true)
                .errorOnDifferentFieldCount(true);
        try (CsvReader tmp = builder.build(inputFile)) {
            totalRowCount = tmp.stream().parallel().count() - 1;
        }
        try (CsvReader tmp = builder.build(inputFile)) {
            Optional<CsvRow> one = tmp.stream().findFirst();
            header = one.map(CsvRow::getFields).orElse(Collections.emptyList());
        }
        typeIdx = header.indexOf("typeName");
        qualifiedNameIdx = header.indexOf("qualifiedName");
        if (typeIdx < 0 || qualifiedNameIdx < 0) {
            throw new IOException(
                    "Unable to find either (or both) the columns 'typeName' and / or 'qualifiedName'. These are both mandatory columns in the input CSV.");
        }
        reader = builder.build(inputFile);
    }

    /**
     * Parallel-read the CSV file into batched asset updates against Atlan.
     * Note: this requires the input CSV file to be fully parallel-loadable without any
     * conflicts. That means: every row is a unique asset, no two rows update any relationship
     * attribute that points at the same related asset (such as an assigned term).
     *
     * @param assetForRow a function (could just be a lambda) that turns a list of string values (from the row) into an Asset
     * @param batchSize maximum number of Assets to bulk-save in Atlan per API request
     */
    public void streamRows(AssetGenerator assetForRow, int batchSize) {
        // Note that for proper parallelism we need to manage a separate AssetBatch per thread
        Map<Long, AssetBatch> batchMap = new ConcurrentHashMap<>();
        // The -1, skip(1) are to avoid counting the header
        log.info("Loading a total of {} assets...", totalRowCount);
        AtomicLong count = new AtomicLong(0);
        reader.stream().skip(1).parallel().forEach(r -> {
            long id = Thread.currentThread().getId();
            if (!batchMap.containsKey(id)) {
                // Initialize a new AssetBatch for each parallel thread
                batchMap.put(
                        id,
                        new AssetBatch(
                                Atlan.getDefaultClient(),
                                "asset",
                                batchSize,
                                true,
                                AssetBatch.CustomMetadataHandling.MERGE,
                                true));
            }
            AssetBatch localBatch = batchMap.get(id);
            long localCount = count.getAndIncrement();
            if (localCount % batchSize == 0) {
                log.info(
                        " ... processed {}/{} ({}%)",
                        localCount, totalRowCount, Math.round(((double) localCount / totalRowCount) * 100));
            }
            try {
                localBatch.add(assetForRow.buildFromRow(r.getFields(), header, typeIdx, qualifiedNameIdx));
            } catch (AtlanException e) {
                log.error("Unable to load batch.", e);
            }
        });
        // Parallel-stream the final flushing of the batches, too
        AtomicLong totalCreates = new AtomicLong(0);
        AtomicLong totalUpdates = new AtomicLong(0);
        AtomicLong totalFailures = new AtomicLong(0);
        batchMap.values().parallelStream().forEach(b -> {
            try {
                b.flush();
                totalCreates.getAndAdd(b.getCreated().size());
                totalUpdates.getAndAdd(b.getUpdated().size());
                if (!b.getFailures().isEmpty()) {
                    for (AssetBatch.FailedBatch f : b.getFailures()) {
                        log.info("Failed batch reason:", f.getFailureReason());
                        totalFailures.getAndAdd(f.getFailedAssets().size());
                        for (Asset failed : f.getFailedAssets()) {
                            log.info(" ... included asset: {}::{}", failed.getTypeName(), failed.getQualifiedName());
                        }
                    }
                }
            } catch (AtlanException e) {
                log.error("Unable to flush final batch.", e);
            }
        });
        log.info("Total assets created: {}", totalCreates);
        log.info("Total assets updated: {}", totalUpdates);
        log.info("Total assets failed : {}", totalFailures);
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        reader.close();
    }
}
