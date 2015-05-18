/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.interpolator;

import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.strata.basics.interpolator.OneDimensionalInterpolator;

/**
 * The standard set of one dimensional interpolators.
 * <p>
 * Currently they are all implementations of {@link Interpolator1D} for compatibility with legacy code.
 * This should be regarded as an implementation detail and will change.
 */
public final class OneDimensionalInterpolators {

  /** Linear interpolator. */
  public static final OneDimensionalInterpolator LINEAR =
      OneDimensionalInterpolator.of(StandardOneDimensionalInterpolators.LINEAR.getName());

  /** Exponential interpolator. */
  public static final OneDimensionalInterpolator EXPONENTIAL =
      OneDimensionalInterpolator.of(StandardOneDimensionalInterpolators.EXPONENTIAL.getName());

  /** Log linear interpolator. */
  public static final OneDimensionalInterpolator LOG_LINEAR =
      OneDimensionalInterpolator.of(StandardOneDimensionalInterpolators.LOG_LINEAR.getName());

  /** Double quadratic interpolator. */
  public static final OneDimensionalInterpolator DOUBLE_QUADRATIC =
      OneDimensionalInterpolator.of(StandardOneDimensionalInterpolators.DOUBLE_QUADRATIC.getName());

  /** Log natural cubic interpolation with monotonicity filter. */
  public static final OneDimensionalInterpolator LOG_NATURAL_CUBIC_MONOTONE =
      OneDimensionalInterpolator.of(StandardOneDimensionalInterpolators.LOG_NATURAL_CUBIC_MONOTONE.getName());

  /** Time square interpolator. */
  public static final OneDimensionalInterpolator TIME_SQUARE =
      OneDimensionalInterpolator.of(StandardOneDimensionalInterpolators.TIME_SQUARE.getName());
}
