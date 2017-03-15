/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.linearalgebra.TridiagonalMatrix;
import com.opengamma.strata.math.impl.linearalgebra.TridiagonalSolver;

/**
 * For specific cubic spline interpolations, polynomial coefficients are determined by the tridiagonal algorithm.
 */
public class LogCubicSplineNaturalSolver extends CubicSplineSolver {

  @Override
  public DoubleMatrix solve(final double[] xValues, final double[] yValues) {
    final double[] intervals = getDiffs(xValues);
    return getCommonSplineCoeffs(xValues, yValues, intervals, matrixEqnSolver(getMatrix(intervals), getCommonVectorElements(yValues, intervals)));
  }

  @Override
  public DoubleMatrix[] solveWithSensitivity(final double[] xValues, final double[] yValues) {
    final double[] intervals = getDiffs(xValues);
    final double[][] toBeInv = getMatrix(intervals);
    final double[] commonVector = getCommonVectorElements(yValues, intervals);
    final double[][] commonVecSensitivity = getCommonVectorSensitivity(intervals);

    return getCommonCoefficientWithSensitivity(xValues, yValues, intervals, toBeInv, commonVector, commonVecSensitivity);
  }

  @Override
  public DoubleMatrix[] solveMultiDim(final double[] xValues, final DoubleMatrix yValuesMatrix) {
    final int dim = yValuesMatrix.rowCount();
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
  private double[][] getMatrix(final double[] intervals) {

    final int nData = intervals.length + 1;
    double[][] res = new double[nData][nData];

    res = getCommonMatrixElements(intervals);
    res[0][0] = 1.;
    res[nData - 1][nData - 1] = 1.;

    return res;
  }

  @Override
  protected double[] matrixEqnSolver(final double[][] doubMat, final double[] doubVec) {
    final int sizeM1 = doubMat.length - 1;
    final double[] a = new double[sizeM1];
    final double[] b = new double[sizeM1 + 1];
    final double[] c = new double[sizeM1];

    for (int i = 0; i < sizeM1; ++i) {
      a[i] = doubMat[i][i + 1];
      b[i] = doubMat[i][i];
      c[i] = doubMat[i + 1][i];
    }
    b[sizeM1] = doubMat[sizeM1][sizeM1];

    final TridiagonalMatrix m = new TridiagonalMatrix(b, a, c);

    return TridiagonalSolver.solvTriDag(m, doubVec);
  }

  @Override
  protected DoubleArray[] combinedMatrixEqnSolver(final double[][] doubMat1, final double[] doubVec, final double[][] doubMat2) {
    final int size = doubVec.length;
    final DoubleArray[] res = new DoubleArray[size + 1];
    final DoubleMatrix doubMat2Matrix = DoubleMatrix.copyOf(doubMat2);

    final double[] u = new double[size - 1];
    final double[] d = new double[size];
    final double[] l = new double[size - 1];

    for (int i = 0; i < size - 1; ++i) {
      u[i] = doubMat1[i][i + 1];
      d[i] = doubMat1[i][i];
      l[i] = doubMat1[i + 1][i];
    }
    d[size - 1] = doubMat1[size - 1][size - 1];
    final TridiagonalMatrix m = new TridiagonalMatrix(d, u, l);
    res[0] = DoubleArray.copyOf(TridiagonalSolver.solvTriDag(m, doubVec));
    for (int i = 0; i < size; ++i) {
      DoubleArray doubMat2Colum = doubMat2Matrix.column(i);
      res[i + 1] = TridiagonalSolver.solvTriDag(m, doubMat2Colum);
    }
    return res;
  }

}
