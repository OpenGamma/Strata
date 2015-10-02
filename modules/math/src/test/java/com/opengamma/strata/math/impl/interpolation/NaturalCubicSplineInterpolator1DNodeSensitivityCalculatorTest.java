/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Arrays;

import org.apache.commons.math3.random.Well44497b;
import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DCubicSplineDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test.
 */
@Test
public class NaturalCubicSplineInterpolator1DNodeSensitivityCalculatorTest {

  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final Interpolator1D INTERPOLATOR = new NaturalCubicSplineInterpolator1D();
  private static final Interpolator1DDataBundle DATA1;
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
    DATA1 = INTERPOLATOR.getDataBundleFromSortedArrays(t, r);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullInputMap() {
    INTERPOLATOR.interpolate(null, 3.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullInterpolateValue() {
    INTERPOLATOR.interpolate(DATA1, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testHighValue() {
    INTERPOLATOR.interpolate(DATA1, 31.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testLowValue() {
    INTERPOLATOR.interpolate(DATA1, -1.);
  }

  @Test
  public void testSensitivities() {
    final double tmax = DATA1.lastKey();
    double[] sensitivity;
    for (int i = 0; i < 100; i++) {
      final double t = tmax * RANDOM.nextDouble();
      sensitivity = INTERPOLATOR.getNodeSensitivitiesForValue(DATA1, t);
      for (int j = 0; j < sensitivity.length; j++) {
        assertEquals(getSensitivity(DATA1, INTERPOLATOR, t, j), sensitivity[j], EPS);
      }
    }
  }

  @Test
  public void testYieldCurve() {
    final double[] fwdTimes = new double[] {0.0, 1.0, 2.0, 5.0, 10.0, 20.0, 31.0 };
    final int n = fwdTimes.length;
    final double[] rates = new double[n];
    for (int i = 0; i < n; i++) {
      rates[i] = FUNCTION.evaluate(fwdTimes[i]);
    }
    final Interpolator1DCubicSplineDataBundle data = new Interpolator1DCubicSplineDataBundle(INTERPOLATOR.getDataBundleFromSortedArrays(fwdTimes, rates));
    final double[] sensitivity1 = INTERPOLATOR.getNodeSensitivitiesForValue(data, 0.25);
    final double[] sensitivity2 = INTERPOLATOR.getNodeSensitivitiesForValue(data, 0.25, true);
    for (int j = 0; j < sensitivity1.length; j++) {
      assertEquals(sensitivity1[j], sensitivity2[j], EPS);
    }
  }

  private double getSensitivity(final Interpolator1DDataBundle model, final Interpolator1D interpolator, final double t, final int node) {
    final double[] x = model.getKeys();
    final double[] y = model.getValues();
    final int n = y.length;
    double[] yUp = new double[n];
    double[] yDown = new double[n];
    yUp = Arrays.copyOf(y, n);
    yDown = Arrays.copyOf(y, n);
    yUp[node] += EPS;
    yDown[node] -= EPS;
    final Interpolator1DDataBundle modelUp = interpolator.getDataBundleFromSortedArrays(x, yUp);
    final Interpolator1DDataBundle modelDown = interpolator.getDataBundleFromSortedArrays(x, yDown);
    final double up = interpolator.interpolate(modelUp, t);
    final double down = interpolator.interpolate(modelDown, t);
    return (up - down) / 2.0 / EPS;
  }
}
