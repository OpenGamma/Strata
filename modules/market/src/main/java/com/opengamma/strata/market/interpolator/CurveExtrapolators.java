/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

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
   * Product polynomial extrapolator.
   * <p>
   * Given a data set {@code (xValues[i], yValues[i])}, extrapolate {@code (x[i], x[i] * y[i])}
   * by a polynomial function.
   */
  public static final CurveExtrapolator PRODUCT_POLYNOMIAL =
      CurveExtrapolator.of(StandardCurveExtrapolators.PRODUCT_POLYNOMIAL.getName());
  /**
   * Reciprocal extrapolator.
   * <p>
   * Given a data set {@code x[i], y[i]}, extrapolate {@code (x[i], x[i] * y[i])} by a linear
   * function by using polynomial coefficients.
   */
  public static final CurveExtrapolator RECIPROCAL =
      CurveExtrapolator.of(StandardCurveExtrapolators.RECIPROCAL.getName());
  /**
   * Extrapolator that does no extrapolation and delegates to the interpolator.
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
