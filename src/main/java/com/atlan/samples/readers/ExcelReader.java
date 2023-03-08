/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.readers;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Utility class for parsing and reading the contents of Excel files, using Apache POI.
 */
public class ExcelReader {

    private final Workbook workbook;
    private final String caseSensitiveDelimiter;

    /**
     * Construct a new Excel file reader.
     *
     * @param fileLocation location of the Excel file
     * @throws IOException on any errors accessing or parsing the file
     */
    public ExcelReader(String fileLocation) throws IOException {
        this(fileLocation, "|");
    }

    /**
     * Construct a new Excel file reader allowing for case-sensitive column headings.
     *
     * @param fileLocation location of the Excel file
     * @param caseSensitiveDelimiter delimiter to look for to leave a column heading as case-sensitive
     * @throws IOException on any errors accessing or parsing the file
     */
    public ExcelReader(String fileLocation, String caseSensitiveDelimiter) throws IOException {
        FileInputStream file = new FileInputStream(fileLocation);
        workbook = new XSSFWorkbook(file);
        this.caseSensitiveDelimiter = caseSensitiveDelimiter;
    }

    /**
     * Retrieve all rows from the specified sheet of the Excel workbook, using the
     * very first row (0) as the header row.
     *
     * @param index the index (0-based) of the worksheet within the workbook
     * @return a list of rows, each being a mapping from column name (upper-cased) to its value
     */
    public List<Map<String, String>> getRowsFromSheet(int index) {
        return getRowsFromSheet(index, 0);
    }

    /**
     * Retrieve all rows from the specified sheet of the Excel workbook, using the
     * very first row (0) as the header row.
     *
     * @param index the index (0-based) of the worksheet within the workbook
     * @param headerRow index of the row containing headers (0-based)
     * @return a list of rows, each being a mapping from column name (upper-cased) to its value
     */
    public List<Map<String, String>> getRowsFromSheet(int index, int headerRow) {
        return getRowsFromSheet(workbook.getSheetAt(index), headerRow);
    }

    /**
     * Retrieve all rows from the specified sheet of the Excel workbook, using the very
     * first row (0) as the header row.
     *
     * @param name of the worksheet from which to retrieve the data
     * @return a list of rows, each being a mapping from column name (upper-cased) to its value
     * @throws IOException if the requested sheet cannot be found in the provided Excel file
     */
    public List<Map<String, String>> getRowsFromSheet(String name) throws IOException {
        return getRowsFromSheet(name, 0);
    }

    /**
     * Retrieve all rows from the specified sheet of the Excel workbook.
     *
     * @param name of the worksheet from which to retrieve the data
     * @param headerRow index of the row containing headers (0-based)
     * @return a list of rows, each being a mapping from column name (upper-cased) to its value
     * @throws IOException if the requested sheet cannot be found in the provided Excel file
     */
    public List<Map<String, String>> getRowsFromSheet(String name, int headerRow) throws IOException {
        Sheet sheet = workbook.getSheet(name);
        if (sheet == null) {
            throw new IOException("Could not find sheet with name '" + name + "' in the provided Excel file.");
        }
        return getRowsFromSheet(sheet, headerRow);
    }

    /**
     * Retrieve all rows from the specified sheet of the Excel workbook.
     *
     * @param data the worksheet from which to retrieve the data
     * @param headerRow index of the row containing headers (0-based)
     * @return a list of rows, each being a mapping from column name (upper-cased) to its value
     */
    private List<Map<String, String>> getRowsFromSheet(Sheet data, int headerRow) {
        List<Map<String, String>> allRows = new ArrayList<>();
        List<String> header = getHeaders(data.getRow(headerRow));
        for (Row row : data) {
            int rowIdx = row.getRowNum();
            if (rowIdx > headerRow) {
                Map<String, String> rowMapping = new HashMap<>();
                for (Cell cell : row) {
                    int colIdx = cell.getColumnIndex();
                    String colName = header.get(colIdx);
                    String value;
                    switch (cell.getCellType()) {
                        case NUMERIC:
                            value = new BigDecimal("" + cell.getNumericCellValue()).toPlainString();
                            break;
                        case BOOLEAN:
                            value = "" + cell.getBooleanCellValue();
                            break;
                        case FORMULA:
                            value = cell.getCellFormula();
                            break;
                        case STRING:
                        default:
                            value = cell.getRichStringCellValue().getString();
                            break;
                    }
                    rowMapping.put(colName, value);
                }
                if (!rowMapping.isEmpty()) {
                    allRows.add(rowMapping);
                }
            }
        }
        return allRows;
    }

    /**
     * Create a list of header names. The position of the name in the list is the column index of the header
     * in the spreadsheet itself.
     *
     * @param header row of header data
     * @return the list of header names, positional based on the column in which they appear in the spreadsheet
     */
    private List<String> getHeaders(Row header) {
        List<String> columns = new ArrayList<>();
        for (Cell cell : header) {
            String name = cell.getRichStringCellValue().getString();
            if (name != null) {
                if (name.contains(caseSensitiveDelimiter)) {
                    columns.add(name);
                } else if (name.length() > 0) {
                    columns.add(name.toUpperCase());
                }
            }
        }
        return columns;
    }
}
