/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.regression;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.CommonsMatrixAlgebra;

/**
 * 
 */
public class GeneralizedLeastSquaresRegression extends LeastSquaresRegression {

  private static CommonsMatrixAlgebra ALGEBRA = new CommonsMatrixAlgebra();

  @Override
  public LeastSquaresRegressionResult regress(double[][] x, double[][] weights, double[] y, boolean useIntercept) {
    if (weights == null) {
      throw new IllegalArgumentException("Cannot perform GLS regression without an array of weights");
    }
    checkData(x, weights, y);
    double[][] dep = addInterceptVariable(x, useIntercept);
    double[] indep = new double[y.length];
    double[][] wArray = new double[y.length][y.length];
    for (int i = 0; i < y.length; i++) {
      indep[i] = y[i];
      for (int j = 0; j < y.length; j++) {
        wArray[i][j] = weights[i][j];
      }
    }
    DoubleMatrix matrix = DoubleMatrix.copyOf(dep);
    DoubleArray vector = DoubleArray.copyOf(indep);
    DoubleMatrix w = DoubleMatrix.copyOf(wArray);
    DoubleMatrix transpose = ALGEBRA.getTranspose(matrix);
    DoubleMatrix betasVector = (DoubleMatrix)
        ALGEBRA.multiply(
            ALGEBRA.multiply(
                ALGEBRA.multiply(
                    ALGEBRA.getInverse(ALGEBRA.multiply(transpose, ALGEBRA.multiply(w, matrix))), transpose),
                w),
            vector);
    double[] yModel = super.writeArrayAsVector(((DoubleMatrix) ALGEBRA.multiply(matrix, betasVector)).toArray());
    double[] betas = super.writeArrayAsVector(betasVector.toArray());
    return getResultWithStatistics(x, y, betas, yModel, useIntercept);
  }

  private LeastSquaresRegressionResult getResultWithStatistics(
      double[][] x, double[] y, double[] betas, double[] yModel, boolean useIntercept) {

    int n = x.length;
    double[] residuals = new double[n];
    for (int i = 0; i < n; i++) {
      residuals[i] = y[i] - yModel[i];
    }
    return new WeightedLeastSquaresRegressionResult(betas, residuals, 0.0, null, 0.0, 0.0, null, null, useIntercept);
  }

}
