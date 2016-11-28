/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

/**
 * Default formatter which returns the value of {@code toString()} on the object.
 */
final class ToStringValueFormatter
    implements ValueFormatter<Object> {

  /**
   * The single shared instance of this formatter.
   */
  static final ToStringValueFormatter INSTANCE = new ToStringValueFormatter();

  // restricted constructor
  private ToStringValueFormatter() {
  }

  //-------------------------------------------------------------------------
  @Override
  public String formatForCsv(Object object) {
    return object.toString();
  }

  @Override
  public String formatForDisplay(Object object) {
    return object.toString();
  }

}
