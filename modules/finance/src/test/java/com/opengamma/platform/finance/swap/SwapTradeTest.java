/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.platform.finance.TradeType;
import com.opengamma.platform.source.id.StandardId;

/**
 * Test.
 */
@Test
public class SwapTradeTest {

  public void test_builder() {
    SwapTrade test = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 12, 3))
        .swap(Swap.of(MockSwapLeg.MOCK_GBP1, MockSwapLeg.MOCK_USD1))
        .build();
    assertEquals(test.getTradeType(), TradeType.of("Swap"));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SwapTrade test = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 12, 3))
        .swap(Swap.of(MockSwapLeg.MOCK_GBP1, MockSwapLeg.MOCK_USD1))
        .build();
    coverImmutableBean(test);
    SwapTrade test2 = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "2"))
        .attributes(ImmutableMap.of("key", "value"))
        .tradeDate(LocalDate.of(2014, 12, 5))
        .swap(Swap.of(MockSwapLeg.MOCK_GBP1))
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SwapTrade test = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 12, 3))
        .swap(Swap.of(MockSwapLeg.MOCK_GBP1, MockSwapLeg.MOCK_USD1))
        .build();
    assertSerialization(test);
  }

}
