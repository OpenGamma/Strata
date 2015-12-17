/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import com.opengamma.strata.math.impl.interpolation.LogNaturalCubicMonotonicityPreservingInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.LogNaturalDiscountFactorInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.NaturalCubicSplineInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.NaturalSplineInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.TimeSquareInterpolator1D;

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
  // Double quadratic interpolator.
  public static final CurveInterpolator DOUBLE_QUADRATIC = DoubleQuadraticCurveInterpolator.INSTANCE;
  // Log natural cubic interpolation with monotonicity filter.
  public static final CurveInterpolator LOG_NATURAL_CUBIC_MONOTONE =
      new StandardCurveInterpolator(
          "LogNaturalCubicWithMonotonicity",
          new LogNaturalCubicMonotonicityPreservingInterpolator1D());
  // Time square interpolator.
  public static final CurveInterpolator TIME_SQUARE =
      new StandardCurveInterpolator("TimeSquare", new TimeSquareInterpolator1D());
  // Natural cubic spline interpolator.
  public static final CurveInterpolator NATURAL_CUBIC_SPLINE =
      new StandardCurveInterpolator("NaturalCubicSpline", new NaturalCubicSplineInterpolator1D());
  // Natural spline interpolator.
  public static final CurveInterpolator NATURAL_SPLINE =
      new StandardCurveInterpolator("NaturalSpline", new NaturalSplineInterpolator1D());
  // Log natural cubic spline interpolation for discount factors
  public static final CurveInterpolator LOG_NATURAL_CUBIC_DISCOUNT_FACTOR =
      new StandardCurveInterpolator("LogNaturalCubicDiscountFactor", new LogNaturalDiscountFactorInterpolator1D());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardCurveInterpolators() {
  }

}
