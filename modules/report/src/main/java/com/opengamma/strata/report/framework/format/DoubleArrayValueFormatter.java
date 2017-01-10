/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;

/**
 * Formats primitive double arrays.
 */
final class DoubleArrayValueFormatter
    implements ValueFormatter<double[]> {

  /**
   * The single shared instance of this formatter.
   */
  static final DoubleArrayValueFormatter INSTANCE = new DoubleArrayValueFormatter();

  // restricted constructor
  private DoubleArrayValueFormatter() {
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the array delimited by spaces and surrounded with square brackets.
   * <pre>
   *   new double[]{1, 2, 3} -> "[1.0 2.0 3.0]"
   * </pre>
   *
   * @param array  an array
   * @return the array formatted for inclusion in a CSV file - space delimited and surrounded with square brackets
   */
  @Override
  public String formatForCsv(double[] array) {
    return Arrays.stream(array).mapToObj(Double::toString).collect(joining(" ", "[", "]"));
  }

  /**
   * Returns the array delimited by commas and spaces and surrounded with square brackets.
   * <pre>
   *   new double[]{1, 2, 3} -> "[1.0, 2.0, 3.0]"
   * </pre>
   *
   * @param array  an array
   * @return the array formatted for display - comma delimited and surrounded with square brackets
   */
  @Override
  public String formatForDisplay(double[] array) {
    return Arrays.toString(array);
  }
}
