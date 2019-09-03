/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link LogLinearCurveInterpolator}.
 */
public class LogLinearCurveInterpolatorTest {

  private static final CurveInterpolator LL_INTERPOLATOR = LogLinearCurveInterpolator.INSTANCE;
  private static final CurveExtrapolator FLAT_EXTRAPOLATOR = CurveExtrapolators.FLAT;

  private static final DoubleArray X_DATA = DoubleArray.of(0.0, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);
  private static final DoubleArray Y_DATA_LOG = DoubleArray.of(
      Math.log(3.0), Math.log(4.0), Math.log(3.1), Math.log(2.0), Math.log(7.0), Math.log(2.0));
  private static final double TOL = 1.e-12;
  private static final double EPS = 1e-9;

  @Test
  public void test_basics() {
    assertThat(LL_INTERPOLATOR.getName()).isEqualTo(LogLinearCurveInterpolator.NAME);
    assertThat(LL_INTERPOLATOR.toString()).isEqualTo(LogLinearCurveInterpolator.NAME);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_interpolation() {
    BoundCurveInterpolator bci = LL_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertThat(bci.interpolate(X_DATA.get(i))).isCloseTo(Y_DATA.get(i), offset(TOL));
    }
    // log-linear same as linear where y-values have had log applied
    BoundCurveInterpolator bciLinear = CurveInterpolators.LINEAR.bind(X_DATA, Y_DATA_LOG, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    assertThat(Math.log(bci.interpolate(0.2))).isCloseTo(bciLinear.interpolate(0.2), offset(EPS));
    assertThat(Math.log(bci.interpolate(0.8))).isCloseTo(bciLinear.interpolate(0.8), offset(EPS));
    assertThat(Math.log(bci.interpolate(1.1))).isCloseTo(bciLinear.interpolate(1.1), offset(EPS));
    assertThat(Math.log(bci.interpolate(2.1))).isCloseTo(bciLinear.interpolate(2.1), offset(EPS));
    assertThat(Math.log(bci.interpolate(3.4))).isCloseTo(bciLinear.interpolate(3.4), offset(EPS));
  }

  @Test
  public void test_firstDerivative() {
    BoundCurveInterpolator bci = LL_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    double eps = 1e-8;
    double lo = bci.interpolate(0.2);
    double hi = bci.interpolate(0.2 + eps);
    double deriv = (hi - lo) / eps;
    assertThat(bci.firstDerivative(0.2)).isCloseTo(deriv, offset(1e-6));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_firstNode() {
    BoundCurveInterpolator bci = LL_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    assertThat(bci.interpolate(0.0)).isCloseTo(3.0, offset(TOL));
    assertThat(bci.firstDerivative(0.0)).isCloseTo(bci.firstDerivative(0.00000001), offset(1e-6));
  }

  @Test
  public void test_allNodes() {
    BoundCurveInterpolator bci = LL_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertThat(bci.interpolate(X_DATA.get(i))).isCloseTo(Y_DATA.get(i), offset(TOL));
    }
  }

  @Test
  public void test_lastNode() {
    BoundCurveInterpolator bci = LL_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    assertThat(bci.interpolate(5.0)).isCloseTo(2.0, offset(TOL));
    assertThat(bci.firstDerivative(5.0)).isCloseTo(bci.firstDerivative(4.99999999), offset(1e-6));
  }

  @Test
  public void test_interpolatorExtrapolator() {
    DoubleArray xValues = DoubleArray.of(1, 2, 3);
    DoubleArray yValues = DoubleArray.of(2, 3, 5);
    DoubleArray yValuesLog = DoubleArray.of(Math.log(2), Math.log(3), Math.log(5));
    CurveExtrapolator extrap = InterpolatorCurveExtrapolator.INSTANCE;
    // log-linear same as linear where y-values have had log applied
    BoundCurveInterpolator bciLinear = CurveInterpolators.LINEAR.bind(xValues, yValuesLog, extrap, extrap);
    BoundCurveInterpolator bci = LL_INTERPOLATOR.bind(xValues, yValues, extrap, extrap);
    assertThat(Math.log(bci.interpolate(0.5))).isCloseTo(bciLinear.interpolate(0.5), offset(EPS));
    assertThat(Math.log(bci.interpolate(1))).isCloseTo(bciLinear.interpolate(1), offset(EPS));
    assertThat(Math.log(bci.interpolate(1.5))).isCloseTo(bciLinear.interpolate(1.5), offset(EPS));
    assertThat(Math.log(bci.interpolate(2))).isCloseTo(bciLinear.interpolate(2), offset(EPS));
    assertThat(Math.log(bci.interpolate(2.5))).isCloseTo(bciLinear.interpolate(2.5), offset(EPS));
    assertThat(Math.log(bci.interpolate(3))).isCloseTo(bciLinear.interpolate(3), offset(EPS));
    assertThat(Math.log(bci.interpolate(3.5))).isCloseTo(bciLinear.interpolate(3.5), offset(EPS));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(LL_INTERPOLATOR);
  }

}
