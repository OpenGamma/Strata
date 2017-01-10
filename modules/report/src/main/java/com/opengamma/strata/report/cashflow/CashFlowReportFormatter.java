/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.cashflow;

import java.util.Collections;
import java.util.List;

import com.opengamma.strata.report.framework.format.FormatCategory;
import com.opengamma.strata.report.framework.format.FormatSettings;
import com.opengamma.strata.report.framework.format.ReportFormatter;
import com.opengamma.strata.report.framework.format.ReportOutputFormat;
import com.opengamma.strata.report.framework.format.ValueFormatters;

/**
 * Formatter for cash flow reports.
 */
public final class CashFlowReportFormatter
    extends ReportFormatter<CashFlowReport> {

  /**
   * The single shared instance of this report formatter.
   */
  public static final CashFlowReportFormatter INSTANCE = new CashFlowReportFormatter();

  // restricted constructor
  private CashFlowReportFormatter() {
    super(FormatSettings.of(FormatCategory.TEXT, ValueFormatters.TO_STRING));
  }

  //-------------------------------------------------------------------------
  @Override
  protected List<Class<?>> getColumnTypes(CashFlowReport report) {
    return Collections.nCopies(report.getColumnCount(), Object.class);
  }

  @Override
  protected String formatData(CashFlowReport report, int rowIdx, int colIdx, ReportOutputFormat format) {
    Object value = report.getData().get(rowIdx, colIdx);
    return formatValue(value, format);
  }

}
