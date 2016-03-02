/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * Test {@link XCcyIborIborSwapConventions}.
 */
@Test
public class XCcyIborIborSwapConventionsTest {

  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);
  private static final HolidayCalendarId GBLO_USNY = GBLO.combinedWith(USNY);

  @DataProvider(name = "spotLag")
  static Object[][] data_spot_lag() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, 2},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, 2}
    };
  }

  @Test(dataProvider = "spotLag")
  public void test_spot_lag(ImmutableXCcyIborIborSwapConvention convention, int lag) {
    assertEquals(convention.getSpotDateOffset().getDays(), lag);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "period")
  static Object[][] data_period() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, Frequency.P3M},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, Frequency.P3M}
    };
  }

  @Test(dataProvider = "period")
  public void test_period(XCcyIborIborSwapConvention convention, Frequency frequency) {
    assertEquals(convention.getSpreadLeg().getPaymentFrequency(), frequency);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "spreadLegIndex")
  static Object[][] data_spread_leg() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, IborIndices.EUR_EURIBOR_3M},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, IborIndices.GBP_LIBOR_3M}
    };
  }

  @Test(dataProvider = "spreadLegIndex")
  public void test_float_leg(XCcyIborIborSwapConvention convention, IborIndex index) {
    assertEquals(convention.getSpreadLeg().getIndex(), index);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "spreadLegBda")
  static Object[][] data_spread_leg_bda() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY)},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY)}
    };
  }

  @Test(dataProvider = "spreadLegBda")
  public void test_spread_leg_bdc(XCcyIborIborSwapConvention convention, BusinessDayAdjustment bda) {
    assertEquals(convention.getSpreadLeg().getAccrualBusinessDayAdjustment(), bda);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "flatLegIndex")
  static Object[][] data_flat_leg() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, IborIndices.USD_LIBOR_3M},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, IborIndices.USD_LIBOR_3M}
    };
  }

  @Test(dataProvider = "flatLegIndex")
  public void test_flat_leg(XCcyIborIborSwapConvention convention, IborIndex index) {
    assertEquals(convention.getFlatLeg().getIndex(), index);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "flatLegBda")
  static Object[][] data_flat_leg_bda() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY)},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY)}
    };
  }

  @Test(dataProvider = "flatLegBda")
  public void test_flat_leg_bdc(XCcyIborIborSwapConvention convention, BusinessDayAdjustment bda) {
    assertEquals(convention.getFlatLeg().getAccrualBusinessDayAdjustment(), bda);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "dayConvention")
  static Object[][] data_day_convention() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING}
    };
  }

  @Test(dataProvider = "dayConvention")
  public void test_day_convention(XCcyIborIborSwapConvention convention, BusinessDayConvention dayConvention) {
    assertEquals(convention.getSpreadLeg().getAccrualBusinessDayAdjustment().getConvention(), dayConvention);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "notionalExchange")
  static Object[][] data_notional_exchange() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, true},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, true}
    };
  }

  @Test(dataProvider = "notionalExchange")
  public void test_notional_exchange(XCcyIborIborSwapConvention convention, boolean notionalExchange) {
    assertEquals(convention.getSpreadLeg().isNotionalExchange(), notionalExchange);
    assertEquals(convention.getFlatLeg().isNotionalExchange(), notionalExchange);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(XCcyIborIborSwapConventions.class);
    coverPrivateConstructor(StandardXCcyIborIborSwapConventions.class);
  }

}
