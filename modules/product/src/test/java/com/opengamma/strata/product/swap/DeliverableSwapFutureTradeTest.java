/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link DeliverableSwapFutureTrade}.
 */
@Test
public class DeliverableSwapFutureTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  static final DeliverableSwapFuture PRODUCT = DeliverableSwapFutureTest.sut();
  static final DeliverableSwapFuture PRODUCT2 = DeliverableSwapFutureTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.builder()
      .tradeDate(LocalDate.of(2014, 6, 12))
      .settlementDate(LocalDate.of(2014, 6, 14))
      .build();
  private static final long QUANTITY = 100L;
  private static final long QUANTITY2 = 200L;
  private static final double PRICE = 0.99;
  private static final double PRICE2 = 0.98;

  //-------------------------------------------------------------------------
  public void test_builder() {
    DeliverableSwapFutureTrade test = sut();
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), PRICE);
    assertEquals(test.getSecurityId(), PRODUCT.getSecurityId());
    assertEquals(test.getCurrency(), PRODUCT.getCurrency());
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    DeliverableSwapFutureTrade test = sut();
    ResolvedDeliverableSwapFutureTrade expected = ResolvedDeliverableSwapFutureTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertEquals(test.resolve(REF_DATA), expected);
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
  static DeliverableSwapFutureTrade sut() {
    return DeliverableSwapFutureTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static DeliverableSwapFutureTrade sut2() {
    return DeliverableSwapFutureTrade.builder()
        .product(PRODUCT2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
