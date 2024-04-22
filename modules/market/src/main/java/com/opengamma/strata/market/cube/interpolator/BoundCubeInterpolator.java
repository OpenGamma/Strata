/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube.interpolator;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * A cube interpolator that has been bound to a specific cube.
 * <p>
 * A bound interpolator is created from a {@link CubeInterpolator}.
 * The bind process takes the definition of the interpolator and combines it with the x-y-z-w values.
 * This allows implementations to optimize interpolation calculations.
 */
public interface BoundCubeInterpolator {

  /**
   * Computes the w-value for the specified x-y-z value by interpolation.
   *
   * @param x  the x-value to find the w-value for
   * @param y  the y-value to find the w-value for
   * @param z  the z-value to find the w-value for
   * @return the value at the x-y-value
   * @throws RuntimeException if the z-value cannot be calculated
   */
  public abstract double interpolate(double x, double y, double z);

  /**
   * Computes the partial derivatives of the cube.
   * <p>
   * The first derivatives are {@code dw/dx, dw/dy, dw/dz}.
   * The derivatives are in the following order:
   * <ul>
   * <li>[0] derivative with respect to x
   * <li>[1] derivative with respect to y
   * <li>[2] derivative with respect to z
   * </ul>
   *
   * @param x  the x-value at which the partial derivative is taken
   * @param y  the y-value at which the partial derivative is taken
   * @param z  the z-value at which the partial derivative is taken
   * @return the w-value and it's partial first derivatives
   * @throws RuntimeException if the derivative cannot be calculated
   */
  public abstract ValueDerivatives firstPartialDerivatives(double x, double y, double z);

  /**
   * Computes the sensitivity of the x-y-z-value with respect to the cube parameters.
   * <p>
   * This returns an array with one element for each parameter of the cube.
   * The array contains the sensitivity of the w-value at the specified x-y-z-value to each parameter.
   *
   * @param x  the x-value at which the parameter sensitivity is computed
   * @param y  the y-value at which the parameter sensitivity is computed
   * @param z  the z-value at which the parameter sensitivity is computed
   * @return the sensitivity
   * @throws RuntimeException if the sensitivity cannot be calculated
   */
  public abstract DoubleArray parameterSensitivity(double x, double y, double z);

}
