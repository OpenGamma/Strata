/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.regression;

/**
 * 
 */
public class WeightedLeastSquaresRegressionResult extends LeastSquaresRegressionResult {

  public WeightedLeastSquaresRegressionResult(LeastSquaresRegressionResult result) {
    super(result);
  }

  public WeightedLeastSquaresRegressionResult(
      double[] betas,
      double[] residuals,
      double meanSquareError,
      double[] standardErrorOfBeta,
      double rSquared,
      double rSquaredAdjusted,
      double[] tStats,
      double[] pValues,
      boolean hasIntercept) {

    super(betas, residuals, meanSquareError, standardErrorOfBeta, rSquared, rSquaredAdjusted, tStats, pValues, hasIntercept);
  }

  public double getWeightedPredictedValue(double[] x, double[] w) {
    if (x == null) {
      throw new IllegalArgumentException("Variable array was null");
    }
    if (w == null) {
      throw new IllegalArgumentException("Weight array was null");
    }
    double[] betas = getBetas();
    if (hasIntercept() && x.length != betas.length - 1 || x.length != betas.length) {
      throw new IllegalArgumentException("Number of variables did not match number used in regression");
    }
    if (x.length != w.length) {
      throw new IllegalArgumentException("Number of weights did not match number of variables");
    }
    double sum = 0;
    for (int i = 0; i < (hasIntercept() ? x.length + 1 : x.length); i++) {
      if (hasIntercept()) {
        if (i == 0) {
          sum += betas[0];
        } else {
          sum += betas[i] * x[i - 1] * w[i - 1];
        }
      } else {
        sum += betas[i] * x[i] * w[i];
      }
    }
    return sum;
  }
}
