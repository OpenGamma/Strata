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
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedFxSingleTrade}.
 */
public class ResolvedFxSingleTradeTest {

  private static final ResolvedFxSingle FWD1 = ResolvedFxSingleTest.sut();
  private static final ResolvedFxSingle FWD2 = ResolvedFxSingleTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2015, 1, 15));

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    ResolvedFxSingleTrade test = ResolvedFxSingleTrade.builder()
        .info(TRADE_INFO)
        .product(FWD1)
        .build();
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getProduct()).isEqualTo(FWD1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedFxSingleTrade sut() {
    return ResolvedFxSingleTrade.builder()
        .info(TRADE_INFO)
        .product(FWD1)
        .build();
  }

  static ResolvedFxSingleTrade sut2() {
    return ResolvedFxSingleTrade.builder()
        .product(FWD2)
        .build();
  }

}
