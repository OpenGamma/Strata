/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.extrapolator;

import com.opengamma.strata.basics.extrapolator.CurveExtrapolator;

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
  static final CurveExtrapolator LOG_LINEAR =
      CurveExtrapolator.of(StandardCurveExtrapolators.LOG_LINEAR.getName());

  /** Quadratic left extrapolator. */
  static final CurveExtrapolator QUADRATIC_LEFT =
      CurveExtrapolator.of(StandardCurveExtrapolators.QUADRATIC_LEFT.getName());

  /** Product polynomial extrapolator. */
  static final CurveExtrapolator PRODUCT_POLYNOMIAL =
      CurveExtrapolator.of(StandardCurveExtrapolators.PRODUCT_POLYNOMIAL.getName());

  /** Reciprocal extrapolator. */
  static final CurveExtrapolator RECIPROCAL =
      CurveExtrapolator.of(StandardCurveExtrapolators.RECIPROCAL.getName());
}
