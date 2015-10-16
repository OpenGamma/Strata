/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Solves cubic spline problem with clamped endpoint conditions, where the first derivative is specified at endpoints
 */
public class CubicSplineClampedSolver extends CubicSplineSolver {

  private double[] _iniConds;
  private double[] _finConds;
  private double _iniCondUse;
  private double _finCondUse;

  /**
   * Constructor for a one-dimensional problem
   * @param iniCond Left endpoint condition
   * @param finCond Right endpoint condition
   */
  public CubicSplineClampedSolver(final double iniCond, final double finCond) {

    _iniCondUse = iniCond;
    _finCondUse = finCond;
  }

  /**
   * Constructor for a multi-dimensional problem
   * @param iniConds Set of left endpoint conditions
   * @param finConds Set of right endpoint conditions 
   */
  public CubicSplineClampedSolver(final double[] iniConds, final double[] finConds) {

    _iniConds = iniConds;
    _finConds = finConds;
  }

  @Override
  public DoubleMatrix solve(final double[] xValues, final double[] yValues) {

    final double[] intervals = getDiffs(xValues);

    return getCommonSplineCoeffs(xValues, yValues, intervals, matrixEqnSolver(getMatrix(intervals), getVector(yValues, intervals)));
  }

  @Override
  public DoubleMatrix[] solveWithSensitivity(final double[] xValues, final double[] yValues) {
    final double[] intervals = getDiffs(xValues);
    final double[][] toBeInv = getMatrix(intervals);
    final double[] vector = getVector(yValues, intervals);
    final double[][] vecSensitivity = getVectorSensitivity(intervals);

    return getCommonCoefficientWithSensitivity(xValues, yValues, intervals, toBeInv, vector, vecSensitivity);
  }

  @Override
  public DoubleMatrix[] solveMultiDim(final double[] xValues, final DoubleMatrix yValuesMatrix) {
    final int dim = yValuesMatrix.rowCount();
    DoubleMatrix[] coefMatrix = new DoubleMatrix[dim];

    for (int i = 0; i < dim; ++i) {
      resetConds(i);
      coefMatrix[i] = solve(xValues, yValuesMatrix.row(i).toArray());
    }

    return coefMatrix;
  }

  /**
   * Reset endpoint conditions
   * @param i  
   */
  private void resetConds(final int i) {
    _iniCondUse = _iniConds[i];
    _finCondUse = _finConds[i];
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
    res[0][0] = 2. * intervals[0];
    res[0][1] = intervals[0];
    res[nData - 1][nData - 2] = intervals[nData - 2];
    res[nData - 1][nData - 1] = 2. * intervals[nData - 2];
    return res;
  }

  /**
   * Cubic spline is obtained by solving a linear problem Ax=b where A is a square matrix and x,b are vector
   * @param yValues Y Values of data
   * @param intervals {xValues[1]-xValues[0], xValues[2]-xValues[1],...}
   * @return Vector b
   */
  private double[] getVector(final double[] yValues, final double[] intervals) {

    final int nData = yValues.length;
    double[] res = new double[nData];
    res = getCommonVectorElements(yValues, intervals);

    res[0] = 6. * yValues[1] / intervals[0] - 6. * yValues[0] / intervals[0] - 6. * _iniCondUse;
    res[nData - 1] = 6. * _finCondUse - 6. * yValues[nData - 1] / intervals[nData - 2] + 6. * yValues[nData - 2] / intervals[nData - 2];

    return res;
  }

  private double[][] getVectorSensitivity(final double[] intervals) {

    final int nData = intervals.length + 1;
    double[][] res = new double[nData][nData];
    res = getCommonVectorSensitivity(intervals);

    res[0][0] = -6. / intervals[0];
    res[0][1] = 6. / intervals[0];
    res[nData - 1][nData - 1] = -6. / intervals[nData - 2];
    res[nData - 1][nData - 2] = 6. / intervals[nData - 2];

    return res;
  }
}
