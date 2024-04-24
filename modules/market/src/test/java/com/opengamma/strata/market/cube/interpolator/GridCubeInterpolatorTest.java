/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.EXPONENTIAL;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.QUADRATIC_LEFT;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.NATURAL_CUBIC_SPLINE;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.NATURAL_SPLINE;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.NATURAL_SPLINE_NONNEGATIVITY_CUBIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.PCHIP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;

/**
 * Test {@link GridCubeInterpolator}
 */
public class GridCubeInterpolatorTest {

  private static final DoubleArray X_DATA = DoubleArray.of(
      0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
      1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
      2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(
      3.0, 3.0, 3.0, 3.0, 4.0, 4.0, 4.0, 4.0, 5.0, 5.0, 5.0, 5.0,
      3.0, 3.0, 3.0, 3.0, 4.0, 4.0, 4.0, 4.0, 5.0, 5.0, 5.0, 5.0,
      3.0, 3.0, 3.0, 3.0, 4.0, 4.0, 4.0, 4.0, 5.0, 5.0, 5.0, 5.0);
  private static final DoubleArray Z_DATA = DoubleArray.of(
      -3.0, 1.0, 3.0, 9.0, -3.0, 1.0, 3.0, 9.0, -3.0, 1.0, 3.0, 9.0,
      -3.0, 1.0, 3.0, 9.0, -3.0, 1.0, 3.0, 9.0, -3.0, 1.0, 3.0, 9.0,
      -3.0, 1.0, 3.0, 9.0, -3.0, 1.0, 3.0, 9.0, -3.0, 1.0, 3.0, 9.0);
  private static final DoubleArray W_DATA = DoubleArray.of(
      3.0, 5.0, 3.1, 2.0, 4.0, 3.5, 2.1, -4.0, -3.0, 2.9, 4.0, 3.0,
      2.0, 4.0, 3.0, 3.5, 2.1, 5.0, 3.5, 2.1, 3.0, -0.5, -2.1, -4.0,
      1.5, 4.5, 2.5, 1.5, -2.2, -4.0, 3.5, 2.1, -4.0, 3.0, 3.5, 2.1);

  private static final DoubleArray X_TEST = DoubleArray.of(0.2, 1.3, 2.5, -1.2);
  private static final DoubleArray Y_TEST = DoubleArray.of(3.4, 4.1, 4.5, 1.2);
  private static final DoubleArray Z_TEST = DoubleArray.of(-1.2, 1.3, 6.5, 11.0);

  private static final DoubleArray W_TEST = DoubleArray.of(
      ((3.0 + (2.0 - 3.0) * 0.2) * (1.0 + 1.2) / (1.0 + 3.0)
          + (5.0 + (4.0 - 5.0) * 0.2) * (-1.2 + 3.0) / (1.0 + 3.0)) * (4.0 - 3.4)
          + ((4.0 + (2.1 - 4.0) * 0.2) * (1.0 + 1.2) / (1.0 + 3.0)
          + (3.5 + (5.0 - 3.5) * 0.2) * (-1.2 + 3.0) / (1.0 + 3.0)) * (3.4 - 3.0),
      ((5.0 + (-4.0 - 5.0) * (1.3 - 1.0)) * (3.0 - 1.3) / (3.0 - 1.0)
          + 3.5 * (1.3 - 1.0) / (3.0 - 1.0)) * (5.0 - 4.1)
          + ((-0.5 + (3.0 + 0.5) * (1.3 - 1.0)) * (3.0 - 1.3) / (3.0 - 1.0)
          + (-2.1 + (3.5 + 2.1) * (1.3 - 1.0)) * (1.3 - 1.0) / (3.0 - 1.0)) * (4.1 - 4.0),
      GridSurfaceInterpolator.of(LINEAR, LINEAR).bind(
          Y_DATA.subArray(25, 36),
          Z_DATA.subArray(25, 36),
          W_DATA.subArray(25, 36)).interpolate(4.5, 6.5),
      2.0);

  private static final Offset<Double> TOL = Offset.strictOffset(1e-12);

  @Test
  public void test_of3() {
    GridCubeInterpolator test = GridCubeInterpolator.of(LINEAR, PCHIP, NATURAL_SPLINE);
    assertThat(test.getXInterpolator()).isEqualTo(LINEAR);
    assertThat(test.getXExtrapolatorLeft()).isEqualTo(FLAT);
    assertThat(test.getXExtrapolatorRight()).isEqualTo(FLAT);
    assertThat(test.getYInterpolator()).isEqualTo(PCHIP);
    assertThat(test.getYExtrapolatorLeft()).isEqualTo(FLAT);
    assertThat(test.getYExtrapolatorRight()).isEqualTo(FLAT);
    assertThat(test.getZInterpolator()).isEqualTo(NATURAL_SPLINE);
    assertThat(test.getZExtrapolatorLeft()).isEqualTo(FLAT);
    assertThat(test.getZExtrapolatorRight()).isEqualTo(FLAT);
  }

  @Test
  public void test_of6() {
    GridCubeInterpolator test = GridCubeInterpolator.of(LINEAR, EXPONENTIAL, NATURAL_SPLINE, EXPONENTIAL, PCHIP, FLAT);
    assertThat(test.getXInterpolator()).isEqualTo(LINEAR);
    assertThat(test.getXExtrapolatorLeft()).isEqualTo(EXPONENTIAL);
    assertThat(test.getXExtrapolatorRight()).isEqualTo(EXPONENTIAL);
    assertThat(test.getYInterpolator()).isEqualTo(NATURAL_SPLINE);
    assertThat(test.getYExtrapolatorLeft()).isEqualTo(EXPONENTIAL);
    assertThat(test.getYExtrapolatorRight()).isEqualTo(EXPONENTIAL);
    assertThat(test.getZInterpolator()).isEqualTo(PCHIP);
    assertThat(test.getZExtrapolatorLeft()).isEqualTo(FLAT);
    assertThat(test.getZExtrapolatorRight()).isEqualTo(FLAT);
  }

  @Test
  public void test_of9() {
    GridCubeInterpolator test = GridCubeInterpolator.of(
        LINEAR,
        QUADRATIC_LEFT,
        EXPONENTIAL,
        NATURAL_CUBIC_SPLINE,
        FLAT,
        EXPONENTIAL,
        NATURAL_SPLINE_NONNEGATIVITY_CUBIC,
        FLAT,
        EXPONENTIAL);
    assertThat(test.getXInterpolator()).isEqualTo(LINEAR);
    assertThat(test.getXExtrapolatorLeft()).isEqualTo(QUADRATIC_LEFT);
    assertThat(test.getXExtrapolatorRight()).isEqualTo(EXPONENTIAL);
    assertThat(test.getYInterpolator()).isEqualTo(NATURAL_CUBIC_SPLINE);
    assertThat(test.getYExtrapolatorLeft()).isEqualTo(FLAT);
    assertThat(test.getYExtrapolatorRight()).isEqualTo(EXPONENTIAL);
    assertThat(test.getZInterpolator()).isEqualTo(NATURAL_SPLINE_NONNEGATIVITY_CUBIC);
    assertThat(test.getZExtrapolatorLeft()).isEqualTo(FLAT);
    assertThat(test.getZExtrapolatorRight()).isEqualTo(EXPONENTIAL);
  }

  @Test
  public void test_bind_invalidXValues() {
    GridCubeInterpolator test = GridCubeInterpolator.of(LINEAR, LINEAR, LINEAR);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.bind(
            DoubleArray.of(1d, 1d, 1d, 1d),
            DoubleArray.of(1d, 1d, 2d, 2d),
            DoubleArray.of(3d, 4d, 3d, 4d),
            DoubleArray.of(1d, 1d, 1d, 1d)));
  }

  @Test
  public void test_bind_invalidOrder() {
    GridCubeInterpolator test = GridCubeInterpolator.of(LINEAR, LINEAR, LINEAR);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.bind(
            DoubleArray.of(1d, 1d, 1d, 1d, 0d, 0d, 0d, 0d),
            DoubleArray.of(1d, 1d, 2d, 2d, 1d, 1d, 2d, 2d),
            DoubleArray.of(1d, 2d, 1d, 2d, 1d, 2d, 1d, 2d),
            DoubleArray.of(1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.bind(
            DoubleArray.of(1d, 1d, 1d, 1d, 2d, 2d, 2d, 2d),
            DoubleArray.of(1d, 1d, 2d, 2d, 0d, 0d, 0d, 0d),
            DoubleArray.of(1d, 2d, 1d, 2d, 1d, 2d, 1d, 2d),
            DoubleArray.of(1d, 1d, 1d, 1d, 1d, 1d, 1d, 1d)));
  }

  @Test
  public void test_interpolation() {
    GridCubeInterpolator test = GridCubeInterpolator.of(LINEAR, LINEAR, LINEAR);
    BoundCubeInterpolator bci = test.bind(X_DATA, Y_DATA, Z_DATA, W_DATA);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertThat(bci.interpolate(X_DATA.get(i), Y_DATA.get(i), Z_DATA.get(i))).isCloseTo(W_DATA.get(i), TOL);
    }
    for (int i = 0; i < X_TEST.size(); i++) {
      assertThat(bci.interpolate(X_TEST.get(i), Y_TEST.get(i), Z_TEST.get(i))).isCloseTo(W_TEST.get(i), TOL);
    }
  }

  @Test
  public void test_firstDerivativeInterpolator() {
    GridCubeInterpolator test = GridCubeInterpolator.of(LINEAR, LINEAR, LINEAR);
    BoundCubeInterpolator bci = test.bind(X_DATA, Y_DATA, Z_DATA, W_DATA);
    double eps = 1e-8;
    for (int i = 0; i < X_TEST.size(); i++) {
      ValueDerivatives computed = bci.firstPartialDerivatives(X_TEST.get(i), Y_TEST.get(i), Z_TEST.get(i));
      double upX = bci.interpolate(X_TEST.get(i) + eps, Y_TEST.get(i), Z_TEST.get(i));
      double downX = bci.interpolate(X_TEST.get(i) - eps, Y_TEST.get(i), Z_TEST.get(i));
      double upY = bci.interpolate(X_TEST.get(i), Y_TEST.get(i) + eps, Z_TEST.get(i));
      double downY = bci.interpolate(X_TEST.get(i), Y_TEST.get(i) - eps, Z_TEST.get(i));
      double upZ = bci.interpolate(X_TEST.get(i), Y_TEST.get(i), Z_TEST.get(i) + eps);
      double downZ = bci.interpolate(X_TEST.get(i), Y_TEST.get(i), Z_TEST.get(i) - eps);
      double expectedX = 0.5 * (upX - downX) / eps;
      double expectedY = 0.5 * (upY - downY) / eps;
      double expectedZ = 0.5 * (upZ - downZ) / eps;
      assertThat(computed.getDerivative(0)).isCloseTo(expectedX, offset(eps * 10));
      assertThat(computed.getDerivative(1)).isCloseTo(expectedY, offset(eps * 10));
      assertThat(computed.getDerivative(2)).isCloseTo(expectedZ, offset(eps * 10));
    }
  }

  @Test
  public void test_parameterSensitivity() {
    GridCubeInterpolator test = GridCubeInterpolator.of(LINEAR, LINEAR, LINEAR);
    BoundCubeInterpolator bci = test.bind(X_DATA, Y_DATA, Z_DATA, W_DATA);
    double eps = 1e-8;
    for (int i = 0; i < X_TEST.size(); i++) {
      DoubleArray computed = bci.parameterSensitivity(X_TEST.get(i), Y_TEST.get(i), Z_TEST.get(i));
      assertThat(computed.size()).isEqualTo(X_DATA.size());
      for (int j = 0; j < X_DATA.size(); j++) {
        BoundCubeInterpolator bciUp = test.bind(X_DATA, Y_DATA, Z_DATA, W_DATA.with(j, W_DATA.get(j) + eps));
        double up = bciUp.interpolate(X_TEST.get(i), Y_TEST.get(i), Z_TEST.get(i));
        BoundCubeInterpolator bciDw = test.bind(X_DATA, Y_DATA, Z_DATA, W_DATA.with(j, W_DATA.get(j) - eps));
        double dw = bciDw.interpolate(X_TEST.get(i), Y_TEST.get(i), Z_TEST.get(i));
        double expected = 0.5 * (up - dw) / eps;
        assertThat(computed.get(j)).isCloseTo(expected, offset(eps * 10));
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    GridCubeInterpolator test = GridCubeInterpolator.of(
        LINEAR, FLAT, FLAT, LINEAR, FLAT, FLAT, LINEAR, FLAT, FLAT);
    coverImmutableBean(test);
    GridCubeInterpolator test2 = GridCubeInterpolator.of(
        DOUBLE_QUADRATIC,
        CurveExtrapolators.LINEAR,
        CurveExtrapolators.LINEAR,
        DOUBLE_QUADRATIC,
        CurveExtrapolators.LINEAR,
        CurveExtrapolators.LINEAR,
        DOUBLE_QUADRATIC,
        CurveExtrapolators.LINEAR,
        CurveExtrapolators.LINEAR);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    GridCubeInterpolator test = GridCubeInterpolator.of(
        LINEAR, FLAT, FLAT, LINEAR, FLAT, FLAT, LINEAR, FLAT, FLAT);
    assertSerialization(test);
  }

}
