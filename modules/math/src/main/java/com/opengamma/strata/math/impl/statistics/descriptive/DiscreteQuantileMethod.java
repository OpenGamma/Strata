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
  protected double expectedShortfall(double level, DoubleArray sortedSample) {
    ArgChecker.isTrue(level > 0, "Quantile should be above 0.");
    ArgChecker.isTrue(level < 1, "Quantile should be below 1.");
    int sampleSize = sampleCorrection(sortedSample.size());
    double fractionalIndex = level * sampleSize;
    int index = (int) checkIndex(index(level * sampleSize), sortedSample.size(), true);
    double interval = 1d / (double) sampleSize;
    double losses = sortedSample.get(0) * interval * indexShift();
    for (int i = 0; i < index - 1; i++) {
      losses += sortedSample.get(i) * interval;
    }
    losses += sortedSample.get(index - 1) * (fractionalIndex - index + 1 - indexShift()) * interval;
    return losses / level;
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

  /**
   * Shift added to/subtracted from index during intermediate steps in the expected shortfall computation. 
   * 
   * @return the index shift
   */
  abstract double indexShift();
}
