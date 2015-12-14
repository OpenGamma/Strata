/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

/**
 * The standard set of curve extrapolators.
 * <p>
 * These are referenced from {@link CurveExtrapolators} where their name is used to look up an
 * instance of {@link CurveExtrapolator}. This allows them to be referenced statically like a
 * constant but also allows them to be redefined and new instances added.
 */
final class StandardCurveExtrapolators {

  // Flat extrapolator.
  public static final CurveExtrapolator FLAT = FlatCurveExtrapolator.INSTANCE;
  // Linear extrapolator.
  public static final CurveExtrapolator LINEAR = LinearCurveExtrapolator.INSTANCE;
  // Log linear extrapolator.
  public static final CurveExtrapolator LOG_LINEAR = LogLinearCurveExtrapolator.INSTANCE;
  // Quadratic left extrapolator.
  public static final CurveExtrapolator QUADRATIC_LEFT = QuadraticLeftCurveExtrapolator.INSTANCE;
  // Product polynomial extrapolator.
  public static final CurveExtrapolator PRODUCT_POLYNOMIAL = ProductPolynomialCurveExtrapolator.INSTANCE;
  // Reciprocal extrapolator.
  public static final CurveExtrapolator RECIPROCAL = ReciprocalCurveExtrapolator.INSTANCE;
  // Exponential extrapolator.
  public static final CurveExtrapolator EXPONENTIAL = ExponentialCurveExtrapolator.INSTANCE;
  // Exception extrapolator.
  public static final CurveExtrapolator EXCEPTION = ExceptionCurveExtrapolator.INSTANCE;

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardCurveExtrapolators() {
  }

}
