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

import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedDeliverableSwapFutureTrade}. 
 */
@Test
public class ResolvedDeliverableSwapFutureTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final long QUANTITY = 100L;
  private static final double TRADE_PRICE = 0.99;

  private static final ResolvedDeliverableSwapFuture PRODUCT = DeliverableSwapFutureTradeTest.DSF_PRODUCT.resolve(REF_DATA);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2014, 6, 30)).build();
  private static final StandardId DSF_ID = StandardId.of("OG-Ticker", "DSF1");
  private static final StandardId DSF_ID2 = StandardId.of("OG-Ticker", "DSF2");

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedDeliverableSwapFutureTrade test = ResolvedDeliverableSwapFutureTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(PRODUCT)
        .securityStandardId(DSF_ID)
        .quantity(QUANTITY)
        .tradePrice(TRADE_PRICE)
        .build();
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getSecurityStandardId(), DSF_ID);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getTradePrice(), TRADE_PRICE);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedDeliverableSwapFutureTrade test = ResolvedDeliverableSwapFutureTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(PRODUCT)
        .securityStandardId(DSF_ID)
        .quantity(QUANTITY)
        .tradePrice(TRADE_PRICE)
        .build();
    coverImmutableBean(test);
    ResolvedDeliverableSwapFutureTrade test2 = ResolvedDeliverableSwapFutureTrade.builder()
        .product(PRODUCT)
        .securityStandardId(DSF_ID2)
        .quantity(QUANTITY + 1)
        .tradePrice(TRADE_PRICE + 0.1)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ResolvedDeliverableSwapFutureTrade test = ResolvedDeliverableSwapFutureTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(PRODUCT)
        .securityStandardId(DSF_ID)
        .quantity(QUANTITY)
        .tradePrice(TRADE_PRICE)
        .build();
    assertSerialization(test);
  }

}
