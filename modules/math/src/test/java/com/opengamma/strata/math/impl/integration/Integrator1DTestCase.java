/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * Abstract test.
 */
public abstract class Integrator1DTestCase {
  private static final Function<Double, Double> DF = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return 1 + Math.exp(-x);
    }

  };
  private static final Function<Double, Double> F = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return x - Math.exp(-x);
    }

  };
  private static final Double LOWER = 0.;
  private static final Double UPPER = 12.;
  private static final double EPS = 1e-5;

  @Test
  public void testNullFunction() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> getIntegrator().integrate(null, LOWER, UPPER));
  }

  @Test
  public void testNullLowerBound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> getIntegrator().integrate(DF, null, UPPER));
  }

  @Test
  public void testNullUpperBound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> getIntegrator().integrate(DF, LOWER, null));
  }

  @Test
  public void test() {
    assertThat(getIntegrator().integrate(DF, LOWER, UPPER))
        .isCloseTo(F.apply(UPPER) - F.apply(LOWER), offset(EPS));
    assertThat(getIntegrator().integrate(DF, UPPER, LOWER))
        .isCloseTo(-getIntegrator().integrate(DF, LOWER, UPPER), offset(EPS));
  }

  protected abstract Integrator1D<Double, Double> getIntegrator();

}
