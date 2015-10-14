/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.swap.type;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.finance.rate.swap.CompoundingMethod;

/**
 * Test {@link FixedIborSwapConventions}.
 * <p>
 * These tests  match the table 18.1 in the following guide:
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
@Test
public class IborIborSwapConventionsTest {

  @DataProvider(name = "spotLag")
  static Object[][] data_spot_lag() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, 2},
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, 2}
    };
  }

  @Test(dataProvider = "spotLag")
  public void test_spot_lag(IborIborSwapConvention convention, int lag) {
    assertEquals(convention.getSpotDateOffset().getDays(), lag);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "period")
  static Object[][] data_period() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, Frequency.P6M},
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, Frequency.P3M}
    };
  }

  @Test(dataProvider = "period")
  public void test_period(IborIborSwapConvention convention, Frequency frequency) {
    assertEquals(convention.getSpreadLeg().getPaymentFrequency(), frequency);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "dayCount")
  static Object[][] data_day_count() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, CompoundingMethod.FLAT},
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, CompoundingMethod.FLAT}
    };
  }

  @Test(dataProvider = "dayCount")
  public void test_composition(IborIborSwapConvention convention, CompoundingMethod comp) {
    assertEquals(convention.getSpreadLeg().getCompoundingMethod(), comp);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "spreadLeg")
  static Object[][] data_spread_leg() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, IborIndices.USD_LIBOR_3M},
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, IborIndices.USD_LIBOR_1M}
    };
  }

  @Test(dataProvider = "spreadLeg")
  public void test_float_leg(IborIborSwapConvention convention, IborIndex index) {
    assertEquals(convention.getSpreadLeg().getIndex(), index);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "flatLeg")
  static Object[][] data_flat_leg() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, IborIndices.USD_LIBOR_6M},
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, IborIndices.USD_LIBOR_3M}
    };
  }

  @Test(dataProvider = "flatLeg")
  public void test_flat_leg(IborIborSwapConvention convention, IborIndex index) {
    assertEquals(convention.getFlatLeg().getIndex(), index);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "dayConvention")
  static Object[][] data_day_convention() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING}
    };
  }

  @Test(dataProvider = "dayConvention")
  public void test_day_convention(IborIborSwapConvention convention, BusinessDayConvention dayConvention) {
    assertEquals(convention.getSpreadLeg().getAccrualBusinessDayAdjustment().getConvention(), dayConvention);
  }

  public void coverage() {
    coverPrivateConstructor(FixedIborSwapConventions.class);
  }

}
