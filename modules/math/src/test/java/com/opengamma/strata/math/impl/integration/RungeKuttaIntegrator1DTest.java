/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class RungeKuttaIntegrator1DTest {

  private static final double ROOT_2PI = Math.sqrt(2.0 * java.lang.Math.PI);

  private static final Function<Double, Double> CUBE = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return x * x * x;
    }

  };

  private static final Function<Double, Double> TRIANGLE = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      if (x > 1.0 || x < 0.0) {
        return x - Math.floor(x);
      }
      return x;
    }
  };

  private static final Function<Double, Double> MIX_NORM = new Function<Double, Double>() {
    private final double[] w = new double[] {0.2, 0.2, 0.2, 0.2, 0.2};
    private final double[] mu = new double[] {0.0, -0.4, 0.5, 0.0, 0.01234583};
    private final double[] sigma = new double[] {3.0, 0.1, 5.0, 0.001, 0.0001};

    @SuppressWarnings("synthetic-access")
    @Override
    public Double apply(final Double x) {
      final int n = w.length;
      double res = 0.0;
      double expo;
      for (int i = 0; i < n; i++) {
        expo = (x - mu[i]) * (x - mu[i]) / sigma[i] / sigma[i];
        res += w[i] * Math.exp(-0.5 * expo) / ROOT_2PI / sigma[i];
      }
      return res;
    }
  };

  private static final Function<Double, Double> SIN_INV_X = new Function<Double, Double>() {
    @Override
    public Double apply(final Double x) {
      final double eps = 1e-127;
      if (Math.abs(x) < eps) {
        return 0.0;
      }
      return Math.sin(1.0 / x);

    }

  };

  @Test
  public void testNegativeAbsTol() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RungeKuttaIntegrator1D(-1.0));
  }

  @Test
  public void testNegativeRelTol() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RungeKuttaIntegrator1D(1e-7, -1.0));
  }

  @Test
  public void testLessTahnOneStep() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RungeKuttaIntegrator1D(0));
  }

  @Test
  public void test() {
    final double eps = 1e-9;
    final int minSteps = 10;
    final Integrator1D<Double, Double> integrator = new RungeKuttaIntegrator1D(eps, eps, minSteps);

    double lower = 0;
    double upper = 2.0;
    assertThat(4.0).isCloseTo(integrator.integrate(CUBE, lower, upper), offset(eps));

    lower = 0.0;
    upper = 1.5;
    assertThat(0.625).isCloseTo(integrator.integrate(TRIANGLE, lower, upper), offset(eps));

    lower = -30;
    upper = 30;
    assertThat(1.0).isCloseTo(integrator.integrate(MIX_NORM, lower, upper), offset(eps));
  }

  @Test
  public void testCutoff() {

    final double eps = 1e-9;
    final int minSteps = 10;
    final Integrator1D<Double, Double> integrator = new RungeKuttaIntegrator1D(eps, eps, minSteps);
    final double lower = -1.0;
    final double upper = 1.0;
    assertThat(0.0).isCloseTo(integrator.integrate(SIN_INV_X, lower, upper), offset(eps));

  }

}
