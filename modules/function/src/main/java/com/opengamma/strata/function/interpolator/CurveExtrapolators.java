/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.interpolator;

import com.opengamma.strata.basics.interpolator.CurveExtrapolator;

/**
 * The standard set of curve extrapolators.
 */
public final class CurveExtrapolators {

  // Private constructor as this class only exists to hold the extrapolator constants
  private CurveExtrapolators() {
  }

  /** Log linear extrapolator. */
  public static final CurveExtrapolator LINEAR =
      CurveExtrapolator.of(StandardCurveExtrapolators.LINEAR.getName());

  /** Log linear extrapolator. */
  public static final CurveExtrapolator LOG_LINEAR =
      CurveExtrapolator.of(StandardCurveExtrapolators.LOG_LINEAR.getName());

  /** Quadratic left extrapolator. */
  public static final CurveExtrapolator QUADRATIC_LEFT =
      CurveExtrapolator.of(StandardCurveExtrapolators.QUADRATIC_LEFT.getName());

  /** Product polynomial extrapolator. */
  public static final CurveExtrapolator PRODUCT_POLYNOMIAL =
      CurveExtrapolator.of(StandardCurveExtrapolators.PRODUCT_POLYNOMIAL.getName());

  /** Reciprocal extrapolator. */
  public static final CurveExtrapolator RECIPROCAL =
      CurveExtrapolator.of(StandardCurveExtrapolators.RECIPROCAL.getName());
}
