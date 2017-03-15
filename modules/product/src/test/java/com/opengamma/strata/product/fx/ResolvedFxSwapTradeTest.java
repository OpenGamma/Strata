/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedFxSwapTrade}.
 */
@Test
public class ResolvedFxSwapTradeTest {

  private static final ResolvedFxSwap SWAP1 = ResolvedFxSwapTest.sut();
  private static final ResolvedFxSwap SWAP2 = ResolvedFxSwapTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2015, 1, 15));

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedFxSwapTrade test = ResolvedFxSwapTrade.builder()
        .info(TRADE_INFO)
        .product(SWAP1)
        .build();
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), SWAP1);
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
  static ResolvedFxSwapTrade sut() {
    return ResolvedFxSwapTrade.builder()
        .info(TRADE_INFO)
        .product(SWAP1)
        .build();
  }

  static ResolvedFxSwapTrade sut2() {
    return ResolvedFxSwapTrade.builder()
        .product(SWAP2)
        .build();
  }

}
