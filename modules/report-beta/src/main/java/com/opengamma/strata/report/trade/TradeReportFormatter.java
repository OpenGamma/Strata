/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.trade;

import java.util.ArrayList;
import java.util.List;

import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.report.format.FormatCategory;
import com.opengamma.strata.report.format.FormatSettings;
import com.opengamma.strata.report.format.ReportFormatter;
import com.opengamma.strata.report.format.ReportOutputFormat;
import com.opengamma.strata.report.format.ValueFormatter;

/**
 * Formatter for trade reports.
 */
public class TradeReportFormatter extends ReportFormatter<TradeReport> {

  /**
   * The static instance.
   */
  public static TradeReportFormatter INSTANCE = new TradeReportFormatter();
  
  public TradeReportFormatter() {
    super(FormatSettings.of(FormatCategory.TEXT, ValueFormatter.unsupported()));
  }
  
  @Override
  protected List<Class<?>> getColumnTypes(TradeReport report) {
    List<Class<?>> columnTypes = new ArrayList<Class<?>>(report.getColumnHeaders().length);
    for (int c = 0; c < report.getColumnHeaders().length; c++) {
      Class<?> type = null;
      for (int r = 0; r < report.getData().length; r++) {
        Result<?> cell = report.getData()[r][c];
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
  
  @Override
  protected String formatData(TradeReport report, int rowIdx, int colIdx, ReportOutputFormat format) {
    TradeReportColumn templateColumn = report.getColumns().get(colIdx);
    Result<?> result = report.getData()[rowIdx][colIdx];
    if (result.isFailure()) {
      return templateColumn.isIgnoreFailures() ? "" : Messages.format("FAIL: {}", result.getFailure().getMessage());
    }
    Object value = result.getValue();
    return formatValue(value, format);
  }
  
}
