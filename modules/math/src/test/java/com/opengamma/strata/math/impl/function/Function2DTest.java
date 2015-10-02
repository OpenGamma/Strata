/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class Function2DTest {
  private static final Function2D<Double, Double> F = new Function2D<Double, Double>() {

    @Override
    public Double evaluate(final Double x1, final Double x2) {
      return 0.;
    }

  };

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullArray() {
    F.evaluate((Double[]) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyArray() {
    F.evaluate(new Double[0]);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testShortArray() {
    F.evaluate(new Double[] {1. });
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFirst() {
    F.evaluate(new Double[] {null, 1. });
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullSecond() {
    F.evaluate(new Double[] {1., null });
  }
}
