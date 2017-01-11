/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

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
   * / (x<sub>2</sub> - x<sub>1</sub>)</i>.
   */
  public static final CurveInterpolator LINEAR =
      CurveInterpolator.of(StandardCurveInterpolators.LINEAR.getName());
  /**
   * Log linear interpolator.
   * <p>
   * The interpolated value of the function <i>y</i> at <i>x</i> between two data points
   * <i>(x<sub>1</sub>, y<sub>1</sub>)</i> and <i>(x<sub>2</sub>, y<sub>2</sub>)</i> is given by:<br>
   * <i>y = y<sub>1</sub> (y<sub>2</sub> / y<sub>1</sub>) ^ ((x - x<sub>1</sub>) /
   * (x<sub>2</sub> - x<sub>1</sub>))</i><br>
   * It is the equivalent of performing a linear interpolation on a data set after
   * taking the logarithm of the y-values.
   */
  public static final CurveInterpolator LOG_LINEAR =
      CurveInterpolator.of(StandardCurveInterpolators.LOG_LINEAR.getName());
  /**
   * Square linear interpolator.
   * <p>
   * The interpolator is used for interpolation on variance for options.
   * Interpolation is linear on y^2. All values of y must be positive.
   */
  public static final CurveInterpolator SQUARE_LINEAR =
      CurveInterpolator.of(StandardCurveInterpolators.SQUARE_LINEAR.getName());
  /**
   * Double quadratic interpolator.
   */
  public static final CurveInterpolator DOUBLE_QUADRATIC =
      CurveInterpolator.of(StandardCurveInterpolators.DOUBLE_QUADRATIC.getName());
  /**
   * Time square interpolator.
   * <p>
   * The interpolation is linear on {@code x y^2}. The interpolator is used for interpolation on
   * integrated variance for options. All values of y must be positive.
   */
  public static final CurveInterpolator TIME_SQUARE =
      CurveInterpolator.of(StandardCurveInterpolators.TIME_SQUARE.getName());

  /**
   * Log natural spline interpolation with monotonicity filter.
   * <p>
   * Finds an interpolant {@code F(x) = exp( f(x) )} where {@code f(x)} is a Natural cubic
   * spline with Monotonicity cubic filter.
   */
  public static final CurveInterpolator LOG_NATURAL_SPLINE_MONOTONE_CUBIC =
      CurveInterpolator.of(StandardCurveInterpolators.LOG_NATURAL_SPLINE_MONOTONE_CUBIC.getName());
  /**
   * Log natural spline interpolator for discount factors.
   * <p>
   * Finds an interpolant {@code F(x) = exp( f(x) )} where {@code f(x)} is a natural cubic spline going through
   * the point (0,1).  
   */
  public static final CurveInterpolator LOG_NATURAL_SPLINE_DISCOUNT_FACTOR =
      CurveInterpolator.of(StandardCurveInterpolators.LOG_NATURAL_SPLINE_DISCOUNT_FACTOR.getName());
  /**
   * Natural cubic spline interpolator.
   */
  public static final CurveInterpolator NATURAL_CUBIC_SPLINE =
      CurveInterpolator.of(StandardCurveInterpolators.NATURAL_CUBIC_SPLINE.getName());
  /**
   * Natural spline interpolator.
   */
  public static final CurveInterpolator NATURAL_SPLINE =
      CurveInterpolator.of(StandardCurveInterpolators.NATURAL_SPLINE.getName());
  /**
   * Natural spline interpolator with non-negativity filter.
   */
  public static final CurveInterpolator NATURAL_SPLINE_NONNEGATIVITY_CUBIC =
      CurveInterpolator.of(StandardCurveInterpolators.NATURAL_SPLINE_NONNEGATIVITY_CUBIC.getName());
  /**
   * Product natural spline interpolator.
   * <p>
   * Given a data set {@code (x[i], y[i])}, interpolate {@code (x[i], x[i] * y[i])} by natural cubic spline.
   * <p>
   * As a curve for the product {@code x * y} is not well-defined at {@code x = 0}, we impose
   * the condition that all of the x data to be the same sign, such that the origin is not within data range.
   * The x key value must not be close to zero.
   */
  public static final CurveInterpolator PRODUCT_NATURAL_SPLINE =
      CurveInterpolator.of(StandardCurveInterpolators.PRODUCT_NATURAL_SPLINE.getName());
  /**
   * Product linear interpolator.
   * <p>
   * Given a data set {@code (x[i], y[i])}, interpolate {@code (x[i], x[i] * y[i])} by linear functions. 
   * <p>
   * As a curve for the product {@code x * y} is not well-defined at {@code x = 0}, we impose
   * the condition that all of the x data to be the same sign, such that the origin is not within data range.
   * The x key value must not be close to zero.
   */
  public static final CurveInterpolator PRODUCT_LINEAR =
      CurveInterpolator.of(StandardCurveInterpolators.PRODUCT_LINEAR.getName());
  /**
   * Step upper interpolator.
   * <p>
   * The interpolated value at <i>x</i> s.t. <i>x<sub>1</sub> < x =< x<sub>2</sub></i> is the value at <i>x<sub>2</sub></i>. 
   */
  public static final CurveInterpolator STEP_UPPER =
      CurveInterpolator.of(StandardCurveInterpolators.STEP_UPPER.getName());
  /**
   * Piecewise cubic Hermite interpolator with monotonicity.
   */
  public static final CurveInterpolator PCHIP =
      CurveInterpolator.of(StandardCurveInterpolators.PCHIP.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private CurveInterpolators() {
  }

}
