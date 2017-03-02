/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Arrays;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

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
  public double[][] solve(double[] values, double[] intervals, double[] slopes, double[] first) {
    int nInt = intervals.length;
    double[][] res = new double[nInt][4];
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
  public DoubleMatrix[] solveWithSensitivity(
      double[] values,
      double[] intervals,
      double[] slopes,
      double[][] slopeSensitivity,
      DoubleArray[] firstWithSensitivity) {

    int nData = values.length;
    double[] first = firstWithSensitivity[0].toArray();
    DoubleMatrix[] res = new DoubleMatrix[nData];

    double[][] coef = solve(values, intervals, slopes, first);
    res[0] = DoubleMatrix.copyOf(coef);

    for (int i = 0; i < nData - 1; ++i) {
      double[][] coefSense = new double[4][nData];
      Arrays.fill(coefSense[3], 0.);
      coefSense[3][i] = 1.;
      for (int k = 0; k < nData; ++k) {
        coefSense[0][k] =
            -(2. * slopeSensitivity[i][k] - firstWithSensitivity[i + 2].get(k) - firstWithSensitivity[i + 1].get(k)) /
                intervals[i] / intervals[i];
        coefSense[1][k] =
            (3. * slopeSensitivity[i][k] - firstWithSensitivity[i + 2].get(k) - 2. * firstWithSensitivity[i + 1].get(k)) /
                intervals[i];
        coefSense[2][k] = firstWithSensitivity[i + 1].get(k);
      }
      res[i + 1] = DoubleMatrix.copyOf(coefSense);
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
  public double[][] solve(double[] values, double[] intervals, double[] slopes, double[] first, double[] second) {
    int nInt = intervals.length;
    double[][] res = new double[nInt][6];
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
  public DoubleMatrix[] solveWithSensitivity(
      double[] values,
      double[] intervals,
      double[] slopes,
      double[][] slopeSensitivity,
      DoubleArray[] firstWithSensitivity,
      DoubleArray[] secondWithSensitivity) {

    int nData = values.length;
    double[] first = firstWithSensitivity[0].toArray();
    double[] second = secondWithSensitivity[0].toArray();
    DoubleMatrix[] res = new DoubleMatrix[nData];

    double[][] coef = solve(values, intervals, slopes, first, second);
    res[0] = DoubleMatrix.copyOf(coef);

    for (int i = 0; i < nData - 1; ++i) {
      double interval = intervals[i];
      double[][] coefSense = new double[6][nData];
      Arrays.fill(coefSense[5], 0.);
      coefSense[5][i] = 1.;
      for (int k = 0; k < nData; ++k) {
        double cs0b = 2d * slopeSensitivity[i][k] - firstWithSensitivity[i + 2].get(k) - firstWithSensitivity[i + 1].get(k);
        double cs0a = secondWithSensitivity[i + 2].get(k) - secondWithSensitivity[i + 1].get(k);
        coefSense[0][k] = 0.5 * cs0a / interval / interval / interval + 3d * cs0b / interval / interval / interval / interval;

        double cs1a = 3d * secondWithSensitivity[i + 1].get(k) - 2d * secondWithSensitivity[i + 2].get(k);
        double cs1b =
            8d * firstWithSensitivity[i + 1].get(k) + 7d * firstWithSensitivity[i + 2].get(k) - 15d * slopeSensitivity[i][k];
        coefSense[1][k] = 0.5 * cs1a / interval / interval + cs1b / interval / interval / interval;

        double cs2a = secondWithSensitivity[i + 2].get(k) - 3d * secondWithSensitivity[i + 1].get(k);
        double cs2b = 5d * slopeSensitivity[i][k] - 3d * firstWithSensitivity[i + 1].get(k) - 2d * firstWithSensitivity[i + 2].get(k);
        coefSense[2][k] = 0.5 * cs2a / interval + 2. * cs2b / interval / interval;

        coefSense[3][k] = 0.5 * secondWithSensitivity[i + 1].get(k);
        coefSense[4][k] = firstWithSensitivity[i + 1].get(k);
      }
      res[i + 1] = DoubleMatrix.copyOf(coefSense);
    }

    return res;
  }

  /**
   * @param xValues The x values
   * @return Intervals of xValues, ( xValues_{i+1} - xValues_i )
   */
  public double[] intervalsCalculator(double[] xValues) {

    int nDataPts = xValues.length;
    double[] intervals = new double[nDataPts - 1];

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
  public double[] slopesCalculator(double[] yValues, double[] intervals) {

    int nDataPts = yValues.length;
    double[] slopes = new double[nDataPts - 1];

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
  public double[][] slopeSensitivityCalculator(double[] intervals) {
    int nDataPts = intervals.length + 1;
    double[][] res = new double[nDataPts - 1][nDataPts];

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
  public double endpointDerivatives(double ints1, double ints2, double slope1, double slope2) {
    double val = (2. * ints1 + ints2) * slope1 / (ints1 + ints2) - ints1 * slope2 / (ints1 + ints2);

    if (Math.signum(val) != Math.signum(slope1)) {
      return 0.;
    }
    if (Math.signum(slope1) != Math.signum(slope2) && Math.abs(val) > 3. * Math.abs(slope1)) {
      return 3. * slope1;
    }
    return val;
  }
}
