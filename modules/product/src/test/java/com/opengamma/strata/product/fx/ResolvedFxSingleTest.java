/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.Payment;

/**
 * Test {@link ResolvedFxSingle}.
 */
@Test
public class ResolvedFxSingleTest {

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
    ResolvedFxSingle test = ResolvedFxSingle.of(PAYMENT_GBP_P1000, PAYMENT_USD_M1600);
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_payments_switchOrder() {
    ResolvedFxSingle test = ResolvedFxSingle.of(PAYMENT_USD_M1600, PAYMENT_GBP_P1000);
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_payments_sameCurrency() {
    assertThrowsIllegalArg(() -> ResolvedFxSingle.of(PAYMENT_GBP_P1000, PAYMENT_GBP_M1000));
  }

  //-------------------------------------------------------------------------
  public void test_of_amounts_rightOrder() {
    ResolvedFxSingle test = sut();
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_amounts_switchOrder() {
    ResolvedFxSingle test = ResolvedFxSingle.of(USD_M1600, GBP_P1000, DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_amounts_bothZero() {
    ResolvedFxSingle test = ResolvedFxSingle.of(CurrencyAmount.zero(GBP), CurrencyAmount.zero(USD), DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyPayment(), Payment.of(CurrencyAmount.zero(GBP), DATE_2015_06_30));
    assertEquals(test.getCounterCurrencyPayment(), Payment.of(CurrencyAmount.zero(USD), DATE_2015_06_30));
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), CurrencyAmount.zero(USD));
  }

  public void test_of_amounts_positiveNegative() {
    assertThrowsIllegalArg(() -> ResolvedFxSingle.of(GBP_P1000, USD_P1600, DATE_2015_06_30));
    assertThrowsIllegalArg(() -> ResolvedFxSingle.of(GBP_M1000, USD_M1600, DATE_2015_06_30));
    assertThrowsIllegalArg(() -> ResolvedFxSingle.of(CurrencyAmount.zero(GBP), USD_M1600, DATE_2015_06_30));
    assertThrowsIllegalArg(() -> ResolvedFxSingle.of(CurrencyAmount.zero(GBP), USD_P1600, DATE_2015_06_30));
  }

  public void test_of_sameCurrency() {
    assertThrowsIllegalArg(() -> ResolvedFxSingle.of(GBP_P1000, GBP_M1000, DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  public void test_of_rate_rightOrder() {
    ResolvedFxSingle test = ResolvedFxSingle.of(GBP_P1000, FxRate.of(GBP, USD, 1.6d), DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyPayment(), Payment.of(GBP_P1000, DATE_2015_06_30));
    assertEquals(test.getCounterCurrencyPayment(), Payment.of(USD_M1600, DATE_2015_06_30));
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_rate_switchOrder() {
    ResolvedFxSingle test = ResolvedFxSingle.of(USD_M1600, FxRate.of(USD, GBP, 1d / 1.6d), DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyPayment(), Payment.of(GBP_P1000, DATE_2015_06_30));
    assertEquals(test.getCounterCurrencyPayment(), Payment.of(USD_M1600, DATE_2015_06_30));
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_rate_bothZero() {
    ResolvedFxSingle test = ResolvedFxSingle.of(CurrencyAmount.zero(GBP), FxRate.of(USD, GBP, 1.6d), DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyPayment().getValue(), CurrencyAmount.zero(GBP));
    assertEquals(test.getCounterCurrencyPayment().getValue().getAmount(), CurrencyAmount.zero(USD).getAmount(), 1e-12);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), CurrencyAmount.of(USD, 0d));
  }

  public void test_of_rate_wrongCurrency() {
    assertThrowsIllegalArg(() -> FxSingle.of(GBP_P1000, FxRate.of(USD, EUR, 1.45d), DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  public void test_builder_rightOrder() {
    ResolvedFxSingle test = ResolvedFxSingle.meta().builder()
        .set(ResolvedFxSingle.meta().baseCurrencyPayment(), PAYMENT_GBP_P1000)
        .set(ResolvedFxSingle.meta().counterCurrencyPayment(), PAYMENT_USD_M1600)
        .build();
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
  }

  public void test_builder_switchOrder() {
    ResolvedFxSingle test = ResolvedFxSingle.meta().builder()
        .set(ResolvedFxSingle.meta().baseCurrencyPayment(), PAYMENT_USD_M1600)
        .set(ResolvedFxSingle.meta().counterCurrencyPayment(), PAYMENT_GBP_P1000)
        .build();
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
  }

  public void test_builder_bothPositive() {
    assertThrowsIllegalArg(() -> ResolvedFxSingle.meta().builder()
        .set(ResolvedFxSingle.meta().baseCurrencyPayment(), PAYMENT_GBP_P1000)
        .set(ResolvedFxSingle.meta().counterCurrencyPayment(), PAYMENT_USD_P1600)
        .build());
  }

  public void test_builder_bothNegative() {
    assertThrowsIllegalArg(() -> ResolvedFxSingle.meta().builder()
        .set(ResolvedFxSingle.meta().baseCurrencyPayment(), PAYMENT_GBP_M1000)
        .set(ResolvedFxSingle.meta().counterCurrencyPayment(), PAYMENT_USD_M1600)
        .build());
  }

  public void test_builder_sameCurrency() {
    assertThrowsIllegalArg(() -> ResolvedFxSingle.meta().builder()
        .set(ResolvedFxSingle.meta().baseCurrencyPayment(), PAYMENT_GBP_P1000)
        .set(ResolvedFxSingle.meta().counterCurrencyPayment(), PAYMENT_GBP_M1000)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_inverse() {
    ResolvedFxSingle test = sut();
    assertEquals(test.inverse(), ResolvedFxSingle.of(GBP_M1000, USD_P1600, DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
    coverBeanEquals(sut(), sut3());
    coverBeanEquals(sut2(), sut3());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedFxSingle sut() {
    return ResolvedFxSingle.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
  }

  static ResolvedFxSingle sut2() {
    return ResolvedFxSingle.of(GBP_M1000, EUR_P1600, DATE_2015_06_29);
  }

  static ResolvedFxSingle sut3() {
    return ResolvedFxSingle.of(USD_M1600, EUR_P1600, DATE_2015_06_30);
  }

}
