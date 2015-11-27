/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

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
 */
final class StandardCurveExtrapolators {

  // Flat extrapolator.
  public static final CurveExtrapolator FLAT = 
      new ImmutableCurveExtrapolator("Flat", new FlatExtrapolator1D());
  // Linear extrapolator.
  public static final CurveExtrapolator LINEAR = 
      new ImmutableCurveExtrapolator("Linear", new LinearExtrapolator1D());
  // Log linear extrapolator.
  public static final CurveExtrapolator LOG_LINEAR = 
      new ImmutableCurveExtrapolator("LogLinear", new LogLinearExtrapolator1D());
  // Quadratic left extrapolator.
  public static final CurveExtrapolator QUADRATIC_LEFT = 
      new ImmutableCurveExtrapolator("QuadraticLeft", new QuadraticPolynomialLeftExtrapolator());
  // Product polynomial extrapolator.
  public static final CurveExtrapolator PRODUCT_POLYNOMIAL = 
      new ImmutableCurveExtrapolator("ProductPolynomial", new ProductPolynomialExtrapolator1D());
  // Reciprocal extrapolator.
  public static final CurveExtrapolator RECIPROCAL = 
      new ImmutableCurveExtrapolator("Reciprocal", new ReciprocalExtrapolator1D());
  // Extrapolator that does no extrapolation and delegates to the interpolator.
  public static final CurveExtrapolator INTERPOLATOR =
      new ImmutableCurveExtrapolator("Interpolator", new InterpolatorExtrapolator());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardCurveExtrapolators() {
  }

}
