/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedIborFutureOptionTrade}.
 */
@Test
public class ResolvedIborFutureOptionTradeTest {

  private static final ResolvedIborFutureOption PRODUCT = ResolvedIborFutureOptionTest.sut();
  private static final ResolvedIborFutureOption PRODUCT2 = ResolvedIborFutureOptionTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2014, 6, 30)).build();
  private static final long QUANTITY = 100L;
  private static final long QUANTITY2 = 200L;
  private static final double PRICE = 0.99;
  private static final double PRICE2 = 0.98;

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedIborFutureOptionTrade test = sut();
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), PRICE);
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
  static ResolvedIborFutureOptionTrade sut() {
    return ResolvedIborFutureOptionTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static ResolvedIborFutureOptionTrade sut2() {
    return ResolvedIborFutureOptionTrade.builder()
        .product(PRODUCT2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
