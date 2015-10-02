/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Arrays;

import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * 
 */
public class NaturalSplineInterpolator extends PiecewisePolynomialInterpolator {

  private CubicSplineSolver _solver;

  /**
   * 
   */
  public NaturalSplineInterpolator() {
    _solver = new CubicSplineNaturalSolver();
  }

  /**
   * 
   * @param inherit  the solver
   */
  public NaturalSplineInterpolator(final CubicSplineSolver inherit) {
    _solver = inherit;
  }

  /**
   * @param xValues X values of data
   * @param yValues Y values of data
   * @return {@link PiecewisePolynomialResult} containing knots, coefficients of piecewise polynomials, number of intervals, degree of polynomials, dimension of spline
   */
  @Override
  public PiecewisePolynomialResult interpolate(final double[] xValues, final double[] yValues) {

    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");

    ArgChecker.isTrue(xValues.length == yValues.length, "xValues length = yValues length");
    ArgChecker.isTrue(xValues.length > 1, "Data points should be more than 1");

    final int nDataPts = xValues.length;

    for (int i = 0; i < nDataPts; ++i) {
      ArgChecker.isFalse(Double.isNaN(xValues[i]), "xData containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xValues[i]), "xData containing Infinity");
      ArgChecker.isFalse(Double.isNaN(yValues[i]), "yData containing NaN");
      ArgChecker.isFalse(Double.isInfinite(yValues[i]), "yData containing Infinity");
    }

    for (int i = 0; i < nDataPts; ++i) {
      for (int j = i + 1; j < nDataPts; ++j) {
        ArgChecker.isFalse(xValues[i] == xValues[j], "Data should be distinct");
      }
    }

    double[] xValuesSrt = new double[nDataPts];
    double[] yValuesSrt = new double[nDataPts];

    xValuesSrt = Arrays.copyOf(xValues, nDataPts);
    yValuesSrt = Arrays.copyOf(yValues, nDataPts);
    DoubleArrayMath.sortPairs(xValuesSrt, yValuesSrt);

    final DoubleMatrix2D coefMatrix = this._solver.solve(xValuesSrt, yValuesSrt);
    final int nCoefs = coefMatrix.getNumberOfColumns();

    final int nInts = this._solver.getKnotsMat1D(xValuesSrt).getNumberOfElements() - 1;
    for (int i = 0; i < nInts; ++i) {
      for (int j = 0; j < nCoefs; ++j) {
        ArgChecker.isFalse(Double.isNaN(coefMatrix.getData()[i][j]), "Too large input");
        ArgChecker.isFalse(Double.isInfinite(coefMatrix.getData()[i][j]), "Too large input");
      }
    }

    return new PiecewisePolynomialResult(this._solver.getKnotsMat1D(xValuesSrt), coefMatrix, nCoefs, 1);

  }

  /**
   * @param xValues X values of data
   * @param yValuesMatrix Y values of data, where NumberOfRow defines dimension of the spline
   * @return {@link PiecewisePolynomialResult} containing knots, coefficients of piecewise polynomials, number of intervals, degree of polynomials, dimension of spline
   */
  @Override
  public PiecewisePolynomialResult interpolate(final double[] xValues, final double[][] yValuesMatrix) {

    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValuesMatrix, "yValuesMatrix");

    ArgChecker.isTrue(xValues.length == yValuesMatrix[0].length,
        "(xValues length = yValuesMatrix's row vector length)");
    ArgChecker.isTrue(xValues.length > 1, "Data points should be more than 1");

    final int nDataPts = xValues.length;
    final int dim = yValuesMatrix.length;

    for (int i = 0; i < nDataPts; ++i) {
      ArgChecker.isFalse(Double.isNaN(xValues[i]), "xData containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xValues[i]), "xData containing Infinity");
      for (int j = 0; j < dim; ++j) {
        ArgChecker.isFalse(Double.isNaN(yValuesMatrix[j][i]), "yValuesMatrix containing NaN");
        ArgChecker.isFalse(Double.isInfinite(yValuesMatrix[j][i]), "yValuesMatrix containing Infinity");
      }
    }

    for (int k = 0; k < dim; ++k) {
      for (int i = 0; i < nDataPts; ++i) {
        for (int j = i + 1; j < nDataPts; ++j) {
          ArgChecker.isFalse(xValues[i] == xValues[j], "Data should be distinct");
        }
      }
    }

    double[] xValuesSrt = new double[nDataPts];
    double[][] yValuesMatrixSrt = new double[dim][nDataPts];

    for (int i = 0; i < dim; ++i) {
      xValuesSrt = Arrays.copyOf(xValues, nDataPts);
      double[] yValuesSrt = Arrays.copyOf(yValuesMatrix[i], nDataPts);
      DoubleArrayMath.sortPairs(xValuesSrt, yValuesSrt);

      yValuesMatrixSrt[i] = Arrays.copyOf(yValuesSrt, nDataPts);
    }

    DoubleMatrix2D[] coefMatrix = this._solver.solveMultiDim(xValuesSrt, new DoubleMatrix2D(yValuesMatrixSrt));

    final int nIntervals = coefMatrix[0].getNumberOfRows();
    final int nCoefs = coefMatrix[0].getNumberOfColumns();
    double[][] resMatrix = new double[dim * nIntervals][nCoefs];

    for (int i = 0; i < nIntervals; ++i) {
      for (int j = 0; j < dim; ++j) {
        resMatrix[dim * i + j] = coefMatrix[j].getRowVector(i).getData();
      }
    }

    for (int i = 0; i < dim * nIntervals; ++i) {
      for (int j = 0; j < nCoefs; ++j) {
        ArgChecker.isFalse(Double.isNaN(resMatrix[i][j]), "Too large input");
        ArgChecker.isFalse(Double.isInfinite(resMatrix[i][j]), "Too large input");
      }
    }

    return new PiecewisePolynomialResult(this._solver.getKnotsMat1D(xValuesSrt), new DoubleMatrix2D(resMatrix), nCoefs, dim);
  }

  @Override
  public PiecewisePolynomialResultsWithSensitivity interpolateWithSensitivity(final double[] xValues, final double[] yValues) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");

    ArgChecker.isTrue(xValues.length == yValues.length, "(xValues length = yValues length)");
    ArgChecker.isTrue(xValues.length > 1, "Data points should be more than 1");

    final int nDataPts = xValues.length;
    final int nYdata = yValues.length;

    for (int i = 0; i < nDataPts; ++i) {
      ArgChecker.isFalse(Double.isNaN(xValues[i]), "xData containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xValues[i]), "xData containing Infinity");
    }
    for (int i = 0; i < nYdata; ++i) {
      ArgChecker.isFalse(Double.isNaN(yValues[i]), "yData containing NaN");
      ArgChecker.isFalse(Double.isInfinite(yValues[i]), "yData containing Infinity");
    }

    for (int i = 0; i < nDataPts; ++i) {
      for (int j = i + 1; j < nDataPts; ++j) {
        ArgChecker.isFalse(xValues[i] == xValues[j], "Data should be distinct");
      }
    }

    final DoubleMatrix2D[] resMatrix = this._solver.solveWithSensitivity(xValues, yValues);
    final int len = resMatrix.length;
    for (int k = 0; k < len; k++) {
      DoubleMatrix2D m = resMatrix[k];
      final int rows = m.getNumberOfRows();
      final int cols = m.getNumberOfColumns();
      for (int i = 0; i < rows; ++i) {
        for (int j = 0; j < cols; ++j) {
          ArgChecker.isTrue(Doubles.isFinite(m.getEntry(i, j)), "Matrix contains a NaN or infinite");
        }
      }
    }

    final DoubleMatrix2D coefMatrix = resMatrix[0];
    final DoubleMatrix2D[] coefSenseMatrix = new DoubleMatrix2D[len - 1];
    System.arraycopy(resMatrix, 1, coefSenseMatrix, 0, len - 1);
    final int nCoefs = coefMatrix.getNumberOfColumns();

    return new PiecewisePolynomialResultsWithSensitivity(this._solver.getKnotsMat1D(xValues), coefMatrix, nCoefs, 1, coefSenseMatrix);
  }
}
