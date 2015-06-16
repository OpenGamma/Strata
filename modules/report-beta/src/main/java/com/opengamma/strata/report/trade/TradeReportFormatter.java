/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.trade;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.bethecoder.table.ASCIITableHeader;
import com.bethecoder.table.AsciiTableInstance;
import com.bethecoder.table.spec.AsciiTable;
import com.opencsv.CSVWriter;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.report.format.FormatCategory;
import com.opengamma.strata.report.format.FormatSettings;
import com.opengamma.strata.report.format.FormatSettingsProvider;
import com.opengamma.strata.report.format.ValueFormatter;

/**
 * Contains the logic required to format a trade report into CSV or an ASCII table.
 */
public class TradeReportFormatter {

  private final FormatSettingsProvider formatSettingsProvider = new FormatSettingsProvider();

  @SuppressWarnings("resource")
  public void writeCsv(TradeReport report, OutputStream out) {
    OutputStreamWriter outputWriter = new OutputStreamWriter(out);
    CSVWriter writer = new CSVWriter(outputWriter);
    writer.writeNext(report.getColumnHeaders());
    for (int r = 0; r < report.getResults().length; r++) {
      writer.writeNext(formatRow(report.getResults()[r], report.getColumns(), true));
    }
    try {
      writer.flush();
    } catch (IOException ex) {
      // do nothing
    }
  }

  public void writeAsciiTable(TradeReport report, OutputStream out) {
    List<Class<?>> columnTypes = getColumnTypes(report);
    ASCIITableHeader[] headers = IntStream.range(0, columnTypes.size())
        .mapToObj(i -> toAsciiTableHeader(report.getColumnHeaders()[i], columnTypes.get(i)))
        .toArray(i -> new ASCIITableHeader[i]);
    String[][] table = getFormattedTable(report.getResults(), report.getColumns(), false);
    String asciiTable = AsciiTableInstance.get().getTable(headers, table);
    PrintWriter pw = new PrintWriter(out);
    pw.println(asciiTable);
    pw.flush();
  }

  //-------------------------------------------------------------------------
  private List<Class<?>> getColumnTypes(TradeReport report) {
    List<Class<?>> columnTypes = new ArrayList<Class<?>>(report.getColumnHeaders().length);
    for (int c = 0; c < report.getColumnHeaders().length; c++) {
      Class<?> type = null;
      for (int r = 0; r < report.getResults().length; r++) {
        Result<?> cell = report.getResults()[r][c];
        if (cell.isFailure()) {
          continue;
        }
        type = cell.getValue().getClass();
        break;
      }
      columnTypes.add(type);
    }
    return columnTypes;
  }

  private ASCIITableHeader toAsciiTableHeader(String header, Class<?> columnType) {
    FormatSettings formatSettings = formatSettingsProvider.getSettings(columnType);
    boolean isNumeric = formatSettings.getCategory() == FormatCategory.NUMERIC ||
        formatSettings.getCategory() == FormatCategory.DATE;
    int align = isNumeric ? AsciiTable.ALIGN_RIGHT : AsciiTable.ALIGN_LEFT;
    return ASCIITableHeader.h(header, align, align);
  }

  private String[][] getFormattedTable(Result<?>[][] results, List<TradeReportColumn> templateColumns, boolean forCsv) {
    String[][] table = new String[results.length][];
    for (int r = 0; r < results.length; r++) {
      table[r] = formatRow(results[r], templateColumns, forCsv);
    }
    return table;
  }

  private String[] formatRow(Result<?>[] resultsRow, List<TradeReportColumn> templateColumns, boolean forCsv) {
    int resultColumnCount = resultsRow.length;
    String[] tableRow = new String[resultColumnCount];
    for (int c = 0; c < resultColumnCount; c++) {
      Result<?> result = resultsRow[c];
      TradeReportColumn templateColumn = templateColumns.get(c);
      tableRow[c] = formatResult(result, templateColumn, forCsv);
    }
    return tableRow;
  }

  @SuppressWarnings("unchecked")
  private String formatResult(Result<?> result, TradeReportColumn templateColumn, boolean forCsv) {
    if (result.isFailure()) {
      return templateColumn.isIgnoreFailures() ? "" : Messages.format("FAIL: {}", result.getFailure().getMessage());
    }
    Object value = result.getValue();
    if (value == null) {
      return "";
    }
    FormatSettings formatSettings = formatSettingsProvider.getSettings(value.getClass());
    ValueFormatter<Object> formatter = (ValueFormatter<Object>) formatSettings.getFormatter();
    return forCsv ? formatter.formatForCsv(value) : formatter.formatForDisplay(value);
  }

}
