/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.internal.junit.ArrayAsserts.assertArrayEquals;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class Interpolator2DDataBundleTest {
  private static final double[] X = new double[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
  private static final double[] Y = new double[] {10, 11, 12, 13, 14, 15, 16, 17, 18, 19 };
  private static final double[] Z = new double[] {20, 21, 22, 23, 24, 25, 26, 27, 28, 29 };

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullX() {
    new Interpolator2DDataBundle(null, Y, Z);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullY() {
    new Interpolator2DDataBundle(X, null, Z);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullZ() {
    new Interpolator2DDataBundle(X, Y, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testWrongLengthY() {
    new Interpolator2DDataBundle(X, new double[] {1, 2, 3 }, Z);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testWrongLengthZ() {
    new Interpolator2DDataBundle(X, Y, new double[] {1, 2, 3 });
  }

  @Test
  public void test() {
    final Interpolator2DDataBundle data = new Interpolator2DDataBundle(X, Y, Z);
    assertArrayEquals(data.getXData(), X, 0);
    assertArrayEquals(data.getYData(), Y, 0);
    assertArrayEquals(data.getZData(), Z, 0);
    Interpolator2DDataBundle other = new Interpolator2DDataBundle(X, Y, Z);
    assertEquals(other, data);
    assertEquals(other.hashCode(), data.hashCode());
    other = new Interpolator2DDataBundle(Y, Y, Z);
    assertFalse(other.equals(data));
    other = new Interpolator2DDataBundle(X, X, Z);
    assertFalse(other.equals(data));
    other = new Interpolator2DDataBundle(X, Y, Y);
    assertFalse(other.equals(data));
  }
}
