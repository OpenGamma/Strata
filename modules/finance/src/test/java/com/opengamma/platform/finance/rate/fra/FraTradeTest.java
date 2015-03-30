/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.rate.fra;

import static com.opengamma.basics.BuySell.BUY;
import static com.opengamma.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import com.opengamma.basics.date.AdjustableDate;
import com.opengamma.basics.date.BusinessDayAdjustment;
import com.opengamma.basics.date.DaysAdjustment;
import com.opengamma.platform.finance.TradeInfo;
import com.opengamma.strata.collect.id.StandardId;

/**
 * Test.
 */
@Test
public class FraTradeTest {

  private static final double NOTIONAL_1M = 1_000_000d;
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment MINUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(-2, GBLO);
  private static final Fra FRA1 = Fra.builder()
      .buySell(BUY)
      .paymentDate(AdjustableDate.of(date(2015, 6, 16), BDA_MOD_FOLLOW))
      .startDate(date(2015, 6, 15))
      .endDate(date(2015, 9, 15))
      .fixedRate(0.25d)
      .index(GBP_LIBOR_3M)
      .fixingOffset(MINUS_TWO_DAYS)
      .notional(NOTIONAL_1M)
      .build();
  private static final Fra FRA2 = FRA1.toBuilder().notional(NOTIONAL_2M).build();

  //-------------------------------------------------------------------------
  public void test_builder() {
    FraTrade test = FraTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .product(FRA1)
        .build();
    assertEquals(test.getStandardId(), StandardId.of("OG-Trade", "1"));
    assertEquals(test.getTradeInfo(), TradeInfo.EMPTY);
    assertEquals(test.getProduct(), FRA1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FraTrade test = FraTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .product(FRA1)
        .build();
    coverImmutableBean(test);
    FraTrade test2 = FraTrade.builder()
        .setString(FraTrade.meta().standardId().name(), "OG-Trade~2")
        .product(FRA2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FraTrade test = FraTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .product(FRA1)
        .build();
    assertSerialization(test);
  }

}
