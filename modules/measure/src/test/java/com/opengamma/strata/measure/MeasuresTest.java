/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link Measures}.
 */
@Test
public class MeasuresTest {

  public void test_standard() {
    assertEquals(Measures.PRESENT_VALUE.isCurrencyConvertible(), true);
    assertEquals(Measures.EXPLAIN_PRESENT_VALUE.isCurrencyConvertible(), false);
    assertEquals(Measures.PV01_CALIBRATED_SUM.isCurrencyConvertible(), true);
    assertEquals(Measures.PV01_CALIBRATED_BUCKETED.isCurrencyConvertible(), true);
    assertEquals(Measures.PV01_MARKET_QUOTE_SUM.isCurrencyConvertible(), true);
    assertEquals(Measures.PV01_MARKET_QUOTE_BUCKETED.isCurrencyConvertible(), true);
    assertEquals(Measures.PAR_RATE.isCurrencyConvertible(), false);
    assertEquals(Measures.PAR_SPREAD.isCurrencyConvertible(), false);
    assertEquals(Measures.CURRENCY_EXPOSURE.isCurrencyConvertible(), false);
    assertEquals(Measures.CURRENT_CASH.isCurrencyConvertible(), true);
  }

  public void coverage() {
    coverPrivateConstructor(Measures.class);
  }

}
