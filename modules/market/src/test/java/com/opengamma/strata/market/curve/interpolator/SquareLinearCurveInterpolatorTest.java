/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link SquareLinearCurveInterpolator}.
 */
@Test
public class SquareLinearCurveInterpolatorTest {

  private static final CurveInterpolator SQUARE_LINEAR_INTERPOLATOR = SquareLinearCurveInterpolator.INSTANCE;
  private static final CurveExtrapolator FLAT_EXTRAPOLATOR = CurveExtrapolators.FLAT;

  private static final DoubleArray X_DATA = DoubleArray.of(0.0, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);
  private static final DoubleArray X_TEST = DoubleArray.of(0.2, 1.1, 2.3);
  private static final DoubleArray Y_TEST = DoubleArray.of(3.5355339059327378, 2.98475292109749, 5.1478150704934995);
  private static final double TOL = 1.e-12;

  public void test_basics() {
    assertEquals(SQUARE_LINEAR_INTERPOLATOR.getName(), SquareLinearCurveInterpolator.NAME);
    assertEquals(SQUARE_LINEAR_INTERPOLATOR.toString(), SquareLinearCurveInterpolator.NAME);
  }

  //-------------------------------------------------------------------------
  public void test_interpolation() {
    BoundCurveInterpolator bci = SQUARE_LINEAR_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertEquals(bci.interpolate(X_DATA.get(i)), Y_DATA.get(i), TOL);
    }
    for (int i = 0; i < X_TEST.size(); i++) {
      assertEquals(bci.interpolate(X_TEST.get(i)), Y_TEST.get(i), TOL);
    }
  }

  public void test_firstDerivative() {
    BoundCurveInterpolator bci = SQUARE_LINEAR_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    double eps = 1e-8;
    double lo = bci.interpolate(0.2);
    double hi = bci.interpolate(0.2 + eps);
    double deriv = (hi - lo) / eps;
    assertEquals(bci.firstDerivative(0.2), deriv, 1e-6);
  }

  //-------------------------------------------------------------------------
  public void test_firstNode() {
    BoundCurveInterpolator bci = SQUARE_LINEAR_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    assertEquals(bci.interpolate(0.0), 3.0, TOL);
    assertEquals(bci.parameterSensitivity(0.0).get(0), 1d, TOL);
    assertEquals(bci.parameterSensitivity(0.0).get(1), 0d, TOL);
  }

  public void test_allNodes() {
    BoundCurveInterpolator bci = SQUARE_LINEAR_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertEquals(bci.interpolate(X_DATA.get(i)), Y_DATA.get(i), TOL);
    }
  }

  public void test_lastNode() {
    BoundCurveInterpolator bci = SQUARE_LINEAR_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    assertEquals(bci.interpolate(5.0), 2.0, TOL);
    assertEquals(bci.parameterSensitivity(5.0).get(X_DATA.size() - 2), 0d, TOL);
    assertEquals(bci.parameterSensitivity(5.0).get(X_DATA.size() - 1), 1d, TOL);
  }

  public void test_interpolatorExtrapolator() {
    DoubleArray xValues = DoubleArray.of(1, 2, 3);
    DoubleArray yValues = DoubleArray.of(2, 3, 5);
    CurveExtrapolator extrap = InterpolatorCurveExtrapolator.INSTANCE;
    BoundCurveInterpolator bci = SQUARE_LINEAR_INTERPOLATOR.bind(xValues, yValues, extrap, extrap);
    assertEquals(bci.interpolate(0.5), calc(0.5, 1, 2, 2, 3), TOL);
    assertEquals(bci.interpolate(1), 2, TOL);
    assertEquals(bci.interpolate(1.5), calc(1.5, 1, 2, 2, 3), TOL);
    assertEquals(bci.interpolate(2), 3, TOL);
    assertEquals(bci.interpolate(2.5), calc(2.5, 2, 3, 3, 5), TOL);
    assertEquals(bci.interpolate(3), 5, TOL);
    assertEquals(bci.interpolate(3.5), calc(3.5, 2, 3, 3, 5), TOL);
  }

  private double calc(double xValue, double x1, double x2, double y1, double y2) {
    double w = (x2 - xValue) / (x2 - x1);
    double y21 = y1 * y1;
    double y22 = y2 * y2;
    double ySq = w * y21 + (1.0 - w) * y22;
    return Math.sqrt(ySq);
  }

  //-------------------------------------------------------------------------
  public void test_serialization() {
    assertSerialization(SQUARE_LINEAR_INTERPOLATOR);
  }

}
