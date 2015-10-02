/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * Abstract class for interpolations based on piecewise polynomial functions 
 */
public abstract class PiecewisePolynomialInterpolator {

  /**
   * @param xValues X values of data
   * @param yValues Y values of data
   * @return {@link PiecewisePolynomialResult} containing knots, coefficients of piecewise polynomials, number of intervals, degree of polynomials, dimension of spline
   */
  public abstract PiecewisePolynomialResult interpolate(final double[] xValues, final double[] yValues);

  /**
   * @param xValues X values of data
   * @param yValuesMatrix Y values of data
   * @return Coefficient matrix whose i-th row vector is {a_n, a_{n-1}, ... } of f(x) = a_n * (x-x_i)^n + a_{n-1} * (x-x_i)^{n-1} +... for the i-th interval
   */
  public abstract PiecewisePolynomialResult interpolate(final double[] xValues, final double[][] yValuesMatrix);

  /**
   * @param xValues X values of data
   * @param yValues Y values of data
   * @param xKey  the key
   * @return value of the underlying cubic spline function at the value of x
   */
  public double interpolate(final double[] xValues, final double[] yValues, final double xKey) {

    ArgChecker.isFalse(Double.isNaN(xKey), "xKey containing NaN");
    ArgChecker.isFalse(Double.isInfinite(xKey), "xKey containing Infinity");

    final PiecewisePolynomialResult result = this.interpolate(xValues, yValues);
    final double[] knots = result.getKnots().getData();
    final int nKnots = knots.length;
    final DoubleMatrix2D coefMatrix = result.getCoefMatrix();

    double res = 0.;

    int indicator = 0;
    if (xKey < knots[1]) {
      indicator = 0;
    } else {
      for (int i = 1; i < nKnots - 1; ++i) {
        if (knots[i] <= xKey) {
          indicator = i;
        }
      }
    }
    final double[] coefs = coefMatrix.getRowVector(indicator).getData();
    res = getValue(coefs, xKey, knots[indicator]);
    ArgChecker.isFalse(Double.isInfinite(res), "Too large input");
    ArgChecker.isFalse(Double.isNaN(res), "Too large input");

    return res;
  }

  /**
   * @param xValues X values of data
   * @param yValues Y values of data
   * @param xKeys  the keys
   * @return Values of the underlying cubic spline function at the values of x
   */
  public DoubleMatrix1D interpolate(final double[] xValues, final double[] yValues, final double[] xKeys) {
    ArgChecker.notNull(xKeys, "xKeys");

    final int keyLength = xKeys.length;
    for (int i = 0; i < keyLength; ++i) {
      ArgChecker.isFalse(Double.isNaN(xKeys[i]), "xKeys containing NaN");
      ArgChecker.isFalse(Double.isInfinite(xKeys[i]), "xKeys containing Infinity");
    }

    final PiecewisePolynomialResult result = this.interpolate(xValues, yValues);
    final double[] knots = result.getKnots().getData();
    final int nKnots = knots.length;
    final DoubleMatrix2D coefMatrix = result.getCoefMatrix();

    double[] res = new double[keyLength];

    for (int j = 0; j < keyLength; ++j) {
      int indicator = 0;
      if (xKeys[j] < knots[1]) {
        indicator = 0;
      } else {
        for (int i = 1; i < nKnots - 1; ++i) {
          if (knots[i] <= xKeys[j]) {
            indicator = i;
          }
        }
      }
      final double[] coefs = coefMatrix.getRowVector(indicator).getData();
      res[j] = getValue(coefs, xKeys[j], knots[indicator]);
      ArgChecker.isFalse(Double.isInfinite(res[j]), "Too large input");
      ArgChecker.isFalse(Double.isNaN(res[j]), "Too large input");
    }

    return new DoubleMatrix1D(res);
  }

  /**
   * @param xValues  the values
   * @param yValues  the values
   * @param xMatrix  the matrix
   * @return Values of the underlying cubic spline function at the values of x
   */
  public DoubleMatrix2D interpolate(final double[] xValues, final double[] yValues, final double[][] xMatrix) {

    ArgChecker.notNull(xMatrix, "xMatrix");

    final int keyLength = xMatrix[0].length;
    final int keyDim = xMatrix.length;

    final DoubleMatrix2D matrix = new DoubleMatrix2D(xMatrix);

    double[][] res = new double[keyDim][keyLength];

    for (int i = 0; i < keyDim; ++i) {
      for (int j = 0; j < keyLength; ++j) {
        res[i][j] = interpolate(xValues, yValues, matrix.getRowVector(i).getData()).getData()[j];
      }
    }

    return new DoubleMatrix2D(res);

  }

  /**
   * @param xValues  the values
   * @param yValuesMatrix  the matrix
   * @param x  the x
   * @return Values of the underlying cubic spline functions interpolating {yValuesMatrix.RowVectors} at the value of x
   */
  public DoubleMatrix1D interpolate(final double[] xValues, final double[][] yValuesMatrix, final double x) {

    final DoubleMatrix2D matrix = new DoubleMatrix2D(yValuesMatrix);
    final int dim = matrix.getNumberOfRows();

    double[] res = new double[dim];

    for (int i = 0; i < dim; ++i) {
      res[i] = interpolate(xValues, matrix.getRowVector(i).getData(), x);
    }

    return new DoubleMatrix1D(res);
  }

  /**
   * @param xValues  the values
   * @param yValuesMatrix  the matrix
   * @param x  the s
   * @return Values of the underlying cubic spline functions interpolating {yValuesMatrix.RowVectors} at the values of x
   */
  public DoubleMatrix2D interpolate(final double[] xValues, final double[][] yValuesMatrix, final double[] x) {
    ArgChecker.notNull(x, "x");

    final int dim = yValuesMatrix.length;
    final int keyLength = x.length;

    final DoubleMatrix2D matrix = new DoubleMatrix2D(yValuesMatrix);

    double[][] res = new double[dim][keyLength];

    for (int i = 0; i < dim; ++i) {
      res[i] = interpolate(xValues, matrix.getRowVector(i).getData(), x).getData();
    }

    return new DoubleMatrix2D(res);
  }

  /**
   * @param xValues  the values
   * @param yValuesMatrix  the matrix
   * @param xMatrix  the matrix
   * @return Values of the underlying cubic spline functions interpolating {yValuesMatrix.RowVectors} at the values of xMatrix
   */
  public DoubleMatrix2D[] interpolate(final double[] xValues, final double[][] yValuesMatrix, final double[][] xMatrix) {
    ArgChecker.notNull(xMatrix, "xMatrix");

    final int keyColumn = xMatrix[0].length;

    final DoubleMatrix2D matrix = new DoubleMatrix2D(xMatrix);

    DoubleMatrix2D[] resMatrix2D = new DoubleMatrix2D[keyColumn];

    for (int i = 0; i < keyColumn; ++i) {
      resMatrix2D[i] = interpolate(xValues, yValuesMatrix, matrix.getColumnVector(i).getData());
    }

    return resMatrix2D;
  }

  /**
   * Derive interpolant on {xValues_i, yValues_i} and (yValues) node sensitivity 
   * @param xValues X values of data
   * @param yValues Y values of data
   * @return {@link PiecewisePolynomialResultsWithSensitivity}
   */
  public abstract PiecewisePolynomialResultsWithSensitivity interpolateWithSensitivity(final double[] xValues, final double[] yValues);

  /**
   * Hyman filter modifies derivative values at knot points which are initially computed by a "primary" interpolator
   * @return The primary interpolator for Hyman filter, interpolation method itself for other interpolators
   */
  public PiecewisePolynomialInterpolator getPrimaryMethod() {
    return this;
  }

  /**
   * @param coefs {a_n,a_{n-1},...} of f(x) = a_n x^{n} + a_{n-1} x^{n-1} + ....
   * @param x  the x
   * @param leftknot Knot specifying underlying interpolation function
   * @return Value of the underlying interpolation function at the value of x
   */
  protected double getValue(final double[] coefs, final double x, final double leftknot) {

    final int nCoefs = coefs.length;

    final double s = x - leftknot;
    double res = coefs[0];
    for (int i = 1; i < nCoefs; i++) {
      res *= s;
      res += coefs[i];
    }

    return res;
  }
}
