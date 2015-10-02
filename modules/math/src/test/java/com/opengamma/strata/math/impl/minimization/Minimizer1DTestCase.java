/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.Assert;

import com.opengamma.strata.math.impl.function.Function1D;

/**
 * Abstract test.
 */
public abstract class Minimizer1DTestCase {
  private static final double EPS = 1e-5;
  private static final Function1D<Double, Double> QUADRATIC = new Function1D<Double, Double>() {

    @Override
    public Double evaluate(final Double x) {
      return x * x + 7 * x + 12;
    }

  };
  private static final Function1D<Double, Double> QUINTIC = new Function1D<Double, Double>() {
    @Override
    public Double evaluate(final Double x) {
      return 1 + x * (-3 + x * (-9 + x * (-1 + x * (4 + x))));
    }
  };

  public void assertInputs(final ScalarMinimizer minimizer) {
    try {
      minimizer.minimize(null, 0.0, 2., 3.);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
  }

  public void assertMinimizer(final ScalarMinimizer minimizer) {
    double result = minimizer.minimize(QUADRATIC, 0.0, -10., 10.);
    assertEquals(-3.5, result, EPS);
    result = minimizer.minimize(QUINTIC, 0.0, 0.5, 2.);
    assertEquals(1.06154, result, EPS);
  }
}
