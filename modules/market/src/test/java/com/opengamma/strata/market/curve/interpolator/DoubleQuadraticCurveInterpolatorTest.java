/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link DoubleQuadraticCurveInterpolator}.
 */
public class DoubleQuadraticCurveInterpolatorTest {

  private static final Random RANDOM = new Random(0L);
  private static final CurveInterpolator DQ_INTERPOLATOR = DoubleQuadraticCurveInterpolator.INSTANCE;
  private static final CurveExtrapolator FLAT_EXTRAPOLATOR = CurveExtrapolators.FLAT;

  private static final DoubleArray X_DATA = DoubleArray.of(0.0, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);
  private static final double[] X_TEST = new double[] {0, 0.3, 1.0, 2.0, 4.5, 5.0};
  private static final double[] Y_TEST = new double[] {3.0, 3.87, 3.1, 2.619393939, 5.068181818, 2.0};
  private static final double TOL = 1.e-12;
  private static final double EPS = 1e-7;
  private static final DoubleArray X_SENS;
  private static final DoubleArray Y_SENS;
  static {
    double a = -0.045;
    double b = 0.03;
    double c = 0.3;
    double d = 0.05;
    double[] x = new double[] {0.0, 0.5, 1.0, 2.0, 3.0, 5.0, 7.0, 10.0, 15.0, 17.5, 20.0, 25.0, 30.0};
    double[] y = new double[x.length];
    for (int i = 0; i < x.length; i++) {
      y[i] = (a + b * x[i]) * Math.exp(-c * x[i]) + d;
    }
    X_SENS = DoubleArray.copyOf(x);
    Y_SENS = DoubleArray.copyOf(y);
  }

  @Test
  public void test_basics() {
    assertThat(DQ_INTERPOLATOR.getName()).isEqualTo(DoubleQuadraticCurveInterpolator.NAME);
    assertThat(DQ_INTERPOLATOR.toString()).isEqualTo(DoubleQuadraticCurveInterpolator.NAME);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_interpolation() {
    BoundCurveInterpolator bci = DQ_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int i = 0; i < X_TEST.length; i++) {
      assertThat(bci.interpolate(X_TEST[i])).isCloseTo(Y_TEST[i], offset(1e-8));
    }
  }

  @Test
  public void test_oneInterval() {
    DoubleArray x = DoubleArray.of(1.4, 1.8);
    DoubleArray y = DoubleArray.of(0.34, 0.56);
    BoundCurveInterpolator bci = DQ_INTERPOLATOR.bind(x, y, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    double value = bci.interpolate(1.6);
    assertThat((y.get(0) + y.get(1)) / 2).isCloseTo(value, offset(0.0));

    double m = (y.get(1) - y.get(0)) / (x.get(1) - x.get(0));
    assertThat(bci.firstDerivative(1.5)).isCloseTo(m, offset(0.0));
    assertThat(bci.firstDerivative(x.get(1))).isCloseTo(m, offset(0.0));
  }

  @Test
  public void test_firstDerivative() {
    BoundCurveInterpolator bci = DQ_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    double eps = 1e-8;
    double lo = bci.interpolate(0.2);
    double hi = bci.interpolate(0.2 + eps);
    double deriv = (hi - lo) / eps;
    assertThat(bci.firstDerivative(0.2)).isCloseTo(deriv, offset(1e-6));
  }

  @Test
  public void test_firstDerivative2() {
    double a = 1.34;
    double b = 7.0 / 3.0;
    double c = -0.52;
    double[] x = new double[] {-11.0 / 2.3, 0.0, 0.01, 2.71, 17.0 / 3.2};
    int n = x.length;
    double[] y = new double[n];
    for (int i = 0; i < n; i++) {
      y[i] = a + b * x[i] + c * x[i] * x[i];
    }
    BoundCurveInterpolator bci =
        DQ_INTERPOLATOR.bind(DoubleArray.copyOf(x), DoubleArray.copyOf(y), FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    double grad = bci.firstDerivative(x[n - 1]);
    assertThat(b + 2 * c * x[n - 1]).isCloseTo(grad, offset(1e-15));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_sensitivities() {
    BoundCurveInterpolator bci = DQ_INTERPOLATOR.bind(X_SENS, Y_SENS, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    double lastXValue = X_SENS.get(X_SENS.size() - 1);
    for (int i = 0; i < 100; i++) {
      double t = lastXValue * RANDOM.nextDouble();
      DoubleArray sensitivity = bci.parameterSensitivity(t);
      assertThat(sensitivity.sum()).isCloseTo(1d, offset(TOL));
    }
  }

  @Test
  public void test_sensitivityEdgeCase() {
    BoundCurveInterpolator bci = DQ_INTERPOLATOR.bind(X_SENS, Y_SENS, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    double lastXValue = X_SENS.get(X_SENS.size() - 1);
    DoubleArray sensitivity = bci.parameterSensitivity(lastXValue);
    for (int i = 0; i < sensitivity.size() - 1; i++) {
      assertThat(sensitivity.get(i)).isCloseTo(0, offset(EPS));
    }
    assertThat(sensitivity.get(sensitivity.size() - 1)).isCloseTo(1, offset(EPS));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_firstNode() {
    BoundCurveInterpolator bci = DQ_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    assertThat(bci.interpolate(0.0)).isCloseTo(3.0, offset(TOL));
    assertThat(bci.firstDerivative(0.0)).isCloseTo(bci.firstDerivative(0.00000001), offset(1e-6));
  }

  @Test
  public void test_allNodes() {
    BoundCurveInterpolator bci = DQ_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertThat(bci.interpolate(X_DATA.get(i))).isCloseTo(Y_DATA.get(i), offset(TOL));
    }
  }

  @Test
  public void test_lastNode() {
    BoundCurveInterpolator bci = DQ_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    assertThat(bci.interpolate(5.0)).isCloseTo(2.0, offset(TOL));
    assertThat(bci.firstDerivative(5.0)).isCloseTo(bci.firstDerivative(4.99999999), offset(1e-6));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(DQ_INTERPOLATOR);
  }

}
