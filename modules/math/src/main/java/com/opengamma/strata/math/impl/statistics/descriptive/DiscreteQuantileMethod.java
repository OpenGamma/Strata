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
 * The estimation is one of the sorted sample data.
 * <p>
 * Reference: Value-At-Risk, OpenGamma Documentation 31, Version 0.1, April 2015.
 */
public abstract class DiscreteQuantileMethod
    extends QuantileCalculationMethod {

  @Override
  protected QuantileResult quantile(double level, DoubleArray sample, boolean isExtrapolated) {
    ArgChecker.isTrue(level > 0, "Quantile should be above 0.");
    ArgChecker.isTrue(level < 1, "Quantile should be below 1.");
    int sampleSize = sampleCorrection(sample.size());
    double[] order = createIndexArray(sample.size());
    double[] s = sample.toArray();
    DoubleArrayMath.sortPairs(s, order);
    int index = (int) checkIndex(index(level * sampleSize), sample.size(), isExtrapolated);
    int[] ind = new int[1];
    ind[0] = (int) order[index - 1];
    return QuantileResult.of(s[index - 1], ind, DoubleArray.of(1));
  }

  @Override
  protected QuantileResult expectedShortfall(double level, DoubleArray sample) {
    ArgChecker.isTrue(level > 0, "Quantile should be above 0.");
    ArgChecker.isTrue(level < 1, "Quantile should be below 1.");
    int sampleSize = sampleCorrection(sample.size());
    double[] order = createIndexArray(sample.size());
    double[] s = sample.toArray();
    DoubleArrayMath.sortPairs(s, order);
    double fractionalIndex = level * sampleSize;
    int index = (int) checkIndex(index(fractionalIndex), sample.size(), true);
    int[] indices = new int[index];
    double[] weights = new double[index];
    double interval = 1d / (double) sampleSize;
    double losses = s[0] * interval * indexShift();
    for (int i = 0; i < index - 1; i++) {
      losses += s[i] * interval;
      indices[i] = (int) order[i];
      weights[i] = interval;
    }
    losses += s[index - 1] * (fractionalIndex - index + 1 - indexShift()) * interval;
    indices[index - 1] = (int) order[index - 1];
    weights[0] += interval * indexShift();
    weights[index - 1] = (fractionalIndex - index + 1 - indexShift()) * interval;
    return QuantileResult.of(losses / level, indices, DoubleArray.ofUnsafe(weights).dividedBy(level));
  }

  //-------------------------------------------------------------------------

  /**
   * Computes the index for a given quantile multiplied by sample size.
   * <p>
   * The quantile size is given by quantile * sample size.
   *
   * @param quantileSize  the quantile size
   * @return the index in the sample
   */
  protected abstract int index(double quantileSize);

  /**
   * Returns the sample size correction for the specific implementation.
   *
   * @param sampleSize  the sample size
   * @return the correction
   */
  protected abstract int sampleCorrection(int sampleSize);

  /**
   * Shift added to/subtracted from index during intermediate steps in the expected shortfall computation.
   *
   * @return the index shift
   */
  protected abstract double indexShift();

  /**
   * Generate an index of doubles.
   * <p>
   * Creates an index of doubles from 0.0 to a stipulated number, in increments of 1.
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
