/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.Function1D;

/**
 * Test.
 */
@Test
public class TopHatFunctionTest {
  private static final double X1 = 2;
  private static final double X2 = 2.5;
  private static final double Y = 10;
  private static final Function1D<Double, Double> F = new TopHatFunction(X1, X2, Y);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testWrongOrder() {
    new TopHatFunction(X2, X1, Y);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull() {
    F.evaluate((Double) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testX1() {
    F.evaluate(X1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testX2() {
    F.evaluate(X2);
  }

  @Test
  public void test() {
    assertEquals(F.evaluate(X1 - 1e-15), 0, 0);
    assertEquals(F.evaluate(X2 + 1e-15), 0, 0);
    assertEquals(F.evaluate((X1 + X2) / 2), Y, 0);
  }
}
