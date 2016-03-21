/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedDeliverableSwapFutureTrade}. 
 */
@Test
public class ResolvedDeliverableSwapFutureTradeTest {

  private static final ResolvedDeliverableSwapFuture PRODUCT = ResolvedDeliverableSwapFutureTest.sut();
  private static final ResolvedDeliverableSwapFuture PRODUCT2 = ResolvedDeliverableSwapFutureTest.sut2();
  private static final long QUANTITY = 100L;
  private static final long QUANTITY2 = 200L;
  private static final double PRICE = 0.99;
  private static final double PRICE2 = 0.98;
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 6, 30));

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedDeliverableSwapFutureTrade test = sut();
    assertEquals(test.getInfo(), TRADE_INFO);
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
  static ResolvedDeliverableSwapFutureTrade sut() {
    return ResolvedDeliverableSwapFutureTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static ResolvedDeliverableSwapFutureTrade sut2() {
    return ResolvedDeliverableSwapFutureTrade.builder()
        .product(PRODUCT2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
