/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.interpolator;

import com.opengamma.analytics.math.interpolation.DoubleQuadraticInterpolator1D;
import com.opengamma.analytics.math.interpolation.ExponentialInterpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.LinearInterpolator1D;
import com.opengamma.analytics.math.interpolation.LogLinearInterpolator1D;
import com.opengamma.analytics.math.interpolation.LogNaturalCubicMonotonicityPreservingInterpolator1D;
import com.opengamma.analytics.math.interpolation.TimeSquareInterpolator1D;
import com.opengamma.strata.basics.interpolator.OneDimensionalInterpolator;

/**
 * The standard set of one dimensional interpolators.
 * <p>
 * These are referenced from {@link OneDimensionalInterpolators} where their name is used to look up an
 * instance of {@link OneDimensionalInterpolator}. This allows them to be referenced statically like a
 * constant but also allows them to be redefined and new instances added.
 * <p>
 * Currently they are all implementations of {@link Interpolator1D} for compatibility with legacy code.
 * This should be regarded as an implementation detail and is likely to change soon.
 */
final class StandardOneDimensionalInterpolators {

  /** Linear interpolator. */
  static final OneDimensionalInterpolator LINEAR =
      new LinearInterpolator1D();

  /** Exponential interpolator. */
  static final OneDimensionalInterpolator EXPONENTIAL =
      new ExponentialInterpolator1D();

  /** Log linear interpolator. */
  static final OneDimensionalInterpolator LOG_LINEAR =
      new LogLinearInterpolator1D();

  /** Double quadratic interpolator. */
  static final OneDimensionalInterpolator DOUBLE_QUADRATIC =
      new DoubleQuadraticInterpolator1D();

  /** Log natural cubic interpolation with monotonicity filter. */
  static final OneDimensionalInterpolator LOG_NATURAL_CUBIC_MONOTONE =
      new LogNaturalCubicMonotonicityPreservingInterpolator1D();

  /** Time square interpolator. */
  static final OneDimensionalInterpolator TIME_SQUARE =
      new TimeSquareInterpolator1D();
}
