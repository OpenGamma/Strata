/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.interpolator;

import com.opengamma.strata.basics.interpolator.CurveExtrapolator;
import com.opengamma.strata.math.impl.interpolation.Extrapolator1D;
import com.opengamma.strata.math.impl.interpolation.FlatExtrapolator1D;
import com.opengamma.strata.math.impl.interpolation.InterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.LinearExtrapolator1D;
import com.opengamma.strata.math.impl.interpolation.LogLinearExtrapolator1D;
import com.opengamma.strata.math.impl.interpolation.ProductPolynomialExtrapolator1D;
import com.opengamma.strata.math.impl.interpolation.QuadraticPolynomialLeftExtrapolator;
import com.opengamma.strata.math.impl.interpolation.ReciprocalExtrapolator1D;

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
