/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class GaussianQuadratureIntegrator1DTest {

  private static final Function<Double, Double> ONE = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return 1.0;
    }
  };

  private static final Function<Double, Double> DF1 = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return x * x * x * (x - 4);
    }

  };
  private static final Function<Double, Double> F1 = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return x * x * x * x * (x / 5. - 1);
    }

  };
  private static final Function<Double, Double> DF2 = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return Math.exp(-2 * x);
    }

  };

  private static final Function<Double, Double> DF3 = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return Math.exp(-x * x);
    }

  };

  private static final Function<Double, Double> COS = new Function<Double, Double>() {
    @Override
    public Double apply(final Double x) {
      return Math.cos(x);
    }
  };

  private static final Function<Double, Double> COS_EXP = new Function<Double, Double>() {
    @Override
    public Double apply(final Double x) {
      return Math.cos(x) * Math.exp(-x * x);
    }
  };

  private static final double EPS = 1e-6;

  @Test
  public void testGaussLegendre() {
    double upper = 2;
    double lower = -6;
    final Integrator1D<Double, Double> integrator = new GaussLegendreQuadratureIntegrator1D(6);
    assertThat(F1.apply(upper) - F1.apply(lower)).isCloseTo(integrator.integrate(DF1, lower, upper), offset(EPS));
    lower = -0.56;
    upper = 1.4;
    assertThat(F1.apply(upper) - F1.apply(lower)).isCloseTo(integrator.integrate(DF1, lower, upper), offset(EPS));
  }

  @Test
  public void testGaussLaguerre() {
    final double upper = Double.POSITIVE_INFINITY;
    final double lower = 0;
    final Integrator1D<Double, Double> integrator = new GaussLaguerreQuadratureIntegrator1D(15);
    assertThat(0.5).isCloseTo(integrator.integrate(DF2, lower, upper), offset(EPS));
  }

  @Test
  public void testRungeKutta() {
    final RungeKuttaIntegrator1D integrator = new RungeKuttaIntegrator1D();
    final double lower = -1;
    final double upper = 2;
    assertThat(F1.apply(upper) - F1.apply(lower)).isCloseTo(integrator.integrate(DF1, lower, upper), offset(EPS));
  }

  @Test
  public void testGaussJacobi() {
    final double upper = 12;
    final double lower = -1;
    final Integrator1D<Double, Double> integrator = new GaussJacobiQuadratureIntegrator1D(7);
    assertThat(F1.apply(upper) - F1.apply(lower)).isCloseTo(integrator.integrate(DF1, lower, upper), offset(EPS));
  }

  @Test
  public void testGaussHermite() {
    final double rootPI = Math.sqrt(Math.PI);
    final double upper = Double.POSITIVE_INFINITY;
    final double lower = Double.NEGATIVE_INFINITY;
    final GaussHermiteQuadratureIntegrator1D integrator = new GaussHermiteQuadratureIntegrator1D(10);
    assertThat(rootPI).isCloseTo(integrator.integrateFromPolyFunc(ONE), offset(1e-15));
    assertThat(rootPI).isCloseTo(integrator.integrate(DF3, lower, upper), offset(EPS));
  }

  @Test
  public void testGaussHermite2() {
    final RungeKuttaIntegrator1D rk = new RungeKuttaIntegrator1D(1e-15);
    final Double expected = 2 * rk.integrate(COS_EXP, 0., 10.);
    final GaussHermiteQuadratureIntegrator1D gh = new GaussHermiteQuadratureIntegrator1D(11);
    final double res1 = gh.integrateFromPolyFunc(COS);
    assertThat(expected).isCloseTo(res1, offset(1e-15)); //11 points gets you machine precision
    final double res2 = gh.integrate(COS_EXP, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    assertThat(expected).isCloseTo(res2, offset(1e-15));
  }

}
