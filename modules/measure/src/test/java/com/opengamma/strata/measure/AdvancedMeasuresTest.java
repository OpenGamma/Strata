/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link AdvancedMeasures}.
 */
@Test
public class AdvancedMeasuresTest {

  public void test_standard() {
    assertEquals(AdvancedMeasures.PV01_SEMI_PARALLEL_GAMMA_BUCKETED.isCurrencyConvertible(), true);
  }

  public void coverage() {
    coverPrivateConstructor(Measures.class);
  }

}
