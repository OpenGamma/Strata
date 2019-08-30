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
 * Test {@link Measures}.
 */
public class MeasuresTest {

  @Test
  public void test_standard() {
    assertThat(Measures.PRESENT_VALUE.isCurrencyConvertible()).isTrue();
    assertThat(Measures.EXPLAIN_PRESENT_VALUE.isCurrencyConvertible()).isFalse();
    assertThat(Measures.PV01_CALIBRATED_SUM.isCurrencyConvertible()).isTrue();
    assertThat(Measures.PV01_CALIBRATED_BUCKETED.isCurrencyConvertible()).isTrue();
    assertThat(Measures.PV01_MARKET_QUOTE_SUM.isCurrencyConvertible()).isTrue();
    assertThat(Measures.PV01_MARKET_QUOTE_BUCKETED.isCurrencyConvertible()).isTrue();
    assertThat(Measures.PAR_RATE.isCurrencyConvertible()).isFalse();
    assertThat(Measures.PAR_SPREAD.isCurrencyConvertible()).isFalse();
    assertThat(Measures.CURRENCY_EXPOSURE.isCurrencyConvertible()).isFalse();
    assertThat(Measures.CURRENT_CASH.isCurrencyConvertible()).isTrue();
  }

  @Test
  public void coverage() {
    coverPrivateConstructor(Measures.class);
  }

}
