/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.data;

import java.io.IOException;
import java.io.Reader;

import org.joda.beans.ser.JodaBeanSer;

import com.google.common.io.CharSource;
import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Contains utilities for working with data in the examples environment.
 */
public final class ExampleData {

  /**
   * Restricted constructor.
   */
  private ExampleData() {
  }

  //-------------------------------------------------------------------------
  /**
   * Loads a golden copy of expected results from a text file.
   * 
   * @param name  the name of the results
   * @return the loaded results
   */
  public static String loadExpectedResults(String name) {
    String classpathResourceName = String.format("classpath:goldencopy/%s.txt", name);
    ResourceLocator resourceLocator = ResourceLocator.of(classpathResourceName);
    try {
      return resourceLocator.getCharSource().read().trim();
    } catch (IOException ex) {
      throw new MissingExampleDataException(name);
    }
  }

  /**
   * Loads an bean from a JSON representation.
   * 
   * @param resourceName  the name of the JSON resource
   * @param clazz  the expected type
   * @return the loaded bean
   */
  public static <T> T loadFromJson(String resourceName, Class<T> clazz) {
    String classpathResourceName = String.format("classpath:%s", resourceName);
    ResourceLocator resourceLocator = ResourceLocator.of(classpathResourceName);
    CharSource charSource = resourceLocator.getCharSource();
    try (Reader reader = charSource.openBufferedStream()) {
      return JodaBeanSer.COMPACT.jsonReader().read(reader, clazz);
    } catch (IOException e) {
      throw new MissingExampleDataException(resourceName);
    }
  }
  
  /**
   * Loads a trade report template from the standard INI format.
   * 
   * @param templateName  the name of the template
   * @return the loaded report template
   */
  public static TradeReportTemplate loadTradeReportTemplate(String templateName) {
    String resourceName = String.format("classpath:reports/%s.ini", templateName);
    ResourceLocator resourceLocator = ResourceLocator.of(resourceName);
    IniFile ini = IniFile.of(resourceLocator.getCharSource());
    return TradeReportTemplate.ofIni(ini);
  }

}
