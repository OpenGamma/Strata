/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test.
 */
@Test
public class SwapTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 6, 30));
  private static final Swap SWAP1 = Swap.of(MockSwapLeg.MOCK_GBP1, MockSwapLeg.MOCK_USD1);
  private static final Swap SWAP2 = Swap.of(MockSwapLeg.MOCK_GBP1);

  //-------------------------------------------------------------------------
  public void test_of() {
    SwapTrade test = SwapTrade.of(TRADE_INFO, SWAP1);
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), SWAP1);
  }

  public void test_builder() {
    SwapTrade test = SwapTrade.builder()
        .product(SWAP1)
        .build();
    assertEquals(test.getInfo(), TradeInfo.empty());
    assertEquals(test.getProduct(), SWAP1);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    SwapTrade test = SwapTrade.of(TRADE_INFO, SWAP1);
    assertEquals(test.resolve(REF_DATA).getInfo(), TRADE_INFO);
    assertEquals(test.resolve(REF_DATA).getProduct(), SWAP1.resolve(REF_DATA));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwapTrade test = SwapTrade.builder()
        .info(TRADE_INFO)
        .product(SWAP1)
        .build();
    coverImmutableBean(test);
    SwapTrade test2 = SwapTrade.builder()
        .product(SWAP2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SwapTrade test = SwapTrade.builder()
        .info(TRADE_INFO)
        .product(SWAP1)
        .build();
    assertSerialization(test);
  }

}
