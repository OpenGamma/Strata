/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;

/**
 * Provide a {@link VectorFunction} give the values of knots.
 */
public class InterpolatedVectorFunctionProvider extends DoublesVectorFunctionProvider {

  private final Interpolator1D _interpolator;
  private final double[] _knots;

  /**
   * Creates an instance.
   * 
   * @param interpolator  the interpolator 
   * @param knots  the knots of the interpolated curve 
   */
  public InterpolatedVectorFunctionProvider(Interpolator1D interpolator, double[] knots) {
    ArgChecker.notNull(interpolator, "interpolator");
    ArgChecker.notEmpty(knots, "knots");
    int n = knots.length;

    for (int i = 1; i < n; i++) {
      ArgChecker.isTrue(knots[i] > knots[i - 1], "knot points must be strictly ascending");
    }
    _interpolator = interpolator;
    _knots = knots.clone();
  }

  //-------------------------------------------------------------------------
  /**
   * {@inheritDoc}
   * @param x  the values at the knots
   * @return a {@link VectorFunction}
   */
  @Override
  public VectorFunction from(double[] x) {
    return new InterpolatedCurveVectorFunction(x, _interpolator, _knots);
  }

  /**
   * Gets the interpolator.
   * 
   * @return the interpolator
   */
  public Interpolator1D getInterpolator() {
    return _interpolator;
  }

  /**
   * Gets the knots.
   * 
   * @return the knots
   */
  public double[] getKnots() {
    return _knots;
  }

}
