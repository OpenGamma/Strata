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
 * The quantile is linearly interpolated between two sample values. The probability dimension <i>p<subscript>i</subscript> 
 * on which the interpolation take place (X axis) varies between actual implementation of the abstract class. 
 * For each probability <i>p<subscript>i</subscript></i>, the cumulative distribution value is the sample value with 
 * same index. The index used above are the Java index plus 1.
 * <p>
 * Reference: Value-At-Risk, OpenGamma Documentation 31, Version 0.1, April 2015.
 */
public abstract class InterpolationQuantileMethod
    extends QuantileCalculationMethod {

  @Override
  protected double quantile(double level, DoubleArray sortedSample, boolean isExtrapolated) {
    ArgChecker.isTrue(level > 0, "Quantile should be above 0.");
    ArgChecker.isTrue(level < 1, "Quantile should be below 1.");
    int sampleSize = sortedSample.size();
    double adjustedLevel =
        checkIndex(level * sampleCorrection(sampleSize) + indexCorrection(), sortedSample.size(), isExtrapolated);
    int lowerIndex = (int) Math.floor(adjustedLevel);
    int upperIndex = (int) Math.ceil(adjustedLevel);
    double lowerWeight = upperIndex - adjustedLevel;
    double upperWeight = 1d - lowerWeight;
    return lowerWeight * sortedSample.get(lowerIndex - 1) + upperWeight * sortedSample.get(upperIndex - 1);
  }

  @Override
  protected double expectedShortfall(double level, DoubleArray sortedSample, boolean isExtrapolated) {
    ArgChecker.isTrue(level > 0, "Quantile should be above 0.");
    ArgChecker.isTrue(level < 1, "Quantile should be below 1.");
    int sampleSize = sortedSample.size();
    double adjustedLevel =
        checkIndex(level * sampleCorrection(sampleSize) + indexCorrection(), sortedSample.size(), isExtrapolated);
    int lowerIndex = (int) Math.floor(adjustedLevel);
    int upperIndex = (int) Math.ceil(adjustedLevel);
    double losses = 0d;
    for (int i = 0; i < lowerIndex; i++) {
      losses += sortedSample.get(i);
    }
    if (lowerIndex != upperIndex) {
      double upperWeight = adjustedLevel - lowerIndex;
      losses += upperWeight * sortedSample.get(upperIndex - 1);
    }
    return losses / adjustedLevel;
  }

  //-------------------------------------------------------------------------
  /**
   * Internal method returning the index correction for the specific implementation.
   * 
   * @return the correction
   */
  abstract double indexCorrection();

  /**
   * Internal method returning the sample size correction for the specific implementation.
   * 
   * @param sampleSize  the sample size
   * @return the correction
   */
  abstract int sampleCorrection(int sampleSize);

}
