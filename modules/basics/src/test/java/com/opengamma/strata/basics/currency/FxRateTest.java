/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link FxRate}.
 */
public class FxRateTest {

  private static final Currency AUD = Currency.AUD;
  private static final Currency CAD = Currency.CAD;
  private static final Currency EUR = Currency.EUR;
  private static final Currency GBP = Currency.GBP;
  private static final Currency USD = Currency.USD;
  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  @Test
  public void test_of_CurrencyCurrencyDouble() {
    FxRate test = FxRate.of(GBP, USD, 1.5d);
    assertThat(test.getPair()).isEqualTo(CurrencyPair.of(GBP, USD));
    assertThat(test.fxRate(GBP, USD)).isEqualTo(1.5d);
    assertThat(test.toString()).isEqualTo("GBP/USD 1.5");
  }

  @Test
  public void test_of_CurrencyCurrencyDouble_reverseStandardOrder() {
    FxRate test = FxRate.of(USD, GBP, 0.8d);
    assertThat(test.getPair()).isEqualTo(CurrencyPair.of(USD, GBP));
    assertThat(test.fxRate(USD, GBP)).isEqualTo(0.8d);
    assertThat(test.toString()).isEqualTo("USD/GBP 0.8");
  }

  @Test
  public void test_of_CurrencyCurrencyDouble_same() {
    FxRate test = FxRate.of(USD, USD, 1d);
    assertThat(test.getPair()).isEqualTo(CurrencyPair.of(USD, USD));
    assertThat(test.fxRate(USD, USD)).isEqualTo(1d);
    assertThat(test.toString()).isEqualTo("USD/USD 1");
  }

  @Test
  public void test_of_CurrencyCurrencyDouble_invalid() {
    assertThatIllegalArgumentException().isThrownBy(() -> FxRate.of(GBP, USD, -1.5d));
    assertThatIllegalArgumentException().isThrownBy(() -> FxRate.of(GBP, GBP, 2d));
  }

  @Test
  public void test_of_CurrencyCurrencyDouble_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> FxRate.of(null, USD, 1.5d));
    assertThatIllegalArgumentException().isThrownBy(() -> FxRate.of(USD, null, 1.5d));
    assertThatIllegalArgumentException().isThrownBy(() -> FxRate.of(null, null, 1.5d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_CurrencyPairDouble() {
    FxRate test = FxRate.of(CurrencyPair.of(GBP, USD), 1.5d);
    assertThat(test.getPair()).isEqualTo(CurrencyPair.of(GBP, USD));
    assertThat(test.fxRate(GBP, USD)).isEqualTo(1.5d);
    assertThat(test.toString()).isEqualTo("GBP/USD 1.5");
  }

  @Test
  public void test_of_CurrencyPairDouble_reverseStandardOrder() {
    FxRate test = FxRate.of(CurrencyPair.of(USD, GBP), 0.8d);
    assertThat(test.getPair()).isEqualTo(CurrencyPair.of(USD, GBP));
    assertThat(test.fxRate(USD, GBP)).isEqualTo(0.8d);
    assertThat(test.toString()).isEqualTo("USD/GBP 0.8");
  }

  @Test
  public void test_of_CurrencyPairDouble_same() {
    FxRate test = FxRate.of(CurrencyPair.of(USD, USD), 1d);
    assertThat(test.getPair()).isEqualTo(CurrencyPair.of(USD, USD));
    assertThat(test.fxRate(USD, USD)).isEqualTo(1d);
    assertThat(test.toString()).isEqualTo("USD/USD 1");
  }

  @Test
  public void test_of_CurrencyPairDouble_invalid() {
    assertThatIllegalArgumentException().isThrownBy(() -> FxRate.of(CurrencyPair.of(GBP, USD), -1.5d));
    assertThatIllegalArgumentException().isThrownBy(() -> FxRate.of(CurrencyPair.of(USD, USD), 2d));
  }

  @Test
  public void test_of_CurrencyPairDouble_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> FxRate.of(null, 1.5d));
  }

  @Test
  public void test_toConventional() {
    assertThat(FxRate.of(GBP, USD, 1.25)).isEqualTo(FxRate.of(USD, GBP, 0.8).toConventional());
    assertThat(FxRate.of(GBP, USD, 1.25)).isEqualTo(FxRate.of(GBP, USD, 1.25).toConventional());
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_parseGood() {
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

  @ParameterizedTest
  @MethodSource("data_parseGood")
  public void test_parse_String_good(String input, Currency base, Currency counter, double rate) {
    assertThat(FxRate.parse(input)).isEqualTo(FxRate.of(base, counter, rate));
  }

  public static Object[][] data_parseBad() {
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

  @ParameterizedTest
  @MethodSource("data_parseBad")
  public void test_parse_String_bad(String input) {
    assertThatIllegalArgumentException().isThrownBy(() -> FxRate.parse(input));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_inverse() {
    FxRate test = FxRate.of(GBP, USD, 1.25d);
    assertThat(test.inverse()).isEqualTo(FxRate.of(USD, GBP, 0.8d));
  }

  @Test
  public void test_inverse_same() {
    FxRate test = FxRate.of(GBP, GBP, 1d);
    assertThat(test.inverse()).isEqualTo(FxRate.of(GBP, GBP, 1d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_fxRate_forBase() {
    FxRate test = FxRate.of(GBP, USD, 1.25d);
    assertThat(test.fxRate(GBP, USD)).isEqualTo(1.25d);
    assertThat(test.fxRate(USD, GBP)).isEqualTo(1d / 1.25d);
    assertThatIllegalArgumentException().isThrownBy(() -> test.fxRate(GBP, AUD));
  }

  @Test
  public void test_fxRate_forPair() {
    FxRate test = FxRate.of(GBP, USD, 1.25d);
    assertThat(test.fxRate(GBP, USD)).isEqualTo(1.25d);
    assertThat(test.fxRate(USD, GBP)).isEqualTo(1d / 1.25d);
    assertThat(test.fxRate(GBP, GBP)).isEqualTo(1d);
    assertThat(test.fxRate(USD, USD)).isEqualTo(1d);
    assertThat(test.fxRate(AUD, AUD)).isEqualTo(1d);
    assertThatIllegalArgumentException().isThrownBy(() -> test.fxRate(AUD, GBP));
    assertThatIllegalArgumentException().isThrownBy(() -> test.fxRate(GBP, AUD));
    assertThatIllegalArgumentException().isThrownBy(() -> test.fxRate(AUD, USD));
    assertThatIllegalArgumentException().isThrownBy(() -> test.fxRate(USD, AUD));
    assertThatIllegalArgumentException().isThrownBy(() -> test.fxRate(EUR, AUD));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convert() {
    FxRate test = FxRate.of(GBP, USD, 1.25d);
    assertThat(test.convert(100, GBP, USD)).isEqualTo(125d);
    assertThat(test.convert(100, USD, GBP)).isEqualTo(100d / 1.25d);
    assertThatIllegalArgumentException().isThrownBy(() -> test.convert(100, GBP, AUD));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_crossRate() {
    FxRate gbpUsd = FxRate.of(GBP, USD, 5d / 4d);
    FxRate usdGbp = FxRate.of(USD, GBP, 4d / 5d);
    FxRate eurUsd = FxRate.of(EUR, USD, 8d / 7d);
    FxRate usdEur = FxRate.of(USD, EUR, 7d / 8d);
    FxRate eurGbp = FxRate.of(EUR, GBP, (8d / 7d) * (4d / 5d));
    FxRate gbpGbp = FxRate.of(GBP, GBP, 1d);
    FxRate usdUsd = FxRate.of(USD, USD, 1d);

    assertThat(eurUsd.crossRate(usdGbp)).isEqualTo(eurGbp);
    assertThat(eurUsd.crossRate(gbpUsd)).isEqualTo(eurGbp);
    assertThat(usdEur.crossRate(usdGbp)).isEqualTo(eurGbp);
    assertThat(usdEur.crossRate(gbpUsd)).isEqualTo(eurGbp);

    assertThat(gbpUsd.crossRate(usdEur)).isEqualTo(eurGbp);
    assertThat(gbpUsd.crossRate(eurUsd)).isEqualTo(eurGbp);
    assertThat(usdGbp.crossRate(usdEur)).isEqualTo(eurGbp);
    assertThat(usdGbp.crossRate(eurUsd)).isEqualTo(eurGbp);

    assertThatIllegalArgumentException().isThrownBy(() -> gbpGbp.crossRate(gbpUsd));  // identity
    assertThatIllegalArgumentException().isThrownBy(() -> usdUsd.crossRate(gbpUsd));  // identity
    assertThatIllegalArgumentException().isThrownBy(() -> gbpUsd.crossRate(gbpUsd));  // same currencies
    assertThatIllegalArgumentException().isThrownBy(() -> gbpUsd.crossRate(usdGbp));  // same currencies
    assertThatIllegalArgumentException().isThrownBy(() -> gbpUsd.crossRate(FxRate.of(EUR, CAD, 12d / 5d)));  // no common currency
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals_hashCode() {
    FxRate a1 = FxRate.of(AUD, GBP, 1.25d);
    FxRate a2 = FxRate.of(AUD, GBP, 1.25d);
    FxRate b = FxRate.of(USD, GBP, 1.25d);
    FxRate c = FxRate.of(USD, GBP, 1.35d);

    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(c)).isEqualTo(false);

    assertThat(b.equals(a1)).isEqualTo(false);
    assertThat(b.equals(a2)).isEqualTo(false);
    assertThat(b.equals(b)).isEqualTo(true);
    assertThat(b.equals(c)).isEqualTo(false);

    assertThat(c.equals(a1)).isEqualTo(false);
    assertThat(c.equals(a2)).isEqualTo(false);
    assertThat(c.equals(b)).isEqualTo(false);
    assertThat(c.equals(c)).isEqualTo(true);

    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
  }

  @Test
  public void test_equals_bad() {
    FxRate test = FxRate.of(AUD, GBP, 1.25d);
    assertThat(test.equals(ANOTHER_TYPE)).isFalse();
    assertThat(test.equals(null)).isFalse();
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(FxRate.of(GBP, USD, 1.25d));
    assertSerialization(FxRate.of(GBP, GBP, 1d));
  }

  //-----------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(FxRate.of(GBP, USD, 1.25d));
  }

}
