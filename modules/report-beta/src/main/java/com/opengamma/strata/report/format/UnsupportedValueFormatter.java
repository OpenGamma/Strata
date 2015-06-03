/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.format;

import com.opengamma.strata.collect.Messages;

/**
 * Catch-all formatter that simply outputs the type of the value in angular brackets,
 * e.g. {@literal <MyCustomType>} 
 */
public class UnsupportedValueFormatter implements ValueFormatter<Object> {

  /** Singleton instance. */
  public static final UnsupportedValueFormatter INSTANCE = new UnsupportedValueFormatter();

  @Override
  public String formatForCsv(Object object) {
    return Messages.format("<{}>", object.getClass().getSimpleName());
  }

  @Override
  public String formatForDisplay(Object object) {
    return Messages.format("<{}>", object.getClass().getSimpleName());
  }

}
