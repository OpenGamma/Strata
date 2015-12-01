/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static com.opengamma.strata.math.impl.matrix.MatrixAlgebraFactory.OG_ALGEBRA;

import java.util.Arrays;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.linearalgebra.Decomposition;
import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionCommons;
import com.opengamma.strata.math.impl.linearalgebra.LUDecompositionResult;

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
  public abstract DoubleMatrix solve(double[] xValues, double[] yValues);

  /**
   * One-dimensional cubic spline
   * If (xValues length) = (yValues length), Not-A-Knot endpoint conditions are used
   * If (xValues length) + 2 = (yValues length), Clamped endpoint conditions are used 
   * @param xValues X values of data
   * @param yValues Y values of data
   * @return Array of  matrices: the 0-th element is Coefficient Matrix (same as the solve method above), the i-th element is \frac{\partial a^{i-1}_j}{\partial yValues_k} 
   * where a_0^i,a_1^i,... are coefficients of f^i(x) = a_0^i + a_1^i (x - xValues_{i}) + .... with x \in [xValues_{i}, xValues_{i+1}]
   */
  public abstract DoubleMatrix[] solveWithSensitivity(double[] xValues, double[] yValues);

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
  public abstract DoubleMatrix[] solveMultiDim(double[] xValues, DoubleMatrix yValuesMatrix);

  /**
   * @param xValues X values of data
   * @return X values of knots (Note that these are NOT necessarily xValues if nDataPts=2,3)
   */
  public DoubleArray getKnotsMat1D(double[] xValues) {
    return DoubleArray.copyOf(xValues);
  }

  /**
   * @param xValues X values of Data
   * @return {xValues[1]-xValues[0], xValues[2]-xValues[1],...}
   * xValues (and corresponding yValues) should be sorted before calling this method
   */
  protected double[] getDiffs(double[] xValues) {

    int nDataPts = xValues.length;
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
  protected DoubleMatrix getCommonSplineCoeffs(double[] xValues, double[] yValues, double[] intervals, double[] solnVector) {

    int nDataPts = xValues.length;
    double[][] res = new double[nDataPts - 1][4];
    for (int i = 0; i < nDataPts - 1; ++i) {
      res[i][0] = solnVector[i + 1] / 6. / intervals[i] - solnVector[i] / 6. / intervals[i];
      res[i][1] = 0.5 * solnVector[i];
      res[i][2] = yValues[i + 1] / intervals[i] - yValues[i] / intervals[i] - intervals[i] * solnVector[i] / 2. - intervals[i] * solnVector[i + 1] / 6. + intervals[i] * solnVector[i] / 6.;
      res[i][3] = yValues[i];
    }
    return DoubleMatrix.copyOf(res);
  }

  /**
   * @param intervals {xValues[1]-xValues[0], xValues[2]-xValues[1],...}
   * @param solnMatrix Sensitivity of second derivatives (x 0.5)  
   * @return Array of i coefficient matrices \frac{\partial a^i_j}{\partial y_k}
   */
  protected DoubleMatrix[] getCommonSensitivityCoeffs(double[] intervals, double[][] solnMatrix) {

    int nDataPts = intervals.length + 1;
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

    DoubleMatrix[] resMat = new DoubleMatrix[nDataPts - 1];
    for (int i = 0; i < nDataPts - 1; ++i) {
      resMat[i] = DoubleMatrix.copyOf(res[i]);
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
  protected DoubleMatrix[] getCommonCoefficientWithSensitivity(double[] xValues, double[] yValues, double[] intervals,
      double[][] toBeInv, double[] commonVector,
      double[][] commonVecSensitivity) {
    int nDataPts = xValues.length;

    DoubleArray[] soln = this.combinedMatrixEqnSolver(toBeInv, commonVector, commonVecSensitivity);
    DoubleMatrix[] res = new DoubleMatrix[nDataPts];

    res[0] = getCommonSplineCoeffs(xValues, yValues, intervals, soln[0].toArray());
    double[][] solnMatrix = new double[nDataPts][nDataPts];
    for (int i = 0; i < nDataPts; ++i) {
      for (int j = 0; j < nDataPts; ++j) {
        solnMatrix[i][j] = soln[j + 1].get(i);
      }
    }
    DoubleMatrix[] tmp = getCommonSensitivityCoeffs(intervals, solnMatrix);
    System.arraycopy(tmp, 0, res, 1, nDataPts - 1);

    return res;
  }

  /**
   * Cubic spline is obtained by solving a linear problem Ax=b where A is a square matrix and x,b are vector
   * @param intervals  the intervals
   * @return Endpoint-independent part of the matrix A
   */
  protected double[][] getCommonMatrixElements(double[] intervals) {

    int nDataPts = intervals.length + 1;

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
  protected double[] getCommonVectorElements(double[] yValues, double[] intervals) {

    int nDataPts = yValues.length;
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
  protected double[][] getCommonVectorSensitivity(double[] intervals) {
    int nDataPts = intervals.length + 1;
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
  protected double[] matrixEqnSolver(double[][] doubMat, double[] doubVec) {
    LUDecompositionResult result = _luObj.apply(DoubleMatrix.copyOf(doubMat));

    double[][] lMat = result.getL().toArray();
    double[][] uMat = result.getU().toArray();
    DoubleArray doubVecMod = ((DoubleArray) OG_ALGEBRA.multiply(result.getP(), DoubleArray.copyOf(doubVec)));

    return backSubstitution(uMat, forwardSubstitution(lMat, doubVecMod));
  }

  /**
   * Cubic spline and its node sensitivity are respectively obtained by solving a linear problem Ax=b where A is a square matrix and x,b are vector and AN=L where N,L are matrices 
   * @param doubMat1 The matrix A
   * @param doubVec The vector b
   * @param doubMat2 The matrix L
   * @return The solutions to the linear systems, x,N
   */
  protected DoubleArray[] combinedMatrixEqnSolver(double[][] doubMat1, double[] doubVec, double[][] doubMat2) {
    int nDataPts = doubVec.length;
    LUDecompositionResult result = _luObj.apply(DoubleMatrix.copyOf(doubMat1));

    double[][] lMat = result.getL().toArray();
    double[][] uMat = result.getU().toArray();
    DoubleMatrix pMat = result.getP();
    DoubleArray doubVecMod = ((DoubleArray) OG_ALGEBRA.multiply(pMat, DoubleArray.copyOf(doubVec)));

    DoubleMatrix doubMat2Matrix = DoubleMatrix.copyOf(doubMat2);
    DoubleArray[] res = new DoubleArray[nDataPts + 1];
    res[0] = DoubleArray.copyOf(backSubstitution(uMat, forwardSubstitution(lMat, doubVecMod)));
    for (int i = 0; i < nDataPts; ++i) {
      DoubleArray doubMat2Colum = doubMat2Matrix.column(i);
      DoubleArray doubVecMod2 = ((DoubleArray) OG_ALGEBRA.multiply(pMat, doubMat2Colum));
      res[i + 1] = DoubleArray.copyOf(backSubstitution(uMat, forwardSubstitution(lMat, doubVecMod2)));
    }
    return res;
  }

  /**
   * Linear problem Ax=b is solved by forward substitution if A is lower triangular.
   * 
   * @param lMat Lower triangular matrix
   * @param doubVec Vector b
   * @return Solution to the linear equation, x
   */
  private double[] forwardSubstitution(double[][] lMat, DoubleArray doubVec) {
    int size = lMat.length;
    double[] res = new double[size];
    for (int i = 0; i < size; ++i) {
      double tmp = doubVec.get(i) / lMat[i][i];
      for (int j = 0; j < i; ++j) {
        tmp -= lMat[i][j] * res[j] / lMat[i][i];
      }
      res[i] = tmp;
    }
    return res;
  }

  /**
   * Linear problem Ax=b is solved by backward substitution if A is upper triangular.
   * 
   * @param uMat Upper triangular matrix
   * @param doubVec Vector b
   * @return Solution to the linear equation, x
   */
  private double[] backSubstitution(double[][] uMat, double[] doubVec) {
    int size = uMat.length;
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
