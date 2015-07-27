/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.swap.type;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapTrade;

/**
 * Test {@link FixedOvernightSwapConvention}.
 */
@Test
public class FixedOvernightSwapConventionTest {

  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, GBLO);
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment NEXT_SAME_BUS_DAY = DaysAdjustment.ofCalendarDays(0, BDA_FOLLOW);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, GBLO);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);

  private static final FixedRateSwapLegConvention FIXED =
      FixedRateSwapLegConvention.of(USD, ACT_360, P6M, BDA_FOLLOW);
  private static final FixedRateSwapLegConvention FIXED2 =
      FixedRateSwapLegConvention.of(GBP, ACT_365F, P3M, BDA_MOD_FOLLOW);
  private static final OvernightRateSwapLegConvention FFUND_LEG =
      OvernightRateSwapLegConvention.of(USD_FED_FUND, P12M, 2);
  private static final OvernightRateSwapLegConvention FFUND_LEG2 =
      OvernightRateSwapLegConvention.of(GBP_SONIA, P12M, 0);

  //-------------------------------------------------------------------------
  public void test_of() {
    FixedOvernightSwapConvention test = FixedOvernightSwapConvention.of(FIXED, FFUND_LEG, PLUS_TWO_DAYS);
    assertEquals(test.getFixedLeg(), FIXED);
    assertEquals(test.getFloatingLeg(), FFUND_LEG);
    assertEquals(test.getSpotDateOffset(), PLUS_TWO_DAYS);
  }

  //-------------------------------------------------------------------------
  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> FixedOvernightSwapConvention.builder()
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    FixedOvernightSwapConvention test = FixedOvernightSwapConvention.of(FIXED, FFUND_LEG, PLUS_TWO_DAYS).expand();
    assertEquals(test.getFixedLeg(), FIXED.expand());
    assertEquals(test.getFloatingLeg(), FFUND_LEG.expand());
    assertEquals(test.getSpotDateOffset(), PLUS_TWO_DAYS);
  }

  public void test_expandAllSpecified() {
    FixedOvernightSwapConvention test = FixedOvernightSwapConvention.builder()
        .fixedLeg(FIXED)
        .floatingLeg(FFUND_LEG)
        .spotDateOffset(PLUS_ONE_DAY)
        .build()
        .expand();
    assertEquals(test.getFixedLeg(), FIXED.expand());
    assertEquals(test.getFloatingLeg(), FFUND_LEG.expand());
    assertEquals(test.getSpotDateOffset(), PLUS_ONE_DAY);
  }

  //-------------------------------------------------------------------------
  public void test_toTrade_tenor() {
    FixedOvernightSwapConvention base = FixedOvernightSwapConvention.of(FIXED, FFUND_LEG, PLUS_TWO_DAYS);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 5, 7);
    LocalDate endDate = date(2025, 5, 7);
    SwapTrade test = base.toTrade(tradeDate, TENOR_10Y, BUY, NOTIONAL_2M, 0.25d);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        FFUND_LEG.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_toTrade_periodTenor() {
    FixedOvernightSwapConvention base = FixedOvernightSwapConvention.of(FIXED, FFUND_LEG, PLUS_TWO_DAYS);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 7);
    LocalDate endDate = date(2025, 8, 7);
    SwapTrade test = base.toTrade(tradeDate, Period.ofMonths(3), TENOR_10Y, BUY, NOTIONAL_2M, 0.25d);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        FFUND_LEG.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_toTrade_dates() {
    FixedOvernightSwapConvention base = FixedOvernightSwapConvention.of(FIXED, FFUND_LEG, PLUS_TWO_DAYS);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 5);
    LocalDate endDate = date(2015, 11, 5);
    SwapTrade test = base.toTrade(tradeDate, startDate, endDate, BUY, NOTIONAL_2M, 0.25d);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        FFUND_LEG.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FixedOvernightSwapConvention test = FixedOvernightSwapConvention.of(FIXED, FFUND_LEG, PLUS_TWO_DAYS);
    coverImmutableBean(test);
    FixedOvernightSwapConvention test2 = FixedOvernightSwapConvention.of(FIXED2, FFUND_LEG2, PLUS_ONE_DAY);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FixedOvernightSwapConvention test = FixedOvernightSwapConvention.of(FIXED, FFUND_LEG, PLUS_TWO_DAYS);
    assertSerialization(test);
  }

}
