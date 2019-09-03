/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
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
  private static final Double[] L = new Double[] {1.3};
  private static final Double[] U = new Double[] {3.4};

  @Test
  public void testNullFunction() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTEGRATOR.integrate(null, L, U));
  }

  @Test
  public void testNullLowerBound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTEGRATOR.integrate(F, null, U));
  }

  @Test
  public void testNullUpperBound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTEGRATOR.integrate(F, L, null));
  }

  @Test
  public void testEmptyLowerBound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTEGRATOR.integrate(F, new Double[0], U));
  }

  @Test
  public void testEmptyUpperBound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTEGRATOR.integrate(F, L, new Double[0]));
  }

  @Test
  public void testNullLowerBoundValue() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTEGRATOR.integrate(F, new Double[] {null}, U));
  }

  @Test
  public void testNullUpperBoundValue() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> INTEGRATOR.integrate(F, L, new Double[] {null}));
  }
}
