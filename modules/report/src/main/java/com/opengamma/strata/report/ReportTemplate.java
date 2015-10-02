/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report;

import com.opengamma.strata.collect.io.IniFile;

/**
 * Marker interface for report templates.
 */
public interface ReportTemplate {

  /**
   * Loads a report template from an ini file.
   *
   * @param iniFile  the ini file containing the definition of a report template
   * @return the template defined in the ini file
   * @throws RuntimeException if the ini file cannot be parsed
   */
  public static ReportTemplate load(IniFile iniFile) {
    return MasterReportTemplateIniLoader.load(iniFile);
  }

}
