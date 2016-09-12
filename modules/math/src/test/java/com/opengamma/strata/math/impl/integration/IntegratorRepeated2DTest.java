/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import static org.testng.AssertJUnit.assertEquals;

import java.util.function.BiFunction;

import org.testng.annotations.Test;

/**
 * Tests related to the repeated one-dimensional integration to integrate 2-D functions.
 */
@Test
public class IntegratorRepeated2DTest {

  @Test
  /** Numerical integral vs a known explicit solution. */
  public void integrate() {

    // Test function.
    BiFunction<Double, Double, Double> f = (x1, x2) -> x1 + Math.sin(x2);

    double absTol = 1.0E-6;
    double relTol = 1.0E-6;
    int minSteps = 6;
    RungeKuttaIntegrator1D integrator1D = new RungeKuttaIntegrator1D(absTol, relTol, minSteps);
    IntegratorRepeated2D integrator2D = new IntegratorRepeated2D(integrator1D);

    Double[] lower;
    Double[] upper;
    double result, resultExpected;
    // First set of limits.
    lower = new Double[] {0.0, 1.0};
    upper = new Double[] {2.0, 10.0};
    result = integrator2D.integrate(f, lower, upper);
    resultExpected =
        (upper[0] * upper[0] - lower[0] * lower[0]) / 2.0 * (upper[1] - lower[1]) +
            (upper[0] - lower[0]) * (-Math.cos(upper[1]) + Math.cos(lower[1]));
    assertEquals("Integration 2D - repeated 1D", resultExpected, result, 1E-8);
    // Second set of limits.
    lower = new Double[] {0.25, 5.25};
    upper = new Double[] {25.25, 35.25};
    result = integrator2D.integrate(f, lower, upper);
    resultExpected =
        (upper[0] * upper[0] - lower[0] * lower[0]) / 2.0 * (upper[1] - lower[1]) +
            (upper[0] - lower[0]) * (-Math.cos(upper[1]) + Math.cos(lower[1]));
    assertEquals("Integration 2D - repeated 1D", resultExpected, result, 1E-6);
  }

}
