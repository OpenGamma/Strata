/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DPiecewisePoynomialDataBundle;

/**
 * Cubic spline interpolation with Clamped endpoint condition
 * Note that the first derivative values at endpoints are set to be 0.0 if the endpoints values are unspecified
 */
public class ClampedCubicSplineInterpolator1D extends PiecewisePolynomialInterpolator1D {

  /**
   * Default constructor where interpolation method is specified
   */
  public ClampedCubicSplineInterpolator1D() {
    super(new CubicSplineInterpolator());
  }

  @Override
  public Interpolator1DDataBundle getDataBundle(final double[] x, final double[] y) {
    return new Interpolator1DPiecewisePoynomialDataBundle(new ArrayInterpolator1DDataBundle(x, y, false), new CubicSplineInterpolator(), 0., 0.);
  }

  @Override
  public Interpolator1DDataBundle getDataBundleFromSortedArrays(final double[] x, final double[] y) {
    return new Interpolator1DPiecewisePoynomialDataBundle(new ArrayInterpolator1DDataBundle(x, y, true), new CubicSplineInterpolator(), 0., 0.);
  }
}
