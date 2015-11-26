/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import static org.testng.AssertJUnit.assertEquals;

import java.util.function.Function;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class ScalarFirstOrderDifferentiatorTest {
  private static final Function<Double, Double> F = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return 3 * x * x + 4 * x - Math.sin(x);
    }
  };

  private static final Function<Double, Boolean> DOMAIN = new Function<Double, Boolean>() {
    @Override
    public Boolean apply(final Double x) {
      return x >= 0 && x <= Math.PI;
    }
  };

  private static final Function<Double, Double> DX_ANALYTIC = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return 6 * x + 4 - Math.cos(x);
    }

  };
  private static final double EPS = 1e-5;
  private static final ScalarFirstOrderDifferentiator FORWARD = new ScalarFirstOrderDifferentiator(FiniteDifferenceType.FORWARD, EPS);
  private static final ScalarFirstOrderDifferentiator CENTRAL = new ScalarFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, EPS);
  private static final ScalarFirstOrderDifferentiator BACKWARD = new ScalarFirstOrderDifferentiator(FiniteDifferenceType.BACKWARD, EPS);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDifferenceType() {
    new ScalarFirstOrderDifferentiator(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction() {
    CENTRAL.differentiate((Function<Double, Double>) null);
  }

  @Test
  public void test() {
    final double x = 0.2245;
    assertEquals(FORWARD.differentiate(F).apply(x), DX_ANALYTIC.apply(x), 10 * EPS);
    assertEquals(CENTRAL.differentiate(F).apply(x), DX_ANALYTIC.apply(x), EPS * EPS); // This is why you use central difference
    assertEquals(BACKWARD.differentiate(F).apply(x), DX_ANALYTIC.apply(x), 10 * EPS);
  }

  @Test
  public void domainTest() {
    final double[] x = new double[] {1.2, 0, Math.PI };
    final Function<Double, Double> alFunc = CENTRAL.differentiate(F, DOMAIN);
    for (int i = 0; i < 3; i++) {
      assertEquals(alFunc.apply(x[i]), DX_ANALYTIC.apply(x[i]), 1e-8);
    }
  }
}
