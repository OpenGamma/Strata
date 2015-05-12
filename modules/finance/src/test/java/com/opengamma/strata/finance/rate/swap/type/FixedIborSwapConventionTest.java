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
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
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
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapTrade;

/**
 * Test {@link FixedIborSwapConvention}.
 */
@Test
public class FixedIborSwapConventionTest {

  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, GBLO);
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment NEXT_SAME_BUS_DAY = DaysAdjustment.ofCalendarDays(0, BDA_FOLLOW);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, GBLO);
  private static final StandardId STANDARD_ID = StandardId.of("A", "B");

  private static final FixedRateSwapLegConvention FIXED =
      FixedRateSwapLegConvention.of(USD, ACT_360, P6M, BDA_FOLLOW);
  private static final FixedRateSwapLegConvention FIXED2 =
      FixedRateSwapLegConvention.of(GBP, ACT_365F, P3M, BDA_MOD_FOLLOW);
  private static final IborRateSwapLegConvention IBOR = IborRateSwapLegConvention.of(USD_LIBOR_3M);
  private static final IborRateSwapLegConvention IBOR2 = IborRateSwapLegConvention.of(GBP_LIBOR_3M);
  private static final IborRateSwapLegConvention IBOR3 = IborRateSwapLegConvention.of(USD_LIBOR_6M);

  //-------------------------------------------------------------------------
  public void test_of() {
    FixedIborSwapConvention test = FixedIborSwapConvention.of(FIXED, IBOR);
    assertEquals(test.getFixedLeg(), FIXED);
    assertEquals(test.getFloatingLeg(), IBOR);
    assertEquals(test.getSpotDateOffset(), USD_LIBOR_3M.getEffectiveDateOffset());
  }

  //-------------------------------------------------------------------------
  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> FixedIborSwapConvention.builder()
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    FixedIborSwapConvention test = FixedIborSwapConvention.of(FIXED, IBOR).expand();
    assertEquals(test.getFixedLeg(), FIXED.expand());
    assertEquals(test.getFloatingLeg(), IBOR.expand());
    assertEquals(test.getSpotDateOffset(), USD_LIBOR_3M.getEffectiveDateOffset());
  }

  public void test_expandAllSpecified() {
    FixedIborSwapConvention test = FixedIborSwapConvention.builder()
        .fixedLeg(FIXED)
        .floatingLeg(IBOR)
        .spotDateOffset(PLUS_ONE_DAY)
        .build()
        .expand();
    assertEquals(test.getFixedLeg(), FIXED.expand());
    assertEquals(test.getFloatingLeg(), IBOR.expand());
    assertEquals(test.getSpotDateOffset(), PLUS_ONE_DAY);
  }

  //-------------------------------------------------------------------------
  public void test_toTrade_tenor() {
    FixedIborSwapConvention base = FixedIborSwapConvention.of(FIXED, IBOR);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 5, 7);
    LocalDate endDate = date(2025, 5, 7);
    SwapTrade test = base.toTrade(STANDARD_ID, tradeDate, TENOR_10Y, BUY, NOTIONAL_2M, 0.25d);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        IBOR.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertEquals(test.getStandardId(), STANDARD_ID);
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_toTrade_periodTenor() {
    FixedIborSwapConvention base = FixedIborSwapConvention.of(FIXED, IBOR);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 7);
    LocalDate endDate = date(2025, 8, 7);
    SwapTrade test = base.toTrade(STANDARD_ID, tradeDate, Period.ofMonths(3), TENOR_10Y, BUY, NOTIONAL_2M, 0.25d);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        IBOR.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertEquals(test.getStandardId(), STANDARD_ID);
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_toTrade_dates() {
    FixedIborSwapConvention base = FixedIborSwapConvention.of(FIXED, IBOR);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 5);
    LocalDate endDate = date(2015, 11, 5);
    SwapTrade test = base.toTrade(STANDARD_ID, tradeDate, startDate, endDate, BUY, NOTIONAL_2M, 0.25d);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        IBOR.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertEquals(test.getStandardId(), STANDARD_ID);
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FixedIborSwapConvention test = FixedIborSwapConvention.of(FIXED, IBOR);
    coverImmutableBean(test);
    FixedIborSwapConvention test2 = FixedIborSwapConvention.of(FIXED2, IBOR2);
    coverBeanEquals(test, test2);
    FixedIborSwapConvention test3 = FixedIborSwapConvention.of(FIXED, IBOR3);
    coverBeanEquals(test, test3);
  }

  public void test_serialization() {
    FixedIborSwapConvention test = FixedIborSwapConvention.of(FIXED, IBOR);
    assertSerialization(test);
  }

}
