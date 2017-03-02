/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class ExtendedTrapezoidIntegrator1DTest extends Integrator1DTestCase {
  private static final Integrator1D<Double, Double> INTEGRATOR = new ExtendedTrapezoidIntegrator1D();

  @Override
  protected Integrator1D<Double, Double> getIntegrator() {
    return INTEGRATOR;
  }

}
