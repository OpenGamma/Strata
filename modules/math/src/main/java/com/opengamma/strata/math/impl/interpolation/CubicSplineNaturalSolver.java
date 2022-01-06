/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Solves cubic spline problem with natural endpoint conditions, where the second derivative at the endpoints is 0.
 */
public class CubicSplineNaturalSolver extends CubicSplineSolver {

  @Override
  public DoubleMatrix solve(double[] xValues, double[] yValues) {
    double[] intervals = getDiffs(xValues);
    return getCommonSplineCoeffs(
        xValues, yValues, intervals, matrixEqnSolver(getMatrix(intervals), getCommonVectorElements(yValues, intervals)));
  }

  @Override
  public DoubleMatrix[] solveWithSensitivity(double[] xValues, double[] yValues) {
    double[] intervals = getDiffs(xValues);
    double[][] toBeInv = getMatrix(intervals);
    double[] commonVector = getCommonVectorElements(yValues, intervals);
    double[][] commonVecSensitivity = getCommonVectorSensitivity(intervals);

    return getCommonCoefficientWithSensitivity(xValues, yValues, intervals, toBeInv, commonVector, commonVecSensitivity);
  }

  @Override
  public DoubleMatrix[] solveMultiDim(double[] xValues, DoubleMatrix yValuesMatrix) {
    int dim = yValuesMatrix.rowCount();
    DoubleMatrix[] coefMatrix = new DoubleMatrix[dim];

    for (int i = 0; i < dim; ++i) {
      coefMatrix[i] = solve(xValues, yValuesMatrix.row(i).toArray());
    }

    return coefMatrix;
  }

  /**
   * Cubic spline is obtained by solving a linear problem Ax=b where A is a square matrix and x,b are vector
   * @param intervals {xValues[1]-xValues[0], xValues[2]-xValues[1],...}
   * @return Matrix A
   */
  private double[][] getMatrix(double[] intervals) {
    int nData = intervals.length + 1;
    double[][] res = new double[nData][nData];

    res = getCommonMatrixElements(intervals);
    res[0][0] = 1.;
    res[nData - 1][nData - 1] = 1.;

    return res;
  }
}
