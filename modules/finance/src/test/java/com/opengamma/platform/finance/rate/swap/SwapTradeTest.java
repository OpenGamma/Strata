/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.rate.swap;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import com.opengamma.platform.finance.TradeInfo;
import com.opengamma.strata.collect.id.StandardId;

/**
 * Test.
 */
@Test
public class SwapTradeTest {

  private static final Swap SWAP1 = Swap.of(MockSwapLeg.MOCK_GBP1, MockSwapLeg.MOCK_USD1);
  private static final Swap SWAP2 = Swap.of(MockSwapLeg.MOCK_GBP1);

  public void test_builder() {
    SwapTrade test = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .product(SWAP1)
        .build();
    assertEquals(test.getStandardId(), StandardId.of("OG-Trade", "1"));
    assertEquals(test.getTradeInfo(), TradeInfo.EMPTY);
    assertEquals(test.getProduct(), SWAP1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwapTrade test = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .product(SWAP1)
        .build();
    coverImmutableBean(test);
    SwapTrade test2 = SwapTrade.builder()
        .setString(SwapTrade.meta().standardId().name(), "OG-Trade~2")
        .product(SWAP2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SwapTrade test = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .product(SWAP1)
        .build();
    assertSerialization(test);
  }

}
