/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.product.TradeInfo;

/**
 * Test.
 */
@Test
public class FraTradeTest {

  private static final double NOTIONAL_1M = 1_000_000d;
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final Fra FRA1 = Fra.builder()
      .buySell(BUY)
      .notional(NOTIONAL_1M)
      .startDate(date(2015, 6, 15))
      .endDate(date(2015, 9, 15))
      .fixedRate(0.25d)
      .index(GBP_LIBOR_3M)
      .build();
  private static final Fra FRA2 = FRA1.toBuilder().notional(NOTIONAL_2M).build();

  //-------------------------------------------------------------------------
  public void test_builder() {
    FraTrade test = FraTrade.builder()
        .product(FRA1)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.EMPTY);
    assertEquals(test.getProduct(), FRA1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FraTrade test = FraTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .product(FRA1)
        .build();
    coverImmutableBean(test);
    FraTrade test2 = FraTrade.builder()
        .product(FRA2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FraTrade test = FraTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .product(FRA1)
        .build();
    assertSerialization(test);
  }

}
