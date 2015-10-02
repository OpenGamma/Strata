/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.interpolator;

import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;

/**
 * The standard set of curve interpolators.
 * <p>
 * Currently they are all implementations of {@link Interpolator1D} for compatibility with legacy code.
 * This should be regarded as an implementation detail and will change.
 */
public final class CurveInterpolators {

  /** Linear interpolator. */
  public static final CurveInterpolator LINEAR =
      CurveInterpolator.of(StandardCurveInterpolators.LINEAR.getName());

  /** Exponential interpolator. */
  public static final CurveInterpolator EXPONENTIAL =
      CurveInterpolator.of(StandardCurveInterpolators.EXPONENTIAL.getName());

  /** Log linear interpolator. */
  public static final CurveInterpolator LOG_LINEAR =
      CurveInterpolator.of(StandardCurveInterpolators.LOG_LINEAR.getName());

  /** Double quadratic interpolator. */
  public static final CurveInterpolator DOUBLE_QUADRATIC =
      CurveInterpolator.of(StandardCurveInterpolators.DOUBLE_QUADRATIC.getName());

  /** Log natural cubic interpolation with monotonicity filter. */
  public static final CurveInterpolator LOG_NATURAL_CUBIC_MONOTONE =
      CurveInterpolator.of(StandardCurveInterpolators.LOG_NATURAL_CUBIC_MONOTONE.getName());

  /** Time square interpolator. */
  public static final CurveInterpolator TIME_SQUARE =
      CurveInterpolator.of(StandardCurveInterpolators.TIME_SQUARE.getName());

  // Private constructor as this class only exists to hold the interpolator constants
  private CurveInterpolators() {
  }
}
