/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.interpolator;

import com.opengamma.analytics.math.interpolation.Extrapolator1D;
import com.opengamma.analytics.math.interpolation.FlatExtrapolator1D;
import com.opengamma.analytics.math.interpolation.InterpolatorExtrapolator;
import com.opengamma.analytics.math.interpolation.LinearExtrapolator1D;
import com.opengamma.analytics.math.interpolation.LogLinearExtrapolator1D;
import com.opengamma.analytics.math.interpolation.ProductPolynomialExtrapolator1D;
import com.opengamma.analytics.math.interpolation.QuadraticPolynomialLeftExtrapolator;
import com.opengamma.analytics.math.interpolation.ReciprocalExtrapolator1D;
import com.opengamma.strata.basics.interpolator.CurveExtrapolator;

/**
 * The standard set of curve extrapolators.
 * <p>
 * These are referenced from {@link CurveExtrapolators} where their name is used to look up an
 * instance of {@link CurveExtrapolator}. This allows them to be referenced statically like a
 * constant but also allows them to be redefined and new instances added.
 * <p>
 * The extrapolators are all implementations of {@link Extrapolator1D} for compatibility with legacy code.
 * This should be regarded as an implementation detail and is likely to change soon.
 */
final class StandardCurveExtrapolators {

  /** Linear extrapolator. */
  public static final CurveExtrapolator LINEAR = new LinearExtrapolator1D();

  /** Log linear extrapolator. */
  public static final CurveExtrapolator LOG_LINEAR = new LogLinearExtrapolator1D();

  /** Quadratic left extrapolator. */
  public static final CurveExtrapolator QUADRATIC_LEFT = new QuadraticPolynomialLeftExtrapolator();

  /** Product polynomial extrapolator. */
  public static final CurveExtrapolator PRODUCT_POLYNOMIAL = new ProductPolynomialExtrapolator1D();

  /** Reciprocal extrapolator. */
  public static final CurveExtrapolator RECIPROCAL = new ReciprocalExtrapolator1D();

  /** Flat extrapolator. */
  public static final CurveExtrapolator FLAT = new FlatExtrapolator1D();

  /** Extrapolator that does no extrapolation and delegates to the interpolator. */
  public static final CurveExtrapolator INTERPOLATOR = new InterpolatorExtrapolator();

  // Private constructor as this class only exists to hold the extrapolator constants
  private StandardCurveExtrapolators() {
  }

}
