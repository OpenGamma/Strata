/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.internal.junit.ArrayAsserts.assertArrayEquals;

import java.util.Arrays;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * Test.
 */
@Test
public class Interpolator1DCubicSplineDataBundleTest {
  private static final RealPolynomialFunction1D LINEAR = new RealPolynomialFunction1D(new double[] {1, 3 });
  private static final RealPolynomialFunction1D CUBIC = new RealPolynomialFunction1D(new double[] {1, 3, 3, 1 });
  private static final Function<Double, Double> NORMAL = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return Math.exp(-x * x / 2);
    }
  };
  private static final double[] X;
  private static final double[] Y;
  private static final Interpolator1DCubicSplineDataBundle DATA;
  private static final double EPS = 1e-12;

  static {
    final int n = 10;
    X = new double[n];
    Y = new double[n];
    for (int i = 0; i < n; i++) {
      X[i] = 2 * (i + 1);
      Y[i] = CUBIC.applyAsDouble(X[i]);
    }
    DATA = new Interpolator1DCubicSplineDataBundle(new ArrayInterpolator1DDataBundle(X, Y));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullData() {
    new Interpolator1DCubicSplineDataBundle(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSetNegativeIndex() {
    DATA.setYValueAtIndex(-2, 3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSetHighIndex() {
    DATA.setYValueAtIndex(100, 4);
  }

  @Test
  public void testGetters() {
    assertTrue(DATA.containsKey(2.));
    assertFalse(DATA.containsKey(3.4));
    assertEquals(DATA.firstKey(), 2., EPS);
    assertEquals(DATA.firstValue(), CUBIC.applyAsDouble(2.), EPS);
    assertEquals(DATA.getIndex(DATA.indexOf(4.)), CUBIC.applyAsDouble(4.), EPS);
    assertArrayEquals(DATA.getKeys(), X, 0);
    assertEquals(DATA.getLowerBoundIndex(7.), 2);
    assertEquals(DATA.getLowerBoundKey(7.), 6, EPS);
    assertArrayEquals(DATA.getValues(), Y, EPS);
    assertEquals(DATA.higherKey(7.), 8, 0);
    assertEquals(DATA.higherValue(7.), CUBIC.applyAsDouble(8.), EPS);
    assertEquals(DATA.lastKey(), 20., EPS);
    assertEquals(DATA.lastValue(), CUBIC.applyAsDouble(20.), EPS);
    assertEquals(DATA.size(), 10);
    final InterpolationBoundedValues boundedValues = DATA.getBoundedValues(4.);
    assertEquals(boundedValues.getLowerBoundIndex(), 1);
    assertEquals(boundedValues.getLowerBoundKey(), 4., EPS);
    assertEquals(boundedValues.getLowerBoundValue(), CUBIC.applyAsDouble(4.), EPS);
    assertEquals(boundedValues.getHigherBoundKey(), 6., EPS);
    assertEquals(boundedValues.getHigherBoundValue(), CUBIC.applyAsDouble(6.), EPS);
  }

  @Test
  public void testEqualsAndHashCode() {
    Interpolator1DCubicSplineDataBundle other = new Interpolator1DCubicSplineDataBundle(
        new ArrayInterpolator1DDataBundle(X, Y));
    assertEquals(other, DATA);
    assertEquals(other.hashCode(), DATA.hashCode());
    other = new Interpolator1DCubicSplineDataBundle(new ArrayInterpolator1DDataBundle(Y, Y));
    assertFalse(other.equals(DATA));
    other = new Interpolator1DCubicSplineDataBundle(new ArrayInterpolator1DDataBundle(X, X));
    assertFalse(other.equals(DATA));
  }

  @Test
  public void testSecondDerivatives() {

    int n = 10;
    double x[] = new double[n];
    double y[] = new double[n];
    for (int i = 0; i < n; i++) {
      x[i] = (i - 5);
      y[i] = LINEAR.applyAsDouble(x[i]);
    }
    Interpolator1DCubicSplineDataBundle data = new Interpolator1DCubicSplineDataBundle(
        new ArrayInterpolator1DDataBundle(x, y));

    double[] y2 = data.getSecondDerivatives();
    assertEquals(y2.length, 10);
    assertEquals(y2[0], 0, EPS);
    assertEquals(y2[y2.length - 1], 0, EPS);

    for (final double element : y2) {
      assertEquals(0.0, element, 0.0);
    }

    n = 150;
    x = new double[n];
    y = new double[n];
    for (int i = 0; i < n; i++) {
      x[i] = (i - 75) / 10.;
      y[i] = NORMAL.apply(x[i]);
    }
    data = new Interpolator1DCubicSplineDataBundle(new ArrayInterpolator1DDataBundle(x, y));

    y2 = data.getSecondDerivatives();
    for (int i = 0; i < n; i++) {
      final double temp = (x[i] * x[i] - 1) * Math.exp(-x[i] * x[i] / 2.0);
      assertEquals(temp, y2[i], 1e-2);
    }
  }

  @Test
  public void testSecondDerivativesSensitivities() {

    final double[][] sense = DATA.getSecondDerivativesSensitivities();
    final int n = X.length;
    assertEquals(sense.length, n, 0);
    assertEquals(sense[0].length, n, 0);
    for (int i = 0; i < n; i++) {
      assertEquals(sense[0][i], 0.0, 0.0);
      assertEquals(sense[n - 1][i], 0.0, 0.0);
    }

  }

  @Test
  public void testSetYValue() {
    final int n = X.length;
    final double[] x = Arrays.copyOf(X, n);
    final double[] y = Arrays.copyOf(Y, n);
    Arrays.sort(x);
    Arrays.sort(y);
    Interpolator1DCubicSplineDataBundle data1 = new Interpolator1DCubicSplineDataBundle(
        new ArrayInterpolator1DDataBundle(x, y));
    Interpolator1DCubicSplineDataBundle data2;
    final double newY = 120;
    final double[] yData = Arrays.copyOf(y, n);
    double[] y21, y22;
    double[][] dy21, dy22;
    for (int i = 0; i < n; i++) {
      yData[i] = newY;
      data1.setYValueAtIndex(i, newY);
      data2 = new Interpolator1DCubicSplineDataBundle(new ArrayInterpolator1DDataBundle(x, yData));
      assertArrayEquals(data1.getKeys(), data2.getKeys(), 0);
      assertArrayEquals(data1.getValues(), data2.getValues(), 0);
      y21 = data1.getSecondDerivatives();
      y22 = data2.getSecondDerivatives();
      dy21 = data1.getSecondDerivativesSensitivities();
      dy22 = data2.getSecondDerivativesSensitivities();
      assertArrayEquals(y21, y22, 0);
      for (int j = 0; j < n; j++) {
        for (int k = 0; k < dy21.length; k++) {
          assertArrayEquals(dy21[k], dy22[k], 0);
        }
      }
      assertEquals(data1, data2);
      yData[i] = y[i];
      data1 = new Interpolator1DCubicSplineDataBundle(new ArrayInterpolator1DDataBundle(x, y));
    }
  }
}
