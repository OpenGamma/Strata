/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

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

  public void test_normalize_sell() {
    assertEquals(BuySell.SELL.normalize(1d), -1d, 0d);
    assertEquals(BuySell.SELL.normalize(0d), -0d, 0d);
    assertEquals(BuySell.SELL.normalize(-1d), -1d, 0d);
  }

  public void test_normalize_buy() {
    assertEquals(BuySell.BUY.normalize(1d), 1d, 0d);
    assertEquals(BuySell.BUY.normalize(0d), 0d, 0d);
    assertEquals(BuySell.BUY.normalize(-1d), 1d, 0d);
  }

  public void test_isBuy() {
    assertEquals(BuySell.BUY.isBuy(), true);
    assertEquals(BuySell.SELL.isBuy(), false);
  }

  public void test_isSell() {
    assertEquals(BuySell.BUY.isSell(), false);
    assertEquals(BuySell.SELL.isSell(), true);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
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

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> BuySell.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> BuySell.of(null));
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
