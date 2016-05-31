/**
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
 * Test {@link FxNdfTrade}.
 */
@Test
public class FxNdfTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FxNdf PRODUCT = FxNdfTest.sut();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2015, 1, 15));

  //-------------------------------------------------------------------------
  public void test_of() {
    FxNdfTrade test = FxNdfTrade.of(TRADE_INFO, PRODUCT);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  public void test_builder() {
    FxNdfTrade test = sut();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    FxNdfTrade test = sut();
    ResolvedFxNdfTrade expected = ResolvedFxNdfTrade.of(TRADE_INFO, PRODUCT.resolve(REF_DATA));
    assertEquals(test.resolve(REF_DATA), expected);
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
  static FxNdfTrade sut() {
    return FxNdfTrade.builder()
        .product(PRODUCT)
        .info(TRADE_INFO)
        .build();
  }

  static FxNdfTrade sut2() {
    return FxNdfTrade.builder()
        .product(PRODUCT)
        .build();
  }

}
