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
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link ResolvedCdsTrade}.
 */
@Test
public class ResolvedCdsTradeTest {

  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> ResolvedCdsTrade.builder().build());
  }

  //-------------------------------------------------------------------------
  public void test_of() {
    ResolvedCdsTrade test = ResolvedCdsTrade.of(CdsTradeTest.tradeInfo(), ResolvedCdsTest.sutSingleName());
    assertEquals(test, sutSingleName());
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
  static ResolvedCdsTrade sutSingleName() {
    return ResolvedCdsTrade.builder()
        .info(CdsTradeTest.tradeInfo())
        .product(ResolvedCdsTest.sutSingleName())
        .build();
  }

  static ResolvedCdsTrade sutIndex() {
    return ResolvedCdsTrade.builder()
        .info(CdsTradeTest.tradeInfo())
        .product(ResolvedCdsTest.sutIndex())
        .build();
  }

}
