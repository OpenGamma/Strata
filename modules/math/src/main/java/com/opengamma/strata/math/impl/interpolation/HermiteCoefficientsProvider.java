/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Arrays;

import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * Hermite interpolation is determined if one specifies first derivatives for a cubic interpolant and first and second derivatives for a quintic interpolant
 */
public class HermiteCoefficientsProvider {

  /**
   * @param values (yValues_i)
   * @param intervals (xValues_{i+1} - xValues_{i})
   * @param slopes (yValues_{i+1} - yValues_{i})/(xValues_{i+1} - xValues_{i})
   * @param first First derivatives at xValues_i
   * @return Coefficient matrix whose i-th row vector is { a_n, a_{n-1}, ...} for the i-th interval,
   * where a_n, a_{n-1},... are coefficients of f(x) = a_n (x-x_i)^n + a_{n-1} (x-x_i)^{n-1} + .... with n=3
   */
  public double[][] solve(final double[] values, final double[] intervals, final double[] slopes, final double[] first) {
    final int nInt = intervals.length;
    final double[][] res = new double[nInt][4];
    for (int i = 0; i < nInt; ++i) {
      Arrays.fill(res[i], 0.);
    }

    for (int i = 0; i < nInt; ++i) {
      res[i][3] = values[i];
      res[i][2] = first[i];
      res[i][1] = (3. * slopes[i] - first[i + 1] - 2. * first[i]) / intervals[i];
      res[i][0] = -(2. * slopes[i] - first[i + 1] - first[i]) / intervals[i] / intervals[i];
    }

    return res;
  }

  /**
   * @param values Y values of data
   * @param intervals (xValues_{i+1} - xValues_{i})
   * @param slopes (yValues_{i+1} - yValues_{i})/(xValues_{i+1} - xValues_{i})
   * @param slopeSensitivity Derivative values of slope with respect to yValues
   * @param firstWithSensitivity First derivative values at xValues_i and their yValues dependencies
   * @return Coefficient matrix and its node dependencies
   */
  public DoubleMatrix2D[] solveWithSensitivity(final double[] values, final double[] intervals, final double[] slopes, final double[][] slopeSensitivity, final DoubleMatrix1D[] firstWithSensitivity) {
    final int nData = values.length;
    final double[] first = firstWithSensitivity[0].getData();
    final DoubleMatrix2D[] res = new DoubleMatrix2D[nData];

    final double[][] coef = solve(values, intervals, slopes, first);
    res[0] = new DoubleMatrix2D(coef);

    for (int i = 0; i < nData - 1; ++i) {
      final double[][] coefSense = new double[4][nData];
      Arrays.fill(coefSense[3], 0.);
      coefSense[3][i] = 1.;
      for (int k = 0; k < nData; ++k) {
        coefSense[0][k] = -(2. * slopeSensitivity[i][k] - firstWithSensitivity[i + 2].getData()[k] - firstWithSensitivity[i + 1].getData()[k]) / intervals[i] / intervals[i];
        coefSense[1][k] = (3. * slopeSensitivity[i][k] - firstWithSensitivity[i + 2].getData()[k] - 2. * firstWithSensitivity[i + 1].getData()[k]) / intervals[i];
        coefSense[2][k] = firstWithSensitivity[i + 1].getData()[k];
      }
      res[i + 1] = new DoubleMatrix2D(coefSense);
    }

    return res;
  }

  /**
   * @param values (yValues_i)
   * @param intervals (xValues_{i+1} - xValues_{i})
   * @param slopes (yValues_{i+1} - yValues_{i})/(xValues_{i+1} - xValues_{i})
   * @param first First derivatives at xValues_i
   * @param second Second derivatives at xValues_i
   * @return Coefficient matrix whose i-th row vector is { a_n, a_{n-1}, ...} for the i-th interval,
   * where a_n, a_{n-1},... are coefficients of f(x) = a_n (x-x_i)^n + a_{n-1} (x-x_i)^{n-1} + .... with n=5
   */
  public double[][] solve(final double[] values, final double[] intervals, final double[] slopes, final double[] first, final double[] second) {
    final int nInt = intervals.length;
    final double[][] res = new double[nInt][6];
    for (int i = 0; i < nInt; ++i) {
      Arrays.fill(res[i], 0.);
    }

    for (int i = 0; i < nInt; ++i) {
      res[i][5] = values[i];
      res[i][4] = first[i];
      res[i][3] = 0.5 * second[i];
      res[i][2] = 0.5 * (second[i + 1] - 3. * second[i]) / intervals[i] + 2. * (5. * slopes[i] - 3. * first[i] - 2. * first[i + 1]) / intervals[i] / intervals[i];
      res[i][1] = 0.5 * (3. * second[i] - 2. * second[i + 1]) / intervals[i] / intervals[i] + (8. * first[i] + 7. * first[i + 1] - 15. * slopes[i]) / intervals[i] / intervals[i] / intervals[i];
      res[i][0] = 0.5 * (second[i + 1] - second[i]) / intervals[i] / intervals[i] / intervals[i] + 3. * (2. * slopes[i] - first[i + 1] - first[i]) / intervals[i] / intervals[i] / intervals[i] /
          intervals[i];
    }

    return res;
  }

  /**
   *
   * @param values (yValues_i)
   * @param intervals (xValues_{i+1} - xValues_{i})
   * @param slopes (yValues_{i+1} - yValues_{i})/(xValues_{i+1} - xValues_{i})
   * @param slopeSensitivity Derivative values of slope with respect to yValues
   * @param firstWithSensitivity First derivative values at xValues_i and their yValues dependencies
   * @param secondWithSensitivity Second derivative values at xValues_i and their yValues dependencies
   * @return Coefficient matrix and its node dependencies
   */
  public DoubleMatrix2D[] solveWithSensitivity(final double[] values, final double[] intervals, final double[] slopes, final double[][] slopeSensitivity, final DoubleMatrix1D[] firstWithSensitivity,
      final DoubleMatrix1D[] secondWithSensitivity) {
    final int nData = values.length;
    final double[] first = firstWithSensitivity[0].getData();
    final double[] second = secondWithSensitivity[0].getData();
    final DoubleMatrix2D[] res = new DoubleMatrix2D[nData];

    final double[][] coef = solve(values, intervals, slopes, first, second);
    res[0] = new DoubleMatrix2D(coef);

    for (int i = 0; i < nData - 1; ++i) {
      final double[][] coefSense = new double[6][nData];
      Arrays.fill(coefSense[5], 0.);
      coefSense[5][i] = 1.;
      for (int k = 0; k < nData; ++k) {
        coefSense[0][k] = 0.5 * (secondWithSensitivity[i + 2].getData()[k] - secondWithSensitivity[i + 1].getData()[k]) / intervals[i] / intervals[i] / intervals[i] + 3. *
            (2. * slopeSensitivity[i][k] - firstWithSensitivity[i + 2].getData()[k] - firstWithSensitivity[i + 1].getData()[k]) / intervals[i] / intervals[i] / intervals[i] / intervals[i];
        coefSense[1][k] = 0.5 * (3. * secondWithSensitivity[i + 1].getData()[k] - 2. * secondWithSensitivity[i + 2].getData()[k]) / intervals[i] / intervals[i] +
            (8. * firstWithSensitivity[i + 1].getData()[k] + 7. * firstWithSensitivity[i + 2].getData()[k] - 15. * slopeSensitivity[i][k]) / intervals[i] / intervals[i] / intervals[i];
        coefSense[2][k] = 0.5 * (secondWithSensitivity[i + 2].getData()[k] - 3. * secondWithSensitivity[i + 1].getData()[k]) / intervals[i] + 2. *
            (5. * slopeSensitivity[i][k] - 3. * firstWithSensitivity[i + 1].getData()[k] - 2. * firstWithSensitivity[i + 2].getData()[k]) / intervals[i] / intervals[i];
        coefSense[3][k] = 0.5 * secondWithSensitivity[i + 1].getData()[k];
        coefSense[4][k] = firstWithSensitivity[i + 1].getData()[k];
      }
      res[i + 1] = new DoubleMatrix2D(coefSense);
    }

    return res;
  }

  /**
   * @param xValues The x values
   * @return Intervals of xValues, ( xValues_{i+1} - xValues_i )
   */
  public double[] intervalsCalculator(final double[] xValues) {

    final int nDataPts = xValues.length;
    final double[] intervals = new double[nDataPts - 1];

    for (int i = 0; i < nDataPts - 1; ++i) {
      intervals[i] = xValues[i + 1] - xValues[i];
    }

    return intervals;
  }

  /**
   * @param yValues Y values of data
   * @param intervals Intervals of x data
   * @return ( yValues_{i+1} - yValues_i )/( xValues_{i+1} - xValues_i )
   */
  public double[] slopesCalculator(final double[] yValues, final double[] intervals) {

    final int nDataPts = yValues.length;
    final double[] slopes = new double[nDataPts - 1];

    for (int i = 0; i < nDataPts - 1; ++i) {
      slopes[i] = (yValues[i + 1] - yValues[i]) / intervals[i];
    }

    return slopes;
  }

  /**
   * Derivative values of slopes_i with respect to yValues_j, s_{ij}
   * @param intervals Intervals of x data
   * @return The matrix s_{ij}
   */
  public double[][] slopeSensitivityCalculator(final double[] intervals) {
    final int nDataPts = intervals.length + 1;
    final double[][] res = new double[nDataPts - 1][nDataPts];

    for (int i = 0; i < nDataPts - 1; ++i) {
      Arrays.fill(res[i], 0.);
      res[i][i] = -1. / intervals[i];
      res[i][i + 1] = 1. / intervals[i];
    }
    return res;
  }

  /**
   * @param ints1 The first interval
   * @param ints2 The second interval
   * @param slope1 The first gradient
   * @param slope2 The second gradient
   * @return Value of derivative at each endpoint
   */
  public double endpointDerivatives(final double ints1, final double ints2, final double slope1, final double slope2) {
    final double val = (2. * ints1 + ints2) * slope1 / (ints1 + ints2) - ints1 * slope2 / (ints1 + ints2);

    if (Math.signum(val) != Math.signum(slope1)) {
      return 0.;
    }
    if (Math.signum(slope1) != Math.signum(slope2) && Math.abs(val) > 3. * Math.abs(slope1)) {
      return 3. * slope1;
    }
    return val;
  }
}
