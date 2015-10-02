/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;

import org.testng.annotations.Test;

/**
 * Abstract test.
 */
@Test
public abstract class Interpolator1DDataBundleTestCase {
  private static final double EPS = 1e-16;
  private static final double[] KEYS1 = new double[] {5., 1., 4., 2., 3. };
  private static final double[] VALUES1 = new double[] {50., 10., 40., 20., 30. };
  private final Interpolator1DDataBundle DATA = createDataBundle(KEYS1, VALUES1);

  protected abstract Interpolator1DDataBundle createDataBundle(double[] keys, double[] values);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testLowLowerBoundKey() {
    DATA.getLowerBoundKey(0.5);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testHighLowerBoundKey() {
    DATA.getLowerBoundKey(10.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testLowLowerBoundIndex() {
    DATA.getLowerBoundIndex(0.5);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testHighLowerBoundIndex() {
    DATA.getLowerBoundIndex(10.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testLowHigherKey() {
    DATA.higherKey(0.5);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testHighHigherKey() {
    DATA.higherKey(10.);
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
  public void size() {
    assertEquals(5, DATA.size());
  }

  @Test
  public void lowerBounds() {
    assertEquals(1., DATA.getLowerBoundKey(1.5), EPS);
    assertEquals(1., DATA.getLowerBoundKey(1.), EPS);
    assertEquals(4, DATA.getLowerBoundKey(4.), EPS);
    assertEquals(5, DATA.getLowerBoundKey(5.), EPS);
  }

  @Test
  public void lowerBoundIndices() {
    assertEquals(0, DATA.getLowerBoundIndex(1.5));
    assertEquals(0, DATA.getLowerBoundIndex(1.));
    assertEquals(3, DATA.getLowerBoundIndex(4.));
    assertEquals(4, DATA.getLowerBoundIndex(5.));
  }

  @Test
  public void lowerBoundValues() {
    assertEquals(10, DATA.get(DATA.getLowerBoundKey(1.5)), EPS);
    assertEquals(10, DATA.get(DATA.getLowerBoundKey(1.)), EPS);
    assertEquals(40, DATA.get(DATA.getLowerBoundKey(4.)), EPS);
    assertEquals(50, DATA.get(DATA.getLowerBoundKey(5.)), EPS);
  }

  @Test
  public void firstKeyValue() {
    assertEquals(1., DATA.firstKey(), EPS);
    assertEquals(10., DATA.firstValue(), EPS);
  }

  @Test
  public void lastKeyValue() {
    assertEquals(5., DATA.lastKey(), EPS);
    assertEquals(50., DATA.lastValue(), EPS);
  }

  @Test
  public void higherKeyValue() {
    assertEquals(5., DATA.higherKey(4.5), EPS);
    assertEquals(50., DATA.higherValue(4.5), EPS);
    assertEquals(5., DATA.higherKey(4.), EPS);
    assertEquals(50., DATA.higherValue(4.), EPS);
    assertEquals(2., DATA.higherKey(1.), EPS);
    assertEquals(20., DATA.higherValue(1.), EPS);
    assertNull(DATA.higherKey(5.));
    assertNull(DATA.higherValue(5.));
  }

  @Test
  public void pointLookup() {
    assertEquals(10., DATA.get(1.), EPS);
    assertEquals(20., DATA.get(2.), EPS);
    assertEquals(30., DATA.get(3.), EPS);
    assertEquals(40., DATA.get(4.), EPS);
    assertEquals(50., DATA.get(5.), EPS);
    assertNull(DATA.get(4.5));
    assertNull(DATA.get(6.));
  }

  @Test
  public void containsKey() {
    assertTrue(DATA.containsKey(1.));
    assertTrue(DATA.containsKey(2.));
    assertTrue(DATA.containsKey(3.));
    assertTrue(DATA.containsKey(4.));
    assertTrue(DATA.containsKey(5.));
    assertFalse(DATA.containsKey(1.5));
  }

  @Test
  public void keys() {
    final double[] keys = DATA.getKeys();
    assertEquals(1., keys[0], EPS);
    assertEquals(2., keys[1], EPS);
    assertEquals(3., keys[2], EPS);
    assertEquals(4., keys[3], EPS);
    assertEquals(5., keys[4], EPS);
    assertEquals(5, keys.length);
  }

  @Test
  public void values() {
    final double[] values = DATA.getValues();
    assertEquals(10., values[0], EPS);
    assertEquals(20., values[1], EPS);
    assertEquals(30., values[2], EPS);
    assertEquals(40., values[3], EPS);
    assertEquals(50., values[4], EPS);
    assertEquals(5, values.length);
  }

  @Test
  public void testBoundedValues() {
    final InterpolationBoundedValues boundedValues = DATA.getBoundedValues(1.5);
    assertEquals(boundedValues.getLowerBoundKey(), 1., 0);
    assertEquals(boundedValues.getLowerBoundValue(), 10., 0);
    assertEquals(boundedValues.getHigherBoundKey(), 2., 0);
    assertEquals(boundedValues.getHigherBoundValue(), 20., 0);
  }

  @Test
  public void testEqualsAndHashCode() {
    Interpolator1DDataBundle other = createDataBundle(KEYS1, VALUES1);
    assertEquals(DATA, other);
    assertEquals(DATA.hashCode(), other.hashCode());
    final double[] x = new double[] {KEYS1[0] + 1.456, KEYS1[1] + 1.456, KEYS1[2] + 1.456, KEYS1[3] + 1.456, KEYS1[4] + 1.456 };
    other = createDataBundle(x, VALUES1);
    assertFalse(DATA.equals(other));
    other = createDataBundle(KEYS1, x);
    assertFalse(DATA.equals(other));
  }

  @Test
  public void testSetYValue() {
    final int n = KEYS1.length;
    final double[] x = Arrays.copyOf(KEYS1, n);
    final double[] y = Arrays.copyOf(VALUES1, n);
    Arrays.sort(x);
    Arrays.sort(y);
    Interpolator1DDataBundle data1 = createDataBundle(x, y);
    final double newY = 120;
    final double[] yData = Arrays.copyOf(y, n);
    for (int i = 0; i < n; i++) {
      yData[i] = newY;
      data1.setYValueAtIndex(i, newY);
      assertEquals(data1, createDataBundle(x, yData));
      yData[i] = y[i];
      data1 = createDataBundle(x, y);
    }
  }
}
