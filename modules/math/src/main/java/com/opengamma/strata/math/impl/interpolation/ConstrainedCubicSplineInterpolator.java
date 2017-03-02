/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Arrays;

import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Cubic spline interpolation based on
 * C.J.C. Kruger, "Constrained Cubic Spline Interpolation for Chemical Engineering Applications," 2002
 */
public class ConstrainedCubicSplineInterpolator extends PiecewisePolynomialInterpolator {
  private static final double ERROR = 1.e-13;
  private final HermiteCoefficientsProvider _solver = new HermiteCoefficientsProvider();

  @Override
  public PiecewisePolynomialResult interpolate(final double[] xValues, final double[] yValues) {

    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");

    ArgChecker.isTrue(xValues.length == yValues.length, "(xValues length = yValues length) should be true");
    ArgChecker.isTrue(xValues.length > 1, "Data points should be >= 2");

    final int nDataPts = xValues.length;

    for (int i = 0; i < nDataPts; ++i) {
      ArgChecker.isFalse(Double.isNaN(xValues[i]), "xValues containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xValues[i]), "xValues containing Infinity");
      ArgChecker.isFalse(Double.isNaN(yValues[i]), "yValues containing NaN");
      ArgChecker.isFalse(Double.isInfinite(yValues[i]), "yValues containing Infinity");
    }

    for (int i = 0; i < nDataPts - 1; ++i) {
      for (int j = i + 1; j < nDataPts; ++j) {
        ArgChecker.isFalse(xValues[i] == xValues[j], "xValues should be distinct");
      }
    }

    double[] xValuesSrt = Arrays.copyOf(xValues, nDataPts);
    double[] yValuesSrt = Arrays.copyOf(yValues, nDataPts);
    DoubleArrayMath.sortPairs(xValuesSrt, yValuesSrt);

    final double[] intervals = _solver.intervalsCalculator(xValuesSrt);
    final double[] slopes = _solver.slopesCalculator(yValuesSrt, intervals);
    final double[] first = firstDerivativeCalculator(slopes);
    final double[][] coefs = _solver.solve(yValuesSrt, intervals, slopes, first);

    for (int i = 0; i < nDataPts - 1; ++i) {
      double ref = 0.;
      for (int j = 0; j < 4; ++j) {
        ref += coefs[i][j] * Math.pow(intervals[i], 3 - j);
        ArgChecker.isFalse(Double.isNaN(coefs[i][j]), "Too large input");
        ArgChecker.isFalse(Double.isInfinite(coefs[i][j]), "Too large input");
      }
      final double bound = Math.max(Math.abs(ref) + Math.abs(yValuesSrt[i + 1]), 1.e-1);
      ArgChecker.isTrue(Math.abs(ref - yValuesSrt[i + 1]) < ERROR * bound, "Input is too large/small or data points are too close");
    }

    return new PiecewisePolynomialResult(DoubleArray.copyOf(xValuesSrt), DoubleMatrix.copyOf(coefs), 4, 1);
  }

  @Override
  public PiecewisePolynomialResult interpolate(final double[] xValues, final double[][] yValuesMatrix) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValuesMatrix, "yValuesMatrix");

    ArgChecker.isTrue(xValues.length == yValuesMatrix[0].length, "(xValues length = yValuesMatrix's row vector length) should be true");
    ArgChecker.isTrue(xValues.length > 1, "Data points should be >= 2");

    final int nDataPts = xValues.length;
    final int yValuesLen = yValuesMatrix[0].length;
    final int dim = yValuesMatrix.length;

    for (int i = 0; i < nDataPts; ++i) {
      ArgChecker.isFalse(Double.isNaN(xValues[i]), "xValues containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xValues[i]), "xValues containing Infinity");
    }
    for (int i = 0; i < yValuesLen; ++i) {
      for (int j = 0; j < dim; ++j) {
        ArgChecker.isFalse(Double.isNaN(yValuesMatrix[j][i]), "yValuesMatrix containing NaN");
        ArgChecker.isFalse(Double.isInfinite(yValuesMatrix[j][i]), "yValuesMatrix containing Infinity");
      }
    }
    for (int i = 0; i < nDataPts; ++i) {
      for (int j = i + 1; j < nDataPts; ++j) {
        ArgChecker.isFalse(xValues[i] == xValues[j], "xValues should be distinct");
      }
    }

    double[] xValuesSrt = new double[nDataPts];
    DoubleMatrix[] coefMatrix = new DoubleMatrix[dim];

    for (int i = 0; i < dim; ++i) {
      xValuesSrt = Arrays.copyOf(xValues, nDataPts);
      double[] yValuesSrt = Arrays.copyOf(yValuesMatrix[i], nDataPts);
      DoubleArrayMath.sortPairs(xValuesSrt, yValuesSrt);

      final double[] intervals = _solver.intervalsCalculator(xValuesSrt);
      final double[] slopes = _solver.slopesCalculator(yValuesSrt, intervals);
      final double[] first = firstDerivativeCalculator(slopes);

      coefMatrix[i] = DoubleMatrix.copyOf(_solver.solve(yValuesSrt, intervals, slopes, first));

      for (int k = 0; k < intervals.length; ++k) {
        double ref = 0.;
        for (int j = 0; j < 4; ++j) {
          ref += coefMatrix[i].get(k, j) * Math.pow(intervals[k], 3 - j);
          ArgChecker.isFalse(Double.isNaN(coefMatrix[i].get(k, j)), "Too large input");
          ArgChecker.isFalse(Double.isInfinite(coefMatrix[i].get(k, j)), "Too large input");
        }
        final double bound = Math.max(Math.abs(ref) + Math.abs(yValuesSrt[k + 1]), 1.e-1);
        ArgChecker.isTrue(Math.abs(ref - yValuesSrt[k + 1]) < ERROR * bound, "Input is too large/small or data points are too close");
      }
    }

    final int nIntervals = coefMatrix[0].rowCount();
    final int nCoefs = coefMatrix[0].columnCount();
    double[][] resMatrix = new double[dim * nIntervals][nCoefs];

    for (int i = 0; i < nIntervals; ++i) {
      for (int j = 0; j < dim; ++j) {
        resMatrix[dim * i + j] = coefMatrix[j].row(i).toArray();
      }
    }

    return new PiecewisePolynomialResult(DoubleArray.copyOf(xValuesSrt), DoubleMatrix.copyOf(resMatrix), nCoefs, dim);
  }

  @Override
  public PiecewisePolynomialResultsWithSensitivity interpolateWithSensitivity(final double[] xValues, final double[] yValues) {
    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValues, "yValues");

    ArgChecker.isTrue(xValues.length == yValues.length, "(xValues length = yValues length) should be true");
    ArgChecker.isTrue(xValues.length > 1, "Data points should be >= 2");

    final int nDataPts = xValues.length;

    for (int i = 0; i < nDataPts; ++i) {
      ArgChecker.isFalse(Double.isNaN(xValues[i]), "xValues containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xValues[i]), "xValues containing Infinity");
      ArgChecker.isFalse(Double.isNaN(yValues[i]), "yValues containing NaN");
      ArgChecker.isFalse(Double.isInfinite(yValues[i]), "yValues containing Infinity");
    }

    for (int i = 0; i < nDataPts - 1; ++i) {
      for (int j = i + 1; j < nDataPts; ++j) {
        ArgChecker.isFalse(xValues[i] == xValues[j], "xValues should be distinct");
      }
    }

    final double[] intervals = _solver.intervalsCalculator(xValues);
    final double[] slopes = _solver.slopesCalculator(yValues, intervals);
    final double[][] slopeSensitivity = _solver.slopeSensitivityCalculator(intervals);
    final DoubleArray[] firstWithSensitivity = firstDerivativeWithSensitivityCalculator(slopes, slopeSensitivity);
    final DoubleMatrix[] resMatrix = _solver.solveWithSensitivity(yValues, intervals, slopes, slopeSensitivity, firstWithSensitivity);

    for (int k = 0; k < nDataPts; k++) {
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
    for (int i = 0; i < nDataPts - 1; ++i) {
      double ref = 0.;
      for (int j = 0; j < 4; ++j) {
        ref += coefMatrix.get(i, j) * Math.pow(intervals[i], 3 - j);
      }
      final double bound = Math.max(Math.abs(ref) + Math.abs(yValues[i + 1]), 1.e-1);
      ArgChecker.isTrue(Math.abs(ref - yValues[i + 1]) < ERROR * bound, "Input is too large/small or data points are too close");
    }
    final DoubleMatrix[] coefSenseMatrix = new DoubleMatrix[nDataPts - 1];
    System.arraycopy(resMatrix, 1, coefSenseMatrix, 0, nDataPts - 1);
    final int nCoefs = coefMatrix.columnCount();

    return new PiecewisePolynomialResultsWithSensitivity(DoubleArray.copyOf(xValues), coefMatrix, nCoefs, 1, coefSenseMatrix);
  }

  private double[] firstDerivativeCalculator(final double[] slopes) {
    final int nData = slopes.length + 1;
    double[] res = new double[nData];

    for (int i = 1; i < nData - 1; ++i) {
      res[i] = Math.signum(slopes[i - 1]) * Math.signum(slopes[i]) <= 0. ? 0. : 2. * slopes[i] * slopes[i - 1] / (slopes[i] + slopes[i - 1]);
    }
    res[0] = 1.5 * slopes[0] - 0.5 * res[1];
    res[nData - 1] = 1.5 * slopes[nData - 2] - 0.5 * res[nData - 2];

    return res;
  }

  private DoubleArray[] firstDerivativeWithSensitivityCalculator(final double[] slopes, final double[][] slopeSensitivity) {
    final int nData = slopes.length + 1;
    final double[] first = new double[nData];
    final double[][] sense = new double[nData][nData];
    DoubleArray[] res = new DoubleArray[nData + 1];

    for (int i = 1; i < nData - 1; ++i) {
      final double sign = Math.signum(slopes[i - 1]) * Math.signum(slopes[i]);
      if (sign <= 0.) {
        first[i] = 0.;
      } else {
        first[i] = 2. * slopes[i] * slopes[i - 1] / (slopes[i] + slopes[i - 1]);
      }
      if (sign < 0.) {
        Arrays.fill(sense[i], 0.);
      } else {
        for (int k = 0; k < nData; ++k) {
          if (Math.abs(slopes[i] + slopes[i - 1]) == 0.) {
            Arrays.fill(sense[i], 0.);
          } else {
            if (sign == 0.) {
              sense[i][k] = (slopes[i] * slopes[i] * slopeSensitivity[i - 1][k] + slopes[i - 1] * slopes[i - 1] * slopeSensitivity[i][k]) / (slopes[i] + slopes[i - 1]) /
                  (slopes[i] + slopes[i - 1]);
            } else {
              sense[i][k] = 2. * (slopes[i] * slopes[i] * slopeSensitivity[i - 1][k] + slopes[i - 1] * slopes[i - 1] * slopeSensitivity[i][k]) / (slopes[i] + slopes[i - 1]) /
                  (slopes[i] + slopes[i - 1]);
            }
          }
        }
      }
      res[i + 1] = DoubleArray.copyOf(sense[i]);
    }
    first[0] = 1.5 * slopes[0] - 0.5 * first[1];
    first[nData - 1] = 1.5 * slopes[nData - 2] - 0.5 * first[nData - 2];
    res[0] = DoubleArray.copyOf(first);
    for (int k = 0; k < nData; ++k) {
      sense[0][k] = 1.5 * slopeSensitivity[0][k] - 0.5 * sense[1][k];
      sense[nData - 1][k] = 1.5 * slopeSensitivity[nData - 2][k] - 0.5 * sense[nData - 2][k];
    }
    res[1] = DoubleArray.copyOf(sense[0]);
    res[nData] = DoubleArray.copyOf(sense[nData - 1]);

    return res;
  }
}
