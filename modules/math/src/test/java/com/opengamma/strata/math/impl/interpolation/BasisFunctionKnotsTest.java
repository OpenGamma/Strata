/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;
import org.testng.internal.junit.ArrayAsserts;

/**
 * 
 */
@Test
public class BasisFunctionKnotsTest {

  private static final double[] KNOTS;
  private static final double[] WRONG_ORDER_KNOTS;

  static {
    final int n = 10;
    KNOTS = new double[n + 1];

    for (int i = 0; i < n + 1; i++) {
      KNOTS[i] = 0 + i * 1.0;
    }
    WRONG_ORDER_KNOTS = KNOTS.clone();
    double a = WRONG_ORDER_KNOTS[6];
    WRONG_ORDER_KNOTS[6] = WRONG_ORDER_KNOTS[4];
    WRONG_ORDER_KNOTS[4] = a;
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullKnots() {
    BasisFunctionKnots.fromKnots(null, 2);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullInternalKnots() {
    BasisFunctionKnots.fromInternalKnots(null, 2);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegDegree() {
    BasisFunctionKnots.fromKnots(KNOTS, -1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegDegree2() {
    BasisFunctionKnots.fromInternalKnots(KNOTS, -1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testWrongOrderUniform() {
    BasisFunctionKnots.fromUniform(2.0, 1.0, 10, 3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testWrongOrderKnots() {
    BasisFunctionKnots.fromKnots(WRONG_ORDER_KNOTS, 3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testWrongOrderInternalKnots() {
    BasisFunctionKnots.fromInternalKnots(WRONG_ORDER_KNOTS, 3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDegreeToHigh1() {
    BasisFunctionKnots.fromUniform(0.0, 10.0, 11, 11);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDegreeToHigh2() {
    BasisFunctionKnots.fromInternalKnots(KNOTS, 11);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDegreeToHigh3() {
    BasisFunctionKnots.fromKnots(KNOTS, 11);
  }

  @Test
  public void testUniform() {
    BasisFunctionKnots knots = BasisFunctionKnots.fromUniform(1.0, 2.0, 10, 3);
    assertEquals(3, knots.getDegree());
    assertEquals(16, knots.getNumKnots());
    assertEquals(12, knots.getNumSplines());
  }

  @Test
  public void testInternalKnots() {
    BasisFunctionKnots knots = BasisFunctionKnots.fromInternalKnots(KNOTS, 2);
    assertEquals(2, knots.getDegree());
    assertEquals(15, knots.getNumKnots());
    assertEquals(12, knots.getNumSplines());
  }

  @Test
  public void testKnots() {
    BasisFunctionKnots knots = BasisFunctionKnots.fromKnots(KNOTS, 3);
    assertEquals(3, knots.getDegree());
    assertEquals(11, knots.getNumKnots());
    assertEquals(7, knots.getNumSplines());
    ArrayAsserts.assertArrayEquals(KNOTS, knots.getKnots(), 1e-15);
  }

}
