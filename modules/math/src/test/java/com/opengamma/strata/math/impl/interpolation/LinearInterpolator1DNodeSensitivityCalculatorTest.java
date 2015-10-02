/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test.
 */
@Test
public class LinearInterpolator1DNodeSensitivityCalculatorTest {
  private static final double EPS = 1e-15;
  private static final LinearInterpolator1D INTERPOLATOR = new LinearInterpolator1D();
  private static final Function1D<Double, Double> FUNCTION = new Function1D<Double, Double>() {

    @Override
    public Double evaluate(final Double x) {
      return 2 * x - 7;
    }

  };
  private static final Interpolator1DDataBundle DATA;

  static {
    final int n = 10;
    final double[] x = new double[n];
    final double[] y = new double[n];
    for (int i = 0; i < n; i++) {
      x[i] = Double.valueOf(i);
      y[i] = FUNCTION.evaluate(x[i]);
    }
    DATA = INTERPOLATOR.getDataBundleFromSortedArrays(x, y);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullData() {
    INTERPOLATOR.getNodeSensitivitiesForValue(null, 1.);
  }

  @Test
  public void test() {
    double[] result = INTERPOLATOR.getNodeSensitivitiesForValue(DATA, 3.4);
    for (int i = 0; i < 3; i++) {
      assertEquals(0, result[i], 0);
    }
    assertEquals(0.6, result[3], EPS);
    assertEquals(0.4, result[4], EPS);
    for (int i = 5; i < 10; i++) {
      assertEquals(result[i], 0, 0);
    }
    result = INTERPOLATOR.getNodeSensitivitiesForValue(DATA, 7.);
    for (int i = 0; i < 7; i++) {
      assertEquals(0, result[i], 0);
    }
    assertEquals(1, result[7], EPS);
    for (int i = 8; i < 10; i++) {
      assertEquals(0, result[i], 0);
    }
  }
}
