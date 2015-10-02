/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.cashflow;

import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.report.ReportTemplate;

/**
 * Marker for a cash flow report template.
 * <p>
 * Cash flow reports are currently parameterless so this class contains no fields.
 */
public class CashFlowReportTemplate
    implements ReportTemplate {

  /**
   * Creates a trade report template by reading a template definition in an ini file.
   *
   * @param ini  the ini file containing the definition of the template
   * @return a trade report template built from the definition in the ini file
   */
  public static CashFlowReportTemplate load(IniFile ini) {
    CashFlowReportTemplateIniLoader loader = new CashFlowReportTemplateIniLoader();
    return loader.load(ini);
  }

}
