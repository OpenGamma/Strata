/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link BuySell}.
 */
public class BuySellTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_ofBuy() {
    assertThat(BuySell.ofBuy(true)).isEqualTo(BuySell.BUY);
    assertThat(BuySell.ofBuy(false)).isEqualTo(BuySell.SELL);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalize_sell_double() {
    assertThat(BuySell.SELL.normalize(1d)).isCloseTo(-1d, offset(0d));
    assertThat(BuySell.SELL.normalize(0d)).isCloseTo(0d, offset(0d));
    assertThat(BuySell.SELL.normalize(-0d)).isCloseTo(0d, offset(0d));
    assertThat(BuySell.SELL.normalize(-1d)).isCloseTo(-1d, offset(0d));
  }

  @Test
  public void test_normalize_sell_amount() {
    assertThat(BuySell.SELL.normalize(CurrencyAmount.of(GBP, 1d))).isEqualTo(CurrencyAmount.of(GBP, -1d));
    assertThat(BuySell.SELL.normalize(CurrencyAmount.of(GBP, 0d))).isEqualTo(CurrencyAmount.of(GBP, 0d));
    assertThat(BuySell.SELL.normalize(CurrencyAmount.of(GBP, -1d))).isEqualTo(CurrencyAmount.of(GBP, -1d));
  }

  @Test
  public void test_normalize_buy_double() {
    assertThat(BuySell.BUY.normalize(1d)).isCloseTo(1d, offset(0d));
    assertThat(BuySell.BUY.normalize(0d)).isCloseTo(0d, offset(0d));
    assertThat(BuySell.BUY.normalize(-0d)).isCloseTo(0d, offset(0d));
    assertThat(BuySell.BUY.normalize(-1d)).isCloseTo(1d, offset(0d));
  }

  @Test
  public void test_normalize_buy_amount() {
    assertThat(BuySell.BUY.normalize(CurrencyAmount.of(GBP, 1d))).isEqualTo(CurrencyAmount.of(GBP, 1d));
    assertThat(BuySell.BUY.normalize(CurrencyAmount.of(GBP, 0d))).isEqualTo(CurrencyAmount.of(GBP, 0d));
    assertThat(BuySell.BUY.normalize(CurrencyAmount.of(GBP, -1d))).isEqualTo(CurrencyAmount.of(GBP, 1d));
  }

  @Test
  public void test_isBuy() {
    assertThat(BuySell.BUY.isBuy()).isTrue();
    assertThat(BuySell.SELL.isBuy()).isFalse();
  }

  @Test
  public void test_isSell() {
    assertThat(BuySell.BUY.isSell()).isFalse();
    assertThat(BuySell.SELL.isSell()).isTrue();
  }

  @Test
  public void test_opposite() {
    assertThat(BuySell.BUY.opposite()).isEqualTo(BuySell.SELL);
    assertThat(BuySell.SELL.opposite()).isEqualTo(BuySell.BUY);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {BuySell.BUY, "Buy"},
        {BuySell.SELL, "Sell"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(BuySell convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(BuySell convention, String name) {
    assertThat(BuySell.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupUpperCase(BuySell convention, String name) {
    assertThat(BuySell.of(name.toUpperCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupLowerCase(BuySell convention, String name) {
    assertThat(BuySell.of(name.toLowerCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BuySell.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BuySell.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(BuySell.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(BuySell.BUY);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(BuySell.class, BuySell.BUY);
  }

}
