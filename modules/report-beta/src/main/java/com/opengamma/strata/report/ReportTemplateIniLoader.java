/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report;

import com.opengamma.strata.collect.io.IniFile;


/**
 * Loads a report template from an ini-based file format.
 */
public interface ReportTemplateIniLoader<T extends ReportTemplate> {

  /** The settings section name. */
  public static final String SETTINGS_SECTION = "settings";
  
  /**
   * Loads the report template.
   * 
   * @param iniFile  the ini file to load
   * @return the loaded report template object
   */
  T load(IniFile iniFile);
  
}
