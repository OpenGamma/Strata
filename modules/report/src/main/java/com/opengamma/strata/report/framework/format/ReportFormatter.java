/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
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
    OutputStreamWriter outputWriter = new OutputStreamWriter(out, StandardCharsets.UTF_8);
    CsvOutput csvOut = new CsvOutput(outputWriter);
    csvOut.writeLine(report.getColumnHeaders());
    IntStream.range(0, report.getRowCount())
        .mapToObj(rowIdx -> formatRow(report, rowIdx, ReportOutputFormat.CSV))
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
    List<AsciiTableAlignment> alignments = IntStream.range(0, columnTypes.size())
        .mapToObj(i -> calculateAlignment(columnTypes.get(i)))
        .collect(toImmutableList());
    List<String> headers = report.getColumnHeaders();
    ImmutableList<ImmutableList<String>> cells = formatAsciiTable(report);
    String asciiTable = AsciiTable.generate(alignments, headers, cells);
    PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
    pw.println(asciiTable);
    pw.flush();
  }

  // calculates the alignment to use
  private AsciiTableAlignment calculateAlignment(Class<?> columnType) {
    FormatSettings<Object> formatSettings = formatSettingsProvider.settings(columnType, defaultSettings);
    boolean isNumeric =
        formatSettings.getCategory() == FormatCategory.NUMERIC || formatSettings.getCategory() == FormatCategory.DATE;
    return isNumeric ? AsciiTableAlignment.RIGHT : AsciiTableAlignment.LEFT;
  }

  // formats the ASCII table
  private ImmutableList<ImmutableList<String>> formatAsciiTable(R report) {
    ImmutableList.Builder<ImmutableList<String>> table = ImmutableList.builder();
    for (int rowIdx = 0; rowIdx < report.getRowCount(); rowIdx++) {
      table.add(formatRow(report, rowIdx, ReportOutputFormat.ASCII_TABLE));
    }
    return table.build();
  }

  // formats a single row
  private ImmutableList<String> formatRow(R report, int rowIdx, ReportOutputFormat format) {
    ImmutableList.Builder<String> tableRow = ImmutableList.builder();
    for (int colIdx = 0; colIdx < report.getColumnCount(); colIdx++) {
      tableRow.add(formatData(report, rowIdx, colIdx, format));
    }
    return tableRow.build();
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

}
