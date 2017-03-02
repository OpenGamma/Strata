/*
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
 * Interpolate consecutive two points by a straight line. 
 */
public class LinearInterpolator extends PiecewisePolynomialInterpolator {

  private static final double ERROR = 1.e-13;

  @Override
  public PiecewisePolynomialResult interpolate(double[] xValues, double[] yValues) {
    ArgChecker.notEmpty(xValues, "xValues");
    ArgChecker.notEmpty(yValues, "yValues");
    int nDataPts = xValues.length;
    ArgChecker.isTrue(nDataPts > 1, "at least two data points required");
    ArgChecker.isTrue(nDataPts == yValues.length, "xValues length = yValues length");
    for (int i = 0; i < nDataPts; ++i) {
      ArgChecker.isFalse(Double.isNaN(xValues[i]), "xData containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xValues[i]), "xData containing Infinity");
      ArgChecker.isFalse(Double.isNaN(yValues[i]), "yData containing NaN");
      ArgChecker.isFalse(Double.isInfinite(yValues[i]), "yData containing Infinity");
    }
    if (nDataPts == 1) {
      return new PiecewisePolynomialResult(DoubleArray.copyOf(xValues), DoubleMatrix.filled(1, 1, yValues[0]), 1, 1);
    }

    for (int i = 0; i < nDataPts; ++i) {
      for (int j = i + 1; j < nDataPts; ++j) {
        ArgChecker.isFalse(xValues[i] == xValues[j], "xValues should be distinct");
      }
    }

    double[] xValuesSrt = Arrays.copyOf(xValues, nDataPts);
    double[] yValuesSrt = Arrays.copyOf(yValues, nDataPts);
    DoubleArrayMath.sortPairs(xValuesSrt, yValuesSrt);

    DoubleMatrix coefMatrix = solve(xValuesSrt, yValuesSrt);

    for (int i = 0; i < coefMatrix.rowCount(); ++i) {
      for (int j = 0; j < coefMatrix.columnCount(); ++j) {
        ArgChecker.isFalse(Double.isNaN(coefMatrix.get(i, j)), "Too large input");
        ArgChecker.isFalse(Double.isInfinite(coefMatrix.get(i, j)), "Too large input");
      }
      double ref = 0.;
      double interval = xValuesSrt[i + 1] - xValuesSrt[i];
      for (int j = 0; j < 2; ++j) {
        ref += coefMatrix.get(i, j) * Math.pow(interval, 1 - j);
        ArgChecker.isFalse(Double.isNaN(coefMatrix.get(i, j)), "Too large input");
        ArgChecker.isFalse(Double.isInfinite(coefMatrix.get(i, j)), "Too large input");
      }
      double bound = Math.max(Math.abs(ref) + Math.abs(yValuesSrt[i + 1]), 1.e-1);
      ArgChecker.isTrue(Math.abs(ref - yValuesSrt[i + 1]) < ERROR * bound, "Input is too large/small or data are not distinct enough");
    }

    return new PiecewisePolynomialResult(DoubleArray.copyOf(xValuesSrt), coefMatrix, coefMatrix.columnCount(), 1);
  }

  @Override
  public PiecewisePolynomialResult interpolate(double[] xValues, double[][] yValuesMatrix) {

    ArgChecker.notEmpty(xValues, "xValues");
    ArgChecker.notEmpty(yValuesMatrix, "yValuesMatrix");

    int nDataPts = xValues.length;
    ArgChecker.isTrue(nDataPts > 1, "at least two data points required");
    ArgChecker.isTrue(nDataPts == yValuesMatrix[0].length, "(xValues length = yValuesMatrix's row vector length)");
    int dim = yValuesMatrix.length;
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
          ArgChecker.isFalse(xValues[i] == xValues[j], "xValues should be distinct");
        }
      }
    }

    double[] xValuesSrt = new double[nDataPts];
    DoubleMatrix[] coefMatrix = new DoubleMatrix[dim];

    for (int i = 0; i < dim; ++i) {
      xValuesSrt = Arrays.copyOf(xValues, nDataPts);
      double[] yValuesSrt = Arrays.copyOf(yValuesMatrix[i], nDataPts);
      DoubleArrayMath.sortPairs(xValuesSrt, yValuesSrt);

      coefMatrix[i] = solve(xValuesSrt, yValuesSrt);

      for (int k = 0; k < xValuesSrt.length - 1; ++k) {
        double ref = 0.;
        double interval = xValuesSrt[k + 1] - xValuesSrt[k];
        for (int j = 0; j < 2; ++j) {
          ref += coefMatrix[i].get(k, j) * Math.pow(interval, 1 - j);
          ArgChecker.isFalse(Double.isNaN(coefMatrix[i].get(k, j)), "Too large input");
          ArgChecker.isFalse(Double.isInfinite(coefMatrix[i].get(k, j)), "Too large input");
        }
        double bound = Math.max(Math.abs(ref) + Math.abs(yValuesSrt[k + 1]), 1.e-1);
        ArgChecker.isTrue(Math.abs(ref - yValuesSrt[k + 1]) < ERROR * bound, "Input is too large/small or data points are too close");
      }
    }

    int nIntervals = coefMatrix[0].rowCount();
    int nCoefs = coefMatrix[0].columnCount();
    double[][] resMatrix = new double[dim * nIntervals][nCoefs];

    for (int i = 0; i < nIntervals; ++i) {
      for (int j = 0; j < dim; ++j) {
        resMatrix[dim * i + j] = coefMatrix[j].row(i).toArray();
      }
    }

    return new PiecewisePolynomialResult(DoubleArray.copyOf(xValuesSrt), DoubleMatrix.copyOf(resMatrix), nCoefs, dim);
  }

  @Override
  public PiecewisePolynomialResultsWithSensitivity interpolateWithSensitivity(double[] xValues, double[] yValues) {
    ArgChecker.notEmpty(xValues, "xValues");
    ArgChecker.notEmpty(yValues, "yValues");
    int nDataPts = xValues.length;
    ArgChecker.isTrue(nDataPts > 1, "at least two data points required");
    ArgChecker.isTrue(nDataPts == yValues.length, "xValues length = yValues length");
    for (int i = 0; i < nDataPts; ++i) {
      ArgChecker.isFalse(Double.isNaN(xValues[i]), "xData containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xValues[i]), "xData containing Infinity");
      ArgChecker.isFalse(Double.isNaN(yValues[i]), "yData containing NaN");
      ArgChecker.isFalse(Double.isInfinite(yValues[i]), "yData containing Infinity");
    }
    for (int i = 0; i < nDataPts; ++i) {
      for (int j = i + 1; j < nDataPts; ++j) {
        ArgChecker.isFalse(xValues[i] == xValues[j], "xValues should be distinct");
      }
    }

    double[] xValuesSrt = Arrays.copyOf(xValues, nDataPts);
    double[] yValuesSrt = Arrays.copyOf(yValues, nDataPts);
    DoubleArrayMath.sortPairs(xValuesSrt, yValuesSrt);

    DoubleMatrix[] res = solveSensitivity(xValuesSrt, yValuesSrt);
    DoubleMatrix coefMatrix = res[nDataPts - 1];
    DoubleMatrix[] coefSenseMatrix = Arrays.copyOf(res, nDataPts - 1);

    for (int i = 0; i < coefMatrix.rowCount(); ++i) {
      for (int j = 0; j < coefMatrix.columnCount(); ++j) {
        ArgChecker.isFalse(Double.isNaN(coefMatrix.get(i, j)), "Too large input");
        ArgChecker.isFalse(Double.isInfinite(coefMatrix.get(i, j)), "Too large input");
      }
      double ref = 0.;
      double interval = xValuesSrt[i + 1] - xValuesSrt[i];
      for (int j = 0; j < 2; ++j) {
        ref += coefMatrix.get(i, j) * Math.pow(interval, 1 - j);
        ArgChecker.isFalse(Double.isNaN(coefMatrix.get(i, j)), "Too large input");
        ArgChecker.isFalse(Double.isInfinite(coefMatrix.get(i, j)), "Too large input");
      }
      double bound = Math.max(Math.abs(ref) + Math.abs(yValuesSrt[i + 1]), 1.e-1);
      ArgChecker.isTrue(Math.abs(ref - yValuesSrt[i + 1]) < ERROR * bound,
          "Input is too large/small or data are not distinct enough");
    }

    return new PiecewisePolynomialResultsWithSensitivity(
        DoubleArray.ofUnsafe(xValuesSrt),
        coefMatrix,
        coefMatrix.columnCount(),
        1,
        coefSenseMatrix);
  }

  /**
   * @param xValues X values of data
   * @param yValues Y values of data
   * @return Coefficient matrix whose i-th row vector is {a1, a0} of f(x) = a1 * (x-x_i) + a0 for the i-th interval
   */
  private DoubleMatrix solve(final double[] xValues, final double[] yValues) {

    final int nDataPts = xValues.length;

    double[][] res = new double[nDataPts - 1][2];

    for (int i = 0; i < nDataPts - 1; ++i) {
      res[i][1] = yValues[i];
      res[i][0] = (yValues[i + 1] - yValues[i]) / (xValues[i + 1] - xValues[i]);
    }

    return DoubleMatrix.copyOf(res);
  }

  /**
   * @param xValues X values of data
   * @param yValues Y values of data
   * @return Coefficient matrix and coefficient sensitivity matrices
   */
  private DoubleMatrix[] solveSensitivity(double[] xValues, double[] yValues) {

    int nDataPts = xValues.length;
    DoubleMatrix[] res = new DoubleMatrix[nDataPts];
    double[][] coef = new double[nDataPts - 1][2];

    for (int i = 0; i < nDataPts - 1; ++i) {
      double[][] coefSensi = new double[2][nDataPts];
      double intervalInv = 1d / (xValues[i + 1] - xValues[i]);
      coef[i][1] = yValues[i];
      coef[i][0] = (yValues[i + 1] - yValues[i]) * intervalInv;
      coefSensi[1][i] = 1d;
      coefSensi[0][i] = -intervalInv;
      coefSensi[0][i + 1] = intervalInv;
      res[i] = DoubleMatrix.ofUnsafe(coefSensi);
    }
    res[nDataPts - 1] = DoubleMatrix.ofUnsafe(coef);

    return res;
  }

}
