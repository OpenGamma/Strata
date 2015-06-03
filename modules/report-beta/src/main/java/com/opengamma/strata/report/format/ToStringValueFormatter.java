/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.format;

/**
 * Default formatter which returns the value of {@code toString()} on the object.
 */
public class ToStringValueFormatter implements ValueFormatter<Object> {

  /** Singleton instance. */
  public static final ToStringValueFormatter INSTANCE = new ToStringValueFormatter();

  @Override
  public String formatForCsv(Object object) {
    return object.toString();
  }

  @Override
  public String formatForDisplay(Object object) {
    return object.toString();
  }

}
