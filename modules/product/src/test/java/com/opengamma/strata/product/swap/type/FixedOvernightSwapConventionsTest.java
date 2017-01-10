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
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link FixedOvernightSwapConventions}.
 * <p>
 * These tests  match the table 18.1 in the following guide:
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
@Test
public class FixedOvernightSwapConventionsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  @DataProvider(name = "spotLag")
  static Object[][] data_spot_lag() {
    return new Object[][] {
        {FixedOvernightSwapConventions.USD_FIXED_TERM_FED_FUND_OIS, 2},
        {FixedOvernightSwapConventions.USD_FIXED_1Y_FED_FUND_OIS, 2},
        {FixedOvernightSwapConventions.EUR_FIXED_TERM_EONIA_OIS, 2},
        {FixedOvernightSwapConventions.EUR_FIXED_1Y_EONIA_OIS, 2},
        {FixedOvernightSwapConventions.GBP_FIXED_TERM_SONIA_OIS, 0},
        {FixedOvernightSwapConventions.GBP_FIXED_1Y_SONIA_OIS, 0},
        {FixedOvernightSwapConventions.JPY_FIXED_TERM_TONAR_OIS, 0},
        {FixedOvernightSwapConventions.JPY_FIXED_1Y_TONAR_OIS, 2},
    };
  }

  @Test(dataProvider = "spotLag")
  public void test_spot_lag(ImmutableFixedOvernightSwapConvention convention, int lag) {
    assertEquals(convention.getSpotDateOffset().getDays(), lag);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "period")
  static Object[][] data_period() {
    return new Object[][] {
        {FixedOvernightSwapConventions.USD_FIXED_TERM_FED_FUND_OIS, Frequency.TERM},
        {FixedOvernightSwapConventions.USD_FIXED_1Y_FED_FUND_OIS, Frequency.P12M},
        {FixedOvernightSwapConventions.EUR_FIXED_TERM_EONIA_OIS, Frequency.TERM},
        {FixedOvernightSwapConventions.EUR_FIXED_1Y_EONIA_OIS, Frequency.P12M},
        {FixedOvernightSwapConventions.GBP_FIXED_TERM_SONIA_OIS, Frequency.TERM},
        {FixedOvernightSwapConventions.GBP_FIXED_1Y_SONIA_OIS, Frequency.P12M},
        {FixedOvernightSwapConventions.JPY_FIXED_TERM_TONAR_OIS, Frequency.TERM},
        {FixedOvernightSwapConventions.JPY_FIXED_1Y_TONAR_OIS, Frequency.P12M},
    };
  }

  @Test(dataProvider = "period")
  public void test_accrualPeriod(FixedOvernightSwapConvention convention, Frequency frequency) {
    assertEquals(convention.getFixedLeg().getAccrualFrequency(), frequency);
  }

  @Test(dataProvider = "period")
  public void test_paymentPeriod(FixedOvernightSwapConvention convention, Frequency frequency) {
    assertEquals(convention.getFixedLeg().getPaymentFrequency(), frequency);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "dayCount")
  static Object[][] data_day_count() {
    return new Object[][] {
        {FixedOvernightSwapConventions.USD_FIXED_TERM_FED_FUND_OIS, DayCounts.ACT_360},
        {FixedOvernightSwapConventions.USD_FIXED_1Y_FED_FUND_OIS, DayCounts.ACT_360},
        {FixedOvernightSwapConventions.EUR_FIXED_TERM_EONIA_OIS, DayCounts.ACT_360},
        {FixedOvernightSwapConventions.EUR_FIXED_1Y_EONIA_OIS, DayCounts.ACT_360},
        {FixedOvernightSwapConventions.GBP_FIXED_TERM_SONIA_OIS, DayCounts.ACT_365F},
        {FixedOvernightSwapConventions.GBP_FIXED_1Y_SONIA_OIS, DayCounts.ACT_365F},
        {FixedOvernightSwapConventions.JPY_FIXED_TERM_TONAR_OIS, DayCounts.ACT_365F},
        {FixedOvernightSwapConventions.JPY_FIXED_1Y_TONAR_OIS, DayCounts.ACT_365F},
    };
  }

  @Test(dataProvider = "dayCount")
  public void test_day_count(FixedOvernightSwapConvention convention, DayCount dayCount) {
    assertEquals(convention.getFixedLeg().getDayCount(), dayCount);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "floatLeg")
  static Object[][] data_float_leg() {
    return new Object[][] {
        {FixedOvernightSwapConventions.USD_FIXED_TERM_FED_FUND_OIS, OvernightIndices.USD_FED_FUND},
        {FixedOvernightSwapConventions.USD_FIXED_1Y_FED_FUND_OIS, OvernightIndices.USD_FED_FUND},
        {FixedOvernightSwapConventions.EUR_FIXED_TERM_EONIA_OIS, OvernightIndices.EUR_EONIA},
        {FixedOvernightSwapConventions.EUR_FIXED_1Y_EONIA_OIS, OvernightIndices.EUR_EONIA},
        {FixedOvernightSwapConventions.GBP_FIXED_TERM_SONIA_OIS, OvernightIndices.GBP_SONIA},
        {FixedOvernightSwapConventions.GBP_FIXED_1Y_SONIA_OIS, OvernightIndices.GBP_SONIA},
        {FixedOvernightSwapConventions.JPY_FIXED_TERM_TONAR_OIS, OvernightIndices.JPY_TONAR},
        {FixedOvernightSwapConventions.JPY_FIXED_1Y_TONAR_OIS, OvernightIndices.JPY_TONAR},
    };
  }

  @Test(dataProvider = "floatLeg")
  public void test_float_leg(FixedOvernightSwapConvention convention, OvernightIndex floatLeg) {
    assertEquals(convention.getFloatingLeg().getIndex(), floatLeg);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "dayConvention")
  static Object[][] data_day_convention() {
    return new Object[][] {
        {FixedOvernightSwapConventions.USD_FIXED_TERM_FED_FUND_OIS, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedOvernightSwapConventions.USD_FIXED_1Y_FED_FUND_OIS, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedOvernightSwapConventions.EUR_FIXED_TERM_EONIA_OIS, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedOvernightSwapConventions.EUR_FIXED_1Y_EONIA_OIS, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedOvernightSwapConventions.GBP_FIXED_TERM_SONIA_OIS, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedOvernightSwapConventions.GBP_FIXED_1Y_SONIA_OIS, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedOvernightSwapConventions.JPY_FIXED_TERM_TONAR_OIS, BusinessDayConventions.MODIFIED_FOLLOWING},
        {FixedOvernightSwapConventions.JPY_FIXED_1Y_TONAR_OIS, BusinessDayConventions.MODIFIED_FOLLOWING},
    };
  }

  @Test(dataProvider = "dayConvention")
  public void test_day_convention(FixedOvernightSwapConvention convention, BusinessDayConvention dayConvention) {
    assertEquals(convention.getFixedLeg().getAccrualBusinessDayAdjustment().getConvention(), dayConvention);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "stubOn")
  static Object[][] data_stub_on() {
    return new Object[][] {
        {FixedOvernightSwapConventions.USD_FIXED_1Y_FED_FUND_OIS, Tenor.TENOR_18M},
        {FixedOvernightSwapConventions.EUR_FIXED_1Y_EONIA_OIS, Tenor.TENOR_18M},
        {FixedOvernightSwapConventions.GBP_FIXED_1Y_SONIA_OIS, Tenor.TENOR_18M},
        {FixedOvernightSwapConventions.JPY_FIXED_1Y_TONAR_OIS, Tenor.TENOR_18M},
    };
  }

  @Test(dataProvider = "stubOn")
  public void test_stub_overnight(FixedOvernightSwapConvention convention, Tenor tenor) {
    LocalDate tradeDate = LocalDate.of(2015, 10, 20);
    SwapTrade swap = convention.createTrade(tradeDate, tenor, BuySell.BUY, 1, 0.01, REF_DATA);
    ResolvedSwap swapResolved = swap.getProduct().resolve(REF_DATA);
    LocalDate endDate = swapResolved.getLeg(PayReceive.PAY).get().getEndDate();
    assertTrue(endDate.isAfter(tradeDate.plus(tenor).minusMonths(1)));
    assertTrue(endDate.isBefore(tradeDate.plus(tenor).plusMonths(1)));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FixedOvernightSwapConventions.class);
    coverPrivateConstructor(StandardFixedOvernightSwapConventions.class);
  }

}
