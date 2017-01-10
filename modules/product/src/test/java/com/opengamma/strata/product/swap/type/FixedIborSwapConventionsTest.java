/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link FixedIborSwapConventions}.
 * <p>
 * These tests  match the table 18.1 in the following guide:
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
@Test
public class FixedIborSwapConventionsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  @DataProvider(name = "spotLag")
  static Object[][] data_spot_lag() {
    return new Object[][] {
        {FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M, 2},
        {FixedIborSwapConventions.USD_FIXED_1Y_LIBOR_3M, 2},
        {FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_3M, 2},
        {FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M, 2},
        {FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M, 0},
        {FixedIborSwapConventions.GBP_FIXED_6M_LIBOR_6M, 0},
        {FixedIborSwapConventions.GBP_FIXED_3M_LIBOR_3M, 0},
        {FixedIborSwapConventions.JPY_FIXED_6M_TIBORJ_3M, 2},
        {FixedIborSwapConventions.JPY_FIXED_6M_LIBOR_6M, 2},
        {FixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_3M, 2},
        {FixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_6M, 2}
    };
  }

  @Test(dataProvider = "spotLag")
  public void test_spot_lag(ImmutableFixedIborSwapConvention convention, int lag) {
    assertEquals(convention.getSpotDateOffset().getDays(), lag);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "period")
  static Object[][] data_period() {
    return new Object[][] {
        {FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M, Frequency.P6M},
        {FixedIborSwapConventions.USD_FIXED_1Y_LIBOR_3M, Frequency.P12M},
        {FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_3M, Frequency.P12M},
        {FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M, Frequency.P12M},
        {FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M, Frequency.P12M},
        {FixedIborSwapConventions.GBP_FIXED_6M_LIBOR_6M, Frequency.P6M},
        {FixedIborSwapConventions.GBP_FIXED_3M_LIBOR_3M, Frequency.P3M},
        {FixedIborSwapConventions.JPY_FIXED_6M_TIBORJ_3M, Frequency.P6M},
        {FixedIborSwapConventions.JPY_FIXED_6M_LIBOR_6M, Frequency.P6M},
        {FixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_3M, Frequency.P12M},
        {FixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_6M, Frequency.P12M}
    };
  }

  @Test(dataProvider = "period")
  public void test_period(FixedIborSwapConvention convention, Frequency frequency) {
    assertEquals(convention.getFixedLeg().getAccrualFrequency(), frequency);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "dayCount")
  static Object[][] data_day_count() {
    return new Object[][] {
        {FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M, DayCounts.THIRTY_U_360},
        {FixedIborSwapConventions.USD_FIXED_1Y_LIBOR_3M, DayCounts.ACT_360},
        {FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_3M, DayCounts.THIRTY_U_360},
        {FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M, DayCounts.THIRTY_U_360},
        {FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M, DayCounts.ACT_365F},
        {FixedIborSwapConventions.GBP_FIXED_6M_LIBOR_6M, DayCounts.ACT_365F},
        {FixedIborSwapConventions.GBP_FIXED_3M_LIBOR_3M, DayCounts.ACT_365F},
        {FixedIborSwapConventions.JPY_FIXED_6M_TIBORJ_3M, DayCounts.ACT_365F},
        {FixedIborSwapConventions.JPY_FIXED_6M_LIBOR_6M, DayCounts.ACT_365F},
        {FixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_3M, DayCounts.THIRTY_U_360},
        {FixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_6M, DayCounts.THIRTY_U_360}
    };
  }

  @Test(dataProvider = "dayCount")
  public void test_day_count(FixedIborSwapConvention convention, DayCount dayCount) {
    assertEquals(convention.getFixedLeg().getDayCount(), dayCount);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "floatLeg")
  static Object[][] data_float_leg() {
    return new Object[][] {
        {FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M, IborIndices.USD_LIBOR_3M},
        {FixedIborSwapConventions.USD_FIXED_1Y_LIBOR_3M, IborIndices.USD_LIBOR_3M},
        {FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_3M, IborIndices.EUR_EURIBOR_3M},
        {FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M, IborIndices.EUR_EURIBOR_6M},
        {FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M, IborIndices.GBP_LIBOR_3M},
        {FixedIborSwapConventions.GBP_FIXED_6M_LIBOR_6M, IborIndices.GBP_LIBOR_6M},
        {FixedIborSwapConventions.GBP_FIXED_3M_LIBOR_3M, IborIndices.GBP_LIBOR_3M},
        {FixedIborSwapConventions.JPY_FIXED_6M_TIBORJ_3M, IborIndices.JPY_TIBOR_JAPAN_3M},
        {FixedIborSwapConventions.JPY_FIXED_6M_LIBOR_6M, IborIndices.JPY_LIBOR_6M},
        {FixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_3M, IborIndices.CHF_LIBOR_3M},
        {FixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_6M, IborIndices.CHF_LIBOR_6M}
    };
  }

  @Test(dataProvider = "floatLeg")
  public void test_float_leg(FixedIborSwapConvention convention, IborIndex floatLeg) {
    assertEquals(convention.getFloatingLeg().getIndex(), floatLeg);
  }

  // For vanilla swaps the holidays calendars on the fixed leg should be
  // consistent with the maturity calendars on the floating leg
  @Test(dataProvider = "floatLeg")
  public void test_holiday_calendars_match(FixedIborSwapConvention convention, IborIndex floatLeg) {
    assertEquals(
        convention.getFixedLeg().getAccrualBusinessDayAdjustment().getCalendar(),
        floatLeg.getMaturityDateOffset().getAdjustment().getCalendar());
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "dayConvention")
  static Object[][] data_day_convention() {
    return new Object[][] {
        {FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.USD_FIXED_1Y_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.GBP_FIXED_6M_LIBOR_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.GBP_FIXED_3M_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.JPY_FIXED_6M_TIBORJ_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.JPY_FIXED_6M_LIBOR_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.CHF_FIXED_1Y_LIBOR_6M, BusinessDayConventions.MODIFIED_FOLLOWING}
    };
  }

  @Test(dataProvider = "dayConvention")
  public void test_day_convention(FixedIborSwapConvention convention, BusinessDayConvention dayConvention) {
    assertEquals(convention.getFixedLeg().getAccrualBusinessDayAdjustment().getConvention(), dayConvention);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "stubIbor")
  static Object[][] data_stub_ibor() {
    return new Object[][] {
        {FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_3M, Tenor.TENOR_18M},
        {FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M, Tenor.TENOR_18M},
        {FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M, Tenor.TENOR_18M},
        {FixedIborSwapConventions.GBP_FIXED_6M_LIBOR_6M, Tenor.TENOR_9M},
        {FixedIborSwapConventions.GBP_FIXED_3M_LIBOR_3M, Tenor.TENOR_10M},
        {FixedIborSwapConventions.JPY_FIXED_6M_TIBORJ_3M, Tenor.TENOR_9M},
        {FixedIborSwapConventions.JPY_FIXED_6M_TIBORJ_3M, Tenor.TENOR_9M},
        {FixedIborSwapConventions.USD_FIXED_1Y_LIBOR_3M, Tenor.TENOR_18M},
        {FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M, Tenor.TENOR_9M},
    };
  }

  @Test(dataProvider = "stubIbor")
  public void test_stub_ibor(FixedIborSwapConvention convention, Tenor tenor) {
    LocalDate tradeDate = LocalDate.of(2015, 10, 20);
    SwapTrade swap = convention.createTrade(tradeDate, tenor, BuySell.BUY, 1, 0.01, REF_DATA);
    ResolvedSwap swapResolved = swap.getProduct().resolve(REF_DATA);
    LocalDate endDate = swapResolved.getLeg(PayReceive.PAY).get().getEndDate();
    assertTrue(endDate.isAfter(tradeDate.plus(tenor).minusMonths(1)));
    assertTrue(endDate.isBefore(tradeDate.plus(tenor).plusMonths(1)));
  }

  public void coverage() {
    coverPrivateConstructor(FixedIborSwapConventions.class);
    coverPrivateConstructor(StandardFixedIborSwapConventions.class);
  }

}
