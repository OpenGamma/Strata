/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import java.util.TreeMap;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test.
 */
@Test
public class LinearInterpolator1DTest {
  private static final Interpolator1D INTERPOLATOR = new LinearInterpolator1D();
  private static final Function1D<Double, Double> FUNCTION = new Function1D<Double, Double>() {

    @Override
    public Double evaluate(final Double x) {
      return 2 * x - 7;
    }
  };
  private static final Interpolator1DDataBundle MODEL = INTERPOLATOR.getDataBundle(new double[] {1, 2, 3 }, new double[] {4, 5, 6 });

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDataBundle() {
    INTERPOLATOR.interpolate(null, 2.3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullValue() {
    INTERPOLATOR.interpolate(MODEL, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testLowValue() {
    INTERPOLATOR.interpolate(MODEL, -4.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testHighValue() {
    INTERPOLATOR.interpolate(MODEL, 10.);
  }

  @Test
  public void testDataBundleType1() {
    assertEquals(INTERPOLATOR.getDataBundle(new double[] {1, 2, 3 }, new double[] {1, 2, 3 }).getClass(), ArrayInterpolator1DDataBundle.class);
  }

  @Test
  public void testDataBundleType2() {
    assertEquals(INTERPOLATOR.getDataBundleFromSortedArrays(new double[] {1, 2, 3 }, new double[] {1, 2, 3 }).getClass(), ArrayInterpolator1DDataBundle.class);
  }

  @Test
  public void test() {
    final TreeMap<Double, Double> data = new TreeMap<>();
    double x;
    for (int i = 0; i < 10; i++) {
      x = Double.valueOf(i);
      data.put(x, FUNCTION.evaluate(x));
    }
    assertEquals(INTERPOLATOR.interpolate(INTERPOLATOR.getDataBundle(data), 3.4), FUNCTION.evaluate(3.4), 1e-15);
  }
}
