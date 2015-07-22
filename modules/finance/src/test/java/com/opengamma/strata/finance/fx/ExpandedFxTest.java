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

/**
 * Test {@link ExpandedFx}.
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
  private static final FxPayment PAYMENT_GBP_P1000 = FxPayment.of(GBP_P1000, DATE_2015_06_30);
  private static final FxPayment PAYMENT_GBP_M1000 = FxPayment.of(GBP_M1000, DATE_2015_06_30);
  private static final FxPayment PAYMENT_USD_P1600 = FxPayment.of(USD_P1600, DATE_2015_06_30);
  private static final FxPayment PAYMENT_USD_M1600 = FxPayment.of(USD_M1600, DATE_2015_06_30);

  //-------------------------------------------------------------------------
  public void test_of_payments_rightOrder() {
    ExpandedFx test = ExpandedFx.of(PAYMENT_GBP_P1000, PAYMENT_USD_M1600);
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_payments_switchOrder() {
    ExpandedFx test = ExpandedFx.of(PAYMENT_USD_M1600, PAYMENT_GBP_P1000);
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_payments_sameCurrency() {
    assertThrowsIllegalArg(() -> ExpandedFx.of(PAYMENT_GBP_P1000, PAYMENT_GBP_M1000));
  }

  //-------------------------------------------------------------------------
  public void test_of_amounts_rightOrder() {
    ExpandedFx test = ExpandedFx.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_amounts_switchOrder() {
    ExpandedFx test = ExpandedFx.of(USD_M1600, GBP_P1000, DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_amounts_bothZero() {
    ExpandedFx test = ExpandedFx.of(CurrencyAmount.zero(GBP), CurrencyAmount.zero(USD), DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyPayment(), FxPayment.of(CurrencyAmount.zero(GBP), DATE_2015_06_30));
    assertEquals(test.getCounterCurrencyPayment(), FxPayment.of(CurrencyAmount.zero(USD), DATE_2015_06_30));
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), CurrencyAmount.zero(USD));
  }

  public void test_of_amounts_positiveNegative() {
    assertThrowsIllegalArg(() -> ExpandedFx.of(GBP_P1000, USD_P1600, DATE_2015_06_30));
    assertThrowsIllegalArg(() -> ExpandedFx.of(GBP_M1000, USD_M1600, DATE_2015_06_30));
    assertThrowsIllegalArg(() -> ExpandedFx.of(CurrencyAmount.zero(GBP), USD_M1600, DATE_2015_06_30));
    assertThrowsIllegalArg(() -> ExpandedFx.of(CurrencyAmount.zero(GBP), USD_P1600, DATE_2015_06_30));
  }

  public void test_of_sameCurrency() {
    assertThrowsIllegalArg(() -> ExpandedFx.of(GBP_P1000, GBP_M1000, DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  public void test_builder_rightOrder() {
    ExpandedFx test = ExpandedFx.meta().builder()
        .set(ExpandedFx.meta().baseCurrencyPayment(), PAYMENT_GBP_P1000)
        .set(ExpandedFx.meta().counterCurrencyPayment(), PAYMENT_USD_M1600)
        .build();
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
  }

  public void test_builder_switchOrder() {
    ExpandedFx test = ExpandedFx.meta().builder()
        .set(ExpandedFx.meta().baseCurrencyPayment(), PAYMENT_USD_M1600)
        .set(ExpandedFx.meta().counterCurrencyPayment(), PAYMENT_GBP_P1000)
        .build();
    assertEquals(test.getBaseCurrencyPayment(), PAYMENT_GBP_P1000);
    assertEquals(test.getCounterCurrencyPayment(), PAYMENT_USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
  }

  public void test_builder_bothPositive() {
    assertThrowsIllegalArg(() -> ExpandedFx.meta().builder()
        .set(ExpandedFx.meta().baseCurrencyPayment(), PAYMENT_GBP_P1000)
        .set(ExpandedFx.meta().counterCurrencyPayment(), PAYMENT_USD_P1600)
        .build());
  }

  public void test_builder_bothNegative() {
    assertThrowsIllegalArg(() -> ExpandedFx.meta().builder()
        .set(ExpandedFx.meta().baseCurrencyPayment(), PAYMENT_GBP_M1000)
        .set(ExpandedFx.meta().counterCurrencyPayment(), PAYMENT_USD_M1600)
        .build());
  }

  public void test_builder_sameCurrency() {
    assertThrowsIllegalArg(() -> ExpandedFx.meta().builder()
        .set(ExpandedFx.meta().baseCurrencyPayment(), PAYMENT_GBP_P1000)
        .set(ExpandedFx.meta().counterCurrencyPayment(), PAYMENT_GBP_M1000)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_inverse() {
    ExpandedFx test = ExpandedFx.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    assertEquals(test.inverse(), ExpandedFx.of(GBP_M1000, USD_P1600, DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    ExpandedFx test = ExpandedFx.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    assertSame(test.expand(), test);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExpandedFx test = ExpandedFx.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    coverImmutableBean(test);
    ExpandedFx test2 = ExpandedFx.of(GBP_M1000, EUR_P1600, DATE_2015_06_29);
    coverBeanEquals(test, test2);
    ExpandedFx test3 = ExpandedFx.of(USD_M1600, EUR_P1600, DATE_2015_06_30);
    coverBeanEquals(test, test3);
    coverBeanEquals(test2, test3);
  }

  public void test_serialization() {
    ExpandedFx test = ExpandedFx.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    assertSerialization(test);
  }

}
