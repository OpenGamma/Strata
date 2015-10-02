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

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * Test.
 */
@Test
public class Interpolator1DDoubleQuadraticDataBundleTest {
  private static final RealPolynomialFunction1D QUADRATIC = new RealPolynomialFunction1D(new double[] {2, 3, 4 });
  private static final Interpolator1DDoubleQuadraticDataBundle DATA;
  private static final double[] X;
  private static final double[] Y;
  private static final double EPS = 1e-12;

  static {
    final int n = 10;
    X = new double[n];
    Y = new double[n];
    for (int i = 0; i < n; i++) {
      X[i] = 3 * i;
      Y[i] = QUADRATIC.evaluate(X[i]);
    }
    DATA = new Interpolator1DDoubleQuadraticDataBundle(new ArrayInterpolator1DDataBundle(X, Y));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullData() {
    new Interpolator1DDoubleQuadraticDataBundle(null);
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
    assertTrue(DATA.containsKey(3.));
    assertFalse(DATA.containsKey(2.));
    assertEquals(DATA.firstKey(), 0., EPS);
    assertEquals(DATA.firstValue(), QUADRATIC.evaluate(0.), EPS);
    assertEquals(DATA.get(6.), QUADRATIC.evaluate(6.), EPS);
    assertArrayEquals(DATA.getKeys(), X, 0);
    assertEquals(DATA.getLowerBoundIndex(11.), 3);
    assertEquals(DATA.getLowerBoundKey(7.), 6, EPS);
    assertArrayEquals(DATA.getValues(), Y, EPS);
    assertEquals(DATA.higherKey(7.), 9, 0);
    assertEquals(DATA.higherValue(7.), QUADRATIC.evaluate(9.), EPS);
    assertEquals(DATA.lastKey(), 27., EPS);
    assertEquals(DATA.lastValue(), QUADRATIC.evaluate(27.), EPS);
    assertEquals(DATA.size(), 10);
    final InterpolationBoundedValues boundedValues = DATA.getBoundedValues(4.);
    assertEquals(boundedValues.getLowerBoundIndex(), 1);
    assertEquals(boundedValues.getLowerBoundKey(), 3., EPS);
    assertEquals(boundedValues.getLowerBoundValue(), QUADRATIC.evaluate(3.), EPS);
    assertEquals(boundedValues.getHigherBoundKey(), 6., EPS);
    assertEquals(boundedValues.getHigherBoundValue(), QUADRATIC.evaluate(6.), EPS);
  }

  @Test
  public void testEqualsAndHashCode() {
    Interpolator1DDoubleQuadraticDataBundle other = new Interpolator1DDoubleQuadraticDataBundle(
        new ArrayInterpolator1DDataBundle(X, Y));
    assertEquals(other, DATA);
    assertEquals(other.hashCode(), DATA.hashCode());
    other = new Interpolator1DDoubleQuadraticDataBundle(new ArrayInterpolator1DDataBundle(Y, Y));
    assertFalse(other.equals(DATA));
    other = new Interpolator1DDoubleQuadraticDataBundle(new ArrayInterpolator1DDataBundle(X, X));
    assertFalse(other.equals(DATA));
  }

  @Test
  public void testQuadratics() {
    for (int i = 0; i < (X.length - 2); i++) {
      RealPolynomialFunction1D quad = DATA.getQuadratic(i);
      double[] coeff = quad.getCoefficients();
      assertTrue(coeff.length == 3);
      double x = X[i + 1];
      assertEquals(2 + 3 * x + 4 * x * x, coeff[0], 0);
      assertEquals(3 + 2 * 4 * x, coeff[1], 0);
      assertEquals(4, coeff[2], 0);
    }
  }

  @Test
  public void testSetYValue() {
    final int n = X.length;
    final double[] x = Arrays.copyOf(X, n);
    final double[] y = Arrays.copyOf(Y, n);
    Arrays.sort(x);
    Arrays.sort(y);
    Interpolator1DDoubleQuadraticDataBundle data1 = new Interpolator1DDoubleQuadraticDataBundle(
        new ArrayInterpolator1DDataBundle(x, y));
    Interpolator1DDoubleQuadraticDataBundle data2;
    final double newY = 120;
    final double[] yData = Arrays.copyOf(y, n);
    for (int i = 0; i < n; i++) {
      yData[i] = newY;
      data1.setYValueAtIndex(i, newY);
      data2 = new Interpolator1DDoubleQuadraticDataBundle(new ArrayInterpolator1DDataBundle(x, yData));
      assertArrayEquals(data1.getKeys(), data2.getKeys(), 0);
      assertArrayEquals(data1.getValues(), data2.getValues(), 0);
      for (int j = 0; j < n - 2; j++) {
        assertEquals(data1.getQuadratic(j), data2.getQuadratic(j));
      }
      assertEquals(data1, data2);
      yData[i] = y[i];
      data1 = new Interpolator1DDoubleQuadraticDataBundle(new ArrayInterpolator1DDataBundle(x, y));
    }
  }
}
