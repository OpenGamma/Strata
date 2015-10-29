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
  public double quantileFromSorted(double level, DoubleArray sortedSample) {
    ArgChecker.isTrue(level > 0, "Quantile should be above 0.");
    ArgChecker.isTrue(level < 1, "Quantile should be below 1.");
    int sampleSize = sortedSample.size();
    double adjustedLevel = level * sampleCorrection(sampleSize) + indexCorrection();
    int lowerIndex = (int) Math.floor(adjustedLevel);
    ArgChecker.isTrue(lowerIndex >= 1, "Quantile can not be computed below the lowest probability level.");
    int upperIndex = (int) Math.ceil(adjustedLevel);
    ArgChecker.isTrue(
        upperIndex <= sortedSample.size(), "Quantile can not be computed above the highest probability level.");
    double lowerWeight = upperIndex - adjustedLevel;
    double upperWeight = 1d - lowerWeight;
    return lowerWeight * sortedSample.get(lowerIndex - 1) + upperWeight * sortedSample.get(upperIndex - 1);
  }

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
