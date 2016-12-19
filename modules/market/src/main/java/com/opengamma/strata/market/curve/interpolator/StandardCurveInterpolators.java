/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

/**
 * The standard set of curve interpolators.
 * <p>
 * These are referenced from {@link CurveInterpolators} where their name is used to look up an
 * instance of {@link CurveInterpolator}. This allows them to be referenced statically like a
 * constant but also allows them to be redefined and new instances added.
 */
final class StandardCurveInterpolators {

  // Linear interpolator.
  public static final CurveInterpolator LINEAR = LinearCurveInterpolator.INSTANCE;
  // Log linear interpolator.
  public static final CurveInterpolator LOG_LINEAR = LogLinearCurveInterpolator.INSTANCE;
  // Square linear interpolator.
  public static final CurveInterpolator SQUARE_LINEAR = SquareLinearCurveInterpolator.INSTANCE;
  // Double quadratic interpolator.
  public static final CurveInterpolator DOUBLE_QUADRATIC = DoubleQuadraticCurveInterpolator.INSTANCE;
  // Time square interpolator.
  public static final CurveInterpolator TIME_SQUARE = TimeSquareCurveInterpolator.INSTANCE;

  //Log natural spline interpolation with monotonicity filter.
  public static final CurveInterpolator LOG_NATURAL_SPLINE_MONOTONE_CUBIC =
      LogNaturalSplineMonotoneCubicInterpolator.INSTANCE;
  // Log natural spline interpolation for discount factors
  public static final CurveInterpolator LOG_NATURAL_SPLINE_DISCOUNT_FACTOR =
      LogNaturalSplineDiscountFactorCurveInterpolator.INSTANCE;
  // Natural cubic spline interpolator.
  public static final CurveInterpolator NATURAL_CUBIC_SPLINE =
      NaturalCubicSplineCurveInterpolator.INSTANCE;
  // Natural spline interpolator.
  public static final CurveInterpolator NATURAL_SPLINE =
      NaturalSplineCurveInterpolator.INSTANCE;
  // Natural spline interpolator with non-negativity filter.
  public static final CurveInterpolator NATURAL_SPLINE_NONNEGATIVITY_CUBIC =
      NaturalSplineNonnegativityCubicCurveInterpolator.INSTANCE;
  // Product natural cubic spline interpolator.
  public static final CurveInterpolator PRODUCT_NATURAL_SPLINE =
      ProductNaturalSplineCurveInterpolator.INSTANCE;
  // Product linear interpolator.
  public static final CurveInterpolator PRODUCT_LINEAR = ProductLinearCurveInterpolator.INSTANCE;
  // Step upper interpolator.
  public static final CurveInterpolator STEP_UPPER = StepUpperCurveInterpolator.INSTANCE;
  // Piecewise cubic Hermite interpolator with monotonicity.
  public static final CurveInterpolator PCHIP = PiecewiseCubicHermiteMonotonicityCurveInterpolator.INSTANCE;

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardCurveInterpolators() {
  }

}
