/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import static org.testng.AssertJUnit.assertEquals;

import java.util.function.Function;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class TopHatFunctionTest {
  private static final double X1 = 2;
  private static final double X2 = 2.5;
  private static final double Y = 10;
  private static final Function<Double, Double> F = new TopHatFunction(X1, X2, Y);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testWrongOrder() {
    new TopHatFunction(X2, X1, Y);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull() {
    F.apply((Double) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testX1() {
    F.apply(X1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testX2() {
    F.apply(X2);
  }

  @Test
  public void test() {
    assertEquals(F.apply(X1 - 1e-15), 0, 0);
    assertEquals(F.apply(X2 + 1e-15), 0, 0);
    assertEquals(F.apply((X1 + X2) / 2), Y, 0);
  }
}
