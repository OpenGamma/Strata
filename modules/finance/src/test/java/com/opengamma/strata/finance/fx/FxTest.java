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

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;

/**
 * Test {@link Fx}.
 */
@Test
public class FxTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final CurrencyAmount USD_P1600 = CurrencyAmount.of(USD, 1_600);
  private static final CurrencyAmount USD_M1600 = CurrencyAmount.of(USD, -1_600);
  private static final CurrencyAmount EUR_P1600 = CurrencyAmount.of(EUR, 1_800);
  private static final LocalDate DATE_2015_06_29 = date(2015, 6, 29);
  private static final LocalDate DATE_2015_06_30 = date(2015, 6, 30);

  //-------------------------------------------------------------------------
  public void test_of_rightOrder() {
    Fx test = Fx.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_switchOrder() {
    Fx test = Fx.of(USD_M1600, GBP_P1000, DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_bothZero() {
    Fx test = Fx.of(CurrencyAmount.zero(GBP), CurrencyAmount.zero(USD), DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), CurrencyAmount.zero(GBP));
    assertEquals(test.getCounterCurrencyAmount(), CurrencyAmount.zero(USD));
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), CurrencyAmount.zero(USD));
  }

  public void test_of_positiveNegative() {
    assertThrowsIllegalArg(() -> Fx.of(GBP_P1000, USD_P1600, DATE_2015_06_30));
    assertThrowsIllegalArg(() -> Fx.of(GBP_M1000, USD_M1600, DATE_2015_06_30));
    assertThrowsIllegalArg(() -> Fx.of(CurrencyAmount.zero(GBP), USD_M1600, DATE_2015_06_30));
    assertThrowsIllegalArg(() -> Fx.of(CurrencyAmount.zero(GBP), USD_P1600, DATE_2015_06_30));
  }

  public void test_of_sameCurrency() {
    assertThrowsIllegalArg(() -> Fx.of(GBP_P1000, GBP_M1000, DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  public void test_of_rate_rightOrder() {
    Fx test = Fx.of(GBP_P1000, FxRate.of(GBP, USD, 1.6d), DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_rate_switchOrder() {
    Fx test = Fx.of(USD_M1600, FxRate.of(USD, GBP, 1d / 1.6d), DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_of_rate_bothZero() {
    Fx test = Fx.of(CurrencyAmount.zero(GBP), FxRate.of(USD, GBP, 1.6d), DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), CurrencyAmount.zero(GBP));
    assertEquals(test.getCounterCurrencyAmount().getAmount(), CurrencyAmount.zero(USD).getAmount(), 1e-12);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), CurrencyAmount.of(USD, -0d));
  }

  public void test_of_rate_wrongCurrency() {
    assertThrowsIllegalArg(() -> Fx.of(GBP_P1000, FxRate.of(USD, EUR, 1.45d), DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  public void test_builder_rightOrder() {
    Fx test = Fx.meta().builder()
        .set(Fx.meta().baseCurrencyAmount(), GBP_P1000)
        .set(Fx.meta().counterCurrencyAmount(), USD_M1600)
        .set(Fx.meta().paymentDate(), DATE_2015_06_30)
        .build();
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_builder_switchOrder() {
    Fx test = Fx.meta().builder()
        .set(Fx.meta().baseCurrencyAmount(), USD_M1600)
        .set(Fx.meta().counterCurrencyAmount(), GBP_P1000)
        .set(Fx.meta().paymentDate(), DATE_2015_06_30)
        .build();
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
    assertEquals(test.getCurrencyPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.getReceiveCurrencyAmount(), GBP_P1000);
  }

  public void test_builder_bothPositive() {
    assertThrowsIllegalArg(() -> Fx.meta().builder()
        .set(Fx.meta().baseCurrencyAmount(), GBP_P1000)
        .set(Fx.meta().counterCurrencyAmount(), USD_P1600)
        .set(Fx.meta().paymentDate(), DATE_2015_06_30)
        .build());
  }

  public void test_builder_bothNegative() {
    assertThrowsIllegalArg(() -> Fx.meta().builder()
        .set(Fx.meta().baseCurrencyAmount(), GBP_M1000)
        .set(Fx.meta().counterCurrencyAmount(), USD_M1600)
        .set(Fx.meta().paymentDate(), DATE_2015_06_30)
        .build());
  }

  public void test_builder_sameCurrency() {
    assertThrowsIllegalArg(() -> Fx.meta().builder()
        .set(Fx.meta().baseCurrencyAmount(), GBP_P1000)
        .set(Fx.meta().counterCurrencyAmount(), GBP_M1000)
        .set(Fx.meta().paymentDate(), DATE_2015_06_30)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    Fx fwd = Fx.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    ExpandedFx test = fwd.expand();
    assertEquals(test.getBaseCurrencyPayment(), FxPayment.of(GBP_P1000, DATE_2015_06_30));
    assertEquals(test.getCounterCurrencyPayment(), FxPayment.of(USD_M1600, DATE_2015_06_30));
    assertEquals(test.getPaymentDate(), DATE_2015_06_30);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Fx test = Fx.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    coverImmutableBean(test);
    Fx test2 = Fx.of(GBP_M1000, EUR_P1600, DATE_2015_06_29);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    Fx test = Fx.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    assertSerialization(test);
  }

}
