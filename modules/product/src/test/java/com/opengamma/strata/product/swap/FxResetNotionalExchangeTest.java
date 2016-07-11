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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.FxIndexObservation;

/**
 * Test.
 */
@Test
public class FxResetNotionalExchangeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_2014_03_28 = date(2014, 3, 28);
  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);

  public void test_of() {
    FxResetNotionalExchange test = FxResetNotionalExchange.of(
        CurrencyAmount.of(USD, 1000d), DATE_2014_06_30, FxIndexObservation.of(GBP_USD_WM, DATE_2014_03_28, REF_DATA));
    assertEquals(test.getPaymentDate(), DATE_2014_06_30);
    assertEquals(test.getReferenceCurrency(), USD);
    assertEquals(test.getNotionalAmount(), CurrencyAmount.of(USD, 1000d));
    assertEquals(test.getNotional(), 1000d, 0d);
  }

  public void test_invalidCurrency() {
    assertThrowsIllegalArg(() -> FxResetNotionalExchange.meta().builder()
        .set(FxResetNotionalExchange.meta().paymentDate(), DATE_2014_06_30)
        .set(FxResetNotionalExchange.meta().notionalAmount(), CurrencyAmount.of(GBP, 1000d))
        .set(FxResetNotionalExchange.meta().observation(), FxIndexObservation.of(EUR_USD_ECB, DATE_2014_03_28, REF_DATA))
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_adjustPaymentDate() {
    FxResetNotionalExchange test = FxResetNotionalExchange.of(
        CurrencyAmount.of(USD, 1000d), DATE_2014_06_30, FxIndexObservation.of(GBP_USD_WM, DATE_2014_03_28, REF_DATA));
    FxResetNotionalExchange expected = FxResetNotionalExchange.of(
        CurrencyAmount.of(USD, 1000d), DATE_2014_06_30.plusDays(2), FxIndexObservation.of(GBP_USD_WM, DATE_2014_03_28, REF_DATA));
    assertEquals(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(0))), test);
    assertEquals(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(2))), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxResetNotionalExchange test = FxResetNotionalExchange.of(
        CurrencyAmount.of(USD, 1000d), DATE_2014_03_28, FxIndexObservation.of(GBP_USD_WM, DATE_2014_03_28, REF_DATA));
    coverImmutableBean(test);
    FxResetNotionalExchange test2 = FxResetNotionalExchange.of(
        CurrencyAmount.of(EUR, 2000d), DATE_2014_06_30, FxIndexObservation.of(EUR_USD_ECB, DATE_2014_06_30, REF_DATA));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FxResetNotionalExchange test = FxResetNotionalExchange.of(
        CurrencyAmount.of(USD, 1000d), DATE_2014_06_30, FxIndexObservation.of(GBP_USD_WM, DATE_2014_03_28, REF_DATA));
    assertSerialization(test);
  }

}
