/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import java.util.function.Function;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test.
 */
@Test
public class CombinedInterpolatorExtrapolatorTest {

  private static final LinearInterpolator1D INTERPOLATOR = new LinearInterpolator1D();
  private static final FlatExtrapolator1D LEFT_EXTRAPOLATOR = new FlatExtrapolator1D();
  private static final LinearExtrapolator1D RIGHT_EXTRAPOLATOR = new LinearExtrapolator1D();
  private static final double[] X;
  private static final double[] Y;
  private static final Interpolator1DDataBundle DATA;
  private static final CombinedInterpolatorExtrapolator COMBINED1 = new CombinedInterpolatorExtrapolator(INTERPOLATOR);
  private static final CombinedInterpolatorExtrapolator COMBINED2 = new CombinedInterpolatorExtrapolator(INTERPOLATOR, LEFT_EXTRAPOLATOR);
  private static final CombinedInterpolatorExtrapolator COMBINED3 = new CombinedInterpolatorExtrapolator(INTERPOLATOR, LEFT_EXTRAPOLATOR, RIGHT_EXTRAPOLATOR);
  private static final Function<Double, Double> F = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return 3 * x + 11;
    }

  };

  static {
    final int n = 10;
    X = new double[n];
    Y = new double[n];
    for (int i = 0; i < n; i++) {
      X[i] = i;
      Y[i] = F.apply(X[i]);
    }
    DATA = INTERPOLATOR.getDataBundleFromSortedArrays(X, Y);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullInterpolator1() {
    new CombinedInterpolatorExtrapolator(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullInterpolator2() {
    new CombinedInterpolatorExtrapolator(null, LEFT_EXTRAPOLATOR);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullInterpolator3() {
    new CombinedInterpolatorExtrapolator(null, LEFT_EXTRAPOLATOR, RIGHT_EXTRAPOLATOR);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullExtrapolator() {
    new CombinedInterpolatorExtrapolator(INTERPOLATOR, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullLeftExtrapolator() {
    new CombinedInterpolatorExtrapolator(INTERPOLATOR, null, RIGHT_EXTRAPOLATOR);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullRightExtrapolator() {
    new CombinedInterpolatorExtrapolator(INTERPOLATOR, LEFT_EXTRAPOLATOR, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullData() {
    COMBINED1.interpolate(null, 2.3);
  }

  @Test
  public void testInterpolatorOnly() {
    final double x = 6.7;
    assertEquals(COMBINED1.interpolate(DATA, x), F.apply(x), 1e-15);
    try {
      COMBINED1.interpolate(DATA, x - 100);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
    }
    try {
      COMBINED1.interpolate(DATA, x + 100);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
    }
  }

  @Test
  public void testDataBundleType1() {
    assertEquals(INTERPOLATOR.getDataBundle(X, Y).getClass(), ArrayInterpolator1DDataBundle.class);
  }

  @Test
  public void testDataBundleType2() {
    assertEquals(INTERPOLATOR.getDataBundleFromSortedArrays(X, Y).getClass(), ArrayInterpolator1DDataBundle.class);
  }

  @Test
  public void testGetters() {
    assertEquals(COMBINED3.getInterpolator().getClass(), INTERPOLATOR.getClass());
    assertEquals(COMBINED3.getLeftExtrapolator().getClass(), LEFT_EXTRAPOLATOR.getClass());
    assertEquals(COMBINED3.getRightExtrapolator().getClass(), RIGHT_EXTRAPOLATOR.getClass());
  }

  @Test
  public void testOneExtrapolator() {
    final double x = 3.6;
    assertEquals(COMBINED2.interpolate(DATA, x), F.apply(x), 1e-15);
    assertEquals(COMBINED2.interpolate(DATA, x - 100), F.apply(0.), 1e-15);
    assertEquals(COMBINED2.interpolate(DATA, x + 100), F.apply(9.), 1e-15);
  }

  @Test
  public void testTwoExtrapolators() {
    final double x = 3.6;
    assertEquals(COMBINED3.interpolate(DATA, x), F.apply(x), 1e-15);
    assertEquals(COMBINED3.interpolate(DATA, x - 100), F.apply(0.), 1e-15);
    assertEquals(COMBINED3.interpolate(DATA, x + 100), F.apply(x + 100), 1e-5);
  }

  @Test
  public void testBoundary() {
    for (final double value : X) {
      assertEquals("dy/dx at " + value, 3d, COMBINED3.firstDerivative(DATA, value), 1e-4);
    }
  }
}
