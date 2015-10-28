/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Abstract method to estimate quantiles from sample observations.
 */
public abstract class QuantileCalculationMethod {

  /**
   * Compute the quantile estimation.
   * <p>
   * The quantile level is in decimal, i.e. 99% = 0.99 and 0 < quantile < 1 should be satisfied.
   * The sample obsrvations are sorted from the smallest to the largest. 
   * 
   * @param level  the quantile level
   * @param sortedSample  the sample observations
   * @return the quantile estimation
   */
  abstract double quantileFromSorted(double level, DoubleArray sortedSample);

  /**
   * Compute the quantile estimation.
   * <p>
   * The quantile level is in decimal, i.e. 99% = 0.99 and 0 < quantile < 1 should be satisfied.
   * The sample observations are supposed to be unsorted, the first step is to sort the data.
   * 
   * @param level  the quantile level
   * @param sample  the sample observations
   * @return The quantile estimation
   */
  public double quantileFromUnsorted(double level, DoubleArray sample) {
    return quantileFromSorted(level, sample.sorted());
  }

}
