/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube.interpolator;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Interface for interpolators that interpolate a cube.
 */
public interface CubeInterpolator {

  /**
   * Binds this interpolator to a cube.
   * <p>
   * The bind process takes the definition of the interpolator and combines it with the x-y-z-w values.
   * This allows implementations to optimize interpolation calculations.
   *
   * @param xValues  the x-values of the cube, must be sorted from low to high
   * @param yValues  the y-values of the cube, must be sorted from low to high within x
   * @param zValues  the z-values of the cube, must be sorted from low to high within x,y
   * @param wValues  the w-values of the cube
   * @return the bound interpolator
   */
  public abstract BoundCubeInterpolator bind(
      DoubleArray xValues,
      DoubleArray yValues,
      DoubleArray zValues,
      DoubleArray wValues);

}
