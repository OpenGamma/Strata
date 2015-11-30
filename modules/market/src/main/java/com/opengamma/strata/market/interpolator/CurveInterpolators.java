/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * The standard set of curve interpolators.
 */
public final class CurveInterpolators {
  // TODO: Check and add Javadoc for each constant

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<CurveInterpolator> ENUM_LOOKUP = ExtendedEnum.of(CurveInterpolator.class);

  /**
   * Linear interpolator.
   * <p>
   * The interpolated value of the function <i>y</i> at <i>x</i> between two data points
   * <i>(x<sub>1</sub>, y<sub>1</sub>)</i> and <i>(x<sub>2</sub>, y<sub>2</sub>)</i> is given by:<br>
   * <i>y = y<sub>1</sub> + (x - x<sub>1</sub>) * (y<sub>2</sub> - y<sub>1</sub>)
   * / (x<sub>2</sub> - x<sub>1</sub>)</i>
   */
  public static final CurveInterpolator LINEAR =
      CurveInterpolator.of(StandardCurveInterpolators.LINEAR.getName());
  /**
   * Exponential interpolator.
   * <p>
   * The interpolated value of the function <i>y</i> at <i>x</i> between two data points
   * <i>(x<sub>1</sub>, y<sub>1</sub>)</i> and <i>(x<sub>2</sub>, y<sub>2</sub>)</i> is given by:<br>
   * <i>y = a * exp( b * x )</i><br />
   * where a, b are real constants. Note that all of y data should have the same sign.
   */
  public static final CurveInterpolator EXPONENTIAL =
      CurveInterpolator.of(StandardCurveInterpolators.EXPONENTIAL.getName());
  /**
   * Log linear interpolator.
   */
  public static final CurveInterpolator LOG_LINEAR =
      CurveInterpolator.of(StandardCurveInterpolators.LOG_LINEAR.getName());
  /**
   * Double quadratic interpolator.
   */
  public static final CurveInterpolator DOUBLE_QUADRATIC =
      CurveInterpolator.of(StandardCurveInterpolators.DOUBLE_QUADRATIC.getName());
  /**
   * Log natural cubic interpolation with monotonicity filter.
   * <p>
   * Finds an interpolant {@code F(x) = exp( f(x) )} where {@code f(x)} is a Natural cubic
   * spline with Monotonicity cubic filter. 
   */
  public static final CurveInterpolator LOG_NATURAL_CUBIC_MONOTONE =
      CurveInterpolator.of(StandardCurveInterpolators.LOG_NATURAL_CUBIC_MONOTONE.getName());
  /**
   * Time square interpolator.
   * <p>
   * The interpolation is linear on {@code x y^2}. The interpolator is used for interpolation on
   * integrated variance for options. All values of y must be positive.
   */
  public static final CurveInterpolator TIME_SQUARE =
      CurveInterpolator.of(StandardCurveInterpolators.TIME_SQUARE.getName());
  /**
   * Natural cubic spline interpolator.
   */
  public static final CurveInterpolator NATURAL_CUBIC_SPLINE =
      CurveInterpolator.of(StandardCurveInterpolators.NATURAL_CUBIC_SPLINE.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private CurveInterpolators() {
  }

}
