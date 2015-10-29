/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Implementation of a quantile estimator.
 * <p>
 * The estimation is one of the sorted sample data.
 * <p> 
 * Reference: Value-At-Risk, OpenGamma Documentation 31, Version 0.1, April 2015.
 */
public abstract class DiscreteQuantileMethod
    extends QuantileCalculationMethod {

  @Override
  public double quantileFromSorted(double level, DoubleArray sortedSample) {
    ArgChecker.isTrue(level > 0, "Quantile should be above 0.");
    ArgChecker.isTrue(level < 1, "Quantile should be below 1.");
    int sampleSize = sortedSample.size();
    int index = index(level * sampleSize);
    return sortedSample.get(index - 1);
  }

  /**
   * Internal method computing the index for a give quantile multiply by sample size.
   * <p>
   * The quantile size is given by quantile * sample size.
   * 
   * @param quantileSize  the quantile size
   * @return the index in the sample
   */
  abstract int index(double quantileSize);

}
