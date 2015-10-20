/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialWithSensitivityFunction1D;
import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DPiecewisePoynomialDataBundle;

/**
 * Wrapper class for {@link PiecewisePolynomialInterpolator} 
 */
public abstract class PiecewisePolynomialInterpolator1D extends Interpolator1D {

  /** Serialization version */
  private static final long serialVersionUID = 1L;

  private PiecewisePolynomialInterpolator _baseMethod;
  private static final PiecewisePolynomialWithSensitivityFunction1D FUNC = new PiecewisePolynomialWithSensitivityFunction1D();

  /**
   * Constructor 
   * @param method Piecewise polynomial interpolation method
   */
  public PiecewisePolynomialInterpolator1D(final PiecewisePolynomialInterpolator method) {
    _baseMethod = method;
  }

  @Override
  public Double interpolate(final Interpolator1DDataBundle data, final Double value) {
    ArgChecker.notNull(value, "value");
    ArgChecker.notNull(data, "data bundle");
    ArgChecker.isTrue(data instanceof Interpolator1DPiecewisePoynomialDataBundle);
    final Interpolator1DPiecewisePoynomialDataBundle polyData = (Interpolator1DPiecewisePoynomialDataBundle) data;
    final DoubleArray res = FUNC.evaluate(polyData.getPiecewisePolynomialResultsWithSensitivity(), value);
    return res.get(0);
  }

  @Override
  public double firstDerivative(final Interpolator1DDataBundle data, final Double value) {
    ArgChecker.notNull(value, "value");
    ArgChecker.notNull(data, "data bundle");
    ArgChecker.isTrue(data instanceof Interpolator1DPiecewisePoynomialDataBundle);
    final Interpolator1DPiecewisePoynomialDataBundle polyData = (Interpolator1DPiecewisePoynomialDataBundle) data;
    final DoubleArray res = FUNC.differentiate(polyData.getPiecewisePolynomialResultsWithSensitivity(), value);
    return res.get(0);
  }

  @Override
  public double[] getNodeSensitivitiesForValue(final Interpolator1DDataBundle data, final Double value) {
    ArgChecker.notNull(value, "value");
    ArgChecker.notNull(data, "data bundle");
    ArgChecker.isTrue(data instanceof Interpolator1DPiecewisePoynomialDataBundle);
    final Interpolator1DPiecewisePoynomialDataBundle polyData = (Interpolator1DPiecewisePoynomialDataBundle) data;
    final DoubleArray res = FUNC.nodeSensitivity(polyData.getPiecewisePolynomialResultsWithSensitivity(), value);
    return res.toArray();
  }

  @Override
  public Interpolator1DDataBundle getDataBundle(final double[] x, final double[] y) {
    return new Interpolator1DPiecewisePoynomialDataBundle(new ArrayInterpolator1DDataBundle(x, y, false), this._baseMethod);
  }

  /**
   * Data bundle builder ONLY FOR cubic spline interpolator or hyman filters on cubic spline interpolator using Clamped endpoint conditions
   * @param x X values of data
   * @param y Y values of data
   * @param leftCond First derivative value at left endpoint 
   * @param rightCond First derivative value at right endpoint 
   * @return {@link Interpolator1DPiecewisePoynomialDataBundle}
   */
  public Interpolator1DDataBundle getDataBundle(final double[] x, final double[] y, final double leftCond, final double rightCond) {
    if (!(_baseMethod.getPrimaryMethod() instanceof CubicSplineInterpolator)) {
      throw new IllegalArgumentException("No degrees of freedom at endpoints for this interpolation method");
    }
    return new Interpolator1DPiecewisePoynomialDataBundle(new ArrayInterpolator1DDataBundle(x, y, false), this._baseMethod, leftCond, rightCond);
  }

  @Override
  public Interpolator1DDataBundle getDataBundleFromSortedArrays(final double[] x, final double[] y) {
    return new Interpolator1DPiecewisePoynomialDataBundle(new ArrayInterpolator1DDataBundle(x, y, true), this._baseMethod);
  }

  /**
   * Data bundle builder ONLY FOR cubic spline interpolator or hyman filters on cubic spline interpolator using Clamped endpoint conditions
   * @param x X values of data
   * @param y Y values of data
   * @param leftCond First derivative value at left endpoint 
   * @param rightCond First derivative value at right endpoint 
   * @return {@link Interpolator1DPiecewisePoynomialDataBundle} 
   */
  public Interpolator1DDataBundle getDataBundleFromSortedArrays(final double[] x, final double[] y, final double leftCond, final double rightCond) {
    if (!(_baseMethod.getPrimaryMethod() instanceof CubicSplineInterpolator)) {
      throw new IllegalArgumentException("No degrees of freedom at endpoints for this interpolation method");
    }
    return new Interpolator1DPiecewisePoynomialDataBundle(new ArrayInterpolator1DDataBundle(x, y, true), this._baseMethod, leftCond, rightCond);
  }

  /**
   * Access interpolator
   * @return _baseMethod
   */
  public PiecewisePolynomialInterpolator getInterpolator() {
    return _baseMethod;
  }
}
