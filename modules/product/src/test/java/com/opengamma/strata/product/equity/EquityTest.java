/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.equity;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;

/**
 * Test.
 */
@Test
public class EquityTest {

  public void test_builder() {
    Equity test = Equity.builder()
        .currency(Currency.GBP)
        .build();
    assertEquals(test.getCurrency(), Currency.GBP);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Equity test = Equity.builder()
        .currency(Currency.GBP)
        .build();
    coverImmutableBean(test);
    Equity test2 = Equity.builder()
        .currency(Currency.USD)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    Equity test = Equity.builder()
        .currency(Currency.GBP)
        .build();
    assertSerialization(test);
  }

}
