/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import java.util.TreeMap;
import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.random.Well44497b;
import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DCubicSplineDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test.
 */
@Test
public class NaturalCubicSplineInterpolator1DTest {

  private static final Well44497b RANDOM = new Well44497b(0L);

  private static final double[] COEFF = new double[] {-0.4, 0.05, 0.2, 1. };

  private static final Interpolator1D INTERPOLATOR = new NaturalCubicSplineInterpolator1D();
  private static final DoubleUnaryOperator CUBIC = new RealPolynomialFunction1D(COEFF);
  private static final double EPS = 1e-2;
  private static final Interpolator1DDataBundle MODEL;

  static {
    final TreeMap<Double, Double> data = new TreeMap<>();
    for (int i = 0; i < 12; i++) {
      final double x = i / 10.;
      data.put(x, CUBIC.applyAsDouble(x));
    }
    MODEL = INTERPOLATOR.getDataBundle(data);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullInputMap() {
    INTERPOLATOR.interpolate((Interpolator1DCubicSplineDataBundle) null, 3.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testHighValue() {
    INTERPOLATOR.interpolate(MODEL, 15.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testLowValue() {
    INTERPOLATOR.interpolate(MODEL, -12.);
  }

  public void testDataBundleType1() {
    assertEquals(INTERPOLATOR.getDataBundle(new double[] {1, 2, 3 }, new double[] {1, 2, 3 }).getClass(), Interpolator1DCubicSplineDataBundle.class);
  }

  public void testDataBundleType2() {
    assertEquals(INTERPOLATOR.getDataBundleFromSortedArrays(new double[] {1, 2, 3 }, new double[] {1, 2, 3 }).getClass(), Interpolator1DCubicSplineDataBundle.class);
  }

  public void test() {
    for (int i = 0; i < 100; i++) {
      final double x = RANDOM.nextDouble();
      assertEquals(CUBIC.applyAsDouble(x), INTERPOLATOR.interpolate(MODEL, x), EPS);
    }
  }

  public void matlabTest() {
    final double[] x = new double[] {0.0, 2.0, 3.0, 4.0, 5.0, 8.0, 12.0 };
    final double[] y = new double[] {0, 0.145000000000000, 0.190000000000000, 0.200000000000000, 0.250000000000000, 0.700000000000000, 1.000000000000000 };

    final double grad = 1. / 12;
    final Interpolator1DDataBundle nat = INTERPOLATOR.getDataBundleFromSortedArrays(x, y);
    final Interpolator1DCubicSplineDataBundle cub = new Interpolator1DCubicSplineDataBundle(nat, grad, grad);

    final double mlCub = 0.931260400907716;
    final double ans2 = INTERPOLATOR.interpolate(cub, 11.0);
    assertEquals("grad given", mlCub, ans2, 1e-5);
  }

}
