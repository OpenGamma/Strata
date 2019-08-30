/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link AdvancedMeasures}.
 */
public class AdvancedMeasuresTest {

  @Test
  public void test_standard() {
    assertThat(AdvancedMeasures.PV01_SEMI_PARALLEL_GAMMA_BUCKETED.isCurrencyConvertible()).isTrue();
  }

  @Test
  public void coverage() {
    coverPrivateConstructor(Measures.class);
  }

}
