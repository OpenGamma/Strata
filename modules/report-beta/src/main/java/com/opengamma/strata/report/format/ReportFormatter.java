/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.format;

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

  private final FormatSettings fallbackSettings;
  private final FormatSettingsProvider formatSettingsProvider = new FormatSettingsProvider();

  public ReportFormatter(FormatSettings fallbackSettings) {
    this.fallbackSettings = fallbackSettings;
  }

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
    csvOut.writeLine(Arrays.asList(report.getColumnHeaders()));
    IntStream.range(0, report.getRowCount())
        .mapToObj(rowIdx -> Arrays.asList(formatRow(report, rowIdx, ReportOutputFormat.CSV)))
        .forEachOrdered(csvOut::writeLine);
    Unchecked.wrap(() -> outputWriter.flush());
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
        .mapToObj(i -> toAsciiTableHeader(report.getColumnHeaders()[i], columnTypes.get(i)))
        .toArray(i -> new ASCIITableHeader[i]);
    String[][] table = formatTable(report, ReportOutputFormat.ASCII_TABLE);
    String asciiTable = AsciiTableInstance.get().getTable(headers, table);
    PrintWriter pw = new PrintWriter(out);
    pw.println(asciiTable);
    pw.flush();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the type of the data in each report column.
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
  @SuppressWarnings("unchecked")
  protected String formatValue(Object value, ReportOutputFormat format) {
    Object formatValue = value instanceof Optional ? ((Optional<?>) value).orElse(null) : value;
    if (formatValue == null) {
      return "";
    }
    FormatSettings formatSettings = formatSettingsProvider.getSettings(formatValue.getClass(), fallbackSettings);
    ValueFormatter<Object> formatter = (ValueFormatter<Object>) formatSettings.getFormatter();
    if (format == ReportOutputFormat.CSV) {
      return formatter.formatForCsv(formatValue);
    } else {
      return formatter.formatForDisplay(formatValue);
    }
  }

  //-------------------------------------------------------------------------
  private ASCIITableHeader toAsciiTableHeader(String header, Class<?> columnType) {
    FormatSettings formatSettings = formatSettingsProvider.getSettings(columnType, fallbackSettings);
    boolean isNumeric = formatSettings.getCategory() == FormatCategory.NUMERIC ||
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
