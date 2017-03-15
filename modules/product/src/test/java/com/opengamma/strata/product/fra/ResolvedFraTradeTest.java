/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedFraTrade}.
 */
@Test
public class ResolvedFraTradeTest {

  private static final ResolvedFra PRODUCT = ResolvedFraTest.sut();
  private static final ResolvedFra PRODUCT2 = ResolvedFraTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 6, 30));

  //-------------------------------------------------------------------------
  public void test_of() {
    ResolvedFraTrade test = ResolvedFraTrade.of(TRADE_INFO, PRODUCT);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  public void test_builder() {
    ResolvedFraTrade test = ResolvedFraTrade.builder()
        .product(PRODUCT)
        .build();
    assertEquals(test.getInfo(), TradeInfo.empty());
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
  static ResolvedFraTrade sut() {
    return ResolvedFraTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .build();
  }

  static ResolvedFraTrade sut2() {
    return ResolvedFraTrade.builder()
        .product(PRODUCT2)
        .build();
  }

}
