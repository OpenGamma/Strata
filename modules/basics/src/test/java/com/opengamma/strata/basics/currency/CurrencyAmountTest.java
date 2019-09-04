/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link CurrencyAmount}.
 */
public class CurrencyAmountTest {

  private static final Currency CCY1 = Currency.AUD;
  private static final Currency CCY2 = Currency.CAD;
  private static final double AMT1 = 100;
  private static final double AMT2 = 200;
  private static final CurrencyAmount CCY_AMOUNT = CurrencyAmount.of(CCY1, AMT1);
  private static final CurrencyAmount CCY_AMOUNT_NEGATIVE = CurrencyAmount.of(CCY1, -AMT1);
  private static final Object ANOTHER_TYPE = "";

  @Test
  public void test_fixture() {
    assertThat(CCY_AMOUNT.getCurrency()).isEqualTo(CCY1);
    assertThat(CCY_AMOUNT.getAmount()).isEqualTo(AMT1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_zero_Currency() {
    CurrencyAmount test = CurrencyAmount.zero(Currency.USD);
    assertThat(test.getCurrency()).isEqualTo(Currency.USD);
    assertThat(test.getAmount()).isEqualTo(0d);
    assertThat(test.isZero()).isTrue();
    assertThat(test.isPositive()).isFalse();
    assertThat(test.isNegative()).isFalse();
  }

  @Test
  public void test_zero_Currency_nullCurrency() {
    assertThatIllegalArgumentException().isThrownBy(() -> CurrencyAmount.zero(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_Currency() {
    CurrencyAmount test = CurrencyAmount.of(Currency.USD, AMT1);
    assertThat(test.getCurrency()).isEqualTo(Currency.USD);
    assertThat(test.getAmount()).isEqualTo(AMT1);
    assertThat(test.isZero()).isFalse();
    assertThat(test.isPositive()).isTrue();
    assertThat(test.isNegative()).isFalse();
  }

  @Test
  public void test_of_Currency_negative() {
    CurrencyAmount test = CurrencyAmount.of(Currency.USD, -1);
    assertThat(test.getCurrency()).isEqualTo(Currency.USD);
    assertThat(test.getAmount()).isEqualTo(-1);
    assertThat(test.isZero()).isFalse();
    assertThat(test.isPositive()).isFalse();
    assertThat(test.isNegative()).isTrue();
  }

  @Test
  public void test_of_Currency_negativeZero() {
    CurrencyAmount test = CurrencyAmount.of(Currency.USD, -0d);
    assertThat(test.getCurrency()).isEqualTo(Currency.USD);
    assertThat(Double.doubleToLongBits(test.getAmount())).isEqualTo(Double.doubleToLongBits(0d));
    assertThat(test.isZero()).isTrue();
    assertThat(test.isPositive()).isFalse();
    assertThat(test.isNegative()).isFalse();
  }

  @Test
  public void test_of_Currency_NaN() {
    assertThatIllegalArgumentException().isThrownBy(() -> CurrencyAmount.of(Currency.USD, Double.NaN));
  }

  @Test
  public void test_of_Currency_nullCurrency() {
    assertThatIllegalArgumentException().isThrownBy(() -> CurrencyAmount.of((Currency) null, AMT1));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_String() {
    CurrencyAmount test = CurrencyAmount.of("USD", AMT1);
    assertThat(test.getCurrency()).isEqualTo(Currency.USD);
    assertThat(test.getAmount()).isEqualTo(AMT1);
  }

  @Test
  public void test_of_String_nullCurrency() {
    assertThatIllegalArgumentException().isThrownBy(() -> CurrencyAmount.of((String) null, AMT1));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parse_String_roundTrip() {
    assertThat(CurrencyAmount.parse(CCY_AMOUNT.toString())).isEqualTo(CCY_AMOUNT);
  }

  public static Object[][] data_parseGood() {
    return new Object[][] {
        {"AUD 100.001", Currency.AUD, 100.001d},
        {"AUD 321.123", Currency.AUD, 321.123d},
        {"AUD 123", Currency.AUD, 123d},
        {"GBP 0", Currency.GBP, 0d},
        {"USD -0", Currency.USD, -0d},
        {"EUR -0.01", Currency.EUR, -0.01d},
    };
  }

  @ParameterizedTest
  @MethodSource("data_parseGood")
  public void test_parse_String_good(String input, Currency currency, double amount) {
    assertThat(CurrencyAmount.parse(input)).isEqualTo(CurrencyAmount.of(currency, amount));
  }

  public static Object[][] data_parseBad() {
    return new Object[][] {
        {"AUD"},
        {"AUD aa"},
        {"AUD -.+-"},
        {"123"},
        {null},
    };
  }

  @ParameterizedTest
  @MethodSource("data_parseBad")
  public void test_parse_String_bad(String input) {
    assertThatIllegalArgumentException().isThrownBy(() -> CurrencyAmount.parse(input));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_plus_CurrencyAmount() {
    CurrencyAmount ccyAmount = CurrencyAmount.of(CCY1, AMT2);
    CurrencyAmount test = CCY_AMOUNT.plus(ccyAmount);
    assertThat(test).isEqualTo(CurrencyAmount.of(CCY1, AMT1 + AMT2));
  }

  @Test
  public void test_plus_CurrencyAmount_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> CCY_AMOUNT.plus(null));
  }

  @Test
  public void test_plus_CurrencyAmount_wrongCurrency() {
    assertThatIllegalArgumentException().isThrownBy(() -> CCY_AMOUNT.plus(CurrencyAmount.of(CCY2, AMT2)));
  }

  @Test
  public void test_plus_double() {
    CurrencyAmount test = CCY_AMOUNT.plus(AMT2);
    assertThat(test).isEqualTo(CurrencyAmount.of(CCY1, AMT1 + AMT2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_minus_CurrencyAmount() {
    CurrencyAmount ccyAmount = CurrencyAmount.of(CCY1, AMT2);
    CurrencyAmount test = CCY_AMOUNT.minus(ccyAmount);
    assertThat(test).isEqualTo(CurrencyAmount.of(CCY1, AMT1 - AMT2));
  }

  @Test
  public void test_minus_CurrencyAmount_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> CCY_AMOUNT.minus(null));
  }

  @Test
  public void test_minus_CurrencyAmount_wrongCurrency() {
    assertThatIllegalArgumentException().isThrownBy(() -> CCY_AMOUNT.minus(CurrencyAmount.of(CCY2, AMT2)));
  }

  @Test
  public void test_minus_double() {
    CurrencyAmount test = CCY_AMOUNT.minus(AMT2);
    assertThat(test).isEqualTo(CurrencyAmount.of(CCY1, AMT1 - AMT2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    CurrencyAmount test = CCY_AMOUNT.multipliedBy(3.5);
    assertThat(test).isEqualTo(CurrencyAmount.of(CCY1, AMT1 * 3.5));
  }

  @Test
  public void test_mapAmount() {
    CurrencyAmount test = CCY_AMOUNT.mapAmount(v -> v * 2 + 1);
    assertThat(test).isEqualTo(CurrencyAmount.of(CCY1, AMT1 * 2 + 1));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_negated() {
    assertThat(CCY_AMOUNT.negated()).isEqualTo(CCY_AMOUNT_NEGATIVE);
    assertThat(CCY_AMOUNT_NEGATIVE.negated()).isEqualTo(CCY_AMOUNT);
    assertThat(CurrencyAmount.zero(Currency.USD)).isEqualTo(CurrencyAmount.zero(Currency.USD).negated());
    assertThat(CurrencyAmount.of(Currency.USD, -0d).negated()).isEqualTo(CurrencyAmount.zero(Currency.USD));
  }

  @Test
  public void test_negative() {
    assertThat(CCY_AMOUNT.negative()).isEqualTo(CCY_AMOUNT_NEGATIVE);
    assertThat(CCY_AMOUNT_NEGATIVE.negative()).isEqualTo(CCY_AMOUNT_NEGATIVE);
  }

  @Test
  public void test_positive() {
    assertThat(CCY_AMOUNT.positive()).isEqualTo(CCY_AMOUNT);
    assertThat(CCY_AMOUNT_NEGATIVE.positive()).isEqualTo(CCY_AMOUNT);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo_explicitRate() {
    assertThat(CCY_AMOUNT.convertedTo(CCY2, 2.5d)).isEqualTo(CurrencyAmount.of(CCY2, AMT1 * 2.5d));
    assertThat(CCY_AMOUNT.convertedTo(CCY1, 1d)).isEqualTo(CCY_AMOUNT);
    assertThatIllegalArgumentException().isThrownBy(() -> CCY_AMOUNT.convertedTo(CCY1, 1.5d));
  }

  @Test
  public void test_convertedTo_rateProvider() {
    FxRateProvider provider = (ccy1, ccy2) -> 2.5d;
    assertThat(CCY_AMOUNT.convertedTo(CCY2, provider)).isEqualTo(CurrencyAmount.of(CCY2, AMT1 * 2.5d));
    assertThat(CCY_AMOUNT.convertedTo(CCY1, provider)).isEqualTo(CCY_AMOUNT);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals_hashCode() {
    CurrencyAmount other = CurrencyAmount.of(CCY1, AMT1);
    assertThat(CCY_AMOUNT.equals(CCY_AMOUNT)).isTrue();
    assertThat(CCY_AMOUNT.equals(other)).isTrue();
    assertThat(other.equals(CCY_AMOUNT)).isTrue();
    assertThat(CCY_AMOUNT.hashCode()).isEqualTo(other.hashCode());
    other = CurrencyAmount.of(CCY1, AMT1);
    assertThat(CCY_AMOUNT).isEqualTo(other);
    assertThat(CCY_AMOUNT.hashCode()).isEqualTo(other.hashCode());
    other = CurrencyAmount.of(CCY2, AMT1);
    assertThat(CCY_AMOUNT.equals(other)).isFalse();
    other = CurrencyAmount.of(CCY1, AMT2);
    assertThat(CCY_AMOUNT.equals(other)).isFalse();
  }

  @Test
  public void test_equals_bad() {
    assertThat(CCY_AMOUNT.equals(ANOTHER_TYPE)).isFalse();
    assertThat(CCY_AMOUNT.equals(null)).isFalse();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toString() {
    assertThat(CurrencyAmount.of(Currency.AUD, 100d).toString()).isEqualTo("AUD 100");
    assertThat(CurrencyAmount.of(Currency.AUD, 100.123d).toString()).isEqualTo("AUD 100.123");
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(CCY_AMOUNT);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(CurrencyAmount.class, CCY_AMOUNT);
  }

}
