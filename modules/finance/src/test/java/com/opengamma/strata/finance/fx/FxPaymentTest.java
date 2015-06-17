/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link FxPayment}.
 */
@Test
public class FxPaymentTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final CurrencyAmount EUR_P1600 = CurrencyAmount.of(EUR, 1_800);
  private static final LocalDate DATE_2015_06_29 = date(2015, 6, 29);
  private static final LocalDate DATE_2015_06_30 = date(2015, 6, 30);

  //-------------------------------------------------------------------------
  public void test_of() {
    FxPayment test = FxPayment.of(GBP_P1000, DATE_2015_06_30);
    assertEquals(test.getValue(), GBP_P1000);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getAmount(), 1_000, 0d);
    assertEquals(test.getPayReceive(), PayReceive.RECEIVE);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
  }

  public void test_ofPay() {
    FxPayment test = FxPayment.ofPay(GBP_P1000, DATE_2015_06_30);
    assertEquals(test.getValue(), GBP_M1000);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getAmount(), -1_000, 0d);
    assertEquals(test.getPayReceive(), PayReceive.PAY);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
  }

  public void test_ofReceive() {
    FxPayment test = FxPayment.ofReceive(GBP_P1000, DATE_2015_06_30);
    assertEquals(test.getValue(), GBP_P1000);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getAmount(), 1_000, 0d);
    assertEquals(test.getPayReceive(), PayReceive.RECEIVE);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
  }

  public void test_builder() {
    FxPayment test = FxPayment.builder()
        .value(GBP_P1000)
        .paymentDate(DATE_2015_06_30)
        .build();
    assertEquals(test.getValue(), GBP_P1000);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getAmount(), 1_000, 0d);
    assertEquals(test.getPayReceive(), PayReceive.RECEIVE);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxPayment test = FxPayment.of(GBP_P1000, DATE_2015_06_30);
    coverImmutableBean(test);
    FxPayment test2 = FxPayment.of(EUR_P1600, DATE_2015_06_29);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FxPayment test = FxPayment.of(GBP_P1000, DATE_2015_06_30);
    assertSerialization(test);
  }

}
