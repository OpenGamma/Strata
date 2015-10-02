/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class GaussHermiteWeightAndAbscissaFunctionTest extends WeightAndAbscissaFunctionTestCase {
  private static final double SQRT_PI = Math.sqrt(Math.PI);
  private static final double DENOM1 = 4 * (3 - Math.sqrt(6));
  private static final double DENOM2 = 4 * (3 + Math.sqrt(6));
  private static final double[] X2 = new double[] {-Math.sqrt(2) / 2., Math.sqrt(2) / 2. };
  private static final double[] W2 = new double[] {SQRT_PI / 2., SQRT_PI / 2. };
  private static final double[] X3 = new double[] {-Math.sqrt(6) / 2., 0, Math.sqrt(6) / 2. };
  private static final double[] W3 = new double[] {SQRT_PI / 6., 2 * SQRT_PI / 3., SQRT_PI / 6. };
  private static final double[] X4 = new double[] {-Math.sqrt((3 + Math.sqrt(6)) / 2.), -Math.sqrt((3 - Math.sqrt(6)) / 2.), Math.sqrt((3 - Math.sqrt(6)) / 2.), Math.sqrt((3 + Math.sqrt(6)) / 2.) };
  private static final double[] W4 = new double[] {SQRT_PI / DENOM2, SQRT_PI / DENOM1, SQRT_PI / DENOM1, SQRT_PI / DENOM2 };
  private static final QuadratureWeightAndAbscissaFunction F = new GaussHermiteWeightAndAbscissaFunction();

  @Override
  protected QuadratureWeightAndAbscissaFunction getFunction() {
    return F;
  }

  @Test
  public void test() {
    assertResults(F.generate(2), X2, W2);
    assertResults(F.generate(3), X3, W3);
    assertResults(F.generate(4), X4, W4);
  }
}
