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
public class FunctionNDTest {
  private static final FunctionND<Double, Double> F = new FunctionND<Double, Double>() {

    @Override
    protected Double evaluateFunction(final Double[] x) {
      return x[1] + x[2] * x[0];
    }

  };

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullParameters() {
    F.evaluate((Double[]) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullParameter() {
    F.evaluate(1., null, 2.);
  }

}
