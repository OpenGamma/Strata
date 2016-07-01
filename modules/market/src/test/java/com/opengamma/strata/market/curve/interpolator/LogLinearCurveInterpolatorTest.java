/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link LogLinearCurveInterpolator}.
 */
@Test
public class LogLinearCurveInterpolatorTest {

  private static final CurveInterpolator LL_INTERPOLATOR = LogLinearCurveInterpolator.INSTANCE;
  private static final CurveExtrapolator FLAT_EXTRAPOLATOR = CurveExtrapolators.FLAT;

  private static final DoubleArray X_DATA = DoubleArray.of(0.0, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);
  private static final DoubleArray Y_DATA_LOG = DoubleArray.of(
      Math.log(3.0), Math.log(4.0), Math.log(3.1), Math.log(2.0), Math.log(7.0), Math.log(2.0));
  private static final double TOL = 1.e-12;
  private static final double EPS = 1e-9;

  public void test_basics() {
    assertEquals(LL_INTERPOLATOR.getName(), LogLinearCurveInterpolator.NAME);
    assertEquals(LL_INTERPOLATOR.toString(), LogLinearCurveInterpolator.NAME);
  }

  //-------------------------------------------------------------------------
  public void test_interpolation() {
    BoundCurveInterpolator bci = LL_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertEquals(bci.interpolate(X_DATA.get(i)), Y_DATA.get(i), TOL);
    }
    // log-linear same as linear where y-values have had log applied
    BoundCurveInterpolator bciLinear = CurveInterpolators.LINEAR.bind(X_DATA, Y_DATA_LOG, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    assertEquals(Math.log(bci.interpolate(0.2)), bciLinear.interpolate(0.2), EPS);
    assertEquals(Math.log(bci.interpolate(0.8)), bciLinear.interpolate(0.8), EPS);
    assertEquals(Math.log(bci.interpolate(1.1)), bciLinear.interpolate(1.1), EPS);
    assertEquals(Math.log(bci.interpolate(2.1)), bciLinear.interpolate(2.1), EPS);
    assertEquals(Math.log(bci.interpolate(3.4)), bciLinear.interpolate(3.4), EPS);
  }

  public void test_firstDerivative() {
    BoundCurveInterpolator bci = LL_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    double eps = 1e-8;
    double lo = bci.interpolate(0.2);
    double hi = bci.interpolate(0.2 + eps);
    double deriv = (hi - lo) / eps;
    assertEquals(bci.firstDerivative(0.2), deriv, 1e-6);
  }

  //-------------------------------------------------------------------------
  public void test_firstNode() {
    BoundCurveInterpolator bci = LL_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    assertEquals(bci.interpolate(0.0), 3.0, TOL);
    assertEquals(bci.firstDerivative(0.0), bci.firstDerivative(0.00000001), 1e-6);
  }

  public void test_allNodes() {
    BoundCurveInterpolator bci = LL_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertEquals(bci.interpolate(X_DATA.get(i)), Y_DATA.get(i), TOL);
    }
  }

  public void test_lastNode() {
    BoundCurveInterpolator bci = LL_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    assertEquals(bci.interpolate(5.0), 2.0, TOL);
    assertEquals(bci.firstDerivative(5.0), bci.firstDerivative(4.99999999), 1e-6);
  }

  //-------------------------------------------------------------------------
  public void test_serialization() {
    assertSerialization(LL_INTERPOLATOR);
  }

}
