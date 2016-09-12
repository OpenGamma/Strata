/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.EXPONENTIAL;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.LOG_LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link GridSurfaceInterpolator}.
 */
@Test
public class GridSurfaceInterpolatorTest {

  private static final DoubleArray X_DATA = DoubleArray.of(0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 2.0, 2.0, 2.0, 3.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 5.0, 3.0, 4.0, 5.0, 3.0, 4.0, 5.0, 4.0);
  private static final DoubleArray Z_DATA = DoubleArray.of(3.0, 5.0, 3.1, 2.0, 4.0, 3.0, 1.5, 4.5, 2.5, 5.7);
  // where x= 0.0, y=3.4 -> z=3.8
  // where x= 1.0, y=3.4 -> z=2.8
  // x= 0.2 -> z=3.6
  //
  // where x= 1.0, y=4.1 -> z=3.9
  // where x= 2.0, y=4.1 -> z=4.3
  // x= 1.3 -> z=3.9 + 0.4 * 0.3
  //
  // where x= 2.0, y=4.5 -> z=3.5
  // where x= 3.0, y=4.5 -> z=5.7
  // x= 2.5 -> z=3.5 + 2.2 * 0.5
  private static final DoubleArray X_TEST = DoubleArray.of(0.2, 1.3, 2.5);
  private static final DoubleArray Y_TEST = DoubleArray.of(3.4, 4.1, 4.5);
  private static final DoubleArray Z_TEST = DoubleArray.of(3.6, 3.9 + (0.4 * 0.3), 3.5 + (2.2 * 0.5));
  private static final double TOL = 1.e-12;

  //-------------------------------------------------------------------------
  public void test_of2() {
    GridSurfaceInterpolator test = GridSurfaceInterpolator.of(LINEAR, LINEAR);
    assertEquals(test.getXInterpolator(), LINEAR);
    assertEquals(test.getXExtrapolatorLeft(), FLAT);
    assertEquals(test.getXExtrapolatorRight(), FLAT);
    assertEquals(test.getYInterpolator(), LINEAR);
    assertEquals(test.getYExtrapolatorLeft(), FLAT);
    assertEquals(test.getYExtrapolatorRight(), FLAT);
  }

  public void test_of4() {
    GridSurfaceInterpolator test = GridSurfaceInterpolator.of(LINEAR, EXPONENTIAL, LINEAR, EXPONENTIAL);
    assertEquals(test.getXInterpolator(), LINEAR);
    assertEquals(test.getXExtrapolatorLeft(), EXPONENTIAL);
    assertEquals(test.getXExtrapolatorRight(), EXPONENTIAL);
    assertEquals(test.getYInterpolator(), LINEAR);
    assertEquals(test.getYExtrapolatorLeft(), EXPONENTIAL);
    assertEquals(test.getYExtrapolatorRight(), EXPONENTIAL);
  }

  public void test_of6() {
    GridSurfaceInterpolator test = GridSurfaceInterpolator.of(
        LINEAR, EXPONENTIAL, EXPONENTIAL, LINEAR, EXPONENTIAL, EXPONENTIAL);
    assertEquals(test.getXInterpolator(), LINEAR);
    assertEquals(test.getXExtrapolatorLeft(), EXPONENTIAL);
    assertEquals(test.getXExtrapolatorRight(), EXPONENTIAL);
    assertEquals(test.getYInterpolator(), LINEAR);
    assertEquals(test.getYExtrapolatorLeft(), EXPONENTIAL);
    assertEquals(test.getYExtrapolatorRight(), EXPONENTIAL);
  }

  //-------------------------------------------------------------------------
  public void test_bind_invalidXValues() {
    GridSurfaceInterpolator test = GridSurfaceInterpolator.of(LINEAR, LINEAR);
    assertThrowsIllegalArg(() -> test.bind(
        DoubleArray.of(1d, 1d), DoubleArray.of(1d, 2d), DoubleArray.of(1d, 1d)));
  }

  public void test_bind_invalidOrder() {
    GridSurfaceInterpolator test = GridSurfaceInterpolator.of(LINEAR, LINEAR);
    assertThrowsIllegalArg(() -> test.bind(
        DoubleArray.of(1d, 1d, 0d, 0d), DoubleArray.of(1d, 2d, 1d, 2d), DoubleArray.of(1d, 1d, 1d, 1d)));
    assertThrowsIllegalArg(() -> test.bind(
        DoubleArray.of(1d, 1d, 2d, 2d), DoubleArray.of(1d, 0d, 1d, 0d), DoubleArray.of(1d, 1d, 1d, 1d)));
  }

  //-------------------------------------------------------------------------
  public void test_interpolation() {
    GridSurfaceInterpolator test = GridSurfaceInterpolator.of(
        LINEAR, FLAT, FLAT, LINEAR, FLAT, FLAT);
    BoundSurfaceInterpolator bci = test.bind(X_DATA, Y_DATA, Z_DATA);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertEquals(bci.interpolate(X_DATA.get(i), Y_DATA.get(i)), Z_DATA.get(i), TOL);
    }
    for (int i = 0; i < X_TEST.size(); i++) {
      assertEquals(bci.interpolate(X_TEST.get(i), Y_TEST.get(i)), Z_TEST.get(i), TOL);
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    GridSurfaceInterpolator test = GridSurfaceInterpolator.of(
        LINEAR, FLAT, FLAT, LINEAR, FLAT, FLAT);
    coverImmutableBean(test);
    GridSurfaceInterpolator test2 = GridSurfaceInterpolator.of(
        DOUBLE_QUADRATIC, LOG_LINEAR, LOG_LINEAR, DOUBLE_QUADRATIC, LOG_LINEAR, LOG_LINEAR);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    GridSurfaceInterpolator test = GridSurfaceInterpolator.of(
        LINEAR, FLAT, FLAT, LINEAR, FLAT, FLAT);
    assertSerialization(test);
  }

}
