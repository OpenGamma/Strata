/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.swap;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.FxIndices.EUR_GBP_ECB;
import static com.opengamma.strata.basics.index.FxIndices.EUR_USD_ECB;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class FxResetTest {

  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);

  public void test_builder() {
    FxReset test = FxReset.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .fixingDate(DATE_2014_06_30)
        .build();
    assertEquals(test.getIndex(), EUR_GBP_ECB);
    assertEquals(test.getReferenceCurrency(), GBP);
    assertEquals(test.getFixingDate(), DATE_2014_06_30);
  }

  public void test_of() {
    FxReset test = FxReset.of(EUR_GBP_ECB, GBP, DATE_2014_06_30);
    assertEquals(test.getIndex(), EUR_GBP_ECB);
    assertEquals(test.getReferenceCurrency(), GBP);
    assertEquals(test.getFixingDate(), DATE_2014_06_30);
  }

  public void test_invalidCurrency() {
    assertThrowsIllegalArg(() -> FxReset.builder()
        .index(EUR_USD_ECB)
        .referenceCurrency(GBP)
        .fixingDate(DATE_2014_06_30)
        .build());
    assertThrowsIllegalArg(() -> FxReset.of(EUR_USD_ECB, GBP, DATE_2014_06_30));
  }

  public void test_of_null() {
    assertThrowsIllegalArg(() -> FxReset.of(null, GBP, DATE_2014_06_30));
    assertThrowsIllegalArg(() -> FxReset.of(EUR_GBP_ECB, null, DATE_2014_06_30));
    assertThrowsIllegalArg(() -> FxReset.of(EUR_GBP_ECB, GBP, null));
    assertThrowsIllegalArg(() -> FxReset.of(null, null, null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxReset test = FxReset.of(EUR_GBP_ECB, GBP, DATE_2014_06_30);
    coverImmutableBean(test);
    FxReset test2 = FxReset.of(EUR_USD_ECB, USD, date(2014, 1, 15));
    coverBeanEquals(test, test2);
    FxReset test3 = FxReset.of(EUR_USD_ECB, EUR, date(2014, 1, 15));
    coverBeanEquals(test2, test3);
  }

  public void test_serialization() {
    FxReset test = FxReset.of(EUR_GBP_ECB, GBP, DATE_2014_06_30);
    assertSerialization(test);
  }

}
