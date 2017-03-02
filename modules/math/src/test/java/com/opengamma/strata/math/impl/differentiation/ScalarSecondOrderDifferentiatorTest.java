/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.AssertJUnit.assertEquals;

import java.util.function.Function;

import org.testng.annotations.Test;

/**
 * Test {@link ScalarSecondOrderDifferentiator}.
 */
@Test
public class ScalarSecondOrderDifferentiatorTest {

  private static final Function<Double, Double> F = new Function<Double, Double>() {
    @Override
    public Double apply(final Double x) {
      return 3d * x * x + 4d * x - Math.sin(x);
    }
  };
  private static final Function<Double, Boolean> DOMAIN = new Function<Double, Boolean>() {
    @Override
    public Boolean apply(final Double x) {
      return x >= 0d && x <= Math.PI;
    }
  };
  private static final Function<Double, Double> DX_ANALYTIC = new Function<Double, Double>() {
    @Override
    public Double apply(final Double x) {
      return 6d + Math.sin(x);
    }
  };
  private static final ScalarSecondOrderDifferentiator CALC = new ScalarSecondOrderDifferentiator();
  private static final double EPS = 1.0e-4;

  public void testNullDifferenceType() {
    assertThrowsIllegalArg(() -> new ScalarFirstOrderDifferentiator(null));
  }

  public void testNullFunction() {
    assertThrowsIllegalArg(() -> CALC.differentiate((Function<Double, Double>) null));
  }

  public void testDomainOut() {
    Function<Double, Boolean> domain = new Function<Double, Boolean>() {
      @Override
      public Boolean apply(final Double x) {
        return x >= 0d && x <= 1.0e-8;
      }
    };
    assertThrowsIllegalArg(() -> CALC.differentiate(F, domain).apply(1.0e-9));
  }

  public void analyticTest() {
    final double x = 0.2245;
    assertEquals(CALC.differentiate(F).apply(x), DX_ANALYTIC.apply(x), EPS);
  }

  public void domainTest() {
    final double[] x = new double[] {1.2, 0, Math.PI };
    final Function<Double, Double> alFunc = CALC.differentiate(F, DOMAIN);
    for (int i = 0; i < 3; i++) {
      assertEquals(alFunc.apply(x[i]), DX_ANALYTIC.apply(x[i]), EPS);
    }
  }

}
