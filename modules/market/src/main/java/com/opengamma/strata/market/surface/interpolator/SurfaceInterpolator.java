/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface.interpolator;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Interface for interpolators that interpolate a surface.
 */
public interface SurfaceInterpolator {

  /**
   * Binds this interpolator to a surface.
   * <p>
   * The bind process takes the definition of the interpolator and combines it with the x-y-z values.
   * This allows implementations to optimize interpolation calculations.
   *
   * @param xValues  the x-values of the surface, must be sorted from low to high
   * @param yValues  the y-values of the surface, must be sorted from low to high within x
   * @param zValues  the z-values of the surface
   * @return the bound interpolator
   */
  public abstract BoundSurfaceInterpolator bind(DoubleArray xValues, DoubleArray yValues, DoubleArray zValues);

}
