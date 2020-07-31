/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.BHD;
import static com.opengamma.strata.basics.currency.Currency.BRL;
import static com.opengamma.strata.basics.currency.Currency.CAD;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link CurrencyPair}.
 */
public class CurrencyPairTest {

  private static final Object ANOTHER_TYPE = "";

  //-----------------------------------------------------------------------
  @Test
  public void test_getAvailable() {
    Set<CurrencyPair> available = CurrencyPair.getAvailablePairs();
    assertThat(available.contains(CurrencyPair.of(EUR, USD))).isTrue();
    assertThat(available.contains(CurrencyPair.of(EUR, GBP))).isTrue();
    assertThat(available.contains(CurrencyPair.of(GBP, USD))).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_CurrencyCurrency() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertThat(test.getBase()).isEqualTo(GBP);
    assertThat(test.getCounter()).isEqualTo(USD);
    assertThat(test.isIdentity()).isEqualTo(false);
    assertThat(test.toSet()).containsOnly(GBP, USD);
    assertThat(test.toString()).isEqualTo("GBP/USD");
  }

  @Test
  public void test_of_CurrencyCurrency_reverseStandardOrder() {
    CurrencyPair test = CurrencyPair.of(USD, GBP);
    assertThat(test.getBase()).isEqualTo(USD);
    assertThat(test.getCounter()).isEqualTo(GBP);
    assertThat(test.isIdentity()).isEqualTo(false);
    assertThat(test.toSet()).containsOnly(GBP, USD);
    assertThat(test.toString()).isEqualTo("USD/GBP");
  }

  @Test
  public void test_of_CurrencyCurrency_same() {
    CurrencyPair test = CurrencyPair.of(USD, USD);
    assertThat(test.getBase()).isEqualTo(USD);
    assertThat(test.getCounter()).isEqualTo(USD);
    assertThat(test.isIdentity()).isEqualTo(true);
    assertThat(test.toString()).isEqualTo("USD/USD");
  }

  @Test
  public void test_of_CurrencyCurrency_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> CurrencyPair.of(null, USD));
    assertThatIllegalArgumentException().isThrownBy(() -> CurrencyPair.of(USD, null));
    assertThatIllegalArgumentException().isThrownBy(() -> CurrencyPair.of(null, null));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_parseGood() {
    return new Object[][]{
        {"USD/EUR", USD, EUR},
        {"EUR/USD", EUR, USD},
        {"EUR/EUR", EUR, EUR},
        {"cAd/GbP", CAD, GBP},
    };
  }

  @ParameterizedTest
  @MethodSource("data_parseGood")
  public void test_parse_String_good(String input, Currency base, Currency counter) {
    assertThat(CurrencyPair.parse(input)).isEqualTo(CurrencyPair.of(base, counter));
  }

  public static Object[][] data_parseBad() {
    return new Object[][]{
        {"AUD"},
        {"AUD/GB"},
        {"AUD GBP"},
        {"AUD:GBP"},
        {"123/456"},
        {""},
        {null},
    };
  }

  @ParameterizedTest
  @MethodSource("data_parseBad")
  public void test_parse_String_bad(String input) {
    assertThatIllegalArgumentException().isThrownBy(() -> CurrencyPair.parse(input));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_inverse() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertThat(test.inverse()).isEqualTo(CurrencyPair.of(USD, GBP));
  }

  @Test
  public void test_inverse_same() {
    CurrencyPair test = CurrencyPair.of(GBP, GBP);
    assertThat(test.inverse()).isEqualTo(CurrencyPair.of(GBP, GBP));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_contains_Currency() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertThat(test.contains(GBP)).isEqualTo(true);
    assertThat(test.contains(USD)).isEqualTo(true);
    assertThat(test.contains(EUR)).isEqualTo(false);
  }

  @Test
  public void test_contains_Currency_same() {
    CurrencyPair test = CurrencyPair.of(GBP, GBP);
    assertThat(test.contains(GBP)).isEqualTo(true);
    assertThat(test.contains(USD)).isEqualTo(false);
    assertThat(test.contains(EUR)).isEqualTo(false);
  }

  @Test
  public void test_contains_Currency_null() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertThatIllegalArgumentException().isThrownBy(() -> test.contains(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_other_Currency() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertThat(test.other(GBP)).isEqualTo(USD);
    assertThat(test.other(USD)).isEqualTo(GBP);
    assertThatIllegalArgumentException().isThrownBy(() -> test.other(EUR));
  }

  @Test
  public void test_other_Currency_same() {
    CurrencyPair test = CurrencyPair.of(GBP, GBP);
    assertThat(test.other(GBP)).isEqualTo(GBP);
    assertThatIllegalArgumentException().isThrownBy(() -> test.other(EUR));
  }

  @Test
  public void test_other_Currency_null() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertThatIllegalArgumentException().isThrownBy(() -> test.other(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_isInverse_CurrencyPair() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertThat(test.isInverse(test)).isEqualTo(false);
    assertThat(test.isInverse(CurrencyPair.of(GBP, USD))).isEqualTo(false);
    assertThat(test.isInverse(CurrencyPair.of(USD, GBP))).isEqualTo(true);
    assertThat(test.isInverse(CurrencyPair.of(GBP, EUR))).isEqualTo(false);
    assertThat(test.isInverse(CurrencyPair.of(EUR, GBP))).isEqualTo(false);
    assertThat(test.isInverse(CurrencyPair.of(USD, EUR))).isEqualTo(false);
    assertThat(test.isInverse(CurrencyPair.of(EUR, USD))).isEqualTo(false);
  }

  @Test
  public void test_isInverse_CurrencyPair_null() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertThatIllegalArgumentException().isThrownBy(() -> test.isInverse(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cross_CurrencyPair() {
    CurrencyPair gbpGbp = CurrencyPair.of(GBP, GBP);
    CurrencyPair gbpUsd = CurrencyPair.of(GBP, USD);
    CurrencyPair usdGbp = CurrencyPair.of(USD, GBP);
    CurrencyPair eurGbp = CurrencyPair.of(EUR, GBP);
    CurrencyPair eurUsd = CurrencyPair.of(EUR, USD);
    CurrencyPair usdEur = CurrencyPair.of(USD, EUR);

    assertThat(gbpUsd.cross(gbpUsd)).isEqualTo(Optional.empty());
    assertThat(gbpUsd.cross(usdGbp)).isEqualTo(Optional.empty());
    assertThat(gbpGbp.cross(gbpUsd)).isEqualTo(Optional.empty());
    assertThat(gbpUsd.cross(gbpGbp)).isEqualTo(Optional.empty());

    assertThat(gbpUsd.cross(usdEur)).isEqualTo(Optional.of(eurGbp));
    assertThat(gbpUsd.cross(eurUsd)).isEqualTo(Optional.of(eurGbp));
    assertThat(usdGbp.cross(usdEur)).isEqualTo(Optional.of(eurGbp));
    assertThat(usdGbp.cross(eurUsd)).isEqualTo(Optional.of(eurGbp));

    assertThat(usdEur.cross(gbpUsd)).isEqualTo(Optional.of(eurGbp));
    assertThat(usdEur.cross(usdGbp)).isEqualTo(Optional.of(eurGbp));
    assertThat(eurUsd.cross(gbpUsd)).isEqualTo(Optional.of(eurGbp));
    assertThat(eurUsd.cross(usdGbp)).isEqualTo(Optional.of(eurGbp));
  }

  @Test
  public void test_cross_CurrencyPair_null() {
    CurrencyPair test = CurrencyPair.of(GBP, USD);
    assertThatIllegalArgumentException().isThrownBy(() -> test.cross(null));
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_isConventional() {
    assertThat(CurrencyPair.of(GBP, USD).isConventional()).isEqualTo(true);
    assertThat(CurrencyPair.of(USD, GBP).isConventional()).isEqualTo(false);
    // There is no configuration for GBP/BRL or BRL/GBP so the ordering list is used to choose a convention pair
    // GBP is in the currency order list and BRL isn't so GBP is the base
    assertThat(CurrencyPair.of(GBP, BRL).isConventional()).isEqualTo(true);
    assertThat(CurrencyPair.of(BRL, GBP).isConventional()).isEqualTo(false);
    // There is no configuration for BHD/BRL or BRL/BHD and neither are in the list specifying currency priority order.
    // Lexicographical ordering is used
    assertThat(CurrencyPair.of(BHD, BRL).isConventional()).isEqualTo(true);
    assertThat(CurrencyPair.of(BRL, BHD).isConventional()).isEqualTo(false);
    assertThat(CurrencyPair.of(GBP, GBP).isConventional()).isEqualTo(true);
  }

  @Test
  public void test_isConventional_Consistency() {

    // Assert that all possible pairs of currencies have 1 'conventional' direction
    ImmutableList<Currency> allCurrencies = ImmutableList.copyOf(Currency.getAvailableCurrencies());

    for (int i = 0; i < allCurrencies.size(); i++) {
      Currency currency1 = allCurrencies.get(i);
      for (int j = i + 1; j < allCurrencies.size(); j++) {
        Currency currency2 = allCurrencies.get(j);
        CurrencyPair pair = CurrencyPair.of(currency1, currency2);
        CurrencyPair inversePair = CurrencyPair.of(currency2, currency1);
        assertThat(pair.isConventional()).isNotEqualTo(inversePair.isConventional());
      }
    }
  }

  @Test
  public void test_toConventional() {
    assertThat(CurrencyPair.of(GBP, USD).toConventional()).isEqualTo(CurrencyPair.of(GBP, USD));
    assertThat(CurrencyPair.of(USD, GBP).toConventional()).isEqualTo(CurrencyPair.of(GBP, USD));

    assertThat(CurrencyPair.of(GBP, BRL).toConventional()).isEqualTo(CurrencyPair.of(GBP, BRL));
    assertThat(CurrencyPair.of(BRL, GBP).toConventional()).isEqualTo(CurrencyPair.of(GBP, BRL));

    assertThat(CurrencyPair.of(BHD, BRL).toConventional()).isEqualTo(CurrencyPair.of(BHD, BRL));
    assertThat(CurrencyPair.of(BRL, BHD).toConventional()).isEqualTo(CurrencyPair.of(BHD, BRL));
  }

  @Test
  public void test_rateDigits() {
    assertThat(CurrencyPair.of(GBP, USD).getRateDigits()).isEqualTo(4);
    assertThat(CurrencyPair.of(USD, GBP).getRateDigits()).isEqualTo(4);
    assertThat(CurrencyPair.of(BRL, GBP).getRateDigits()).isEqualTo(4);
    assertThat(CurrencyPair.of(GBP, BRL).getRateDigits()).isEqualTo(4);
    assertThat(CurrencyPair.of(BRL, BHD).getRateDigits()).isEqualTo(5);
    assertThat(CurrencyPair.of(BHD, BRL).getRateDigits()).isEqualTo(5);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals_hashCode() {
    CurrencyPair a1 = CurrencyPair.of(AUD, GBP);
    CurrencyPair a2 = CurrencyPair.of(AUD, GBP);
    CurrencyPair b = CurrencyPair.of(USD, GBP);
    CurrencyPair c = CurrencyPair.of(USD, EUR);

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
    CurrencyPair test = CurrencyPair.of(AUD, GBP);
    assertThat(test.equals(ANOTHER_TYPE)).isFalse();
    assertThat(test.equals(null)).isFalse();
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(CurrencyPair.of(GBP, USD));
    assertSerialization(CurrencyPair.of(GBP, GBP));
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(CurrencyPair.class, CurrencyPair.of(GBP, USD));
    assertJodaConvert(CurrencyPair.class, CurrencyPair.of(GBP, GBP));
  }

}
