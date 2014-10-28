/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.interp;

/**
 * Interpolates values in 1 dimension.
 */
public class Interpolator1D {
  private double[] _x;
  private double[] _y;
  private PP_t _pp;
  private InterpMethod _meth;

  /**
   * Construct a new {@link Interpolator1D} from raw 'x' and 'y' coordinates with a specified interpolation method.
   * @param x the 'x' coordinates
   * @param y the 'y' coordinates
   * @param meth the method to be used in interpolation
   */
  public Interpolator1D(double[] x, double[] y, InterpMethod meth)
  {
    _x = x.clone();
    _y = y.clone();
    _meth = meth;
    _pp = Interpolator1DRaw.interp(x, y, meth);
  }

  /**
   * Interpolates values at a vector of locations
   * @param xx the locations at which to obtain an interpolated value
   * @return the values interpolated from locations in 'xx'
   */
  public double[] interpolate(double[] xx)
  {
    return Interpolator1DRaw.interp(xx, _meth, _pp);
  }

  /**
   * Gets the x coordinate.
   * @return the x coordinate.
   */
  public double[] get_x() {
    return _x;
  }

  /**
   * Gets the y coordinate.
   * @return the y coordinate.
   */
  public double[] get_y() {
    return _y;
  }

  /**
   * Gets the pp, the underlying pp struct used in interpolation.
   * @return the pp struct
   */
  public PP_t get_pp() {
    return _pp;
  }

  /**
   * Gets the interpolation method.
   * @return the interpolation method.
   */
  public InterpMethod get_meth() {
    return _meth;
  }
}
