/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link FxSingleTrade}.
 */
@Test
public class FxSingleTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FxSingle PRODUCT = FxSingleTest.sut();
  private static final FxSingle PRODUCT2 = FxSingleTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2015, 1, 15));

  //-------------------------------------------------------------------------
  public void test_of() {
    FxSingleTrade test = FxSingleTrade.of(TRADE_INFO, PRODUCT);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  public void test_builder() {
    FxSingleTrade test = FxSingleTrade.builder()
        .product(PRODUCT)
        .build();
    assertEquals(test.getInfo(), TradeInfo.empty());
    assertEquals(test.getProduct(), PRODUCT);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    FxSingleTrade test = FxSingleTrade.builder()
        .product(PRODUCT)
        .info(TRADE_INFO)
        .build();
    ResolvedFxSingleTrade expected = ResolvedFxSingleTrade.of(TRADE_INFO, PRODUCT.resolve(REF_DATA));
    assertEquals(test.resolve(REF_DATA), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxSingleTrade test = sut();
    coverImmutableBean(test);
    FxSingleTrade test2 = sut2();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static FxSingleTrade sut() {
    return FxSingleTrade.builder()
        .info(TradeInfo.of(date(2014, 6, 30)))
        .product(PRODUCT)
        .build();
  }

  static FxSingleTrade sut2() {
    return FxSingleTrade.builder()
        .product(PRODUCT2)
        .build();
  }

}
