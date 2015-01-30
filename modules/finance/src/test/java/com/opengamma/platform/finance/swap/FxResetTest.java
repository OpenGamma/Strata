/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import static com.opengamma.basics.currency.Currency.EUR;
import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.index.FxIndices.ECB_EUR_GBP;
import static com.opengamma.basics.index.FxIndices.ECB_EUR_USD;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
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
        .index(ECB_EUR_GBP)
        .referenceCurrency(GBP)
        .fixingDate(DATE_2014_06_30)
        .build();
    assertEquals(test.getIndex(), ECB_EUR_GBP);
    assertEquals(test.getReferenceCurrency(), GBP);
    assertEquals(test.getFixingDate(), DATE_2014_06_30);
  }

  public void test_of() {
    FxReset test = FxReset.of(ECB_EUR_GBP, GBP, DATE_2014_06_30);
    assertEquals(test.getIndex(), ECB_EUR_GBP);
    assertEquals(test.getReferenceCurrency(), GBP);
    assertEquals(test.getFixingDate(), DATE_2014_06_30);
  }

  public void test_invalidCurrency() {
    assertThrowsIllegalArg(() -> FxReset.builder()
        .index(ECB_EUR_USD)
        .referenceCurrency(GBP)
        .fixingDate(DATE_2014_06_30)
        .build());
    assertThrowsIllegalArg(() -> FxReset.of(ECB_EUR_USD, GBP, DATE_2014_06_30));
  }

  public void test_of_null() {
    assertThrowsIllegalArg(() -> FxReset.of(null, GBP, DATE_2014_06_30));
    assertThrowsIllegalArg(() -> FxReset.of(ECB_EUR_GBP, null, DATE_2014_06_30));
    assertThrowsIllegalArg(() -> FxReset.of(ECB_EUR_GBP, GBP, null));
    assertThrowsIllegalArg(() -> FxReset.of(null, null, null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxReset test = FxReset.of(ECB_EUR_GBP, GBP, DATE_2014_06_30);
    coverImmutableBean(test);
    FxReset test2 = FxReset.of(ECB_EUR_USD, USD, date(2014, 1, 15));
    coverBeanEquals(test, test2);
    FxReset test3 = FxReset.of(ECB_EUR_USD, EUR, date(2014, 1, 15));
    coverBeanEquals(test2, test3);
  }

  public void test_serialization() {
    FxReset test = FxReset.of(ECB_EUR_GBP, GBP, DATE_2014_06_30);
    assertSerialization(test);
  }

}
