/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.bethecoder.table.ASCIITableHeader;
import com.bethecoder.table.AsciiTableInstance;
import com.bethecoder.table.spec.AsciiTable;
import com.opengamma.strata.collect.Unchecked;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.report.Report;

/**
 * Common base class for formatting reports into ASCII tables or CSV format.
 * 
 * @param <R>  the report type
 */
public abstract class ReportFormatter<R extends Report> {

  /**
   * The default format settings, used if there are no settings for a data type.
   */
  private final FormatSettings<Object> defaultSettings;
  /**
   * The format settings provider.
   */
  private final FormatSettingsProvider formatSettingsProvider = FormatSettingsProvider.INSTANCE;

  /**
   * Creates a new formatter with a set of default format settings.
   *
   * @param defaultSettings  default format settings, used if there are no settings for a data type.
   */
  protected ReportFormatter(FormatSettings<Object> defaultSettings) {
    this.defaultSettings = defaultSettings;
  }

  //-------------------------------------------------------------------------
  /**
   * Outputs the report table in CSV format.
   * 
   * @param report  the report
   * @param out  the output stream to write to
   */
  @SuppressWarnings("resource")
  public void writeCsv(R report, OutputStream out) {
    OutputStreamWriter outputWriter = new OutputStreamWriter(out);
    CsvOutput csvOut = new CsvOutput(outputWriter);
    csvOut.writeLine(report.getColumnHeaders());
    IntStream.range(0, report.getRowCount())
        .mapToObj(rowIdx -> Arrays.asList(formatRow(report, rowIdx, ReportOutputFormat.CSV)))
        .forEach(csvOut::writeLine);
    Unchecked.wrap(outputWriter::flush);
  }

  /**
   * Outputs the report as an ASCII table.
   * 
   * @param report  the report
   * @param out  the output stream to write to
   */
  public void writeAsciiTable(R report, OutputStream out) {
    List<Class<?>> columnTypes = getColumnTypes(report);
    ASCIITableHeader[] headers = IntStream.range(0, columnTypes.size())
        .mapToObj(i -> toAsciiTableHeader(report.getColumnHeaders().get(i), columnTypes.get(i)))
        .toArray(ASCIITableHeader[]::new);
    String[][] table = formatTable(report, ReportOutputFormat.ASCII_TABLE);
    String asciiTable = AsciiTableInstance.get().getTable(headers, table);
    PrintWriter pw = new PrintWriter(out);
    pw.println(asciiTable);
    pw.flush();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the type of the data in each report column.
   * <p>
   * If every value in a column is a failure the type will be {@code Object.class}.
   * 
   * @param report  the report
   * @return a list of column types
   */
  protected abstract List<Class<?>> getColumnTypes(R report);

  /**
   * Formats a piece of data for display.
   * 
   * @param report the report containing the data
   * @param rowIdx  the row index of the data
   * @param colIdx  the column index of the data
   * @param format  the report output format
   * @return the formatted data
   */
  protected abstract String formatData(R report, int rowIdx, int colIdx, ReportOutputFormat format);

  //-------------------------------------------------------------------------
  /**
   * Formats a value into a string.
   *
   * @param value  the value
   * @param format  the format that controls how the value is formatted
   * @return the formatted value
   */
  @SuppressWarnings("unchecked")
  protected String formatValue(Object value, ReportOutputFormat format) {
    Object formatValue = value instanceof Optional ? ((Optional<?>) value).orElse(null) : value;

    if (formatValue == null) {
      return "";
    }
    FormatSettings<Object> formatSettings = formatSettingsProvider.settings(formatValue.getClass(), defaultSettings);
    ValueFormatter<Object> formatter = formatSettings.getFormatter();

    return format == ReportOutputFormat.CSV ?
        formatter.formatForCsv(formatValue) :
        formatter.formatForDisplay(formatValue);
  }

  //-------------------------------------------------------------------------
  private ASCIITableHeader toAsciiTableHeader(String header, Class<?> columnType) {
    FormatSettings<Object> formatSettings = formatSettingsProvider.settings(columnType, defaultSettings);
    boolean isNumeric =
        formatSettings.getCategory() == FormatCategory.NUMERIC ||
            formatSettings.getCategory() == FormatCategory.DATE;
    int align = isNumeric ? AsciiTable.ALIGN_RIGHT : AsciiTable.ALIGN_LEFT;
    return ASCIITableHeader.h(header, align, align);
  }

  private String[][] formatTable(R report, ReportOutputFormat format) {
    String[][] table = new String[report.getRowCount()][];

    for (int r = 0; r < table.length; r++) {
      table[r] = formatRow(report, r, format);
    }
    return table;
  }

  private String[] formatRow(R report, int rowIdx, ReportOutputFormat format) {
    String[] tableRow = new String[report.getColumnCount()];
    for (int colIdx = 0; colIdx < report.getColumnCount(); colIdx++) {
      tableRow[colIdx] = formatData(report, rowIdx, colIdx, format);
    }
    return tableRow;
  }

}
