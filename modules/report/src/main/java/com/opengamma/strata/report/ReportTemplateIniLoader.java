/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report;

import com.opengamma.strata.collect.io.IniFile;

/**
 * Loads a report template from an ini-based file format.
 * 
 * @param <T>  the report template type
 */
public interface ReportTemplateIniLoader<T extends ReportTemplate> {

  /** The settings section name. */
  public static final String SETTINGS_SECTION = "settings";

  /** The report type property name, in the settings section. */
  public static final String SETTINGS_REPORT_TYPE = "reportType";

  /**
   * Gets the type of report handled by this loader.
   * 
   * @return the type of report handled by this loader
   */
  public abstract String getReportType();

  /**
   * Loads the report template.
   * 
   * @param iniFile  the ini file to load
   * @return the loaded report template object
   */
  public abstract T load(IniFile iniFile);

}
