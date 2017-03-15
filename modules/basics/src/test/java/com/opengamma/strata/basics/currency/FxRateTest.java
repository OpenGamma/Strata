/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link FxRate}.
 */
@Test
public class FxRateTest {

  private static final Currency AUD = Currency.AUD;
  private static final Currency CAD = Currency.CAD;
  private static final Currency EUR = Currency.EUR;
  private static final Currency GBP = Currency.GBP;
  private static final Currency USD = Currency.USD;

  //-------------------------------------------------------------------------
  public void test_of_CurrencyCurrencyDouble() {
    FxRate test = FxRate.of(GBP, USD, 1.5d);
    assertEquals(test.getPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.fxRate(GBP, USD), 1.5d, 0);
    assertEquals(test.toString(), "GBP/USD 1.5");
  }

  public void test_of_CurrencyCurrencyDouble_reverseStandardOrder() {
    FxRate test = FxRate.of(USD, GBP, 0.8d);
    assertEquals(test.getPair(), CurrencyPair.of(USD, GBP));
    assertEquals(test.fxRate(USD, GBP), 0.8d, 0);
    assertEquals(test.toString(), "USD/GBP 0.8");
  }

  public void test_of_CurrencyCurrencyDouble_same() {
    FxRate test = FxRate.of(USD, USD, 1d);
    assertEquals(test.getPair(), CurrencyPair.of(USD, USD));
    assertEquals(test.fxRate(USD, USD), 1d, 0);
    assertEquals(test.toString(), "USD/USD 1");
  }

  public void test_of_CurrencyCurrencyDouble_invalid() {
    assertThrowsIllegalArg(() -> FxRate.of(GBP, USD, -1.5d));
    assertThrowsIllegalArg(() -> FxRate.of(GBP, GBP, 2d));
  }

  public void test_of_CurrencyCurrencyDouble_null() {
    assertThrowsIllegalArg(() -> FxRate.of(null, USD, 1.5d));
    assertThrowsIllegalArg(() -> FxRate.of(USD, null, 1.5d));
    assertThrowsIllegalArg(() -> FxRate.of(null, null, 1.5d));
  }

  //-------------------------------------------------------------------------
  public void test_of_CurrencyPairDouble() {
    FxRate test = FxRate.of(CurrencyPair.of(GBP, USD), 1.5d);
    assertEquals(test.getPair(), CurrencyPair.of(GBP, USD));
    assertEquals(test.fxRate(GBP, USD), 1.5d, 0);
    assertEquals(test.toString(), "GBP/USD 1.5");
  }

  public void test_of_CurrencyPairDouble_reverseStandardOrder() {
    FxRate test = FxRate.of(CurrencyPair.of(USD, GBP), 0.8d);
    assertEquals(test.getPair(), CurrencyPair.of(USD, GBP));
    assertEquals(test.fxRate(USD, GBP), 0.8d, 0);
    assertEquals(test.toString(), "USD/GBP 0.8");
  }

  public void test_of_CurrencyPairDouble_same() {
    FxRate test = FxRate.of(CurrencyPair.of(USD, USD), 1d);
    assertEquals(test.getPair(), CurrencyPair.of(USD, USD));
    assertEquals(test.fxRate(USD, USD), 1d, 0);
    assertEquals(test.toString(), "USD/USD 1");
  }

  public void test_of_CurrencyPairDouble_invalid() {
    assertThrowsIllegalArg(() -> FxRate.of(CurrencyPair.of(GBP, USD), -1.5d));
    assertThrowsIllegalArg(() -> FxRate.of(CurrencyPair.of(USD, USD), 2d));
  }

  public void test_of_CurrencyPairDouble_null() {
    assertThrowsIllegalArg(() -> FxRate.of(null, 1.5d));
  }

  public void test_toConventional() {
    assertEquals(FxRate.of(GBP, USD, 1.25), FxRate.of(USD, GBP, 0.8).toConventional());
    assertEquals(FxRate.of(GBP, USD, 1.25), FxRate.of(GBP, USD, 1.25).toConventional());
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "parseGood")
  Object[][] data_parseGood() {
    return new Object[][] {
        {"USD/EUR 205.123", USD, EUR, 205.123d},
        {"USD/EUR 3.00000000", USD, EUR, 3d},
        {"USD/EUR 2", USD, EUR, 2d},
        {"USD/EUR 0.1", USD, EUR, 0.1d},
        {"EUR/USD 0.001", EUR, USD, 0.001d},
        {"EUR/EUR 1", EUR, EUR, 1d},
        {"cAd/GbP 1.25", CAD, GBP, 1.25d},
    };
  }

  @Test(dataProvider = "parseGood")
  public void test_parse_String_good(String input, Currency base, Currency counter, double rate) {
    assertEquals(FxRate.parse(input), FxRate.of(base, counter, rate));
  }

  @DataProvider(name = "parseBad")
  Object[][] data_parseBad() {
    return new Object[][] {
        {"AUD 1.25"},
        {"AUD/GB 1.25"},
        {"AUD GBP 1.25"},
        {"AUD:GBP 1.25"},
        {"123/456"},
        {"EUR/GBP -1.25"},
        {"EUR/GBP 0"},
        {"EUR/EUR 1.25"},
        {""},
        {null},
    };
  }

  @Test(dataProvider = "parseBad", expectedExceptions = IllegalArgumentException.class)
  public void test_parse_String_bad(String input) {
    FxRate.parse(input);
  }

  //-------------------------------------------------------------------------
  public void test_inverse() {
    FxRate test = FxRate.of(GBP, USD, 1.25d);
    assertEquals(test.inverse(), FxRate.of(USD, GBP, 0.8d));
  }

  public void test_inverse_same() {
    FxRate test = FxRate.of(GBP, GBP, 1d);
    assertEquals(test.inverse(), FxRate.of(GBP, GBP, 1d));
  }

  //-------------------------------------------------------------------------
  public void test_fxRate_forBase() {
    FxRate test = FxRate.of(GBP, USD, 1.25d);
    assertEquals(test.fxRate(GBP, USD), 1.25d);
    assertEquals(test.fxRate(USD, GBP), 1d / 1.25d);
    assertThrowsIllegalArg(() -> test.fxRate(GBP, AUD));
  }

  public void test_fxRate_forPair() {
    FxRate test = FxRate.of(GBP, USD, 1.25d);
    assertEquals(test.fxRate(GBP, USD), 1.25d);
    assertEquals(test.fxRate(USD, GBP), 1d / 1.25d);
    assertEquals(test.fxRate(GBP, GBP), 1d);
    assertEquals(test.fxRate(USD, USD), 1d);
    assertEquals(test.fxRate(AUD, AUD), 1d);
    assertThrowsIllegalArg(() -> test.fxRate(AUD, GBP));
    assertThrowsIllegalArg(() -> test.fxRate(GBP, AUD));
    assertThrowsIllegalArg(() -> test.fxRate(AUD, USD));
    assertThrowsIllegalArg(() -> test.fxRate(USD, AUD));
    assertThrowsIllegalArg(() -> test.fxRate(EUR, AUD));
  }

  //-------------------------------------------------------------------------
  public void test_convert() {
    FxRate test = FxRate.of(GBP, USD, 1.25d);
    assertEquals(test.convert(100, GBP, USD), 125d);
    assertEquals(test.convert(100, USD, GBP), 100d / 1.25d);
    assertThrowsIllegalArg(() -> test.convert(100, GBP, AUD));
  }

  //-------------------------------------------------------------------------
  public void test_crossRate() {
    FxRate gbpUsd = FxRate.of(GBP, USD, 5d / 4d);
    FxRate usdGbp = FxRate.of(USD, GBP, 4d / 5d);
    FxRate eurUsd = FxRate.of(EUR, USD, 8d / 7d);
    FxRate usdEur = FxRate.of(USD, EUR, 7d / 8d);
    FxRate eurGbp = FxRate.of(EUR, GBP, (8d / 7d) * (4d / 5d));
    FxRate gbpGbp = FxRate.of(GBP, GBP, 1d);
    FxRate usdUsd = FxRate.of(USD, USD, 1d);

    assertEquals(eurUsd.crossRate(usdGbp), eurGbp);
    assertEquals(eurUsd.crossRate(gbpUsd), eurGbp);
    assertEquals(usdEur.crossRate(usdGbp), eurGbp);
    assertEquals(usdEur.crossRate(gbpUsd), eurGbp);

    assertEquals(gbpUsd.crossRate(usdEur), eurGbp);
    assertEquals(gbpUsd.crossRate(eurUsd), eurGbp);
    assertEquals(usdGbp.crossRate(usdEur), eurGbp);
    assertEquals(usdGbp.crossRate(eurUsd), eurGbp);

    assertThrowsIllegalArg(() -> gbpGbp.crossRate(gbpUsd));  // identity
    assertThrowsIllegalArg(() -> usdUsd.crossRate(gbpUsd));  // identity
    assertThrowsIllegalArg(() -> gbpUsd.crossRate(gbpUsd));  // same currencies
    assertThrowsIllegalArg(() -> gbpUsd.crossRate(usdGbp));  // same currencies
    assertThrowsIllegalArg(() -> gbpUsd.crossRate(FxRate.of(EUR, CAD, 12d / 5d)));  // no common currency
  }

  //-------------------------------------------------------------------------
  public void test_equals_hashCode() {
    FxRate a1 = FxRate.of(AUD, GBP, 1.25d);
    FxRate a2 = FxRate.of(AUD, GBP, 1.25d);
    FxRate b = FxRate.of(USD, GBP, 1.25d);
    FxRate c = FxRate.of(USD, GBP, 1.35d);

    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);

    assertEquals(b.equals(a1), false);
    assertEquals(b.equals(a2), false);
    assertEquals(b.equals(b), true);
    assertEquals(b.equals(c), false);

    assertEquals(c.equals(a1), false);
    assertEquals(c.equals(a2), false);
    assertEquals(c.equals(b), false);
    assertEquals(c.equals(c), true);

    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void test_equals_bad() {
    FxRate test = FxRate.of(AUD, GBP, 1.25d);
    assertFalse(test.equals(""));
    assertFalse(test.equals(null));
  }

  //-----------------------------------------------------------------------
  public void test_serialization() {
    assertSerialization(FxRate.of(GBP, USD, 1.25d));
    assertSerialization(FxRate.of(GBP, GBP, 1d));
  }

  //-----------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(FxRate.of(GBP, USD, 1.25d));
  }

}
