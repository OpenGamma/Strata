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

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link OvernightIborSwapConventions}.
 */
@Test
public class OvernightIborSwapConventionsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  @DataProvider(name = "spotLag")
  static Object[][] data_spot_lag() {
    return new Object[][] {
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, 2},
        {OvernightIborSwapConventions.GBP_SONIA_OIS_LIBOR_3M, 0},
    };
  }

  @Test(dataProvider = "spotLag")
  public void test_spot_lag(ImmutableOvernightIborSwapConvention convention, int lag) {
    assertEquals(convention.getSpotDateOffset().getDays(), lag);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "period")
  static Object[][] data_period() {
    return new Object[][] {
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, Frequency.P3M},
        {OvernightIborSwapConventions.GBP_SONIA_OIS_LIBOR_3M, Frequency.P3M},
    };
  }

  @Test(dataProvider = "period")
  public void test_accrualPeriod(OvernightIborSwapConvention convention, Frequency frequency) {
    assertEquals(convention.getOnLeg().getAccrualFrequency(), frequency);
    assertEquals(convention.getIborLeg().getAccrualFrequency(), frequency);
  }

  @Test(dataProvider = "period")
  public void test_paymentPeriod(OvernightIborSwapConvention convention, Frequency frequency) {
    assertEquals(convention.getOnLeg().getPaymentFrequency(), frequency);
    assertEquals(convention.getIborLeg().getPaymentFrequency(), frequency);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "dayCount")
  static Object[][] data_day_count() {
    return new Object[][] {
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, DayCounts.ACT_360},
        {OvernightIborSwapConventions.GBP_SONIA_OIS_LIBOR_3M, DayCounts.ACT_365F},
    };
  }

  @Test(dataProvider = "dayCount")
  public void test_day_count(OvernightIborSwapConvention convention, DayCount dayCount) {
    assertEquals(convention.getOnLeg().getDayCount(), dayCount);
    assertEquals(convention.getIborLeg().getDayCount(), dayCount);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "floatLeg")
  static Object[][] data_float_leg() {
    return new Object[][] {
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, OvernightIndices.USD_FED_FUND},
        {OvernightIborSwapConventions.GBP_SONIA_OIS_LIBOR_3M, OvernightIndices.GBP_SONIA},
    };
  }

  @Test(dataProvider = "floatLeg")
  public void test_float_leg(OvernightIborSwapConvention convention, OvernightIndex floatLeg) {
    assertEquals(convention.getOnLeg().getIndex(), floatLeg);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "dayConvention")
  static Object[][] data_day_convention() {
    return new Object[][] {
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {OvernightIborSwapConventions.GBP_SONIA_OIS_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
    };
  }

  @Test(dataProvider = "dayConvention")
  public void test_day_convention(OvernightIborSwapConvention convention, BusinessDayConvention dayConvention) {
    assertEquals(convention.getOnLeg().getAccrualBusinessDayAdjustment().getConvention(), dayConvention);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "stubOn")
  static Object[][] data_stub_on() {
    return new Object[][] {
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, Tenor.TENOR_4M},
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, Tenor.TENOR_4M},
    };
  }
  
  @Test(dataProvider = "stubOn")
  public void test_stub_overnight(OvernightIborSwapConvention convention, Tenor tenor) {
    LocalDate tradeDate = LocalDate.of(2015, 10, 20);
    SwapTrade swap = convention.createTrade(tradeDate, tenor, BuySell.BUY, 1, 0.01, REF_DATA);
    ResolvedSwap swapResolved = swap.getProduct().resolve(REF_DATA);
    LocalDate endDate = swapResolved.getLeg(PayReceive.PAY).get().getEndDate();
    assertTrue(endDate.isAfter(tradeDate.plus(tenor).minusDays(7)));
    assertTrue(endDate.isBefore(tradeDate.plus(tenor).plusDays(7)));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(OvernightIborSwapConventions.class);
    coverPrivateConstructor(StandardOvernightIborSwapConventions.class);
  }

}
