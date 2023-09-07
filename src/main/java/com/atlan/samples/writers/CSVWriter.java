/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.writers;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.atlan.model.assets.Asset;
import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import de.siegmar.fastcsv.writer.QuoteStrategy;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for creating and writing to CSV files, using FastCSV.
 */
@Slf4j
public class CSVWriter implements Closeable {

    private final CsvWriter writer;

    /**
     * Construct a new CSV file writer, using a comma (as the C in CSV implies)
     * as the field separator.
     *
     * @param path location and filename of the CSV file to produce
     * @throws IOException on any errors creating or accessing the file
     */
    public CSVWriter(String path) throws IOException {
        this(path, ',');
    }

    /**
     * Construct a new CSV file writer, using a specific field separator character.
     *
     * @param path location and filename of the CSV file to produce
     * @param fieldSeparator character to use to separate fields (for example ',' or ';')
     * @throws IOException on any errors creating or accessing the file
     */
    public CSVWriter(String path, char fieldSeparator) throws IOException {
        writer = CsvWriter.builder()
                .fieldSeparator(fieldSeparator)
                .quoteCharacter('"')
                .quoteStrategy(QuoteStrategy.REQUIRED)
                .lineDelimiter(LineDelimiter.PLATFORM)
                .build(new ThreadSafeWriter(path));
    }

    /**
     * Write a header row into the CSV file.
     *
     * @param values to use for the header
     */
    public void writeHeader(Iterable<String> values) {
        writer.writeRow(values);
    }

    /**
     * Parallel-write the provided asset stream into the CSV file.
     * (For the highest performance, we recommend sending in a parallel stream of assets.)
     *
     * @param stream of assets, typically from a FluentSearch (parallel stream recommended)
     * @param valuesForRow a function (could just be a lambda) that turns a single Asset into an iterable of String values
     * @param totalAssetCount the total number of assets that will be output (used for logging / completion tracking)
     * @param pageSize the page size being used by the asset stream
     */
    public void streamAssets(
            Stream<Asset> stream, RowGenerator valuesForRow, final long totalAssetCount, final int pageSize) {
        log.info("Extracting a total of {} assets...", totalAssetCount);
        AtomicLong count = new AtomicLong(0);
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        stream.forEach(a -> {
            long localCount = count.getAndIncrement();
            if (localCount % pageSize == 0) {
                log.info(
                        " ... processed {}/{} ({}%)",
                        localCount, totalAssetCount, Math.round(((double) localCount / totalAssetCount) * 100));
            }
            String duplicate = map.put(a.getGuid(), a.getTypeName() + "::" + a.getGuid());
            if (duplicate != null) {
                log.warn("Hit a duplicate asset entry — there could be page skew: {}", duplicate);
            }
            Iterable<String> values = valuesForRow.valuesFromAsset(a);
            synchronized (writer) {
                writer.writeRow(values);
            }
        });
        log.info("Total unique assets extracted: {}", map.size());
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        writer.close();
    }

    /**
     * To allow us to safely parallel-write files across threads.
     */
    private static final class ThreadSafeWriter extends Writer implements Closeable {
        private final BufferedWriter writer;

        public ThreadSafeWriter(String filePath) throws IOException {
            writer = Files.newBufferedWriter(Paths.get(filePath), UTF_8);
        }

        /** {@inheritDoc} */
        @Override
        public synchronized void write(char[] cbuf, int off, int len) throws IOException {
            writer.write(cbuf, off, len);
        }

        /** {@inheritDoc} */
        @Override
        public synchronized void flush() throws IOException {
            writer.flush();
        }

        /** {@inheritDoc} */
        @Override
        public synchronized void close() throws IOException {
            writer.close();
        }
    }
}
