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
public class NaturalLogGammaFunctionTest {
  private static final Function<Double, Double> LN_GAMMA = new NaturalLogGammaFunction();
  private static final double EPS = 1e-9;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeNumber() {
    LN_GAMMA.apply(-0.1);
  }

  @Test
  public void testRecurrence() {
    double z = 12;
    double gamma = getGammaFunction(LN_GAMMA.apply(z));
    assertEquals(getGammaFunction(LN_GAMMA.apply(z + 1)), z * gamma, gamma * EPS);
    z = 11.34;
    gamma = getGammaFunction(LN_GAMMA.apply(z));
    assertEquals(getGammaFunction(LN_GAMMA.apply(z + 1)), z * gamma, gamma * EPS);
  }

  @Test
  public void testIntegerArgument() {
    final int x = 5;
    final double factorial = 24;
    assertEquals(getGammaFunction(LN_GAMMA.apply(Double.valueOf(x))), factorial, EPS);
  }

  private double getGammaFunction(final double x) {
    return Math.exp(x);
  }
}
