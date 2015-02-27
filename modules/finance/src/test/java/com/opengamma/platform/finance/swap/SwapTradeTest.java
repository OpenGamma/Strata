/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.collect.id.StandardId;
import com.opengamma.platform.finance.TradeInfo;

/**
 * Test.
 */
@Test
public class SwapTradeTest {

  public void test_builder() {
    SwapTrade test = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 12, 3)).build())
        .swap(Swap.of(MockSwapLeg.MOCK_GBP1, MockSwapLeg.MOCK_USD1))
        .build();
    assertEquals(test.getStandardId(), StandardId.of("OG-Trade", "1"));
    assertEquals(test.getTradeInfo(), TradeInfo.builder().tradeDate(date(2014, 12, 3)).build());
    assertEquals(test.getSwap(), Swap.of(MockSwapLeg.MOCK_GBP1, MockSwapLeg.MOCK_USD1));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwapTrade test = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 12, 3)).build())
        .swap(Swap.of(MockSwapLeg.MOCK_GBP1, MockSwapLeg.MOCK_USD1))
        .build();
    coverImmutableBean(test);
    SwapTrade test2 = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "2"))
        .attributes(ImmutableMap.of("key", "value"))
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 12, 5)).build())
        .swap(Swap.of(MockSwapLeg.MOCK_GBP1))
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SwapTrade test = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 12, 3)).build())
        .swap(Swap.of(MockSwapLeg.MOCK_GBP1, MockSwapLeg.MOCK_USD1))
        .build();
    assertSerialization(test);
  }

}
