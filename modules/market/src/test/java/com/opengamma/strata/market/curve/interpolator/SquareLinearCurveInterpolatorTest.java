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
 * Test {@link SquareLinearCurveInterpolator}.
 */
public class SquareLinearCurveInterpolatorTest {

  private static final CurveInterpolator SQUARE_LINEAR_INTERPOLATOR = SquareLinearCurveInterpolator.INSTANCE;
  private static final CurveExtrapolator FLAT_EXTRAPOLATOR = CurveExtrapolators.FLAT;

  private static final DoubleArray X_DATA = DoubleArray.of(0.0, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);
  private static final DoubleArray X_TEST = DoubleArray.of(0.2, 1.1, 2.3);
  private static final DoubleArray Y_TEST = DoubleArray.of(3.5355339059327378, 2.98475292109749, 5.1478150704934995);
  private static final double TOL = 1.e-12;

  @Test
  public void test_basics() {
    assertThat(SQUARE_LINEAR_INTERPOLATOR.getName()).isEqualTo(SquareLinearCurveInterpolator.NAME);
    assertThat(SQUARE_LINEAR_INTERPOLATOR.toString()).isEqualTo(SquareLinearCurveInterpolator.NAME);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_interpolation() {
    BoundCurveInterpolator bci = SQUARE_LINEAR_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertThat(bci.interpolate(X_DATA.get(i))).isCloseTo(Y_DATA.get(i), offset(TOL));
    }
    for (int i = 0; i < X_TEST.size(); i++) {
      assertThat(bci.interpolate(X_TEST.get(i))).isCloseTo(Y_TEST.get(i), offset(TOL));
    }
  }

  @Test
  public void test_firstDerivative() {
    BoundCurveInterpolator bci = SQUARE_LINEAR_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    double eps = 1e-8;
    double lo = bci.interpolate(0.2);
    double hi = bci.interpolate(0.2 + eps);
    double deriv = (hi - lo) / eps;
    assertThat(bci.firstDerivative(0.2)).isCloseTo(deriv, offset(1e-6));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_firstNode() {
    BoundCurveInterpolator bci = SQUARE_LINEAR_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    assertThat(bci.interpolate(0.0)).isCloseTo(3.0, offset(TOL));
    assertThat(bci.parameterSensitivity(0.0).get(0)).isCloseTo(1d, offset(TOL));
    assertThat(bci.parameterSensitivity(0.0).get(1)).isCloseTo(0d, offset(TOL));
  }

  @Test
  public void test_allNodes() {
    BoundCurveInterpolator bci = SQUARE_LINEAR_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertThat(bci.interpolate(X_DATA.get(i))).isCloseTo(Y_DATA.get(i), offset(TOL));
    }
  }

  @Test
  public void test_lastNode() {
    BoundCurveInterpolator bci = SQUARE_LINEAR_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    assertThat(bci.interpolate(5.0)).isCloseTo(2.0, offset(TOL));
    assertThat(bci.parameterSensitivity(5.0).get(X_DATA.size() - 2)).isCloseTo(0d, offset(TOL));
    assertThat(bci.parameterSensitivity(5.0).get(X_DATA.size() - 1)).isCloseTo(1d, offset(TOL));
  }

  @Test
  public void test_interpolatorExtrapolator() {
    DoubleArray xValues = DoubleArray.of(1, 2, 3);
    DoubleArray yValues = DoubleArray.of(2, 3, 5);
    CurveExtrapolator extrap = InterpolatorCurveExtrapolator.INSTANCE;
    BoundCurveInterpolator bci = SQUARE_LINEAR_INTERPOLATOR.bind(xValues, yValues, extrap, extrap);
    assertThat(bci.interpolate(0.5)).isCloseTo(calc(0.5, 1, 2, 2, 3), offset(TOL));
    assertThat(bci.interpolate(1)).isCloseTo(2, offset(TOL));
    assertThat(bci.interpolate(1.5)).isCloseTo(calc(1.5, 1, 2, 2, 3), offset(TOL));
    assertThat(bci.interpolate(2)).isCloseTo(3, offset(TOL));
    assertThat(bci.interpolate(2.5)).isCloseTo(calc(2.5, 2, 3, 3, 5), offset(TOL));
    assertThat(bci.interpolate(3)).isCloseTo(5, offset(TOL));
    assertThat(bci.interpolate(3.5)).isCloseTo(calc(3.5, 2, 3, 3, 5), offset(TOL));
  }

  private double calc(double xValue, double x1, double x2, double y1, double y2) {
    double w = (x2 - xValue) / (x2 - x1);
    double y21 = y1 * y1;
    double y22 = y2 * y2;
    double ySq = w * y21 + (1.0 - w) * y22;
    return Math.sqrt(ySq);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(SQUARE_LINEAR_INTERPOLATOR);
  }

}
