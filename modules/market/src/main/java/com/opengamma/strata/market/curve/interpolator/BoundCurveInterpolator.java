/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * A curve interpolator that has been bound to a specific curve.
 * <p>
 * A bound interpolator is created from a {@link CurveInterpolator}.
 * The bind process takes the definition of the interpolator and combines it with the x-y values.
 * This allows implementations to optimize interpolation calculations.
 * <p>
 * A bound interpolator is typically linked to two {@linkplain BoundCurveExtrapolator extrapolators}.
 * If an attempt is made to interpolate an x-value outside the range defined by
 * the first and last nodes, the appropriate extrapolator will be used.
 */
public interface BoundCurveInterpolator {

  /**
   * Computes the y-value for the specified x-value by interpolation.
   * 
   * @param x  the x-value to find the y-value for
   * @return the value at the x-value
   * @throws RuntimeException if the y-value cannot be calculated
   */
  public abstract double interpolate(double x);

  /**
   * Computes the first derivative of the y-value for the specified x-value.
   * <p>
   * The first derivative is {@code dy/dx}.
   * 
   * @param x  the x-value at which the derivative is taken
   * @return the first derivative
   * @throws RuntimeException if the derivative cannot be calculated
   */
  public abstract double firstDerivative(double x);

  /**
   * Computes the sensitivity of the y-value with respect to the curve parameters.
   * <p>
   * This returns an array with one element for each parameter of the curve.
   * The array contains the sensitivity of the y-value at the specified x-value to each parameter.
   * 
   * @param x  the x-value at which the parameter sensitivity is computed
   * @return the sensitivity
   * @throws RuntimeException if the sensitivity cannot be calculated
   */
  public abstract DoubleArray parameterSensitivity(double x);

  //-------------------------------------------------------------------------
  /**
   * Binds this interpolator to the specified extrapolators.
   * <p>
   * The bound interpolator provides methods to interpolate the y-value for a x-value.
   * If an attempt is made to interpolate an x-value outside the range defined by
   * the first and last nodes, the appropriate extrapolator will be used.
   * <p>
   * This method is intended to be called from within
   * {@link CurveInterpolator#bind(DoubleArray, DoubleArray, CurveExtrapolator, CurveExtrapolator)}.
   *
   * @param extrapolatorLeft  the extrapolator for x-values on the left
   * @param extrapolatorRight  the extrapolator for x-values on the right
   * @return the bound interpolator
   */
  public abstract BoundCurveInterpolator bind(
      BoundCurveExtrapolator extrapolatorLeft,
      BoundCurveExtrapolator extrapolatorRight);

}
