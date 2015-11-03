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
  protected double quantile(double level, DoubleArray sortedSample, boolean isExtrapolated) {
    ArgChecker.isTrue(level > 0, "Quantile should be above 0.");
    ArgChecker.isTrue(level < 1, "Quantile should be below 1.");
    int sampleSize = sampleCorrection(sortedSample.size());
    int index = (int) checkIndex(index(level * sampleSize), sortedSample.size(), isExtrapolated);
    return sortedSample.get(index - 1);
  }

  @Override
  protected double expectedShortfall(double level, DoubleArray sortedSample, boolean isExtrapolated) {
    ArgChecker.isTrue(level > 0, "Quantile should be above 0.");
    ArgChecker.isTrue(level < 1, "Quantile should be below 1.");
    int sampleSize = sampleCorrection(sortedSample.size());
    int index = (int) checkIndex(index(level * sampleSize), sortedSample.size(), isExtrapolated);
    double losses = 0d;
    for (int i = 0; i < index; i++) {
      losses += sortedSample.get(i);
    }
    return losses / (double) index;
  }

  //-------------------------------------------------------------------------
  /**
   * Internal method computing the index for a give quantile multiply by sample size.
   * <p>
   * The quantile size is given by quantile * sample size.
   * 
   * @param quantileSize  the quantile size
   * @return the index in the sample
   */
  abstract int index(double quantileSize);

  /**
   * Internal method returning the sample size correction for the specific implementation.
   * 
   * @param sampleSize  the sample size
   * @return the correction
   */
  abstract int sampleCorrection(int sampleSize);
}
