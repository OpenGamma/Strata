/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * C1 cubic interpolation preserving monotonicity based on 
 * Fritsch, F. N.; Carlson, R. E. (1980) 
 * "Monotone Piecewise Cubic Interpolation", SIAM Journal on Numerical Analysis 17 (2): 238–246. 
 * Fritsch, F. N. and Butland, J. (1984)
 * "A method for constructing local monotone piecewise cubic interpolants", SIAM Journal on Scientific and Statistical Computing 5 (2): 300-304.
 */
public class PiecewiseCubicHermiteSplineInterpolator extends PiecewisePolynomialInterpolator {

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

    for (int i = 1; i < nDataPts; ++i) {
      ArgChecker.isFalse(xValuesSrt[i - 1] == xValuesSrt[i], "xValues should be distinct");
    }

    final DoubleMatrix coefMatrix = solve(xValuesSrt, yValuesSrt);

    for (int i = 0; i < coefMatrix.rowCount(); ++i) {
      for (int j = 0; j < coefMatrix.columnCount(); ++j) {
        ArgChecker.isFalse(Double.isNaN(coefMatrix.get(i, j)), "Too large input");
        ArgChecker.isFalse(Double.isInfinite(coefMatrix.get(i, j)), "Too large input");
      }
    }

    return new PiecewisePolynomialResult(DoubleArray.copyOf(xValuesSrt), coefMatrix, coefMatrix.columnCount(), 1);
  }

  @Override
  public PiecewisePolynomialResult interpolate(final double[] xValues, final double[][] yValuesMatrix) {

    ArgChecker.notNull(xValues, "xValues");
    ArgChecker.notNull(yValuesMatrix, "yValuesMatrix");

    ArgChecker.isTrue(xValues.length == yValuesMatrix[0].length, "(xValues length = yValuesMatrix's row vector length)");
    ArgChecker.isTrue(xValues.length > 1, "Data points should be more than 1");

    final int nDataPts = xValues.length;
    final int dim = yValuesMatrix.length;

    for (int i = 0; i < nDataPts; ++i) {
      ArgChecker.isFalse(Double.isNaN(xValues[i]), "xValues containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xValues[i]), "xValues containing Infinity");
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

      coefMatrix[i] = solve(xValuesSrt, yValuesSrt);
    }

    final int nIntervals = coefMatrix[0].rowCount();
    final int nCoefs = coefMatrix[0].columnCount();
    double[][] resMatrix = new double[dim * nIntervals][nCoefs];

    for (int i = 0; i < nIntervals; ++i) {
      for (int j = 0; j < dim; ++j) {
        resMatrix[dim * i + j] = coefMatrix[j].row(i).toArray();
      }
    }

    for (int i = 0; i < (nIntervals * dim); ++i) {
      for (int j = 0; j < nCoefs; ++j) {
        ArgChecker.isFalse(Double.isNaN(resMatrix[i][j]), "Too large input");
        ArgChecker.isFalse(Double.isInfinite(resMatrix[i][j]), "Too large input");
      }
    }

    return new PiecewisePolynomialResult(DoubleArray.copyOf(xValuesSrt), DoubleMatrix.copyOf(resMatrix), nCoefs, dim);
  }

  @Override
  public PiecewisePolynomialResultsWithSensitivity interpolateWithSensitivity(final double[] xValues, final double[] yValues) {
    final PiecewiseCubicHermiteSplineInterpolatorWithSensitivity interp = new PiecewiseCubicHermiteSplineInterpolatorWithSensitivity();
    return interp.interpolateWithSensitivity(xValues, yValues);
  }

  /**
   * @param xValues X values of data
   * @param yValues Y values of data
   * @return Coefficient matrix whose i-th row vector is {a3, a2, a1, a0} of f(x) = a3 * (x-x_i)^3 + a2 * (x-x_i)^2 +... for the i-th interval
   */
  private DoubleMatrix solve(final double[] xValues, final double[] yValues) {

    final int nDataPts = xValues.length;

    double[][] res = new double[nDataPts - 1][4];
    double[] intervals = new double[nDataPts - 1];
    double[] grads = new double[nDataPts - 1];

    for (int i = 0; i < nDataPts - 1; ++i) {
      intervals[i] = xValues[i + 1] - xValues[i];
      grads[i] = (yValues[i + 1] - yValues[i]) / intervals[i];
    }

    if (nDataPts == 2) {
      res[0][2] = grads[0];
      res[0][3] = yValues[0];
    } else {
      double[] derivatives = slopeFinder(intervals, grads);
      for (int i = 0; i < nDataPts - 1; ++i) {
        res[i][0] = (derivatives[i] - 2 * grads[i] + derivatives[i + 1]) / intervals[i] / intervals[i];
        res[i][1] = (3 * grads[i] - 2. * derivatives[i] - derivatives[i + 1]) / intervals[i];
        res[i][2] = derivatives[i];
        res[i][3] = yValues[i];
      }
    }
    return DoubleMatrix.copyOf(res);
  }

  /**
   * @param intervals 
   * @param grads 
   * @return A set of the first derivatives at knots
   */
  private double[] slopeFinder(final double[] intervals, final double[] grads) {
    final int nInts = intervals.length;
    double[] res = new double[nInts + 1];

    res[0] = endpointSlope(intervals[0], intervals[1], grads[0], grads[1]);
    res[nInts] = endpointSlope(intervals[nInts - 1], intervals[nInts - 2], grads[nInts - 1], grads[nInts - 2]);

    for (int i = 1; i < nInts; ++i) {
      if (grads[i] * grads[i - 1] <= 0) {
        res[i] = 0.;
      } else {
        final double den1 = 2. * intervals[i] + intervals[i - 1];
        final double den2 = intervals[i] + 2. * intervals[i - 1];
        res[i] = (den1 + den2) / (den1 / grads[i - 1] + den2 / grads[i]);
      }
    }

    return res;
  }

  private double endpointSlope(final double ints1, final double ints2, final double grads1, final double grads2) {
    final double val = ((2. * ints1 + ints2) * grads1 - ints1 * grads2) / (ints1 + ints2);

    if (Math.signum(val) != Math.signum(grads1)) {
      return 0.;
    } else if (Math.signum(grads1) != Math.signum(grads2) && Math.abs(val) > 3. * Math.abs(grads1)) {
      return 3. * grads1;
    }
    return val;
  }

}
