/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.Function1D;

/**
 * Abstract test.
 */
public abstract class Integrator1DTestCase {
  private static final Function1D<Double, Double> DF = new Function1D<Double, Double>() {

    @Override
    public Double evaluate(final Double x) {
      return 1 + Math.exp(-x);
    }

  };
  private static final Function1D<Double, Double> F = new Function1D<Double, Double>() {

    @Override
    public Double evaluate(final Double x) {
      return x - Math.exp(-x);
    }

  };
  private static final Double LOWER = 0.;
  private static final Double UPPER = 12.;
  private static final double EPS = 1e-5;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction() {
    getIntegrator().integrate(null, LOWER, UPPER);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullLowerBound() {
    getIntegrator().integrate(DF, null, UPPER);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullUpperBound() {
    getIntegrator().integrate(DF, LOWER, null);
  }

  @Test
  public void test() {
    assertEquals(getIntegrator().integrate(DF, LOWER, UPPER), F.evaluate(UPPER) - F.evaluate(LOWER), EPS);
    assertEquals(getIntegrator().integrate(DF, UPPER, LOWER), -getIntegrator().integrate(DF, LOWER, UPPER), EPS);
  }

  protected abstract Integrator1D<Double, Double> getIntegrator();

}
