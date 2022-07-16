/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.report.trade.TradeReportTemplate;

import java.util.Locale;

/**
 * Contains utilities for working with data in the examples environment.
 */
public final class CorporateActionExampleData {

  /**
   * Restricted constructor.
   */
  private CorporateActionExampleData() {
  }


  /**
   * Loads a trade report template from the standard INI format.
   * 
   * @param templateName  the name of the template
   * @return the loaded report template
   */
  public static TradeReportTemplate loadCorporateActionReportTemplate(String templateName) {
    String resourceName = String.format(Locale.ENGLISH, "classpath:example-reports/%s.ini", templateName);
    ResourceLocator resourceLocator = ResourceLocator.of(resourceName);
    IniFile ini = IniFile.of(resourceLocator.getCharSource());
    return TradeReportTemplate.load(ini);
  }

}
