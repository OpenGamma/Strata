/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * A curve interpolator that has been bound to a specific curve.
 * <p>
 * A bound interpolator is created from a {@link CurveInterpolator}.
 * The bind process takes the definition of the interpolator and combines it with the x-y values.
 * This allows implementations to optimize interpolation calculations.
 */
public interface BoundCurveInterpolator {

  /**
   * Computes the y-value for the specified x-value by interpolation.
   * 
   * @param x  the x-value to find the y-value for
   * @return the value at the x-value
   * @throws RuntimeException if the y-value cannot be calculated
   */
  public abstract double yValue(double x);

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
  public abstract DoubleArray yValueParameterSensitivity(double x);

  /**
   * Computes the first derivative of the curve.
   * <p>
   * The first derivative is {@code dy/dx}.
   * 
   * @param x  the x-value at which the derivative is taken
   * @return the first derivative
   * @throws RuntimeException if the derivative cannot be calculated
   */
  public abstract double firstDerivative(double x);

}
