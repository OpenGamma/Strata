/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static com.opengamma.strata.math.impl.matrix.MatrixAlgebraFactory.OG_ALGEBRA;

import java.util.Arrays;

import com.opengamma.strata.math.impl.linearalgebra.Decomposition;
import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionCommons;
import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionResult;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * Abstract class of solving cubic spline problem
 * Implementation depends on endpoint conditions
 * "solve" for 1-dimensional problem and "solveMultiDim" for  multi-dimensional problem should be implemented in inherited classes
 * "getKnotsMat1D" is overridden in certain cases 
 */
abstract class CubicSplineSolver {

  private final Decomposition<LUDecompositionResult> _luObj = new LUDecompositionCommons();

  /**
   * One-dimensional cubic spline
   * If (xValues length) = (yValues length), Not-A-Knot endpoint conditions are used
   * If (xValues length) + 2 = (yValues length), Clamped endpoint conditions are used 
   * @param xValues X values of data
   * @param yValues Y values of data
   * @return Coefficient matrix whose i-th row vector is (a_0,a_1,...) for i-th intervals, where a_0,a_1,... are coefficients of f(x) = a_0 + a_1 x^1 + ....
   * Note that the degree of polynomial is NOT necessarily 3
   */
  public abstract DoubleMatrix2D solve(final double[] xValues, final double[] yValues);

  /**
   * One-dimensional cubic spline
   * If (xValues length) = (yValues length), Not-A-Knot endpoint conditions are used
   * If (xValues length) + 2 = (yValues length), Clamped endpoint conditions are used 
   * @param xValues X values of data
   * @param yValues Y values of data
   * @return Array of  matrices: the 0-th element is Coefficient Matrix (same as the solve method above), the i-th element is \frac{\partial a^{i-1}_j}{\partial yValues_k} 
   * where a_0^i,a_1^i,... are coefficients of f^i(x) = a_0^i + a_1^i (x - xValues_{i}) + .... with x \in [xValues_{i}, xValues_{i+1}]
   */
  public abstract DoubleMatrix2D[] solveWithSensitivity(final double[] xValues, final double[] yValues);

  /**
   * Multi-dimensional cubic spline
   * If (xValues length) = (yValuesMatrix NumberOfColumn), Not-A-Knot endpoint conditions are used
   * If (xValues length) + 2 = (yValuesMatrix NumberOfColumn), Clamped endpoint conditions are used 
   * @param xValues X values of data
   * @param yValuesMatrix Y values of data, where NumberOfRow defines dimension of the spline
   * @return A set of coefficient matrices whose i-th row vector is (a_0,a_1,...) for the i-th interval, where a_0,a_1,... are coefficients of f(x) = a_0 + a_1 x^1 + .... 
   * Each matrix corresponds to an interpolation (xValues, yValuesMatrix RowVector)
   * Note that the degree of polynomial is NOT necessarily 3
   */
  public abstract DoubleMatrix2D[] solveMultiDim(final double[] xValues, final DoubleMatrix2D yValuesMatrix);

  /**
   * @param xValues X values of data
   * @return X values of knots (Note that these are NOT necessarily xValues if nDataPts=2,3)
   */
  public DoubleMatrix1D getKnotsMat1D(final double[] xValues) {
    return new DoubleMatrix1D(xValues);
  }

  /**
   * @param xValues X values of Data
   * @return {xValues[1]-xValues[0], xValues[2]-xValues[1],...}
   * xValues (and corresponding yValues) should be sorted before calling this method
   */
  protected double[] getDiffs(final double[] xValues) {

    final int nDataPts = xValues.length;
    double[] res = new double[nDataPts - 1];

    for (int i = 0; i < nDataPts - 1; ++i) {
      res[i] = xValues[i + 1] - xValues[i];
    }

    return res;
  }

  /**
   * @param xValues X values of Data
   * @param yValues Y values of Data
   * @param intervals {xValues[1]-xValues[0], xValues[2]-xValues[1],...}
   * @param solnVector Values of second derivative at knots
   * @return Coefficient matrix whose i-th row vector is {a_0,a_1,...} for i-th intervals, where a_0,a_1,... are coefficients of f(x) = a_0 + a_1 x^1 + ....
   */
  protected DoubleMatrix2D getCommonSplineCoeffs(final double[] xValues, final double[] yValues, final double[] intervals, final double[] solnVector) {

    final int nDataPts = xValues.length;
    double[][] res = new double[nDataPts - 1][4];
    for (int i = 0; i < nDataPts - 1; ++i) {
      res[i][0] = solnVector[i + 1] / 6. / intervals[i] - solnVector[i] / 6. / intervals[i];
      res[i][1] = 0.5 * solnVector[i];
      res[i][2] = yValues[i + 1] / intervals[i] - yValues[i] / intervals[i] - intervals[i] * solnVector[i] / 2. - intervals[i] * solnVector[i + 1] / 6. + intervals[i] * solnVector[i] / 6.;
      res[i][3] = yValues[i];
    }
    return new DoubleMatrix2D(res);
  }

  /**
   * @param intervals {xValues[1]-xValues[0], xValues[2]-xValues[1],...}
   * @param solnMatrix Sensitivity of second derivatives (x 0.5)  
   * @return Array of i coefficient matrices \frac{\partial a^i_j}{\partial y_k}
   */
  protected DoubleMatrix2D[] getCommonSensitivityCoeffs(final double[] intervals, final double[][] solnMatrix) {

    final int nDataPts = intervals.length + 1;
    double[][][] res = new double[nDataPts - 1][4][nDataPts];
    for (int i = 0; i < nDataPts - 1; ++i) {
      res[i][3][i] = 1.;
      res[i][2][i + 1] = 1. / intervals[i];
      res[i][2][i] = -1. / intervals[i];
      for (int k = 0; k < nDataPts; ++k) {
        res[i][0][k] = solnMatrix[i + 1][k] / 6. / intervals[i] - solnMatrix[i][k] / 6. / intervals[i];
        res[i][1][k] = 0.5 * solnMatrix[i][k];
        res[i][2][k] += -intervals[i] * solnMatrix[i][k] / 2. - intervals[i] * solnMatrix[i + 1][k] / 6. + intervals[i] * solnMatrix[i][k] / 6.;
      }
    }

    DoubleMatrix2D[] resMat = new DoubleMatrix2D[nDataPts - 1];
    for (int i = 0; i < nDataPts - 1; ++i) {
      resMat[i] = new DoubleMatrix2D(res[i]);
    }
    return resMat;
  }

  /**
   * Cubic spline and its node sensitivity are respectively obtained by solving a linear problem Ax=b where A is a square matrix and x,b are vector and AN=L where N,L are matrices 
   * @param xValues X values of data
   * @param yValues Y values of data
   * @param intervals {xValues[1]-xValues[0], xValues[2]-xValues[1],...}
   * @param toBeInv The matrix A
   * @param commonVector The vector b 
   * @param commonVecSensitivity The matrix L
   * @return Coefficient matrices of interpolant (x) and its node sensitivity (N)
   */
  protected DoubleMatrix2D[] getCommonCoefficientWithSensitivity(final double[] xValues, final double[] yValues, final double[] intervals, final double[][] toBeInv, final double[] commonVector,
      final double[][] commonVecSensitivity) {
    final int nDataPts = xValues.length;

    final DoubleMatrix1D[] soln = this.combinedMatrixEqnSolver(toBeInv, commonVector, commonVecSensitivity);
    final DoubleMatrix2D[] res = new DoubleMatrix2D[nDataPts];

    res[0] = getCommonSplineCoeffs(xValues, yValues, intervals, soln[0].getData());
    final double[][] solnMatrix = new double[nDataPts][nDataPts];
    for (int i = 0; i < nDataPts; ++i) {
      for (int j = 0; j < nDataPts; ++j) {
        solnMatrix[i][j] = soln[j + 1].getData()[i];
      }
    }
    final DoubleMatrix2D[] tmp = getCommonSensitivityCoeffs(intervals, solnMatrix);
    System.arraycopy(tmp, 0, res, 1, nDataPts - 1);

    return res;
  }

  /**
   * Cubic spline is obtained by solving a linear problem Ax=b where A is a square matrix and x,b are vector
   * @param intervals  the intervals
   * @return Endpoint-independent part of the matrix A
   */
  protected double[][] getCommonMatrixElements(final double[] intervals) {

    final int nDataPts = intervals.length + 1;

    double[][] res = new double[nDataPts][nDataPts];

    for (int i = 0; i < nDataPts; ++i) {
      Arrays.fill(res[i], 0.);
    }

    for (int i = 1; i < nDataPts - 1; ++i) {
      res[i][i - 1] = intervals[i - 1];
      res[i][i] = 2. * (intervals[i - 1] + intervals[i]);
      res[i][i + 1] = intervals[i];
    }

    return res;
  }

  /**
   * Cubic spline is obtained by solving a linear problem Ax=b where A is a square matrix and x,b are vector
   * @param yValues Y values of Data
   * @param intervals {xValues[1]-xValues[0], xValues[2]-xValues[1],...}
   * @return Endpoint-independent part of vector b
   */
  protected double[] getCommonVectorElements(final double[] yValues, final double[] intervals) {

    final int nDataPts = yValues.length;
    double[] res = new double[nDataPts];
    Arrays.fill(res, 0.);

    for (int i = 1; i < nDataPts - 1; ++i) {
      res[i] = 6. * yValues[i + 1] / intervals[i] - 6. * yValues[i] / intervals[i] - 6. * yValues[i] / intervals[i - 1] + 6. * yValues[i - 1] / intervals[i - 1];
    }

    return res;
  }

  /**
   * Node sensitivity is obtained by solving a linear problem AN = L where A,N,L are matrices
   * @param intervals {xValues[1]-xValues[0], xValues[2]-xValues[1],...}
   * @return The matrix L
   */
  protected double[][] getCommonVectorSensitivity(final double[] intervals) {
    final int nDataPts = intervals.length + 1;
    double[][] res = new double[nDataPts][nDataPts];
    for (int i = 0; i < nDataPts; ++i) {
      Arrays.fill(res[i], 0.);
    }

    for (int i = 1; i < nDataPts - 1; ++i) {
      res[i][i - 1] = 6. / intervals[i - 1];
      res[i][i] = -6. / intervals[i] - 6. / intervals[i - 1];
      res[i][i + 1] = 6. / intervals[i];
    }

    return res;
  }

  /**
   * Cubic spline is obtained by solving a linear problem Ax=b where A is a square matrix and x,b are vector
   * This can be done by LU decomposition
   * @param doubMat Matrix A
   * @param doubVec Vector B
   * @return Solution to the linear equation, x
   */
  protected double[] matrixEqnSolver(final double[][] doubMat, final double[] doubVec) {
    final LUDecompositionResult result = _luObj.evaluate(new DoubleMatrix2D(doubMat));

    final double[][] lMat = result.getL().getData();
    final double[][] uMat = result.getU().getData();
    double[] doubVecMod = ((DoubleMatrix1D) OG_ALGEBRA.multiply(result.getP(), new DoubleMatrix1D(doubVec))).getData();

    return backSubstitution(uMat, forwardSubstitution(lMat, doubVecMod));
  }

  /**
   * Cubic spline and its node sensitivity are respectively obtained by solving a linear problem Ax=b where A is a square matrix and x,b are vector and AN=L where N,L are matrices 
   * @param doubMat1 The matrix A
   * @param doubVec The vector b
   * @param doubMat2 The matrix L
   * @return The solutions to the linear systems, x,N
   */
  protected DoubleMatrix1D[] combinedMatrixEqnSolver(final double[][] doubMat1, final double[] doubVec, final double[][] doubMat2) {
    final int nDataPts = doubVec.length;
    final LUDecompositionResult result = _luObj.evaluate(new DoubleMatrix2D(doubMat1));

    final double[][] lMat = result.getL().getData();
    final double[][] uMat = result.getU().getData();
    final DoubleMatrix2D pMat = result.getP();
    double[] doubVecMod = ((DoubleMatrix1D) OG_ALGEBRA.multiply(pMat, new DoubleMatrix1D(doubVec))).getData();

    final DoubleMatrix2D doubMat2Matrix = new DoubleMatrix2D(doubMat2);
    final DoubleMatrix1D[] res = new DoubleMatrix1D[nDataPts + 1];
    res[0] = new DoubleMatrix1D(backSubstitution(uMat, forwardSubstitution(lMat, doubVecMod)));
    for (int i = 0; i < nDataPts; ++i) {
      final double[] doubMat2Colum = doubMat2Matrix.getColumnVector(i).getData();
      final double[] doubVecMod2 = ((DoubleMatrix1D) OG_ALGEBRA.multiply(pMat, new DoubleMatrix1D(doubMat2Colum))).getData();
      res[i + 1] = new DoubleMatrix1D(backSubstitution(uMat, forwardSubstitution(lMat, doubVecMod2)));
    }
    return res;
  }

  /**
   * Linear problem Ax=b is solved by forward substitution if A is lower triangular
   * @param lMat Lower triangular matrix
   * @param doubVec Vector b
   * @return Solution to the linear equation, x
   */
  private double[] forwardSubstitution(final double[][] lMat, final double[] doubVec) {

    final int size = lMat.length;
    double[] res = new double[size];

    for (int i = 0; i < size; ++i) {
      double tmp = doubVec[i] / lMat[i][i];
      for (int j = 0; j < i; ++j) {
        tmp -= lMat[i][j] * res[j] / lMat[i][i];
      }
      res[i] = tmp;
    }

    return res;
  }

  /**
   * Linear problem Ax=b is solved by backward substitution if A is upper triangular
   * @param uMat Upper triangular matrix
   * @param doubVec Vector b
   * @return Solution to the linear equation, x
   */
  private double[] backSubstitution(final double[][] uMat, final double[] doubVec) {

    final int size = uMat.length;
    double[] res = new double[size];

    for (int i = size - 1; i > -1; --i) {
      double tmp = doubVec[i] / uMat[i][i];
      for (int j = i + 1; j < size; ++j) {
        tmp -= uMat[i][j] * res[j] / uMat[i][i];
      }
      res[i] = tmp;
    }

    return res;
  }

}
