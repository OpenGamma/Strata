/*
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
public class GaussLaguerreWeightAndAbscissaFunctionTest extends WeightAndAbscissaFunctionTestCase {
  private static final double[] X2 = new double[] {0.585786, 3.41421 };
  private static final double[] W2 = new double[] {0.853553, 0.146447 };
  private static final double[] X3 = new double[] {0.415775, 2.29428, 6.28995 };
  private static final double[] W3 = new double[] {0.711093, 0.278518, 0.0103893 };
  private static final double[] X4 = new double[] {0.322548, 1.74576, 4.53662, 9.39507 };
  private static final double[] W4 = new double[] {0.603154, 0.357419, 0.0388879, 0.000539295 };
  private static final double[] X5 = new double[] {0.26356, 1.4134, 3.59643, 7.08581, 12.6408 };
  private static final double[] W5 = new double[] {0.521756, 0.398667, 0.0759424, 0.00361176, 0.00002337 };
  private static final QuadratureWeightAndAbscissaFunction F = new GaussLaguerreWeightAndAbscissaFunction(0);

  @Test
  public void test() {
    assertResults(F.generate(2), X2, W2);
    assertResults(F.generate(3), X3, W3);
    assertResults(F.generate(4), X4, W4);
    assertResults(F.generate(5), X5, W5);
  }

  @Override
  protected QuadratureWeightAndAbscissaFunction getFunction() {
    return F;
  }
}
