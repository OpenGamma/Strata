/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link NameCurrencySensitivityKey}.
 */
@Test
public class NameCurrencySensitivityKeyTest {

  private static final String NAME = "USD-LIBOR-3M";
  private static final CurveName CURVE_NAME = CurveName.of(NAME);

  public void test_of_String() {
    NameCurrencySensitivityKey test = NameCurrencySensitivityKey.of(NAME, EUR);
    assertEquals(test.getCurveName(), CURVE_NAME);
    assertEquals(test.getCurrency(), EUR);
  }

  public void test_of_CurveName() {
    NameCurrencySensitivityKey test = NameCurrencySensitivityKey.of(CURVE_NAME, EUR);
    assertEquals(test.getCurveName(), CURVE_NAME);
    assertEquals(test.getCurrency(), EUR);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    NameCurrencySensitivityKey test = NameCurrencySensitivityKey.of(NAME, EUR);
    coverImmutableBean(test);
    NameCurrencySensitivityKey test2 = NameCurrencySensitivityKey.of("Other", GBP);
    coverBeanEquals(test, test2);
  }

}
