/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedSwapTrade}.
 */
public class ResolvedSwapTradeTest {

  private static final ResolvedSwap SWAP1 = ResolvedSwap.of(ResolvedSwapTest.LEG1, ResolvedSwapTest.LEG2);
  private static final ResolvedSwap SWAP2 = ResolvedSwap.of(ResolvedSwapTest.LEG1);
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 6, 30));

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    ResolvedSwapTrade test = ResolvedSwapTrade.of(TRADE_INFO, SWAP1);
    assertThat(test.getProduct()).isEqualTo(SWAP1);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
  }

  @Test
  public void test_builder() {
    ResolvedSwapTrade test = ResolvedSwapTrade.builder()
        .product(SWAP1)
        .build();
    assertThat(test.getInfo()).isEqualTo(TradeInfo.empty());
    assertThat(test.getProduct()).isEqualTo(SWAP1);
  }

  //-------------------------------------------------------------------------
  @Test
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

  @Test
  public void test_serialization() {
    ResolvedSwapTrade test = ResolvedSwapTrade.builder()
        .info(TradeInfo.of(date(2014, 6, 30)))
        .product(SWAP1)
        .build();
    assertSerialization(test);
  }

}
