/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class RealFunctionIntegrator1DFactoryTest {

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadName() {
    RealFunctionIntegrator1DFactory.getIntegrator("a");
  }

  @Test
  public void testNullCalculator() {
    assertNull(RealFunctionIntegrator1DFactory.getIntegratorName(null));
  }

  @Test
  public void test() {
    assertEquals(RealFunctionIntegrator1DFactory.EXTENDED_TRAPEZOID, RealFunctionIntegrator1DFactory.getIntegratorName(RealFunctionIntegrator1DFactory
        .getIntegrator(RealFunctionIntegrator1DFactory.EXTENDED_TRAPEZOID)));
    assertEquals(RealFunctionIntegrator1DFactory.ROMBERG, RealFunctionIntegrator1DFactory.getIntegratorName(RealFunctionIntegrator1DFactory.getIntegrator(RealFunctionIntegrator1DFactory.ROMBERG)));
    assertEquals(RealFunctionIntegrator1DFactory.SIMPSON, RealFunctionIntegrator1DFactory.getIntegratorName(RealFunctionIntegrator1DFactory.getIntegrator(RealFunctionIntegrator1DFactory.SIMPSON)));
  }
}
