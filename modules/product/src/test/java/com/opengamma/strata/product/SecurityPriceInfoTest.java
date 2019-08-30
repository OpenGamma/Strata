/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link SecurityPriceInfo}.
 */
public class SecurityPriceInfoTest {

  @Test
  public void test_of() {
    SecurityPriceInfo test = SecurityPriceInfo.of(0.01, CurrencyAmount.of(GBP, 0.01));
    assertThat(test.getTickSize()).isEqualTo(0.01);
    assertThat(test.getTickValue()).isEqualTo(CurrencyAmount.of(GBP, 0.01));
    assertThat(test.getContractSize()).isEqualTo(1d);
    assertThat(test.getCurrency()).isEqualTo(GBP);
  }

  @Test
  public void test_of_withContractSize() {
    SecurityPriceInfo test = SecurityPriceInfo.of(0.01, CurrencyAmount.of(GBP, 0.01), 20);
    assertThat(test.getTickSize()).isEqualTo(0.01);
    assertThat(test.getTickValue()).isEqualTo(CurrencyAmount.of(GBP, 0.01));
    assertThat(test.getContractSize()).isEqualTo(20d);
    assertThat(test.getCurrency()).isEqualTo(GBP);
  }

  @Test
  public void test_ofCurrencyMinorUnit_GBP() {
    SecurityPriceInfo test = SecurityPriceInfo.ofCurrencyMinorUnit(GBP);
    assertThat(test.getTickSize()).isEqualTo(0.01);
    assertThat(test.getTickValue()).isEqualTo(CurrencyAmount.of(GBP, 0.01));
    assertThat(test.getContractSize()).isEqualTo(1d);
    assertThat(test.getCurrency()).isEqualTo(GBP);
  }

  @Test
  public void test_ofCurrencyMinorUnit_JPY() {
    SecurityPriceInfo test = SecurityPriceInfo.ofCurrencyMinorUnit(JPY);
    assertThat(test.getTickSize()).isEqualTo(1d);
    assertThat(test.getTickValue()).isEqualTo(CurrencyAmount.of(JPY, 1));
    assertThat(test.getContractSize()).isEqualTo(1d);
    assertThat(test.getCurrency()).isEqualTo(JPY);
  }

  @Test
  public void test_ofTradeUnitValue() {
    SecurityPriceInfo priceInfo = SecurityPriceInfo.of(USD, 2000);
    double value = priceInfo.calculateMonetaryValue(3, 2);
    assertThat(value).isEqualTo(12_000d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_calculateMonetaryAmount1() {
    // CME-ED, 1bp = $25
    SecurityPriceInfo test = SecurityPriceInfo.of(0.005, CurrencyAmount.of(USD, 12.50), 1);
    assertThat(test.calculateMonetaryAmount(1, 98)).isEqualTo(CurrencyAmount.of(USD, 245_000));
    assertThat(test.calculateMonetaryAmount(1, 98.02)).isEqualTo(CurrencyAmount.of(USD, 245_050));
    // quantity is simple multiplier
    assertThat(test.calculateMonetaryAmount(2, 98)).isEqualTo(CurrencyAmount.of(USD, 2 * 245_000));
    assertThat(test.calculateMonetaryAmount(3, 98)).isEqualTo(CurrencyAmount.of(USD, 3 * 245_000));
    // contract size is simple multiplier
    SecurityPriceInfo test2 = SecurityPriceInfo.of(0.005, CurrencyAmount.of(USD, 12.50), 2);
    assertThat(test2.calculateMonetaryAmount(1, 98)).isEqualTo(CurrencyAmount.of(USD, 2 * 245_000));
  }

  @Test
  public void test_calculateMonetaryValue() {
    // CME-ED, 1bp = $25
    SecurityPriceInfo test = SecurityPriceInfo.of(0.005, CurrencyAmount.of(USD, 12.50), 1);
    assertThat(test.calculateMonetaryValue(1, 98)).isEqualTo(245_000d);
    assertThat(test.calculateMonetaryValue(1, 98.02)).isEqualTo(245_050d);
    // quantity is simple multiplier
    assertThat(test.calculateMonetaryValue(2, 98)).isEqualTo(2 * 245_000d);
    assertThat(test.calculateMonetaryValue(3, 98)).isEqualTo(3 * 245_000d);
    // contract size is simple multiplier
    SecurityPriceInfo test2 = SecurityPriceInfo.of(0.005, CurrencyAmount.of(USD, 12.50), 2);
    assertThat(test2.calculateMonetaryValue(1, 98)).isEqualTo(2 * 245_000d);
  }

  @Test
  public void test_getTradeUnitValue() {
    SecurityPriceInfo test = SecurityPriceInfo.of(0.005, CurrencyAmount.of(USD, 12.50), 2);
    assertThat(test.getTradeUnitValue()).isEqualTo(5000d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SecurityPriceInfo test = SecurityPriceInfo.of(0.01, CurrencyAmount.of(GBP, 0.01));
    coverImmutableBean(test);
    SecurityPriceInfo test2 = SecurityPriceInfo.of(0.02, CurrencyAmount.of(GBP, 1), 20);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    SecurityPriceInfo test = SecurityPriceInfo.of(0.01, CurrencyAmount.of(GBP, 0.01));
    assertSerialization(test);
  }

}
