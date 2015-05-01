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

/**
 * Test.
 */
@Test
public class FxForwardTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final CurrencyAmount USD_P1600 = CurrencyAmount.of(USD, 1_600);
  private static final CurrencyAmount USD_M1600 = CurrencyAmount.of(USD, -1_600);
  private static final CurrencyAmount EUR_P1600 = CurrencyAmount.of(EUR, 1_800);
  private static final LocalDate DATE_2015_06_29 = date(2015, 6, 29);
  private static final LocalDate DATE_2015_06_30 = date(2015, 6, 30);

  //-------------------------------------------------------------------------
  public void test_of_rightOrder() {
    FxForward test = FxForward.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getValueDate(), DATE_2015_06_30);
  }

  public void test_of_switchOrder() {
    FxForward test = FxForward.of(USD_M1600, GBP_P1000, DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getValueDate(), DATE_2015_06_30);
  }

  public void test_of_bothZero() {
    FxForward test = FxForward.of(CurrencyAmount.zero(GBP), CurrencyAmount.zero(USD), DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), CurrencyAmount.zero(GBP));
    assertEquals(test.getCounterCurrencyAmount(), CurrencyAmount.zero(USD));
    assertEquals(test.getValueDate(), DATE_2015_06_30);
  }

  public void test_of_positiveNegative() {
    assertThrowsIllegalArg(() -> FxForward.of(GBP_P1000, USD_P1600, DATE_2015_06_30));
    assertThrowsIllegalArg(() -> FxForward.of(GBP_M1000, USD_M1600, DATE_2015_06_30));
    assertThrowsIllegalArg(() -> FxForward.of(CurrencyAmount.zero(GBP), USD_M1600, DATE_2015_06_30));
    assertThrowsIllegalArg(() -> FxForward.of(CurrencyAmount.zero(GBP), USD_P1600, DATE_2015_06_30));
  }

  public void test_of_sameCurrency() {
    assertThrowsIllegalArg(() -> FxForward.of(GBP_P1000, GBP_M1000, DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  public void test_of_rate_rightOrder() {
    FxForward test = FxForward.of(GBP_P1000, USD, 1.6d, DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getValueDate(), DATE_2015_06_30);
  }

  public void test_of_rate_switchOrder() {
    FxForward test = FxForward.of(USD_M1600, GBP, 1d / 1.6d, DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getValueDate(), DATE_2015_06_30);
  }

  public void test_of_rate_bothZero() {
    FxForward test = FxForward.of(CurrencyAmount.zero(GBP), USD, 1.6d, DATE_2015_06_30);
    assertEquals(test.getBaseCurrencyAmount(), CurrencyAmount.zero(GBP));
    assertEquals(test.getCounterCurrencyAmount().getAmount(), CurrencyAmount.zero(USD).getAmount(), 1e-12);
    assertEquals(test.getValueDate(), DATE_2015_06_30);
  }

  public void test_of_rate_sameCurrency() {
    assertThrowsIllegalArg(() -> FxForward.of(GBP_P1000, GBP, 1d, DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  public void test_builder_rightOrder() {
    FxForward test = FxForward.meta().builder()
        .set(FxForward.meta().baseCurrencyAmount(), GBP_P1000)
        .set(FxForward.meta().counterCurrencyAmount(), USD_M1600)
        .set(FxForward.meta().valueDate(), DATE_2015_06_30)
        .build();
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getValueDate(), DATE_2015_06_30);
  }

  public void test_builder_switchOrder() {
    FxForward test = FxForward.meta().builder()
        .set(FxForward.meta().baseCurrencyAmount(), USD_M1600)
        .set(FxForward.meta().counterCurrencyAmount(), GBP_P1000)
        .set(FxForward.meta().valueDate(), DATE_2015_06_30)
        .build();
    assertEquals(test.getBaseCurrencyAmount(), GBP_P1000);
    assertEquals(test.getCounterCurrencyAmount(), USD_M1600);
    assertEquals(test.getValueDate(), DATE_2015_06_30);
  }

  public void test_builder_bothPositive() {
    assertThrowsIllegalArg(() -> FxForward.meta().builder()
        .set(FxForward.meta().baseCurrencyAmount(), GBP_P1000)
        .set(FxForward.meta().counterCurrencyAmount(), USD_P1600)
        .set(FxForward.meta().valueDate(), DATE_2015_06_30)
        .build());
  }

  public void test_builder_bothNegative() {
    assertThrowsIllegalArg(() -> FxForward.meta().builder()
        .set(FxForward.meta().baseCurrencyAmount(), GBP_M1000)
        .set(FxForward.meta().counterCurrencyAmount(), USD_M1600)
        .set(FxForward.meta().valueDate(), DATE_2015_06_30)
        .build());
  }

  public void test_builder_sameCurrency() {
    assertThrowsIllegalArg(() -> FxForward.meta().builder()
        .set(FxForward.meta().baseCurrencyAmount(), GBP_P1000)
        .set(FxForward.meta().counterCurrencyAmount(), GBP_M1000)
        .set(FxForward.meta().valueDate(), DATE_2015_06_30)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    FxForward fwd = FxForward.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    ExpandedFx test = fwd.expand();
    assertEquals(test.getBaseCurrencyPayment(), FxPayment.of(DATE_2015_06_30, GBP_P1000));
    assertEquals(test.getCounterCurrencyPayment(), FxPayment.of(DATE_2015_06_30, USD_M1600));
    assertEquals(test.getValueDate(), DATE_2015_06_30);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxForward test = FxForward.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    coverImmutableBean(test);
    FxForward test2 = FxForward.of(GBP_M1000, EUR_P1600, DATE_2015_06_29);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FxForward test = FxForward.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
    assertSerialization(test);
  }

}
