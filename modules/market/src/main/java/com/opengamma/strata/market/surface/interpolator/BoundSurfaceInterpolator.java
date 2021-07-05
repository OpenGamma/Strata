/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface.interpolator;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * A surface interpolator that has been bound to a specific surface.
 * <p>
 * A bound interpolator is created from a {@link SurfaceInterpolator}.
 * The bind process takes the definition of the interpolator and combines it with the x-y-z values.
 * This allows implementations to optimize interpolation calculations.
 */
public interface BoundSurfaceInterpolator {

  /**
   * Computes the z-value for the specified x-y-value by interpolation.
   * 
   * @param x  the x-value to find the z-value for
   * @param y  the y-value to find the z-value for
   * @return the value at the x-y-value
   * @throws RuntimeException if the z-value cannot be calculated
   */
  public abstract double interpolate(double x, double y);

  /**
   * Computes the partial derivatives of the surface.
   * <p>
   * The first derivatives are {@code dz/dx and dz/dy}.
   * The derivatives are in the following order:
   * <ul>
   * <li>[0] derivative with respect to x
   * <li>[1] derivative with respect to y
   * </ul>
   *
   * @param x  the x-value at which the partial derivative is taken
   * @param y  the y-value at which the partial derivative is taken
   * @return the z-value and it's partial first derivatives
   * @throws RuntimeException if the derivative cannot be calculated
   */
  public abstract ValueDerivatives firstPartialDerivatives(double x, double y);

  /**
   * Computes the sensitivity of the x-y-value with respect to the surface parameters.
   * <p>
   * This returns an array with one element for each parameter of the surface.
   * The array contains the sensitivity of the z-value at the specified x-y-value to each parameter.
   * 
   * @param x  the x-value at which the parameter sensitivity is computed
   * @param y  the y-value at which the parameter sensitivity is computed
   * @return the sensitivity
   * @throws RuntimeException if the sensitivity cannot be calculated
   */
  public abstract DoubleArray parameterSensitivity(double x, double y);

}
