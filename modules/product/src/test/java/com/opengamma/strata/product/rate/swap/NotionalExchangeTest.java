/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
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
public class NotionalExchangeTest {

  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);
  private static final CurrencyAmount GBP_1000 = CurrencyAmount.of(GBP, 1000d);

  public void test_builder() {
    NotionalExchange test = NotionalExchange.builder()
        .paymentDate(DATE_2014_06_30)
        .paymentAmount(GBP_1000)
        .build();
    assertEquals(test.getPaymentDate(), DATE_2014_06_30);
    assertEquals(test.getPaymentAmount(), GBP_1000);
    assertEquals(test.getCurrency(), GBP);
  }

  public void test_of() {
    NotionalExchange test = NotionalExchange.of(DATE_2014_06_30, GBP_1000);
    assertEquals(test.getPaymentDate(), DATE_2014_06_30);
    assertEquals(test.getPaymentAmount(), GBP_1000);
    assertEquals(test.getCurrency(), GBP);
  }

  public void test_of_null() {
    assertThrowsIllegalArg(() -> NotionalExchange.of(null, GBP_1000));
    assertThrowsIllegalArg(() -> NotionalExchange.of(DATE_2014_06_30, null));
    assertThrowsIllegalArg(() -> NotionalExchange.of(null, null));
  }

  //-------------------------------------------------------------------------
  public void test_adjustPaymentDate() {
    NotionalExchange test = NotionalExchange.of(DATE_2014_06_30, GBP_1000);
    NotionalExchange expected = NotionalExchange.of(DATE_2014_06_30.plusDays(2), GBP_1000);
    assertEquals(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(0))), test);
    assertEquals(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(2))), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    NotionalExchange test = NotionalExchange.of(DATE_2014_06_30, GBP_1000);
    coverImmutableBean(test);
    NotionalExchange test2 = NotionalExchange.of(date(2014, 1, 15), CurrencyAmount.of(GBP, 200d));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    NotionalExchange test = NotionalExchange.of(DATE_2014_06_30, GBP_1000);
    assertSerialization(test);
  }

}
