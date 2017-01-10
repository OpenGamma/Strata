/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.statistics.descriptive.QuantileCalculationMethod;

/**
 * Implementation of a quantile and expected shortfall estimator for series with exponentially weighted probabilities.
 * <p> 
 * Reference: "Value-at-risk", OpenGamma Documentation 31, Version 0.2, January 2016. Section A.4.
 */
public final class ExponentiallyWeightedInterpolationQuantileMethod
    extends QuantileCalculationMethod {
  
  /** The exponential weight. */
  private final double lambda;

  /**
   * Constructor. 
   * <p>
   * The exponential weight lambda must be > 0 and < 1.0.
   * 
   * @param lambda  the exponential weight
   */
  public ExponentiallyWeightedInterpolationQuantileMethod(double lambda) {
    ArgChecker.inRangeExclusive(lambda, 0.0d, 1.0d, "exponential weight");
    this.lambda = lambda;
  }

  @Override
  public double quantileFromUnsorted(double level, DoubleArray sample) {
    QuantileResult q = quantileDetails(level, sample, false, false);
    return q.getValue();
  }

  @Override
  public double quantileWithExtrapolationFromUnsorted(double level, DoubleArray sample) {
    QuantileResult q = quantileDetails(level, sample, true, false);
    return q.getValue();
  }
  
  @Override
  public double expectedShortfallFromUnsorted(double level, DoubleArray sample) {
    QuantileResult q = quantileDetails(level, sample, true, true);
    return q.getValue();
  }
  
  /**
   * Compute the quantile estimation and the details used in the result.
   * <p>
   * The quantile level is in decimal, i.e. 99% = 0.99 and 0 < level < 1 should be satisfied.
   * This is measured from the bottom, that is, Thus the quantile estimation with the level 99% corresponds to 
   * the smallest 99% observations.
   * <p>
   * The details consists on the indices of the samples actually used in the quantile computation - indices in the 
   * input sample - and the weights for each of those samples. The details are sufficient to recompute the 
   * quantile directly from the input sample.
   * <p>
   * The sample observations are supposed to be unsorted, the first step is to sort the data.
   * 
   * @param level  the quantile level
   * @param sample  the sample observations
   * @return The quantile estimation and its details
   */
  public QuantileResult quantileDetailsFromUnsorted(double level, DoubleArray sample) {
    return quantileDetails(level, sample, true, false);
  }

  /**
   * Compute the expected shortfall and the details used in the result.
   * <p>
   * The quantile level is in decimal, i.e. 99% = 0.99 and 0 < level < 1 should be satisfied.
   * This is measured from the bottom, that is, Thus the expected shortfall with the level 99% corresponds to 
   * the smallest 99% observations.
   * <p>
   * If index value computed from the level is outside of the sample data range, the nearest data point is used, i.e., 
   * expected short fall is computed with flat extrapolation.
   * Thus this is coherent to {@link #quantileWithExtrapolationFromUnsorted(double, DoubleArray)}.
   * <p>
   * The details consists on the indices of the samples actually used in the expected shortfall computation - indices 
   * in the input sample - and the weights for each of those samples. The details are sufficient to recompute the 
   * expected shortfall directly from the input sample.
   * <p> 
   * The sample observations are supposed to be unsorted, the first step is to sort the data.
   * 
   * @param level  the quantile level
   * @param sample  the sample observations
   * @return The expected shortfall estimation and its detail
   */
  public QuantileResult expectedShortfallDetailsFromUnsorted(double level, DoubleArray sample) {
    return quantileDetails(level, sample, true, true);
  }
  
  // Generic quantile computation with quantile details.
  private QuantileResult quantileDetails(
      double level, 
      DoubleArray sample, 
      boolean isExtrapolated,
      boolean isEs) {
    int nbData = sample.size();
    double[] w = weights(nbData);
    /* Sorting data and keeping weight information. The arrays are modified */
    double[] s = sample.toArray();
    DoubleArrayMath.sortPairs(s, w);

    double[] s2 = sample.toArray();
    double[] order = new double[s2.length];
    for (int i = 0; i < s2.length; i++) {
      order[i] = i;
    }
    DoubleArrayMath.sortPairs(s2, order);
    /* Find the index. */
    double runningWeight = 0.0d;
    int index = nbData;
    while (runningWeight < 1.0d - level) {
      index--;
      runningWeight += w[index];
    }
    if (isEs) {
      return esFromIndexRunningWeight(index, runningWeight, s2, w, order, level);
    }
    return quantileFromIndexRunningWeight(index, runningWeight, isExtrapolated, s2, w, order, level);
  }

  /**
   * Computes value-at-risk.
   * @param index  the index from which the VaR should be computed
   * @param runningWeight  the running weight up to index
   * @param isExtrapolated  flag indicating if value should be extrapolated (flat) beyond the last value
   * @param s  the sorted sample
   * @param w  the sorted weights
   * @param order  the order of the sorted sample in the unsorted sample
   * @param level  the level at which the VaR should be computed
   * @return the VaR and the details of sample data used to compute it
   */
  private QuantileResult quantileFromIndexRunningWeight(
      int index, 
      double runningWeight, 
      boolean isExtrapolated,
      double[] s,
      double[] w,
      double[] order,
      double level) {
    int nbData = s.length;
    if ((index == nbData - 1) || (index == nbData)) {
      ArgChecker.isTrue(isExtrapolated, "Quantile can not be computed above the highest probability level.");
      return QuantileResult.of(s[nbData - 1], new int[] {(int) Math.round(order[nbData - 1])}, DoubleArray.of(1.0d));
    }
    double alpha = (runningWeight - (1.0 - level)) / w[index];
    int[] indices = new int[nbData - index];
    double[] impacts = new double[nbData - index];
    for (int i = 0; i < nbData - index; i++) {
      indices[i] = (int) Math.round(order[index + i]);
    }
    impacts[0] = 1 - alpha;
    impacts[1] = alpha;
    return QuantileResult.of((1 - alpha) * s[index] + alpha * s[index + 1], indices, DoubleArray.ofUnsafe(impacts));    
  }
  
  /**
   * Computes expected shortfall.
   * @param index  the index from which the ES should be computed
   * @param runningWeight  the running weight up to index
   * @param isExtrapolated  flag indicating if value should be extrapolated (flat) beyond the last value
   * @param s  the sorted sample
   * @param w  the sorted weights
   * @param order  the order of the sorted sample in the unsorted sample
   * @param level  the level at which the ES should be computed
   * @return the expected shortfall and the details of sample data used to compute it
   */
  private QuantileResult esFromIndexRunningWeight(
      int index, 
      double runningWeight, 
      double[] s,
      double[] w,
      double[] order,
      double level) {
    int nbData = s.length;
    if ((index == nbData - 1) || (index == nbData)) {
      return QuantileResult.of(s[nbData - 1], new int[] {(int) Math.round(order[nbData - 1])}, DoubleArray.of(1.0d));
    }
    double alpha = (runningWeight - (1.0 - level)) / w[index];
    int[] indices = new int[nbData - index];
    double[] impacts = new double[nbData - index];
    for (int i = 0; i < nbData - index; i++) {
      indices[i] = (int) Math.round(order[index + i]);
    }
    impacts[0] = 0.5 * (1 - alpha) * (1 - alpha) * w[index] / (1.0 - level);
    impacts[1] = (alpha + 1) * 0.5 * (1 - alpha) * w[index] / (1.0 - level);
    for (int i = 1; i < nbData - index - 1; i++) {
      impacts[i] += 0.5 * w[index + i] / (1.0 - level);
      impacts[i + 1] += 0.5 * w[index + i] / (1.0 - level);
    }
    impacts[nbData - index - 1] += w[nbData - 1] / (1.0 - level);
    double es = 0;
    for (int i = 0; i < nbData - index; i++) {
      es += s[index + i] * impacts[i];
    }
    return QuantileResult.of(es, indices, DoubleArray.ofUnsafe(impacts));    
  }

  @Override
  protected double quantile(double level, DoubleArray sortedSample, boolean isExtrapolated) {
    throw new UnsupportedOperationException("Quantile available only from unsorted sample due to weights.");
  }

  @Override
  protected double expectedShortfall(double level, DoubleArray sortedSample) {
    throw new UnsupportedOperationException("Expected Shortfall only from unsorted sample due to weights.");
  }
  
  /**
   * Returns the weights for a given sample size.
   * @param size  the sample size
   * @return the weights
   */
  public double[] weights(int size) {
    double w1 = (1.0 - 1.0D / lambda) / (1.0d - Math.pow(lambda, -size));
    double[] w = new double[size];
    for (int i = 0; i < size; i++) {
      w[i] = w1 / Math.pow(lambda, i);
    }
    return w;
  }

}
