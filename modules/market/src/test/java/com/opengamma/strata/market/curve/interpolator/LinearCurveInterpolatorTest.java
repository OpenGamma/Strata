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
 * Test {@link LinearCurveInterpolator}.
 */
public class LinearCurveInterpolatorTest {

  private static final CurveInterpolator LINEAR_INTERPOLATOR = LinearCurveInterpolator.INSTANCE;
  private static final CurveExtrapolator FLAT_EXTRAPOLATOR = CurveExtrapolators.FLAT;

  private static final DoubleArray X_DATA = DoubleArray.of(0.0, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);
  private static final DoubleArray X_TEST = DoubleArray.of(0.2, 1.1, 2.3);
  private static final DoubleArray Y_TEST = DoubleArray.of(3.5, 3.1 - (1.1 / 8), 4.5);
  private static final double TOL = 1.e-12;

  @Test
  public void test_basics() {
    assertThat(LINEAR_INTERPOLATOR.getName()).isEqualTo(LinearCurveInterpolator.NAME);
    assertThat(LINEAR_INTERPOLATOR.toString()).isEqualTo(LinearCurveInterpolator.NAME);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_interpolation() {
    BoundCurveInterpolator bci = LINEAR_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertThat(bci.interpolate(X_DATA.get(i))).isCloseTo(Y_DATA.get(i), offset(TOL));
    }
    for (int i = 0; i < X_TEST.size(); i++) {
      assertThat(bci.interpolate(X_TEST.get(i))).isCloseTo(Y_TEST.get(i), offset(TOL));
    }
  }

  @Test
  public void test_firstDerivative() {
    BoundCurveInterpolator bci = LINEAR_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    double eps = 1e-8;
    double lo = bci.interpolate(0.2);
    double hi = bci.interpolate(0.2 + eps);
    double deriv = (hi - lo) / eps;
    assertThat(bci.firstDerivative(0.2)).isCloseTo(deriv, offset(1e-6));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_firstNode() {
    BoundCurveInterpolator bci = LINEAR_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    assertThat(bci.interpolate(0.0)).isCloseTo(3.0, offset(TOL));
    assertThat(bci.firstDerivative(0.0)).isCloseTo(bci.firstDerivative(0.01), offset(TOL));
    assertThat(bci.parameterSensitivity(0.0).get(0)).isCloseTo(1d, offset(TOL));
    assertThat(bci.parameterSensitivity(0.0).get(1)).isCloseTo(0d, offset(TOL));
  }

  @Test
  public void test_allNodes() {
    BoundCurveInterpolator bci = LINEAR_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertThat(bci.interpolate(X_DATA.get(i))).isCloseTo(Y_DATA.get(i), offset(TOL));
    }
  }

  @Test
  public void test_lastNode() {
    BoundCurveInterpolator bci = LINEAR_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    assertThat(bci.interpolate(5.0)).isCloseTo(2.0, offset(TOL));
    assertThat(bci.firstDerivative(5.0)).isCloseTo(bci.firstDerivative(4.99), offset(TOL));
    assertThat(bci.parameterSensitivity(5.0).get(X_DATA.size() - 2)).isCloseTo(0d, offset(TOL));
    assertThat(bci.parameterSensitivity(5.0).get(X_DATA.size() - 1)).isCloseTo(1d, offset(TOL));
  }

  @Test
  public void test_interpolatorExtrapolator() {
    DoubleArray xValues = DoubleArray.of(1, 2, 3);
    DoubleArray yValues = DoubleArray.of(2, 3, 5);
    CurveExtrapolator extrap = InterpolatorCurveExtrapolator.INSTANCE;
    BoundCurveInterpolator boundInterp = LINEAR_INTERPOLATOR.bind(xValues, yValues, extrap, extrap);
    assertThat(boundInterp.interpolate(0.5)).isCloseTo(1.5, offset(TOL));
    assertThat(boundInterp.interpolate(1)).isCloseTo(2, offset(TOL));
    assertThat(boundInterp.interpolate(1.5)).isCloseTo(2.5, offset(TOL));
    assertThat(boundInterp.interpolate(2)).isCloseTo(3, offset(TOL));
    assertThat(boundInterp.interpolate(2.5)).isCloseTo(4, offset(TOL));
    assertThat(boundInterp.interpolate(3)).isCloseTo(5, offset(TOL));
    assertThat(boundInterp.interpolate(3.5)).isCloseTo(6, offset(TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(LINEAR_INTERPOLATOR);
  }

}
