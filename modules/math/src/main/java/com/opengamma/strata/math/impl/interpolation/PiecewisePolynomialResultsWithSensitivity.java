/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Result of interpolation by piecewise polynomial containing
 * knots: Positions of knots
 * coefMatrix: Coefficient matrix whose i-th row vector is { a_n, a_{n-1}, ...} for the i-th interval, where a_n, a_{n-1},... are coefficients of f(x) = a_n (x-x_i)^n + a_{n-1} (x-x_i)^{n-1} + ....
 * In multidimensional cases, coefficients for the i-th interval of the j-th spline is in (j*(i-1) + i) -th row vector.
 * nIntervals: Number of intervals, which should be (Number of knots) - 1
 * order: Number of coefficients in polynomial, which is equal to (polynomial degree) + 1
 * dim: Number of splines
 * which are in the super class, and 
 * _coeffSense Node sensitivity of the coefficients _coeffSense[i].get(j, k) is \frac{\partial a^i_{n-j}}{\partial y_k}
 */
public class PiecewisePolynomialResultsWithSensitivity extends PiecewisePolynomialResult {

  private final DoubleMatrix[] _coeffSense;

  /**
   * 
   * @param knots  the knots
   * @param coefMatrix  the coefMatrix
   * @param order  the order
   * @param dim  the dim
   * @param coeffSense the sensitivity of the coefficients to the nodes (y-values)
   */
  public PiecewisePolynomialResultsWithSensitivity(DoubleArray knots, DoubleMatrix coefMatrix, int order, int dim, final DoubleMatrix[] coeffSense) {
    super(knots, coefMatrix, order, dim);
    if (dim != 1) {
      throw new UnsupportedOperationException();
    }
    ArgChecker.noNulls(coeffSense, "null coeffSense"); // coefficient
    _coeffSense = coeffSense;
  }

  /**
   * Access _coeffSense.
   * @return _coeffSense
   */
  public DoubleMatrix[] getCoefficientSensitivityAll() {
    return _coeffSense;
  }

  /**
   * Access _coeffSense for the i-th interval.
   * @param interval  the interval
   * @return _coeffSense for the i-th interval
   */
  public DoubleMatrix getCoefficientSensitivity(final int interval) {
    return _coeffSense[interval];
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Arrays.hashCode(_coeffSense);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof PiecewisePolynomialResultsWithSensitivity)) {
      return false;
    }
    PiecewisePolynomialResultsWithSensitivity other = (PiecewisePolynomialResultsWithSensitivity) obj;
    if (!Arrays.equals(_coeffSense, other._coeffSense)) {
      return false;
    }
    return true;
  }

}
