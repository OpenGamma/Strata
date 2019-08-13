/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link Currency}.
 */
public class CurrencyTest {

  //-----------------------------------------------------------------------
  @Test
  public void test_constants() {
    assertThat(Currency.of("USD")).isEqualTo(Currency.USD);
    assertThat(Currency.of("EUR")).isEqualTo(Currency.EUR);
    assertThat(Currency.of("JPY")).isEqualTo(Currency.JPY);
    assertThat(Currency.of("GBP")).isEqualTo(Currency.GBP);
    assertThat(Currency.of("CHF")).isEqualTo(Currency.CHF);
    assertThat(Currency.of("AUD")).isEqualTo(Currency.AUD);
    assertThat(Currency.of("CAD")).isEqualTo(Currency.CAD);
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_getAvailable() {
    Set<Currency> available = Currency.getAvailableCurrencies();
    assertThat(available.contains(Currency.USD)).isTrue();
    assertThat(available.contains(Currency.EUR)).isTrue();
    assertThat(available.contains(Currency.JPY)).isTrue();
    assertThat(available.contains(Currency.GBP)).isTrue();
    assertThat(available.contains(Currency.CHF)).isTrue();
    assertThat(available.contains(Currency.AUD)).isTrue();
    assertThat(available.contains(Currency.CAD)).isTrue();
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_of_String() {
    Currency test = Currency.of("SEK");
    assertThat(test.getCode()).isEqualTo("SEK");
    assertThat(test).isSameAs(Currency.of("SEK"));
  }

  @Test
  public void test_of_String_historicCurrency() {
    Currency test = Currency.of("BEF");
    assertThat(test.getCode()).isEqualTo("BEF");
    assertThat(test.getMinorUnitDigits()).isEqualTo(2);
    assertThat(test.getTriangulationCurrency()).isEqualTo(Currency.EUR);
    assertThat(test).isSameAs(Currency.of("BEF"));
  }

  @Test
  public void test_of_String_unknownCurrencyCreated() {
    Currency test = Currency.of("AAA");
    assertThat(test.getCode()).isEqualTo("AAA");
    assertThat(test.getMinorUnitDigits()).isEqualTo(0);
    assertThat(test).isSameAs(Currency.of("AAA"));
  }

  @Test
  public void test_of_String_lowerCase() {
    assertThatIllegalArgumentException().isThrownBy(() -> Currency.of("gbp"));
  }

  public static Object[][] data_ofBad() {
    return new Object[][] {
        {""},
        {"AB"},
        {"gbp"},
        {"ABCD"},
        {"123"},
        {" GBP"},
        {null},
    };
  }

  @ParameterizedTest
  @MethodSource("data_ofBad")
  public void test_of_String_bad(String input) {
    assertThatIllegalArgumentException().isThrownBy(() -> Currency.of(input));
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_parse_String() {
    Currency test = Currency.parse("GBP");
    assertThat(test.getCode()).isEqualTo("GBP");
    assertThat(test).isSameAs(Currency.GBP);
  }

  @Test
  public void test_parse_String_unknownCurrencyCreated() {
    Currency test = Currency.parse("zyx");
    assertThat(test.getCode()).isEqualTo("ZYX");
    assertThat(test.getMinorUnitDigits()).isEqualTo(0);
    assertThat(test).isSameAs(Currency.of("ZYX"));
  }

  @Test
  public void test_parse_String_lowerCase() {
    Currency test = Currency.parse("gbp");
    assertThat(test.getCode()).isEqualTo("GBP");
    assertThat(test).isSameAs(Currency.GBP);
  }

  public static Object[][] data_parseBad() {
    return new Object[][] {
        {""},
        {"AB"},
        {"ABCD"},
        {"123"},
        {" GBP"},
        {null},
    };
  }

  @ParameterizedTest
  @MethodSource("data_parseBad")
  public void test_parse_String_bad(String input) {
    assertThatIllegalArgumentException().isThrownBy(() -> Currency.parse(input));
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_minorUnits() {
    assertThat(Currency.of("USD").getMinorUnitDigits()).isEqualTo(2);
    assertThat(Currency.of("EUR").getMinorUnitDigits()).isEqualTo(2);
    assertThat(Currency.of("JPY").getMinorUnitDigits()).isEqualTo(0);
    assertThat(Currency.of("GBP").getMinorUnitDigits()).isEqualTo(2);
    assertThat(Currency.of("CHF").getMinorUnitDigits()).isEqualTo(2);
    assertThat(Currency.of("AUD").getMinorUnitDigits()).isEqualTo(2);
    assertThat(Currency.of("CAD").getMinorUnitDigits()).isEqualTo(2);
  }

  @Test
  public void test_triangulatonCurrency() {
    assertThat(Currency.of("USD").getTriangulationCurrency()).isEqualTo(Currency.USD);
    assertThat(Currency.of("EUR").getTriangulationCurrency()).isEqualTo(Currency.USD);
    assertThat(Currency.of("JPY").getTriangulationCurrency()).isEqualTo(Currency.USD);
    assertThat(Currency.of("GBP").getTriangulationCurrency()).isEqualTo(Currency.USD);
    assertThat(Currency.of("CHF").getTriangulationCurrency()).isEqualTo(Currency.USD);
    assertThat(Currency.of("AUD").getTriangulationCurrency()).isEqualTo(Currency.USD);
    assertThat(Currency.of("CAD").getTriangulationCurrency()).isEqualTo(Currency.USD);
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_roundMinorUnits_double() {
    assertThat(Currency.USD.roundMinorUnits(63.347d)).isEqualTo(63.35d);
    assertThat(Currency.USD.roundMinorUnits(63.34500001d)).isEqualTo(63.35d);
    assertThat(Currency.USD.roundMinorUnits(63.34499999d)).isEqualTo(63.34d);
    assertThat(Currency.JPY.roundMinorUnits(63.347d)).isEqualTo(63d);
    assertThat(Currency.JPY.roundMinorUnits(63.5347d)).isEqualTo(64d);
  }

  @Test
  public void test_roundMinorUnits_BigDecimal() {
    assertThat(Currency.USD.roundMinorUnits(new BigDecimal(63.347d))).isEqualTo(new BigDecimal("63.35"));
    assertThat(Currency.USD.roundMinorUnits(new BigDecimal(63.34500001d))).isEqualTo(new BigDecimal("63.35"));
    assertThat(Currency.USD.roundMinorUnits(new BigDecimal(63.34499999d))).isEqualTo(new BigDecimal("63.34"));
    assertThat(Currency.JPY.roundMinorUnits(new BigDecimal(63.347d))).isEqualTo(new BigDecimal("63"));
    assertThat(Currency.JPY.roundMinorUnits(new BigDecimal(63.5347d))).isEqualTo(new BigDecimal("64"));
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_compareTo() {
    Currency a = Currency.EUR;
    Currency b = Currency.GBP;
    Currency c = Currency.JPY;
    assertThat(0).isEqualTo(a.compareTo(a));
    assertThat(0).isEqualTo(b.compareTo(b));
    assertThat(0).isEqualTo(c.compareTo(c));

    assertThat(a.compareTo(b) < 0).isTrue();
    assertThat(b.compareTo(a) > 0).isTrue();

    assertThat(a.compareTo(c) < 0).isTrue();
    assertThat(c.compareTo(a) > 0).isTrue();

    assertThat(b.compareTo(c) < 0).isTrue();
    assertThat(c.compareTo(b) > 0).isTrue();
  }

  @Test
  public void test_compareTo_null() {
    assertThatNullPointerException().isThrownBy(() -> Currency.EUR.compareTo(null));
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_equals_hashCode() {
    Object a1 = Currency.GBP;
    Object a2 = Currency.of("GBP");
    Object b = Currency.EUR;
    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(a2)).isEqualTo(true);

    assertThat(a2.equals(a1)).isEqualTo(true);
    assertThat(a2.equals(a2)).isEqualTo(true);
    assertThat(a2.equals(b)).isEqualTo(false);

    assertThat(b.equals(a1)).isEqualTo(false);
    assertThat(b.equals(a2)).isEqualTo(false);
    assertThat(b.equals(b)).isEqualTo(true);

    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
  }

  @Test
  public void test_equals_bad() {
    Object a = Currency.GBP;
    assertThat(a.equals(null)).isEqualTo(false);
    assertThat(a.equals("String")).isEqualTo(false);
    assertThat(a.equals(new Object())).isEqualTo(false);
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_toString() {
    Currency test = Currency.GBP;
    assertThat("GBP").isEqualTo(test.toString());
  }

  //-----------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(CurrencyDataLoader.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(Currency.GBP);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(Currency.class, Currency.GBP);
  }

}
