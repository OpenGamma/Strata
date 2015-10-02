/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import org.apache.commons.math3.random.Well44497b;
import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDoubleQuadraticDataBundle;

/**
 * Test.
 */
@Test
public class DoubleQuadraticInterpolator1DNodeSensitivityCalculatorTest {

  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final DoubleQuadraticInterpolator1D INTERPOLATOR = new DoubleQuadraticInterpolator1D();
  private static final Interpolator1DDoubleQuadraticDataBundle DATA;
  private static final double EPS = 1e-7;
  private static final Function1D<Double, Double> FUNCTION = new Function1D<Double, Double>() {

    private static final double a = -0.045;
    private static final double b = 0.03;
    private static final double c = 0.3;
    private static final double d = 0.05;

    @Override
    public Double evaluate(final Double x) {
      return (a + b * x) * Math.exp(-c * x) + d;
    }

  };

  static {
    final double[] t = new double[] {0.0, 0.5, 1.0, 2.0, 3.0, 5.0, 7.0, 10.0, 15.0, 17.5, 20.0, 25.0, 30.0 };
    final int n = t.length;
    final double[] r = new double[n];
    for (int i = 0; i < n; i++) {
      r[i] = FUNCTION.evaluate(t[i]);
    }
    DATA = INTERPOLATOR.getDataBundleFromSortedArrays(t, r);

  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullInputData() {
    INTERPOLATOR.getNodeSensitivitiesForValue(null, 3.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testHighValue() {
    INTERPOLATOR.getNodeSensitivitiesForValue(DATA, 31.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testLowValue() {
    INTERPOLATOR.getNodeSensitivitiesForValue(DATA, -1.);
  }

  @Test
  public void testSensitivities() {
    final double tmax = DATA.lastKey();
    double t;
    double[] sensitivity, fdSensitivity;
    for (int i = 0; i < 100; i++) {
      t = tmax * RANDOM.nextDouble();
      sensitivity = INTERPOLATOR.getNodeSensitivitiesForValue(DATA, t);
      fdSensitivity = INTERPOLATOR.getNodeSensitivitiesForValue(DATA, t, true);
      for (int j = 0; j < sensitivity.length; j++) {
        assertEquals(fdSensitivity[j], sensitivity[j], EPS);
      }
    }
  }

  @Test
  public void testEdgeCase() {
    final double tmax = DATA.lastKey();
    final double[] sensitivity = INTERPOLATOR.getNodeSensitivitiesForValue(DATA, tmax);
    for (int j = 0; j < sensitivity.length - 1; j++) {
      assertEquals(0, sensitivity[j], EPS);
    }
    assertEquals(1.0, sensitivity[sensitivity.length - 1], EPS);
  }

}
