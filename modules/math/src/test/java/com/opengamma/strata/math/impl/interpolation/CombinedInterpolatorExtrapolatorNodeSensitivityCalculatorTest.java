/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.internal.junit.ArrayAsserts.assertArrayEquals;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test.
 */
@Test
public class CombinedInterpolatorExtrapolatorNodeSensitivityCalculatorTest {
  private static final LinearInterpolator1D LINEAR = new LinearInterpolator1D();
  private static final FlatExtrapolator1D LEFT = new FlatExtrapolator1D();
  private static final LinearExtrapolator1D RIGHT = new LinearExtrapolator1D();
  private static final double[] X;
  private static final double[] Y;
  private static final Interpolator1DDataBundle DATA;
  private static final CombinedInterpolatorExtrapolator COMBINED1 = new CombinedInterpolatorExtrapolator(LINEAR);
  private static final CombinedInterpolatorExtrapolator COMBINED2 = new CombinedInterpolatorExtrapolator(LINEAR, LEFT);
  private static final CombinedInterpolatorExtrapolator COMBINED3 = new CombinedInterpolatorExtrapolator(LINEAR, LEFT, RIGHT);
  private static final Function1D<Double, Double> F = new Function1D<Double, Double>() {

    @Override
    public Double evaluate(final Double x) {
      return 3 * x + 11;
    }

  };

  static {
    final int n = 10;
    X = new double[n];
    Y = new double[n];
    for (int i = 0; i < n; i++) {
      X[i] = i;
      Y[i] = F.evaluate(X[i]);
    }
    DATA = new ArrayInterpolator1DDataBundle(X, Y);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullInterpolator1() {
    new CombinedInterpolatorExtrapolator(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullInterpolator2() {
    new CombinedInterpolatorExtrapolator(null, LEFT);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullInterpolator3() {
    new CombinedInterpolatorExtrapolator(null, LEFT, RIGHT);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullExtrapolator() {
    new CombinedInterpolatorExtrapolator(LINEAR, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullLeftExtrapolator() {
    new CombinedInterpolatorExtrapolator(LINEAR, null, RIGHT);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullRightExtrapolator() {
    new CombinedInterpolatorExtrapolator(LINEAR, LEFT, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullData() {
    COMBINED1.getNodeSensitivitiesForValue(null, 2.3);
  }

  @Test
  public void testInterpolatorOnly() {
    final double x = 6.7;
    assertArrayEquals(COMBINED1.getNodeSensitivitiesForValue(DATA, x), LINEAR.getNodeSensitivitiesForValue(DATA, x), 1e-15);
    try {
      COMBINED1.getNodeSensitivitiesForValue(DATA, x - 100);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
    }
    try {
      COMBINED1.getNodeSensitivitiesForValue(DATA, x + 100);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
    }
  }

  @Test
  public void testOneExtrapolator() {
    final double x = 3.6;
    assertArrayEquals(COMBINED2.getNodeSensitivitiesForValue(DATA, x), LINEAR.getNodeSensitivitiesForValue(DATA, x), 1e-15);
    assertArrayEquals(COMBINED2.getNodeSensitivitiesForValue(DATA, x - 100), LEFT.getNodeSensitivitiesForValue(DATA, x - 100, LINEAR), 1e-15);
    assertArrayEquals(COMBINED2.getNodeSensitivitiesForValue(DATA, x + 100), LEFT.getNodeSensitivitiesForValue(DATA, x + 100, LINEAR), 1e-15);
  }

  @Test
  public void testTwoExtrapolators() {
    final double x = 3.6;
    assertArrayEquals(COMBINED3.getNodeSensitivitiesForValue(DATA, x), LINEAR.getNodeSensitivitiesForValue(DATA, x), 1e-15);
    assertArrayEquals(COMBINED3.getNodeSensitivitiesForValue(DATA, x - 100), LEFT.getNodeSensitivitiesForValue(DATA, x - 100, LINEAR), 1e-15);
    assertArrayEquals(COMBINED3.getNodeSensitivitiesForValue(DATA, x + 100), RIGHT.getNodeSensitivitiesForValue(DATA, x + 100, LINEAR), 1e-5);
  }
}
