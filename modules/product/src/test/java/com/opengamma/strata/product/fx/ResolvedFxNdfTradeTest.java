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
 * Test {@link ResolvedFxNdfTrade}.
 */
@Test
public class ResolvedFxNdfTradeTest {

  private static final ResolvedFxNdf PRODUCT = ResolvedFxNdfTest.sut();
  private static final ResolvedFxNdf PRODUCT2 = ResolvedFxNdfTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2015, 1, 15));

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedFxNdfTrade test = ResolvedFxNdfTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .build();
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), PRODUCT);
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
  static ResolvedFxNdfTrade sut() {
    return ResolvedFxNdfTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .build();
  }

  static ResolvedFxNdfTrade sut2() {
    return ResolvedFxNdfTrade.builder()
        .product(PRODUCT2)
        .build();
  }

}
