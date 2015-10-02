/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;

/**
 * Provide a {@link VectorFunction} give the values of knots 
 */
public class InterpolatedVectorFunctionProvider extends DoublesVectorFunctionProvider {
  private final Interpolator1D _interpolator;
  private final double[] _knots;

  /**
   * set up the {@link VectorFunctionProvider}
   * @param interpolator The interpolator 
   * @param knots knots of the interpolated curve 
   */
  public InterpolatedVectorFunctionProvider(final Interpolator1D interpolator, final double[] knots) {
    ArgChecker.notNull(interpolator, "interpolator");
    ArgChecker.notEmpty(knots, "knots");
    final int n = knots.length;

    for (int i = 1; i < n; i++) {
      ArgChecker.isTrue(knots[i] > knots[i - 1], "knot points must be strictly ascending");
    }
    _interpolator = interpolator;
    _knots = knots.clone();
  }

  /**
   * {@inheritDoc}
   * @param x The values at the knots
   * @return a {@link VectorFunction}
   */
  @Override
  public VectorFunction from(final double[] x) {
    return new InterpolatedCurveVectorFunction(x, _interpolator, _knots);
  }

  /**
   * get the interpolator
   * @return the interpolator
   */
  public Interpolator1D getInterpolator() {
    return _interpolator;
  }

  /**
   * get the knots
   * @return the knots
   */
  public double[] getKnots() {
    return _knots;
  }

}
