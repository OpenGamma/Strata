/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.cashflow;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.opengamma.strata.report.format.FormatCategory;
import com.opengamma.strata.report.format.FormatSettings;
import com.opengamma.strata.report.format.ReportFormatter;
import com.opengamma.strata.report.format.ReportOutputFormat;
import com.opengamma.strata.report.format.ValueFormatter;

/**
 * Formatter for cash flow reports.
 */
public class CashFlowReportFormatter extends ReportFormatter<CashFlowReport> {

  /**
   * The static instance.
   */
  public static CashFlowReportFormatter INSTANCE = new CashFlowReportFormatter();
  
  
  public CashFlowReportFormatter() {
    super(FormatSettings.of(FormatCategory.TEXT, ValueFormatter.defaultToString()));
  }
  
  @Override
  protected List<Class<?>> getColumnTypes(CashFlowReport report) {
    return IntStream.range(0, report.getColumnCount())
        .mapToObj(i -> Object.class)
        .collect(Collectors.toList());
  }

  @Override
  protected String formatData(CashFlowReport report, int rowIdx, int colIdx, ReportOutputFormat format) {
    Object value = report.getData()[rowIdx][colIdx];
    return formatValue(value, format);
  }

}
