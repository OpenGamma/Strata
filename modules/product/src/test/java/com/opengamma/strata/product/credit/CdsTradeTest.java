/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link CdsTrade}.
 */
@Test
public class CdsTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_of() {
    CdsTrade test = CdsTrade.of(tradeInfo(), CdsTest.sutSingleName());
    assertEquals(test, sutSingleName());
  }

  //-------------------------------------------------------------------------
  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> CdsTrade.builder().build());
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    assertEquals(sutSingleName().resolve(REF_DATA), ResolvedCdsTradeTest.sutSingleName());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sutIndex());
    coverImmutableBean(sutSingleName());
    coverBeanEquals(sutSingleName(), sutIndex());
  }

  public void test_serialization() {
    assertSerialization(sutIndex());
    assertSerialization(sutSingleName());
  }

  //-------------------------------------------------------------------------
  static CdsTrade sutSingleName() {
    return CdsTrade.builder()
        .info(tradeInfo())
        .product(CdsTest.sutSingleName())
        .build();
  }

  static CdsTrade sutIndex() {
    return CdsTrade.builder()
        .info(tradeInfo())
        .product(CdsTest.sutIndex())
        .build();
  }

  static TradeInfo tradeInfo() {
    return TradeInfo.builder().tradeDate(date(2014, 6, 30)).build();
  }

}
