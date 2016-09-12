/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.function.Function;

import org.testng.annotations.Test;

/**
 * 
 */
@Test
public class AdaptiveCompositeIntegrator1DTest extends Integrator1DTestCase {
  private static final Integrator1D<Double, Double> INTEGRATOR = new AdaptiveCompositeIntegrator1D(new SimpsonIntegrator1D());

  @Override
  protected Integrator1D<Double, Double> getIntegrator() {
    return INTEGRATOR;
  }

  /**
   * 
   */
  @Test
  public void sampleDataTest() {
    final Integrator1D<Double, Double> localInt = new AdaptiveCompositeIntegrator1D(new SimpsonIntegrator1D(), 10., 1.e-4);
    assertEquals(-0.368924186060527, localInt.integrate(sampleFunc(), 1.1, 3.), 1.e-6); // answer from quadpack
  }

  private Function<Double, Double> sampleFunc() {
    return new Function<Double, Double>() {
      @Override
      public Double apply(final Double x) {
        return 100. * Math.sin(10. / x) / x / x;
      }
    };
  }

  /**
   * 
   */
  @Test
  public void equalsHashCodetest() {
    final Integrator1D<Double, Double> integBase = new SimpsonIntegrator1D();
    final Integrator1D<Double, Double> integ0 = new AdaptiveCompositeIntegrator1D(integBase);
    final Integrator1D<Double, Double> integ1 = new AdaptiveCompositeIntegrator1D(integBase);
    final Integrator1D<Double, Double> integ2 = new AdaptiveCompositeIntegrator1D(new RungeKuttaIntegrator1D());
    final Integrator1D<Double, Double> integ3 = new AdaptiveCompositeIntegrator1D(integBase, 1., 1.e-5);
    final Integrator1D<Double, Double> integ4 = new AdaptiveCompositeIntegrator1D(integBase, 2., 1.e-5);
    final Integrator1D<Double, Double> integ5 = new AdaptiveCompositeIntegrator1D(integBase, 1., 1.e-6);

    assertTrue(integ0.equals(integ0));

    assertTrue(integ0.equals(integ1));
    assertTrue(integ1.equals(integ0));
    assertTrue(integ1.hashCode() == integ0.hashCode());

    assertTrue(!(integ0.equals(integ2)));
    assertTrue(!(integ0.equals(integ3)));
    assertTrue(!(integ0.equals(integ4)));
    assertTrue(!(integ0.equals(integ5)));
    assertTrue(!(integ0.equals(integBase)));
    assertTrue(!(integ0.equals(null)));
    assertTrue(!(integ3.equals(integ5)));

    assertTrue(!(integ1.hashCode() == INTEGRATOR.hashCode()));
  }
}
