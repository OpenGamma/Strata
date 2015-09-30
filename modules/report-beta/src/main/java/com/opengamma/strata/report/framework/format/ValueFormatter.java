/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

/**
 * Formats a value into a string.
 * 
 * @param <T>  the type of the value
 */
public interface ValueFormatter<T> {

  /**
   * Formats a value for use in a CSV file.
   * <p>
   * Typically this retains all information from the object and keeps the representation compact.
   * 
   * @param object  the object to format.
   * @return the object formatted into a string
   */
  String formatForCsv(T object);

  /**
   * Formats a value for display.
   * <p>
   * Typically this may add characters intended to make the value easier to read, or perform
   * rounding on numeric values.
   * 
   * @param object  the object to format
   * @return the object formatted into a string
   */
  String formatForDisplay(T object);

  //-------------------------------------------------------------------------
  /**
   * Gets a formatter which returns the value of the object's {@code toString()} method.
   * 
   * @return the formatter
   */
  public static ValueFormatter<Object> defaultToString() {
    return ToStringValueFormatter.INSTANCE;
  }

  /**
   * Gets a formatter to be used when no specific formatter exists for the object.
   * 
   * @return the formatter
   */
  public static ValueFormatter<Object> unsupported() {
    return UnsupportedValueFormatter.INSTANCE;
  }

}
