/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.testng.AssertJUnit.assertEquals;

import org.apache.commons.math3.random.Well44497b;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class RealPolynomialFunction1DTest {

  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final double[] C = new double[] {3.4, 5.6, 1., -4. };
  private static final DoubleFunction1D F = new RealPolynomialFunction1D(C);
  private static final double EPS = 1e-12;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullCoefficients() {
    new RealPolynomialFunction1D(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyCoefficients() {
    new RealPolynomialFunction1D(new double[0]);
  }

  @Test
  public void testEvaluate() {
    final double x = RANDOM.nextDouble();
    assertEquals(C[3] * Math.pow(x, 3) + C[2] * Math.pow(x, 2) + C[1] * x + C[0], F.applyAsDouble(x), EPS);
  }

  @Test
  public void testDerivative() {
    final double x = RANDOM.nextDouble();
    assertEquals(3 * C[3] * Math.pow(x, 2) + 2 * C[2] * x + C[1], F.derivative().applyAsDouble(x), EPS);
  }
}
