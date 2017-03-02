/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import java.util.Arrays;
import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * For a series of data $x_1, x_2, \dots, x_n$, the percentile is the value $x$
 * below which a certain percentage of the data fall. 
 */
public class PercentileCalculator implements Function<double[], Double> {

  private double _percentile;

  /**
   * @param percentile The percentile, must be between 0 and 1
   */
  public PercentileCalculator(double percentile) {
    ArgChecker.isTrue(percentile > 0 && percentile < 1, "Percentile must be between 0 and 1");
    _percentile = percentile;
  }

  /**
   * @param percentile The percentile, must be between 0 and 1
   */
  public void setPercentile(double percentile) {
    ArgChecker.isTrue(percentile > 0 && percentile < 1, "Percentile must be between 0 and 1");
    _percentile = percentile;
  }

  /**
   * @param x The data, not null or empty
   * @return The percentile
   */
  @Override
  public Double apply(double[] x) {
    ArgChecker.notNull(x, "x");
    ArgChecker.isTrue(x.length > 0, "x cannot be empty");
    int length = x.length;
    double[] copy = Arrays.copyOf(x, length);
    Arrays.sort(copy);
    double n = _percentile * (length - 1) + 1;
    if (Math.round(n) == 1) {
      return copy[0];
    }
    if (Math.round(n) == length) {
      return copy[length - 1];
    }
    double d = n % 1;
    int k = (int) Math.round(n - d);
    return copy[k - 1] + d * (copy[k] - copy[k - 1]);
  }

}
