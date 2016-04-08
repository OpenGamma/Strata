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
 * Test {@link ResolvedSwapTrade}.
 */
@Test
public class ResolvedSwapTradeTest {

  private static final ResolvedSwap SWAP1 = ResolvedSwap.of(ResolvedSwapTest.LEG1, ResolvedSwapTest.LEG2);
  private static final ResolvedSwap SWAP2 = ResolvedSwap.of(ResolvedSwapTest.LEG1);
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 6, 30));

  //-------------------------------------------------------------------------
  public void test_of() {
    ResolvedSwapTrade test = ResolvedSwapTrade.of(TRADE_INFO, SWAP1);
    assertEquals(test.getProduct(), SWAP1);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  public void test_builder() {
    ResolvedSwapTrade test = ResolvedSwapTrade.builder()
        .product(SWAP1)
        .build();
    assertEquals(test.getInfo(), TradeInfo.empty());
    assertEquals(test.getProduct(), SWAP1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedSwapTrade test = ResolvedSwapTrade.builder()
        .info(TradeInfo.of(date(2014, 6, 30)))
        .product(SWAP1)
        .build();
    coverImmutableBean(test);
    ResolvedSwapTrade test2 = ResolvedSwapTrade.builder()
        .product(SWAP2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ResolvedSwapTrade test = ResolvedSwapTrade.builder()
        .info(TradeInfo.of(date(2014, 6, 30)))
        .product(SWAP1)
        .build();
    assertSerialization(test);
  }

}
