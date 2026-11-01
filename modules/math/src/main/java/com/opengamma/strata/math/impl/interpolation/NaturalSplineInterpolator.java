/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Arrays;
import java.util.stream.IntStream;

import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Natural cubic spline interpolation. 
 * <p>
 * C2 cubic spline interpolator with the natural endpoint condition, i.e., the second derivative values are zero 
 * at the first data point and the last data point. 
 * <p>
 * The latest ordered one-dimensional result is retained to reuse coefficients when node sensitivities are requested.
 * Sensitivities and matrix decompositions are not cached.
 */
public class NaturalSplineInterpolator extends PiecewisePolynomialInterpolator {

  private final CubicSplineSolver _solver;
  private volatile SplineInterpolationResultCache _resultCache;

  /**
   * Constructor.
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
   * @return {@link PiecewisePolynomialResult} containing knots, coefficients of piecewise polynomials,
   *  number of intervals, degree of polynomials, dimension of spline
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

    double[] xValuesSrt = Arrays.copyOf(xValues, nDataPts);
    double[] yValuesSrt = Arrays.copyOf(yValues, nDataPts);
    DoubleArrayMath.sortPairs(xValuesSrt, yValuesSrt);
    ArgChecker.noDuplicatesSorted(xValuesSrt, "xValues");

    final DoubleMatrix coefMatrix = this._solver.solve(xValuesSrt, yValuesSrt);
    final int nCoefs = coefMatrix.columnCount();

    final int nInts = this._solver.getKnotsMat1D(xValuesSrt).size() - 1;
    for (int i = 0; i < nInts; ++i) {
      for (int j = 0; j < nCoefs; ++j) {
        ArgChecker.isFalse(Double.isNaN(coefMatrix.get(i, j)), "Too large input");
        ArgChecker.isFalse(Double.isInfinite(coefMatrix.get(i, j)), "Too large input");
      }
    }

    PiecewisePolynomialResult result = new PiecewisePolynomialResult(
        this._solver.getKnotsMat1D(xValuesSrt), coefMatrix, nCoefs, 1);
    // Only built-in solvers can bypass solveWithSensitivity; custom solver hooks must still be invoked.
    _resultCache = _solver.getClass() == CubicSplineNaturalSolver.class && Arrays.equals(xValues, xValuesSrt) ?
        new SplineInterpolationResultCache(xValuesSrt, yValuesSrt, result) : null;
    return result;

  }

  /**
   * @param xValues X values of data
   * @param yValuesMatrix Y values of data, where NumberOfRow defines dimension of the spline
   * @return {@link PiecewisePolynomialResult} containing knots, coefficients of piecewise polynomials,
   *  number of intervals, degree of polynomials, dimension of spline
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

    double[] xValuesSrt = Arrays.copyOf(xValues, nDataPts);
    int[] sortedPositions = IntStream.range(0, nDataPts).toArray();
    DoubleArrayMath.sortPairs(xValuesSrt, sortedPositions);
    ArgChecker.noDuplicatesSorted(xValuesSrt, "xValues");

    double[][] yValuesMatrixSrt = new double[dim][nDataPts];
    for (int i = 0; i < dim; ++i) {
      yValuesMatrixSrt[i] = DoubleArrayMath.reorderedCopy(yValuesMatrix[i], sortedPositions);
    }

    DoubleMatrix[] coefMatrix = this._solver.solveMultiDim(xValuesSrt, DoubleMatrix.copyOf(yValuesMatrixSrt));

    final int nIntervals = coefMatrix[0].rowCount();
    final int nCoefs = coefMatrix[0].columnCount();
    double[][] resMatrix = new double[dim * nIntervals][nCoefs];

    for (int i = 0; i < nIntervals; ++i) {
      for (int j = 0; j < dim; ++j) {
        resMatrix[dim * i + j] = coefMatrix[j].row(i).toArray();
      }
    }

    for (int i = 0; i < dim * nIntervals; ++i) {
      for (int j = 0; j < nCoefs; ++j) {
        ArgChecker.isFalse(Double.isNaN(resMatrix[i][j]), "Too large input");
        ArgChecker.isFalse(Double.isInfinite(resMatrix[i][j]), "Too large input");
      }
    }

    return new PiecewisePolynomialResult(this._solver.getKnotsMat1D(xValuesSrt), DoubleMatrix.copyOf(resMatrix), nCoefs, dim);
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

    ArgChecker.noDuplicates(xValues, "xValues");

    SplineInterpolationResultCache cached = _resultCache;
    if (cached != null && cached.matches(xValues, yValues)) {
      return cached.withSensitivity(_solver.solveCoefficientSensitivity(xValues, yValues));
    }

    final DoubleMatrix[] resMatrix = this._solver.solveWithSensitivity(xValues, yValues);
    final int len = resMatrix.length;
    for (int k = 0; k < len; k++) {
      DoubleMatrix m = resMatrix[k];
      final int rows = m.rowCount();
      final int cols = m.columnCount();
      for (int i = 0; i < rows; ++i) {
        for (int j = 0; j < cols; ++j) {
          ArgChecker.isTrue(Doubles.isFinite(m.get(i, j)), "Matrix contains a NaN or infinite");
        }
      }
    }

    final DoubleMatrix coefMatrix = resMatrix[0];
    final DoubleMatrix[] coefSenseMatrix = new DoubleMatrix[len - 1];
    System.arraycopy(resMatrix, 1, coefSenseMatrix, 0, len - 1);
    final int nCoefs = coefMatrix.columnCount();

    return new PiecewisePolynomialResultsWithSensitivity(this._solver.getKnotsMat1D(xValues), coefMatrix, nCoefs, 1, coefSenseMatrix);
  }
}
