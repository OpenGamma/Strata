/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Implementation of a quantile estimator.
 * <p>
 * The quantile is linearly interpolated between two sample values. The probability dimension
 * <i>p<subscript>i</subscript>
 * on which the interpolation take place (X axis) varies between actual implementation of the abstract class.
 * For each probability <i>p<subscript>i</subscript></i>, the cumulative distribution value is the sample value with
 * same index. The index used above are the Java index plus 1.
 * <p>
 * Reference: Value-At-Risk, OpenGamma Documentation 31, Version 0.1, April 2015.
 */
public abstract class InterpolationQuantileMethod
    extends QuantileCalculationMethod {

  @Override
  protected QuantileResult quantile(double level, DoubleArray sample, boolean isExtrapolated) {
    ArgChecker.isTrue(level > 0, "Quantile should be above 0.");
    ArgChecker.isTrue(level < 1, "Quantile should be below 1.");
    int sampleSize = sampleCorrection(sample.size());
    double adjustedLevel =
        checkIndex(level * sampleCorrection(sampleSize) + indexCorrection(), sample.size(), isExtrapolated);
    double[] order = createIndexArray(sampleSize);
    double[] s = sample.toArray();
    DoubleArrayMath.sortPairs(s, order);
    int[] indices = new int[2];
    indices[0] = (int) Math.floor(adjustedLevel);
    indices[1] = (int) Math.ceil(adjustedLevel);
    DoubleArray weights = DoubleArray.of(indices[1] - adjustedLevel, 1d - indices[0]);
    return QuantileResult.of(weights.get(0) * s[indices[0] - 1] + weights.get(1) * s[indices[1] - 1], indices, weights);
  }

  @Override
  protected QuantileResult expectedShortfall(double level, DoubleArray sample) {
    ArgChecker.isTrue(level > 0, "Quantile should be above 0.");
    ArgChecker.isTrue(level < 1, "Quantile should be below 1.");
    int sampleSize = sampleCorrection(sample.size());
    double fractionalIndex = level * sampleSize + indexCorrection();
    double adjustedLevel = checkIndex(fractionalIndex, sample.size(), true);
    double[] order = createIndexArray(sampleSize);
    double[] s = sample.toArray();
    DoubleArrayMath.sortPairs(s, order);



    int lowerIndex = (int) Math.floor(adjustedLevel);
    int upperIndex = (int) Math.ceil(adjustedLevel);
    double interval = 1d / (double) sampleSize;
    double losses = s[0] * interval * (Math.min(fractionalIndex, 1d) - indexCorrection());
    for (int i = 0; i < lowerIndex - 1; i++) {
      losses += 0.5 * (s[i] + s[i + 1]) * interval;
    }
    if (lowerIndex != upperIndex) {
      double lowerWeight = upperIndex - adjustedLevel;
      double upperWeight = 1d - lowerWeight;
      double quantile = lowerWeight * s[lowerIndex - 1] + upperWeight * s[upperIndex - 1];
      losses += 0.5 * (s[lowerIndex - 1] + quantile) * interval * upperWeight;
    }
    if (fractionalIndex > sample.size()) {
      losses += s[sample.size() - 1] * (fractionalIndex - sample.size()) * interval;
    }
    return losses / level;
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
   * @param sampleSize the sample size
   * @return the correction
   */
  abstract int sampleCorrection(int sampleSize);

  /**
   * Generate an index of doubles.
   * <p>
   * Creates an index of doubles from 1.0 to a stipulated number, in increments of 1.
   *
   * @param indexArrayLength length of index array to be created
   * @return array of indices
   */
  private double[] createIndexArray(int indexArrayLength) {
    double[] indexArray = new double[indexArrayLength];
    for (int i = 0; i < indexArrayLength; i++) {
      indexArray[i] = i;
    }
    return indexArray;
  }

}
