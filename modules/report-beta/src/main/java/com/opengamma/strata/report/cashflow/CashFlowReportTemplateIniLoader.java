/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.cashflow;

import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.report.ReportTemplateIniLoader;

/**
 * Loads a cash flow report template from the standard INI file format.
 */
public class CashFlowReportTemplateIniLoader
    implements ReportTemplateIniLoader<CashFlowReportTemplate> {

  /**
   * The report type.
   */
  private static final String REPORT_TYPE = "cashflow";

  @Override
  public String getReportType() {
    return REPORT_TYPE;
  }

  @Override
  public CashFlowReportTemplate load(IniFile iniFile) {
    return new CashFlowReportTemplate();
  }

}
