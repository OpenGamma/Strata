/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.equity;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link EquityTrade}.
 */
@Test
public class EquityTradeTest {

  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2016, 6, 30));
  private static final Equity PRODUCT = EquityTest.sut();
  private static final Equity PRODUCT2 = EquityTest.sut2();
  private static final double QUANTITY = 100;
  private static final double QUANTITY2 = 200;
  private static final double PRICE = 123.50;
  private static final double PRICE2 = 120.50;

  //-------------------------------------------------------------------------
  public void test_builder() {
    EquityTrade test = sut();
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), PRICE);
    assertEquals(test.getSecurityId(), PRODUCT.getSecurityId());
    assertEquals(test.getCurrency(), PRODUCT.getCurrency());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static EquityTrade sut() {
    return EquityTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static EquityTrade sut2() {
    return EquityTrade.builder()
        .info(TradeInfo.empty())
        .product(PRODUCT2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
