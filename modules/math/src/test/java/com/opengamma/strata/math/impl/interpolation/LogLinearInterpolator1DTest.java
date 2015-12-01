/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import java.util.TreeMap;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test.
 */
@Test
public class LogLinearInterpolator1DTest {
  private static final Interpolator1D LINEAR = new LinearInterpolator1D();
  private static final Interpolator1D INTERPOLATOR = new LogLinearInterpolator1D();
  private static final Function<Double, Double> FUNCTION = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return 2 * x + 7;
    }
  };
  private static final Interpolator1DDataBundle MODEL;
  private static final Interpolator1DDataBundle TRANSFORMED_MODEL;
  private static final double EPS = 1e-9;

  static {
    final TreeMap<Double, Double> data = new TreeMap<>();
    final TreeMap<Double, Double> transformedData = new TreeMap<>();
    double x;
    for (int i = 0; i < 10; i++) {
      x = Double.valueOf(i);
      data.put(x, FUNCTION.apply(x));
      transformedData.put(x, Math.log(FUNCTION.apply(x)));
    }
    MODEL = LINEAR.getDataBundle(data);
    TRANSFORMED_MODEL = INTERPOLATOR.getDataBundle(transformedData);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDataBundle() {
    INTERPOLATOR.interpolate(null, 3.4);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testLowValue() {
    INTERPOLATOR.interpolate(MODEL, -2.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testHighValue() {
    INTERPOLATOR.interpolate(MODEL, 12.);
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
    assertEquals(Math.log(INTERPOLATOR.interpolate(MODEL, 3.4)), LINEAR.interpolate(TRANSFORMED_MODEL, 3.4), EPS);
  }
}
