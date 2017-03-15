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
 * Test {@link ResolvedFxSingleTrade}.
 */
@Test
public class ResolvedFxSingleTradeTest {

  private static final ResolvedFxSingle FWD1 = ResolvedFxSingleTest.sut();
  private static final ResolvedFxSingle FWD2 = ResolvedFxSingleTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2015, 1, 15));

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedFxSingleTrade test = ResolvedFxSingleTrade.builder()
        .info(TRADE_INFO)
        .product(FWD1)
        .build();
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), FWD1);
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
