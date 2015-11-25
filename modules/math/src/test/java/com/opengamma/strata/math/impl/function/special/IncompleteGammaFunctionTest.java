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
public class IncompleteGammaFunctionTest {
  private static final double A = 1;
  private static final Function1D<Double, Double> FUNCTION = new IncompleteGammaFunction(A);
  private static final double EPS = 1e-9;
  private static final int MAX_ITER = 10000;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeA1() {
    new IncompleteGammaFunction(-A);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeA2() {
    new IncompleteGammaFunction(-A, MAX_ITER, EPS);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeIter() {
    new IncompleteGammaFunction(A, -MAX_ITER, EPS);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeEps() {
    new IncompleteGammaFunction(A, MAX_ITER, -EPS);
  }

  @Test
  public void testLimits() {
    assertEquals(FUNCTION.apply(0.), 0, EPS);
    assertEquals(FUNCTION.apply(100.), 1, EPS);
  }

  @Test
  public void test() {
    final Function1D<Double, Double> f = new Function1D<Double, Double>() {

      @Override
      public Double apply(final Double x) {
        return 1 - Math.exp(-x);
      }

    };
    final double x = 4.6;
    assertEquals(f.apply(x), FUNCTION.apply(x), EPS);
  }
}
