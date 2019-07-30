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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link BuySell}.
 */
@Test
public class BuySellTest {

  //-------------------------------------------------------------------------
  public void test_ofBuy() {
    assertEquals(BuySell.ofBuy(true), BuySell.BUY);
    assertEquals(BuySell.ofBuy(false), BuySell.SELL);
  }

  //-------------------------------------------------------------------------
  public void test_normalize_sell_double() {
    assertEquals(BuySell.SELL.normalize(1d), -1d, 0d);
    assertEquals(BuySell.SELL.normalize(0d), 0d, 0d);
    assertEquals(BuySell.SELL.normalize(-0d), 0d, 0d);
    assertEquals(BuySell.SELL.normalize(-1d), -1d, 0d);
  }

  public void test_normalize_sell_amount() {
    assertEquals(BuySell.SELL.normalize(CurrencyAmount.of(GBP, 1d)), CurrencyAmount.of(GBP, -1d));
    assertEquals(BuySell.SELL.normalize(CurrencyAmount.of(GBP, 0d)), CurrencyAmount.of(GBP, 0d));
    assertEquals(BuySell.SELL.normalize(CurrencyAmount.of(GBP, -1d)), CurrencyAmount.of(GBP, -1d));
  }

  public void test_normalize_buy_double() {
    assertEquals(BuySell.BUY.normalize(1d), 1d, 0d);
    assertEquals(BuySell.BUY.normalize(0d), 0d, 0d);
    assertEquals(BuySell.BUY.normalize(-0d), 0d, 0d);
    assertEquals(BuySell.BUY.normalize(-1d), 1d, 0d);
  }

  public void test_normalize_buy_amount() {
    assertEquals(BuySell.BUY.normalize(CurrencyAmount.of(GBP, 1d)), CurrencyAmount.of(GBP, 1d));
    assertEquals(BuySell.BUY.normalize(CurrencyAmount.of(GBP, 0d)), CurrencyAmount.of(GBP, 0d));
    assertEquals(BuySell.BUY.normalize(CurrencyAmount.of(GBP, -1d)), CurrencyAmount.of(GBP, 1d));
  }

  public void test_isBuy() {
    assertEquals(BuySell.BUY.isBuy(), true);
    assertEquals(BuySell.SELL.isBuy(), false);
  }

  public void test_isSell() {
    assertEquals(BuySell.BUY.isSell(), false);
    assertEquals(BuySell.SELL.isSell(), true);
  }

  public void test_opposite() {
    assertEquals(BuySell.BUY.opposite(), BuySell.SELL);
    assertEquals(BuySell.SELL.opposite(), BuySell.BUY);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {BuySell.BUY, "Buy"},
        {BuySell.SELL, "Sell"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(BuySell convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(BuySell convention, String name) {
    assertEquals(BuySell.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupUpperCase(BuySell convention, String name) {
    assertEquals(BuySell.of(name.toUpperCase(Locale.ENGLISH)), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupLowerCase(BuySell convention, String name) {
    assertEquals(BuySell.of(name.toLowerCase(Locale.ENGLISH)), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BuySell.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BuySell.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(BuySell.class);
  }

  public void test_serialization() {
    assertSerialization(BuySell.BUY);
  }

  public void test_jodaConvert() {
    assertJodaConvert(BuySell.class, BuySell.BUY);
  }

}
