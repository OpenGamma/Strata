/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Two dimensional integration by repeated one dimensional integration with a Integrator1D integration {@link Integrator1D}.
 */
public class IntegratorRepeated2D extends Integrator2D<Double, Double> {

  /**
   * The 1-D integrator to be used for each repeated integral.
   */
  private final Integrator1D<Double, Double> _integrator1D;

  /**
   * Constructor.
   * @param integrator1D The 1-D integrator to be used for each repeated integral.
   */
  public IntegratorRepeated2D(Integrator1D<Double, Double> integrator1D) {
    _integrator1D = integrator1D;
  }

  @Override
  public Double integrate(BiFunction<Double, Double, Double> f, Double[] lower, Double[] upper) {
    return _integrator1D.integrate(innerIntegral(f, lower[0], upper[0]), lower[1], upper[1]);
  }

  /**
   * The inner integral function of the repeated 1-D integrations. For a given
   * $y$ it returns $\int_{x_1}^{x_2} f(x,y) dx$.
   * @param f The bi-function.
   * @param lower The lower bound (for the inner-first variable).
   * @param upper The upper bound (for the inner-first variable).
   * @return The inner integral function.
   */
  private Function<Double, Double> innerIntegral(BiFunction<Double, Double, Double> f, Double lower, Double upper) {

    return new Function<Double, Double>() {

      @SuppressWarnings("synthetic-access")
      @Override
      public Double apply(Double y) {
        Function<Double, Double> fy = new Function<Double, Double>() {
          @Override
          public Double apply(Double x) {
            return f.apply(x, y);
          }
        };
        return _integrator1D.integrate(fy, lower, upper);
      }
    };
  }
}
