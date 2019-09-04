/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class RealFunctionIntegrator1DFactoryTest {

  @Test
  public void testBadName() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RealFunctionIntegrator1DFactory.getIntegrator("a"));
  }

  @Test
  public void testNullCalculator() {
    assertThat(RealFunctionIntegrator1DFactory.getIntegratorName(null)).isNull();
  }

  @Test
  public void test() {
    assertThat(RealFunctionIntegrator1DFactory.EXTENDED_TRAPEZOID)
        .isEqualTo(RealFunctionIntegrator1DFactory.getIntegratorName(RealFunctionIntegrator1DFactory
            .getIntegrator(RealFunctionIntegrator1DFactory.EXTENDED_TRAPEZOID)));
    assertThat(RealFunctionIntegrator1DFactory.ROMBERG)
        .isEqualTo(RealFunctionIntegrator1DFactory
            .getIntegratorName(RealFunctionIntegrator1DFactory.getIntegrator(RealFunctionIntegrator1DFactory.ROMBERG)));
    assertThat(RealFunctionIntegrator1DFactory.SIMPSON)
        .isEqualTo(RealFunctionIntegrator1DFactory
            .getIntegratorName(RealFunctionIntegrator1DFactory.getIntegrator(RealFunctionIntegrator1DFactory.SIMPSON)));
  }
}
