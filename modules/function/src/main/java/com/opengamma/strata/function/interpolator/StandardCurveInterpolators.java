/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.interpolator;

import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.math.impl.interpolation.DoubleQuadraticInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.ExponentialInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.LinearInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.LogLinearInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.LogNaturalCubicMonotonicityPreservingInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.TimeSquareInterpolator1D;

/**
 * The standard set of curve interpolators.
 * <p>
 * These are referenced from {@link CurveInterpolators} where their name is used to look up an
 * instance of {@link CurveInterpolator}. This allows them to be referenced statically like a
 * constant but also allows them to be redefined and new instances added.
 * <p>
 * Currently they are all implementations of {@link Interpolator1D} for compatibility with legacy code.
 * This should be regarded as an implementation detail and is likely to change soon.
 */
final class StandardCurveInterpolators {

  // Private constructor as this class only exists to hold the interpolator constants
  private StandardCurveInterpolators() {
  }

  /** Linear interpolator. */
  public static final CurveInterpolator LINEAR = new LinearInterpolator1D();

  /** Exponential interpolator. */
  public static final CurveInterpolator EXPONENTIAL = new ExponentialInterpolator1D();

  /** Log linear interpolator. */
  public static final CurveInterpolator LOG_LINEAR = new LogLinearInterpolator1D();

  /** Double quadratic interpolator. */
  public static final CurveInterpolator DOUBLE_QUADRATIC = new DoubleQuadraticInterpolator1D();

  /** Log natural cubic interpolation with monotonicity filter. */
  public static final CurveInterpolator LOG_NATURAL_CUBIC_MONOTONE = new LogNaturalCubicMonotonicityPreservingInterpolator1D();

  /** Time square interpolator. */
  public static final CurveInterpolator TIME_SQUARE = new TimeSquareInterpolator1D();

}
