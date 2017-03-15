/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import static org.testng.AssertJUnit.assertEquals;

import java.util.function.Function;

import org.apache.commons.math3.random.Well44497b;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class IncompleteBetaFunctionTest {

  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final double EPS = 1e-9;
  private static final double A = 0.4;
  private static final double B = 0.2;
  private static final int MAX_ITER = 10000;
  private static final Function<Double, Double> BETA = new IncompleteBetaFunction(A, B);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeA1() {
    new IncompleteBetaFunction(-A, B);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeA2() {
    new IncompleteBetaFunction(-A, B, EPS, MAX_ITER);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeB1() {
    new IncompleteBetaFunction(A, -B);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeB2() {
    new IncompleteBetaFunction(A, -B, EPS, MAX_ITER);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeEps() {
    new IncompleteBetaFunction(A, B, -EPS, MAX_ITER);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeIter() {
    new IncompleteBetaFunction(A, B, EPS, -MAX_ITER);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testLow() {
    BETA.apply(-0.3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testHigh() {
    BETA.apply(1.5);
  }

  @Test
  public void test() {
    final double a = RANDOM.nextDouble();
    final double b = RANDOM.nextDouble();
    final double x = RANDOM.nextDouble();
    final Function<Double, Double> f1 = new IncompleteBetaFunction(a, b);
    final Function<Double, Double> f2 = new IncompleteBetaFunction(b, a);
    assertEquals(f1.apply(0.), 0, EPS);
    assertEquals(f1.apply(1.), 1, EPS);
    assertEquals(f1.apply(x), 1 - f2.apply(1 - x), EPS);
  }
}
