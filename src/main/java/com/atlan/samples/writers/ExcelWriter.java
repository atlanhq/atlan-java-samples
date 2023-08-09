/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2023 Atlan Pte. Ltd. */
package com.atlan.samples.writers;

import com.atlan.model.enums.AtlanEnum;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Utility class for creating and writing to Excel files, using Apache POI.
 */
public class ExcelWriter {

    private final SXSSFWorkbook workbook;
    private final CellStyle headerStyle;
    private final CellStyle dataStyle;
    private final CellStyle linkStyle;

    /**
     * Construct a new Excel file writer.
     *
     * @param batch how many records to write in-memory before flushing to disk
     * @throws IOException on any errors creating or accessing the file
     */
    public ExcelWriter(int batch) throws IOException {
        workbook = new SXSSFWorkbook(batch);
        headerStyle = createHeaderStyle();
        dataStyle = createDataStyle();
        linkStyle = createLinkStyle();
    }

    /**
     * Create a new worksheet within the workbook.
     *
     * @param name of the sheet to create
     * @return the worksheet
     */
    public Sheet createSheet(String name) {
        return workbook.createSheet(name);
    }

    /**
     * Flush out the contents of the workbook to a file, creating the XLSX file.
     *
     * @param fileLocation location of the Excel file to create
     * @throws IOException on any errors creating or accessing the file
     */
    public void create(String fileLocation) throws IOException {
        FileOutputStream fos = new FileOutputStream(fileLocation);
        workbook.write(fos);
        workbook.close();
        workbook.dispose(); // cleanup temporary files
        fos.close();
    }

    /**
     * Flush out the contents of the workbook to a ByteArrayOutputStream.
     *
     * @return the contents of the workbook as an output stream of bytes
     * @throws IOException on any errors creating or writing to the byte array output stream
     */
    public ByteArrayOutputStream asByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        workbook.dispose(); // cleanup temporary files
        return baos;
    }

    /**
     * Create styling for a header row.
     *
     * @return style
     */
    private CellStyle createHeaderStyle() {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setBorderBottom(BorderStyle.THICK);
        style.setBottomBorderColor(IndexedColors.BLACK1.getIndex());
        Font font = workbook.createFont();
        font.setFontName("Helvetica");
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * Create styling for a data row.
     *
     * @return style
     */
    private CellStyle createDataStyle() {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        Font font = workbook.createFont();
        font.setFontName("Helvetica");
        font.setBold(false);
        style.setFont(font);
        return style;
    }

    /**
     * Create styling for a link within a cell.
     *
     * @return style
     */
    private CellStyle createLinkStyle() {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        Font font = workbook.createFont();
        font.setFontName("Helvetica");
        font.setBold(false);
        font.setUnderline(Font.U_SINGLE);
        font.setColor(IndexedColors.BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    /**
     * Create a header row for the worksheet.
     *
     * @param worksheet in which to create the header row
     * @param headers ordered map of header names and descriptions
     */
    public void addHeader(Sheet worksheet, Map<String, String> headers) {
        Row header = worksheet.createRow(0);
        int colIdx = 0;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String name = entry.getKey();
            String desc = entry.getValue();
            addHeaderCell(header, colIdx, name, desc);
            worksheet.setColumnWidth(colIdx, Math.min(name.length() * 256, 255 * 256));
            colIdx++;
        }
    }

    /**
     * Add a row of data to the end of a worksheet.
     *
     * @param worksheet the worksheet into which to add the row
     * @param data the row of data to add
     */
    public void appendRow(Sheet worksheet, List<DataCell> data) {
        Row row = worksheet.createRow(worksheet.getLastRowNum() + 1);
        for (int i = 0; i < data.size(); i++) {
            DataCell datum = data.get(i);
            switch (datum.getType()) {
                case DOUBLE:
                    addDataCell(row, i, datum.getDecValue());
                    break;
                case LONG:
                    addDataCell(row, i, datum.getLongValue());
                    break;
                case BOOLEAN:
                    addDataCell(row, i, datum.getBoolValue());
                    break;
                case STRING:
                default:
                    addDataCell(row, i, datum.getStrValue());
                    break;
            }
        }
    }

    /**
     * Add a header cell to the worksheet.
     *
     * @param header row for the header
     * @param index location of the cell (column index)
     * @param name of the header column
     * @param description of the header column (will be put into a comment)
     */
    public void addHeaderCell(Row header, int index, String name, String description) {
        Drawing<?> drawing = header.getSheet().createDrawingPatriarch();
        CreationHelper factory = workbook.getCreationHelper();
        Cell cell = header.createCell(index, CellType.STRING);
        cell.setCellValue(name);
        cell.setCellStyle(headerStyle);
        ClientAnchor anchor = factory.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + 1);
        anchor.setRow1(cell.getRowIndex());
        anchor.setRow2(cell.getRowIndex() + 1);
        Comment comment = drawing.createCellComment(anchor);
        RichTextString str = factory.createRichTextString(description);
        comment.setVisible(false);
        comment.setString(str);
        cell.setCellComment(comment);
    }

    /**
     * Add a data cell with a string value.
     *
     * @param row in which to add the cell
     * @param index column for the cell
     * @param value to set for the cell
     */
    public void addDataCell(Row row, int index, String value) {
        Cell cell = row.createCell(index, CellType.STRING);
        if (value == null) {
            cell.setCellValue("");
            cell.setCellStyle(dataStyle);
        } else {
            // Maximum length of a cell in Excel is 32767 characters, so
            // if the value exceeds this truncate it and replace the ending
            // with ellipses
            if (value.length() > 32767) {
                cell.setCellValue(value.substring(0, 32764) + "...");
            } else {
                cell.setCellValue(value);
            }
            if (value.startsWith("https://")) {
                Hyperlink link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                link.setAddress(value);
                cell.setHyperlink(link);
                cell.setCellStyle(linkStyle);
            } else {
                cell.setCellStyle(dataStyle);
            }
        }
    }

    /**
     * Add a data cell with a boolean value.
     *
     * @param row in which to add the cell
     * @param index column for the cell
     * @param value to set for the cell
     */
    public void addDataCell(Row row, int index, boolean value) {
        Cell cell = row.createCell(index, CellType.BOOLEAN);
        cell.setCellValue(value);
        cell.setCellStyle(dataStyle);
    }

    /**
     * Add a data cell with a numeric value.
     *
     * @param row in which to add the cell
     * @param index column for the cell
     * @param value to set for the cell
     */
    public void addDataCell(Row row, int index, long value) {
        Cell cell = row.createCell(index, CellType.NUMERIC);
        cell.setCellValue((double) value);
        cell.setCellStyle(dataStyle);
    }

    /**
     * Add a data cell with a numeric value.
     *
     * @param row in which to add the cell
     * @param index column for the cell
     * @param value to set for the cell
     */
    public void addDataCell(Row row, int index, double value) {
        Cell cell = row.createCell(index, CellType.NUMERIC);
        cell.setCellValue(value);
        cell.setCellStyle(dataStyle);
    }

    @Getter
    public static final class DataCell {

        enum TYPE {
            DOUBLE,
            LONG,
            BOOLEAN,
            STRING
        }

        private final TYPE type;
        private final Double decValue;
        private final Long longValue;
        private final Boolean boolValue;
        private final String strValue;

        private DataCell(TYPE type, Double d, Long l, Boolean b, String s) {
            this.type = type;
            this.decValue = d;
            this.longValue = l;
            this.boolValue = b;
            this.strValue = s;
        }

        public static DataCell of(double value) {
            return new DataCell(TYPE.DOUBLE, value, null, null, null);
        }

        public static DataCell of(long value) {
            return new DataCell(TYPE.LONG, null, value, null, null);
        }

        public static DataCell of(boolean value) {
            return new DataCell(TYPE.BOOLEAN, null, null, value, null);
        }

        public static DataCell of(String value) {
            return new DataCell(TYPE.STRING, null, null, null, value == null ? "" : value);
        }

        public static DataCell of(AtlanEnum value) {
            return new DataCell(TYPE.STRING, null, null, null, value == null ? "" : value.getValue());
        }
    }
}
