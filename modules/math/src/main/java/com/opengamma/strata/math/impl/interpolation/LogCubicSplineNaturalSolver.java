/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.math.impl.linearalgebra.TridiagonalMatrix;
import com.opengamma.strata.math.impl.linearalgebra.TridiagonalSolver;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * For specific cubic spline interpolations, polynomial coefficients are determined by the tridiagonal algorithm
 */
public class LogCubicSplineNaturalSolver extends CubicSplineSolver {

  @Override
  public DoubleMatrix2D solve(final double[] xValues, final double[] yValues) {
    final double[] intervals = getDiffs(xValues);
    return getCommonSplineCoeffs(xValues, yValues, intervals, matrixEqnSolver(getMatrix(intervals), getCommonVectorElements(yValues, intervals)));
  }

  @Override
  public DoubleMatrix2D[] solveWithSensitivity(final double[] xValues, final double[] yValues) {
    final double[] intervals = getDiffs(xValues);
    final double[][] toBeInv = getMatrix(intervals);
    final double[] commonVector = getCommonVectorElements(yValues, intervals);
    final double[][] commonVecSensitivity = getCommonVectorSensitivity(intervals);

    return getCommonCoefficientWithSensitivity(xValues, yValues, intervals, toBeInv, commonVector, commonVecSensitivity);
  }

  @Override
  public DoubleMatrix2D[] solveMultiDim(final double[] xValues, final DoubleMatrix2D yValuesMatrix) {
    final int dim = yValuesMatrix.getNumberOfRows();
    DoubleMatrix2D[] coefMatrix = new DoubleMatrix2D[dim];

    for (int i = 0; i < dim; ++i) {
      coefMatrix[i] = solve(xValues, yValuesMatrix.getRowVector(i).getData());
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
  protected DoubleMatrix1D[] combinedMatrixEqnSolver(final double[][] doubMat1, final double[] doubVec, final double[][] doubMat2) {
    final int size = doubVec.length;
    final DoubleMatrix1D[] res = new DoubleMatrix1D[size + 1];
    final DoubleMatrix2D doubMat2Matrix = new DoubleMatrix2D(doubMat2);

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
    res[0] = new DoubleMatrix1D(TridiagonalSolver.solvTriDag(m, doubVec));
    for (int i = 0; i < size; ++i) {
      final double[] doubMat2Colum = doubMat2Matrix.getColumnVector(i).getData();
      res[i + 1] = new DoubleMatrix1D(TridiagonalSolver.solvTriDag(m, doubMat2Colum));
    }

    return res;
  }
}
