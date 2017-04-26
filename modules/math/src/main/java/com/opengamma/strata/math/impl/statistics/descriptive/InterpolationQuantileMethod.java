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
 * <i>p<subscript>i</subscript> on which the interpolation take place (X axis) varies between actual implementation
 * of the abstract class. For each probability <i>p<subscript>i</subscript></i>, the cumulative distribution value is
 * the sample value with same index. The index used above are the Java index plus 1.
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
    double adjustedLevel = checkIndex(level * sampleSize + indexCorrection(), sample.size(), isExtrapolated);
    double[] order = createIndexArray(sample.size());
    double[] s = sample.toArray();
    DoubleArrayMath.sortPairs(s, order);
    int lowerIndex = (int) Math.floor(adjustedLevel);
    int upperIndex = (int) Math.ceil(adjustedLevel);
    double lowerWeight = upperIndex - adjustedLevel;
    double upperWeight = 1d - lowerWeight;
    return QuantileResult.of(
        lowerWeight * s[lowerIndex - 1] + upperWeight * s[upperIndex - 1],
        new int[]{(int) order[lowerIndex - 1], (int) order[upperIndex - 1]},
        DoubleArray.of(lowerWeight, upperWeight));
  }

  @Override
  protected QuantileResult expectedShortfall(double level, DoubleArray sample) {
    ArgChecker.isTrue(level > 0, "Quantile should be above 0.");
    ArgChecker.isTrue(level < 1, "Quantile should be below 1.");
    int sampleSize = sampleCorrection(sample.size());
    double fractionalIndex = level * sampleSize + indexCorrection();
    double adjustedLevel = checkIndex(fractionalIndex, sample.size(), true);
    double[] order = createIndexArray(sample.size());
    double[] s = sample.toArray();
    DoubleArrayMath.sortPairs(s, order);
    int lowerIndex = (int) Math.floor(adjustedLevel);
    int upperIndex = (int) Math.ceil(adjustedLevel);
    int[] indices = new int[upperIndex];
    double[] weights = new double[upperIndex];
    double interval = 1d / (double) sampleSize;
    weights[0] = interval * (Math.min(fractionalIndex, 1d) - indexCorrection());
    double losses = s[0] * weights[0];
    for (int i = 0; i < lowerIndex - 1; i++) {
      losses += 0.5 * (s[i] + s[i + 1]) * interval;
      indices[i] = (int) order[i];
      weights[i] += 0.5 * interval;
      weights[i + 1] += 0.5 * interval;
    }
    if (lowerIndex != upperIndex) {
      double lowerWeight = upperIndex - adjustedLevel;
      double upperWeight = 1d - lowerWeight;
      double quantile = lowerWeight * s[lowerIndex - 1] + upperWeight * s[upperIndex - 1];
      losses += 0.5 * (s[lowerIndex - 1] + quantile) * interval * upperWeight;
      indices[lowerIndex - 1] = (int) order[lowerIndex - 1];
      indices[upperIndex - 1] = (int) order[upperIndex - 1];
      weights[lowerIndex - 1] += 0.5 * (1d + lowerWeight) * interval * upperWeight;
      weights[upperIndex - 1] = 0.5 * upperWeight * interval * upperWeight;
    }
    if (fractionalIndex > sample.size()) {
      losses += s[sample.size() - 1] * (fractionalIndex - sample.size()) * interval;
      indices[sample.size() - 1] = (int) order[sample.size() - 1];
      weights[sample.size() - 1] += (fractionalIndex - sample.size()) * interval;
    }
    return QuantileResult.of(losses / level, indices, DoubleArray.ofUnsafe(weights).dividedBy(level));
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

  /**
   * Generate an index of doubles.
   * <p>
   * Creates an index of doubles from 1.0 to a stipulated number, in increments of 1.
   *
   * @param indexArrayLength  length of index array to be created
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
