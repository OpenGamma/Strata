/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.Payment;

/**
 * Test {@link ExpandedFxSingle}.
 */
@Test
public class ExpandedFxTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final CurrencyAmount USD_P1600 = CurrencyAmount.of(USD, 1_600);
  private static final CurrencyAmount USD_M1600 = CurrencyAmount.of(USD, -1_600);
  private static final CurrencyAmount EUR_P1600 = CurrencyAmount.of(EUR, 1_800);
  private static final LocalDate DATE_2015_06_29 = date(2015, 6, 29);
  private static final LocalDate DATE_2015_06_30 = date(2015, 6, 30);
  private static final Payment PAYMENT_GBP_P1000 = Payment.of(GBP_P1000, DATE_2015_06_30);
  private static final Payment PAYMENT_GBP_M1000 = Payment.of(GBP_M1000, DATE_2015_06_30);
  private static final Payment PAYMENT_USD_P1600 = Payment.of(USD_P1600, DATE_2015_06_30);
  private static final Payment PAYMENT_USD_M1600 = Payment.of(USD_M1600, DATE_2015_06_30);

  //-------------------------------------------------------------------------
  public void test_of_payments_rightOrder() {
    ExpandedFxSingle test = ExpandedFxSingle.of(PAYMENT_GBP_P1000, PAYMENT_USD_M1600);
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_payments_switchOrder() {
    ExpandedFxSingle test = ExpandedFxSingle.of(PAYMENT_USD_M1600, PAYMENT_GBP_P1000);
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_payments_sameCurrency() {
    assertThrowsIllegalArg(() -> ExpandedFxSingle.of(PAYMENT_GBP_P1000, PAYMENT_GBP_M1000));
  }

  //-------------------------------------------------------------------------
  public void test_of_amounts_rightOrder() {
    ExpandedFxSingle test = ExpandedFxSingle.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_amounts_switchOrder() {
    ExpandedFxSingle test = ExpandedFxSingle.of(USD_M1600, GBP_P1000, DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_amounts_bothZero() {
    ExpandedFxSingle test = ExpandedFxSingle.of(CurrencyAmount.zero(GBP), CurrencyAmount.zero(USD), DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyPayment(), Payment.of(CurrencyAmount.zero(GBP), DATE_2015_06_30));
    assertEquals(test.getCounterCurrencyPayment(), Payment.of(CurrencyAmount.zero(USD), DATE_2015_06_30));
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), CurrencyAmount.zero(USD));
  }

  public void test_of_amounts_positiveNegative() {
    assertThrowsIllegalArg(() -> ExpandedFxSingle.of(GBP_P1000, USD_P1600, DATE_2015_06_30));
    assertThrowsIllegalArg(() -> ExpandedFxSingle.of(GBP_M1000, USD_M1600, DATE_2015_06_30));
    assertThrowsIllegalArg(() -> ExpandedFxSingle.of(CurrencyAmount.zero(GBP), USD_M1600, DATE_2015_06_30));
    assertThrowsIllegalArg(() -> ExpandedFxSingle.of(CurrencyAmount.zero(GBP), USD_P1600, DATE_2015_06_30));
  }

  public void test_of_sameCurrency() {
    assertThrowsIllegalArg(() -> ExpandedFxSingle.of(GBP_P1000, GBP_M1000, DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  public void test_builder_rightOrder() {
    ExpandedFxSingle test = ExpandedFxSingle.meta().builder()
        .set(ExpandedFxSingle.meta().baseCurrencyPayment(), PAYMENT_GBP_P1000)
        .set(ExpandedFxSingle.meta().counterCurrencyPayment(), PAYMENT_USD_M1600)
        .build();
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
  }

  public void test_builder_switchOrder() {
    ExpandedFxSingle test = ExpandedFxSingle.meta().builder()
        .set(ExpandedFxSingle.meta().baseCurrencyPayment(), PAYMENT_USD_M1600)
        .set(ExpandedFxSingle.meta().counterCurrencyPayment(), PAYMENT_GBP_P1000)
        .build();
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
  }

  public void test_builder_bothPositive() {
    assertThrowsIllegalArg(() -> ExpandedFxSingle.meta().builder()
        .set(ExpandedFxSingle.meta().baseCurrencyPayment(), PAYMENT_GBP_P1000)
        .set(ExpandedFxSingle.meta().counterCurrencyPayment(), PAYMENT_USD_P1600)
        .build());
  }

  public void test_builder_bothNegative() {
    assertThrowsIllegalArg(() -> ExpandedFxSingle.meta().builder()
        .set(ExpandedFxSingle.meta().baseCurrencyPayment(), PAYMENT_GBP_M1000)
        .set(ExpandedFxSingle.meta().counterCurrencyPayment(), PAYMENT_USD_M1600)
        .build());
  }

  public void test_builder_sameCurrency() {
    assertThrowsIllegalArg(() -> ExpandedFxSingle.meta().builder()
        .set(ExpandedFxSingle.meta().baseCurrencyPayment(), PAYMENT_GBP_P1000)
        .set(ExpandedFxSingle.meta().counterCurrencyPayment(), PAYMENT_GBP_M1000)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_inverse() {
    ExpandedFxSingle test = ExpandedFxSingle.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    assertEquals(test.inverse(), ExpandedFxSingle.of(GBP_M1000, USD_P1600, DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    ExpandedFxSingle test = ExpandedFxSingle.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    assertSame(test.expand(), test);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExpandedFxSingle test = ExpandedFxSingle.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    coverImmutableBean(test);
    ExpandedFxSingle test2 = ExpandedFxSingle.of(GBP_M1000, EUR_P1600, DATE_2015_06_29);
    coverBeanEquals(test, test2);
    ExpandedFxSingle test3 = ExpandedFxSingle.of(USD_M1600, EUR_P1600, DATE_2015_06_30);
    coverBeanEquals(test, test3);
    coverBeanEquals(test2, test3);
  }

  public void test_serialization() {
    ExpandedFxSingle test = ExpandedFxSingle.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    assertSerialization(test);
  }

}
