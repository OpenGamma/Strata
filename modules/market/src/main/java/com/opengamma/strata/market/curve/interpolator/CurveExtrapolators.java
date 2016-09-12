/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * The standard set of curve extrapolators.
 */
public final class CurveExtrapolators {
  // TODO: Check and add Javadoc for each constant

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<CurveExtrapolator> ENUM_LOOKUP = ExtendedEnum.of(CurveExtrapolator.class);

  /**
   * Flat extrapolator.
   * <p>
   * The leftmost (rightmost) point of the data set is used for all extrapolated values.
   */
  public static final CurveExtrapolator FLAT =
      CurveExtrapolator.of(StandardCurveExtrapolators.FLAT.getName());
  /**
   * Linear extrapolator.
   * <p>
   * The extrapolation continues linearly from the leftmost (rightmost) point of the data set.
   */
  public static final CurveExtrapolator LINEAR =
      CurveExtrapolator.of(StandardCurveExtrapolators.LINEAR.getName());
  /**
   * Log linear extrapolator.
   * <p>
   * The extrapolant is {@code exp(f(x))} where {@code f(x)} is a linear function
   * which is smoothly connected with a log-interpolator {@code exp(F(x))}.
   */
  public static final CurveExtrapolator LOG_LINEAR =
      CurveExtrapolator.of(StandardCurveExtrapolators.LOG_LINEAR.getName());
  /**
   * Quadratic left extrapolator.
   * <p>
   * This left extrapolator is designed for extrapolating a discount factor where the
   * trivial point (0d,1d) is NOT involved in the data.
   * The extrapolation is completed by applying a quadratic extrapolant on the discount
   * factor (not log of the discount factor), where the point (0d,1d) is inserted and
   * the first derivative value is assumed to be continuous at the first key.
   */
  public static final CurveExtrapolator QUADRATIC_LEFT =
      CurveExtrapolator.of(StandardCurveExtrapolators.QUADRATIC_LEFT.getName());
  /**
   * Product linear extrapolator.
   * <p>
   * Given a data set {@code (xValues[i], yValues[i])}, extrapolate {@code (x[i], x[i] * y[i])}
   * by a linear function.
   * <p>
   * The gradient of the extrapolation is obtained from the gradient of the interpolated
   * curve on {@code (x[i], x[i] * y[i])} at the first/last node.
   * <p>
   * The extrapolation is ambiguous at x=0. Thus the following rule applies: 
   * The x value of the first node must be strictly negative for the left extrapolation, whereas the x value of 
   * the last node must be strictly positive for the right extrapolation.
   */
  public static final CurveExtrapolator PRODUCT_LINEAR =
      CurveExtrapolator.of(StandardCurveExtrapolators.PRODUCT_LINEAR.getName());
  /**
   * Exponential extrapolator.
   * <p>
   * Outside the data range the function is an exponential exp(m*x) where m is such that
   * on the left {@code exp(m * firstXValue) = firstYValue} and on the right
   * {@code exp(m * lastXValue) = lastYValue}.
   */
  public static final CurveExtrapolator EXPONENTIAL =
      CurveExtrapolator.of(StandardCurveExtrapolators.EXPONENTIAL.getName());
  /**
   * Extrapolator that throws an exception if extrapolation is attempted.
   */
  public static final CurveExtrapolator EXCEPTION =
      CurveExtrapolator.of(StandardCurveExtrapolators.EXCEPTION.getName());
  /**
   * Interpolator extrapolator.
   * <p>
   * The extrapolator does no extrapolation itself and delegates to the interpolator for all operations.
   */
  public static final CurveExtrapolator INTERPOLATOR =
      CurveExtrapolator.of(StandardCurveExtrapolators.INTERPOLATOR.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private CurveExtrapolators() {
  }

}
