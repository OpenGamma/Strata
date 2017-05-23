/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.swap.type;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Test {@link FixedIborSwapConventions}.
 * <p>
 * These tests  match the table 18.1 in the following guide:
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
@Test
public class FixedIborSwapConventionsTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "spotLag")
  static Object[][] data_spot_lag() {
    return new Object[][]{
        {FixedIborSwapConventions.VANILLA_USD_USNY, 2},
        {FixedIborSwapConventions.VANILLA_USD_GBLO, 2},
        {FixedIborSwapConventions.VANILLA_EUR_3M, 2},
        {FixedIborSwapConventions.VANILLA_EUR_6M, 2},
        {FixedIborSwapConventions.VANILLA_GBP_3M, 0},
        {FixedIborSwapConventions.VANILLA_GBP_6M, 0},
        {FixedIborSwapConventions.VANILLA_JPY_TIBOR, 2},
        {FixedIborSwapConventions.VANILLA_JPY_LIBOR, 2},
        {FixedIborSwapConventions.VANILLA_CHF_3M, 2},
        {FixedIborSwapConventions.VANILLA_CHF_6M, 2}
    };
  }

  @Test(dataProvider = "spotLag")
  public void test_spot_lag(FixedIborSwapConvention convention, int lag) {
    assertEquals(convention.getSpotDateOffset().getDays(), lag);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "period")
  static Object[][] data_period() {
    return new Object[][]{
        {FixedIborSwapConventions.VANILLA_USD_USNY, Frequency.P6M},
        {FixedIborSwapConventions.VANILLA_USD_GBLO, Frequency.P12M},
        {FixedIborSwapConventions.VANILLA_EUR_3M, Frequency.P12M},
        {FixedIborSwapConventions.VANILLA_EUR_6M, Frequency.P12M},
        {FixedIborSwapConventions.VANILLA_GBP_3M, Frequency.P12M},
        {FixedIborSwapConventions.VANILLA_GBP_6M, Frequency.P6M},
        {FixedIborSwapConventions.VANILLA_JPY_TIBOR, Frequency.P6M},
        {FixedIborSwapConventions.VANILLA_JPY_LIBOR, Frequency.P6M},
        {FixedIborSwapConventions.VANILLA_CHF_3M, Frequency.P12M},
        {FixedIborSwapConventions.VANILLA_CHF_6M, Frequency.P12M}
    };
  }

  @Test(dataProvider = "period")
  public void test_period(FixedIborSwapConvention convention, Frequency frequency) {
    assertEquals(convention.getFixedLeg().getAccrualFrequency(), frequency);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "dayCount")
  static Object[][] data_day_count() {
    return new Object[][]{
        {FixedIborSwapConventions.VANILLA_USD_USNY, DayCounts.THIRTY_360_ISDA},
        {FixedIborSwapConventions.VANILLA_USD_GBLO, DayCounts.ACT_360},
        {FixedIborSwapConventions.VANILLA_EUR_3M, DayCounts.THIRTY_360_ISDA},
        {FixedIborSwapConventions.VANILLA_EUR_6M, DayCounts.THIRTY_360_ISDA},
        {FixedIborSwapConventions.VANILLA_GBP_3M, DayCounts.ACT_365F},
        {FixedIborSwapConventions.VANILLA_GBP_6M, DayCounts.ACT_365F},
        {FixedIborSwapConventions.VANILLA_JPY_TIBOR, DayCounts.ACT_365F},
        {FixedIborSwapConventions.VANILLA_JPY_LIBOR, DayCounts.ACT_365F},
        {FixedIborSwapConventions.VANILLA_CHF_3M, DayCounts.THIRTY_360_ISDA},
        {FixedIborSwapConventions.VANILLA_CHF_6M, DayCounts.THIRTY_360_ISDA}
    };
  }

  @Test(dataProvider = "dayCount")
  public void test_day_count(FixedIborSwapConvention convention, DayCount dayCount) {
    assertEquals(convention.getFixedLeg().getDayCount(), dayCount);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "floatLeg")
  static Object[][] data_float_leg() {
    return new Object[][]{
        {FixedIborSwapConventions.VANILLA_USD_USNY, IborIndices.USD_LIBOR_3M},
        {FixedIborSwapConventions.VANILLA_USD_GBLO, IborIndices.USD_LIBOR_3M},
        {FixedIborSwapConventions.VANILLA_EUR_3M, IborIndices.EUR_EURIBOR_3M},
        {FixedIborSwapConventions.VANILLA_EUR_6M, IborIndices.EUR_EURIBOR_6M},
        {FixedIborSwapConventions.VANILLA_GBP_3M, IborIndices.GBP_LIBOR_3M},
        {FixedIborSwapConventions.VANILLA_GBP_6M, IborIndices.GBP_LIBOR_6M},
        {FixedIborSwapConventions.VANILLA_JPY_TIBOR, IborIndices.JPY_TIBOR_JAPAN_3M},
        {FixedIborSwapConventions.VANILLA_JPY_LIBOR, IborIndices.JPY_LIBOR_6M},
        {FixedIborSwapConventions.VANILLA_CHF_3M, IborIndices.CHF_LIBOR_3M},
        {FixedIborSwapConventions.VANILLA_CHF_6M, IborIndices.CHF_LIBOR_6M}
    };
  }

  @Test(dataProvider = "floatLeg")
  public void test_float_leg(FixedIborSwapConvention convention, IborIndex floatLeg) {
    assertEquals(convention.getFloatingLeg().getIndex(), floatLeg);
  }

  /**
   * For vanilla swaps the holidays calendars on the fixed leg should be
   * consistent with the maturity calendars on the floating leg
   */
  @Test(dataProvider = "floatLeg")
  public void test_holiday_calendars_match(FixedIborSwapConvention convention, IborIndex floatLeg) {
    assertEquals(
        convention.getFixedLeg().getAccrualBusinessDayAdjustment().getCalendar(),
        floatLeg.getMaturityDateOffset().getAdjustment().getCalendar()
    );
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "dayConvention")
  static Object[][] data_day_convention() {
    return new Object[][]{
        {FixedIborSwapConventions.VANILLA_USD_USNY, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.VANILLA_USD_GBLO, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.VANILLA_EUR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.VANILLA_EUR_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.VANILLA_GBP_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.VANILLA_GBP_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.VANILLA_JPY_TIBOR, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.VANILLA_JPY_LIBOR, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.VANILLA_CHF_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedIborSwapConventions.VANILLA_CHF_6M, BusinessDayConventions.MODIFIED_FOLLOWING}
    };
  }

  @Test(dataProvider = "dayConvention")
  public void test_day_convention(FixedIborSwapConvention convention, BusinessDayConvention dayConvention) {
    assertEquals(convention.getFixedLeg().getAccrualBusinessDayAdjustment().getConvention(), dayConvention);
  }



}
