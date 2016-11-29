/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.regression;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.CommonsMatrixAlgebra;

/**
 * 
 */
public class WeightedLeastSquaresRegression extends LeastSquaresRegression {

  private static final Logger log = LoggerFactory.getLogger(WeightedLeastSquaresRegression.class);
  private static CommonsMatrixAlgebra ALGEBRA = new CommonsMatrixAlgebra();

  @Override
  public LeastSquaresRegressionResult regress(double[][] x, double[][] weights, double[] y, boolean useIntercept) {
    if (weights == null) {
      throw new IllegalArgumentException("Cannot perform WLS regression without an array of weights");
    }
    checkData(x, weights, y);
    log
        .info("Have a two-dimensional array for what should be a one-dimensional array of weights. " +
            "The weights used in this regression will be the diagonal elements only");
    double[] w = new double[weights.length];
    for (int i = 0; i < w.length; i++) {
      w[i] = weights[i][i];
    }
    return regress(x, w, y, useIntercept);
  }

  public LeastSquaresRegressionResult regress(double[][] x, double[] weights, double[] y, boolean useIntercept) {
    if (weights == null) {
      throw new IllegalArgumentException("Cannot perform WLS regression without an array of weights");
    }
    checkData(x, weights, y);
    double[][] dep = addInterceptVariable(x, useIntercept);
    double[] w = new double[weights.length];
    for (int i = 0; i < y.length; i++) {
      w[i] = weights[i];
    }
    DoubleMatrix matrix = DoubleMatrix.copyOf(dep);
    DoubleArray vector = DoubleArray.copyOf(y);
    RealMatrix wDiag = new DiagonalMatrix(w);
    DoubleMatrix transpose = ALGEBRA.getTranspose(matrix);

    DoubleMatrix wDiagTimesMatrix = DoubleMatrix.ofUnsafe(wDiag.multiply(
        new Array2DRowRealMatrix(matrix.toArrayUnsafe())).getData());
    DoubleMatrix tmp = (DoubleMatrix) ALGEBRA.multiply(
        ALGEBRA.getInverse(ALGEBRA.multiply(transpose, wDiagTimesMatrix)), transpose);

    DoubleMatrix wTmpTimesDiag =
        DoubleMatrix.copyOf(wDiag.preMultiply(new Array2DRowRealMatrix(tmp.toArrayUnsafe())).getData());
    DoubleMatrix betasVector = (DoubleMatrix) ALGEBRA.multiply(wTmpTimesDiag, vector);
    double[] yModel = super.writeArrayAsVector(((DoubleMatrix) ALGEBRA.multiply(matrix, betasVector)).toArray());
    double[] betas = super.writeArrayAsVector(betasVector.toArray());
    return getResultWithStatistics(x, convertArray(wDiag.getData()), y, betas, yModel, transpose, matrix, useIntercept);
  }

  private LeastSquaresRegressionResult getResultWithStatistics(
      double[][] x, double[][] w, double[] y, double[] betas, double[] yModel,
      DoubleMatrix transpose, DoubleMatrix matrix, boolean useIntercept) {
    double yMean = 0.;
    for (double y1 : y) {
      yMean += y1;
    }
    yMean /= y.length;
    double totalSumOfSquares = 0.;
    double errorSumOfSquares = 0.;
    int n = x.length;
    int k = betas.length;
    double[] residuals = new double[n];
    double[] standardErrorsOfBeta = new double[k];
    double[] tStats = new double[k];
    double[] pValues = new double[k];
    for (int i = 0; i < n; i++) {
      totalSumOfSquares += w[i][i] * (y[i] - yMean) * (y[i] - yMean);
      residuals[i] = y[i] - yModel[i];
      errorSumOfSquares += w[i][i] * residuals[i] * residuals[i];
    }
    double regressionSumOfSquares = totalSumOfSquares - errorSumOfSquares;
    double[][] covarianceBetas = convertArray(ALGEBRA.getInverse(ALGEBRA.multiply(transpose, matrix)).toArray());
    double rSquared = regressionSumOfSquares / totalSumOfSquares;
    double adjustedRSquared = 1. - (1 - rSquared) * (n - 1) / (n - k);
    double meanSquareError = errorSumOfSquares / (n - k);
    TDistribution studentT = new TDistribution(n - k);
    for (int i = 0; i < k; i++) {
      standardErrorsOfBeta[i] = Math.sqrt(meanSquareError * covarianceBetas[i][i]);
      tStats[i] = betas[i] / standardErrorsOfBeta[i];
      pValues[i] = 1 - studentT.cumulativeProbability(Math.abs(tStats[i]));
    }
    return new WeightedLeastSquaresRegressionResult(
        betas, residuals, meanSquareError, standardErrorsOfBeta, rSquared, adjustedRSquared, tStats, pValues, useIntercept);
  }
}
