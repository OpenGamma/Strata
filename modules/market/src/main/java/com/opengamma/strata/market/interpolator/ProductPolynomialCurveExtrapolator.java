/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import java.io.Serializable;

import com.opengamma.strata.math.impl.function.PiecewisePolynomialFunction1D;
import com.opengamma.strata.math.impl.interpolation.ProductPiecewisePolynomialInterpolator1D;

/**
 * Extrapolator implementation that uses a polynomial function.
 * <p>
 * Given a data set {xValues[i], yValues[i]}, extrapolate {x[i], x[i] * y[i]} by
 * a polynomial function defined by {@link ProductPiecewisePolynomialInterpolator1D}.
 * That is, use polynomial coefficients for the extrapolated interval obtained
 * in {@code ProductPiecewisePolynomialInterpolator1D}.
 */
final class ProductPolynomialCurveExtrapolator
    extends AbstractProductPolynomialCurveExtrapolator
    implements Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The extrapolator name.
   */
  public static final String NAME = "ProductPolynomial";
  /**
   * The function.
   */
  private static final PiecewisePolynomialFunction1D FUNCTION = new PiecewisePolynomialFunction1D();
  /**
   * The extrapolator instance.
   */
  public static final CurveExtrapolator INSTANCE = new ProductPolynomialCurveExtrapolator();

  /**
   * Restricted constructor.
   */
  private ProductPolynomialCurveExtrapolator() {
    super(FUNCTION);
  }

  // resolve instance
  private Object readResolve() {
    return INSTANCE;
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String toString() {
    return NAME;
  }

}
