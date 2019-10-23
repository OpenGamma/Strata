/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link LogNaturalSplineDiscountFactorCurveInterpolator}.
 */
public class LogNaturalSplineDiscountFactorCurveInterpolatorTest {

  private static final CurveInterpolator LNDFC_INTERPOLATOR = LogNaturalSplineDiscountFactorCurveInterpolator.INSTANCE;
  private static final CurveExtrapolator INTERPOLATOR_EXTRAPOLATOR = CurveExtrapolators.INTERPOLATOR;

  private static final DoubleArray X_DATA = DoubleArray.of(0.2, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);
  private static final int NUM_DATA = X_DATA.size();
  private static final DoubleArray X_DATA_CLAMPED;
  private static final DoubleArray Y_DATA_CLAMPED;
  static {
    double[] xValues = new double[NUM_DATA + 1];
    double[] yValues = new double[NUM_DATA + 1];
    xValues[0] = 0d;
    yValues[0] = 0d;
    for (int i = 0; i < NUM_DATA; ++i) {
      xValues[i + 1] = X_DATA.get(i);
      yValues[i + 1] = Math.log(Y_DATA.get(i));
    }
    X_DATA_CLAMPED = DoubleArray.ofUnsafe(xValues);
    Y_DATA_CLAMPED = DoubleArray.ofUnsafe(yValues);
  }
  private static final DoubleArray X_TEST = DoubleArray.of(1., 1.3, 1.6);
  private static final DoubleArray Y_TEST = DoubleArray.of(3.1, 2.375168445874886, 1.9885112466306356);
  private static final DoubleArray X_TEST_1 = DoubleArray.of(0.0, 0.075, 0.2, 0.6, 1.8, 3.6, 5.0, 7.4);

  private static final double TOL = 1.e-12;

  @Test
  public void test_basics() {
    assertThat(LNDFC_INTERPOLATOR.getName()).isEqualTo(LogNaturalSplineDiscountFactorCurveInterpolator.NAME);
    assertThat(LNDFC_INTERPOLATOR.toString()).isEqualTo(LogNaturalSplineDiscountFactorCurveInterpolator.NAME);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_interpolation() {
    BoundCurveInterpolator bci = LNDFC_INTERPOLATOR.bind(X_DATA, Y_DATA, INTERPOLATOR_EXTRAPOLATOR, INTERPOLATOR_EXTRAPOLATOR);
    for (int i = 0; i < X_TEST.size(); i++) {
      assertThat(bci.interpolate(X_TEST.get(i))).isCloseTo(Y_TEST.get(i), offset(TOL));
    }
    BoundCurveInterpolator bciUnderlying = NaturalSplineCurveInterpolator.INSTANCE.bind(
        X_DATA_CLAMPED, Y_DATA_CLAMPED, INTERPOLATOR_EXTRAPOLATOR, INTERPOLATOR_EXTRAPOLATOR);
    for (int i = X_TEST_1.size() - 1; i < X_TEST_1.size(); i++) {
      assertThat(bci.interpolate(X_TEST_1.get(i))).isCloseTo(Math.exp(bciUnderlying.interpolate(X_TEST_1.get(i))), offset(TOL));
    }
  }

  @Test
  public void test_firstDerivative() {
    BoundCurveInterpolator bci = LNDFC_INTERPOLATOR.bind(X_DATA, Y_DATA, INTERPOLATOR_EXTRAPOLATOR, INTERPOLATOR_EXTRAPOLATOR);
    double eps = 1e-7;
    for (int i = 0; i < X_TEST_1.size(); ++i) {
      double key = X_TEST_1.get(i);
      double lo = bci.interpolate(key - eps);
      double hi = bci.interpolate(key + eps);
      double deriv = 0.5 * (hi - lo) / eps;
      assertThat(bci.firstDerivative(key)).isCloseTo(deriv, offset(eps));
    }
  }

  @Test
  public void test_parameterSensitivity() {
    BoundCurveInterpolator bci = LNDFC_INTERPOLATOR.bind(X_DATA, Y_DATA, INTERPOLATOR_EXTRAPOLATOR, INTERPOLATOR_EXTRAPOLATOR);
    double eps = 1e-7;
    for (int i = 0; i < X_TEST_1.size(); ++i) {
      double key = X_TEST_1.get(i);
      DoubleArray computed = bci.parameterSensitivity(key);
      for (int j = 0; j < NUM_DATA; ++j) {
        BoundCurveInterpolator bciUp = LNDFC_INTERPOLATOR.bind(
            X_DATA, Y_DATA.with(j, Y_DATA.get(j) + eps), INTERPOLATOR_EXTRAPOLATOR, INTERPOLATOR_EXTRAPOLATOR);
        BoundCurveInterpolator bciDw = LNDFC_INTERPOLATOR.bind(
            X_DATA, Y_DATA.with(j, Y_DATA.get(j) - eps), INTERPOLATOR_EXTRAPOLATOR, INTERPOLATOR_EXTRAPOLATOR);
        double expected = 0.5 * (bciUp.interpolate(key) - bciDw.interpolate(key)) / eps;
        assertThat(computed.get(j)).isCloseTo(expected, offset(eps));
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_firstNode() {
    BoundCurveInterpolator bci = LNDFC_INTERPOLATOR.bind(X_DATA, Y_DATA, INTERPOLATOR_EXTRAPOLATOR, INTERPOLATOR_EXTRAPOLATOR);
    assertThat(bci.interpolate(0.0)).isCloseTo(1.0, offset(TOL));
    assertThat(bci.firstDerivative(0.0)).isCloseTo(bci.firstDerivative(0.00000001), offset(1e-6));
  }

  @Test
  public void test_allNodes() {
    BoundCurveInterpolator bci = LNDFC_INTERPOLATOR.bind(X_DATA, Y_DATA, INTERPOLATOR_EXTRAPOLATOR, INTERPOLATOR_EXTRAPOLATOR);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertThat(bci.interpolate(X_DATA.get(i))).isCloseTo(Y_DATA.get(i), offset(TOL));
    }
  }

  @Test
  public void test_lastNode() {
    BoundCurveInterpolator bci = LNDFC_INTERPOLATOR.bind(X_DATA, Y_DATA, INTERPOLATOR_EXTRAPOLATOR, INTERPOLATOR_EXTRAPOLATOR);
    assertThat(bci.interpolate(5.0)).isCloseTo(2.0, offset(TOL));
    assertThat(bci.firstDerivative(5.0)).isCloseTo(bci.firstDerivative(4.99999999), offset(1e-6));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(LNDFC_INTERPOLATOR);
  }

}
