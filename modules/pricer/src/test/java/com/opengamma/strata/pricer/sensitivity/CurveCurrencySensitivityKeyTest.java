/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link CurveCurrencySensitivityKey}.
 */
@Test
public class CurveCurrencySensitivityKeyTest {

  private static final String NAME = "USD-LIBOR-3M";

  public void test_of() {
    CurveCurrencySensitivityKey test = CurveCurrencySensitivityKey.of(NAME, EUR);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getCurrency(), EUR);
  }

  public void coverage() {
    CurveCurrencySensitivityKey test = CurveCurrencySensitivityKey.of(NAME, EUR);
    coverImmutableBean(test);
    CurveCurrencySensitivityKey test2 = CurveCurrencySensitivityKey.of("Other", GBP);
    coverBeanEquals(test, test2);
  }

}
