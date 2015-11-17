/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.FxIndices.EUR_USD_ECB;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test.
 */
@Test
public class FxResetNotionalExchangeTest {

  private static final LocalDate DATE_2014_03_28 = date(2014, 3, 28);
  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);

  public void test_builder() {
    FxResetNotionalExchange test = FxResetNotionalExchange.builder()
        .paymentDate(DATE_2014_06_30)
        .referenceCurrency(USD)
        .notional(1000d)
        .index(GBP_USD_WM)
        .fixingDate(DATE_2014_03_28)
        .build();
    assertEquals(test.getPaymentDate(), DATE_2014_06_30);
    assertEquals(test.getReferenceCurrency(), USD);
    assertEquals(test.getIndex(), GBP_USD_WM);
    assertEquals(test.getFixingDate(), DATE_2014_03_28);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getNotional(), 1000d);
    assertEquals(test.getNotionalAmount(), CurrencyAmount.of(USD, 1000d));
  }

  public void test_invalidCurrency() {
    assertThrowsIllegalArg(() -> FxResetNotionalExchange.builder()
        .paymentDate(DATE_2014_06_30)
        .referenceCurrency(GBP)
        .notional(1000d)
        .index(EUR_USD_ECB)
        .fixingDate(DATE_2014_03_28)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_adjustPaymentDate() {
    FxResetNotionalExchange test = FxResetNotionalExchange.builder()
        .paymentDate(DATE_2014_06_30)
        .referenceCurrency(USD)
        .notional(1000d)
        .index(GBP_USD_WM)
        .fixingDate(DATE_2014_03_28)
        .build();
    FxResetNotionalExchange expected = FxResetNotionalExchange.builder()
        .paymentDate(DATE_2014_06_30.plusDays(2))
        .referenceCurrency(USD)
        .notional(1000d)
        .index(GBP_USD_WM)
        .fixingDate(DATE_2014_03_28)
        .build();
    assertEquals(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(0))), test);
    assertEquals(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(2))), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxResetNotionalExchange test = FxResetNotionalExchange.builder()
        .paymentDate(DATE_2014_06_30)
        .referenceCurrency(USD)
        .notional(1000d)
        .index(GBP_USD_WM)
        .fixingDate(DATE_2014_03_28)
        .build();
    coverImmutableBean(test);
    FxResetNotionalExchange test2 = FxResetNotionalExchange.builder()
        .paymentDate(date(2014, 9, 30))
        .referenceCurrency(EUR)
        .notional(2000d)
        .index(EUR_USD_ECB)
        .fixingDate(DATE_2014_06_30)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FxResetNotionalExchange test = FxResetNotionalExchange.builder()
        .paymentDate(DATE_2014_06_30)
        .referenceCurrency(USD)
        .notional(1000d)
        .index(GBP_USD_WM)
        .fixingDate(DATE_2014_03_28)
        .build();
    assertSerialization(test);
  }

}
