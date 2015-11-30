/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import com.opengamma.strata.math.impl.interpolation.DoubleQuadraticInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.ExponentialInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.LinearInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.LogLinearInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.LogNaturalCubicMonotonicityPreservingInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.NaturalCubicSplineInterpolator1D;
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
  public static final CurveInterpolator LINEAR =
      new ImmutableCurveInterpolator("Linear", new LinearInterpolator1D());
  // Exponential interpolator.
  public static final CurveInterpolator EXPONENTIAL =
      new ImmutableCurveInterpolator("Exponential", new ExponentialInterpolator1D());
  // Log linear interpolator.
  public static final CurveInterpolator LOG_LINEAR =
      new ImmutableCurveInterpolator("LogLinear", new LogLinearInterpolator1D());
  // Double quadratic interpolator.
  public static final CurveInterpolator DOUBLE_QUADRATIC =
      new ImmutableCurveInterpolator("DoubleQuadratic", new DoubleQuadraticInterpolator1D());
  // Log natural cubic interpolation with monotonicity filter.
  public static final CurveInterpolator LOG_NATURAL_CUBIC_MONOTONE =
      new ImmutableCurveInterpolator(
          "LogNaturalCubicWithMonotonicity",
          new LogNaturalCubicMonotonicityPreservingInterpolator1D());
  // Time square interpolator.
  public static final CurveInterpolator TIME_SQUARE =
      new ImmutableCurveInterpolator("TimeSquare", new TimeSquareInterpolator1D());
  // Natural cubic spline interpolator.
  public static final CurveInterpolator NATURAL_CUBIC_SPLINE =
      new ImmutableCurveInterpolator("NaturalCubicSpline", new NaturalCubicSplineInterpolator1D());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardCurveInterpolators() {
  }

}
