/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import java.util.function.Function;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class Integrator1DTest {
  private static final Integrator1D<Double, Double> INTEGRATOR = new Integrator1D<Double, Double>() {

    @Override
    public Double integrate(final Function<Double, Double> f, final Double lower, final Double upper) {
      return 0.;
    }

  };
  private static final Function<Double, Double> F = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return 0.;
    }

  };
  private static final Double[] L = new Double[] {1.3 };
  private static final Double[] U = new Double[] {3.4 };

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction() {
    INTEGRATOR.integrate(null, L, U);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullLowerBound() {
    INTEGRATOR.integrate(F, null, U);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullUpperBound() {
    INTEGRATOR.integrate(F, L, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyLowerBound() {
    INTEGRATOR.integrate(F, new Double[0], U);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyUpperBound() {
    INTEGRATOR.integrate(F, L, new Double[0]);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullLowerBoundValue() {
    INTEGRATOR.integrate(F, new Double[] {null }, U);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullUpperBoundValue() {
    INTEGRATOR.integrate(F, L, new Double[] {null });
  }
}
