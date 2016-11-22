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
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link IborIborSwapConventions}.
 * <p>
 * These tests  match the table 18.1 in the following guide:
 * http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
 */
@Test
public class IborIborSwapConventionsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  @DataProvider(name = "spotLag")
  static Object[][] data_spot_lag() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, 2},
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, 2},
        {IborIborSwapConventions.JPY_LIBOR_1M_LIBOR_6M, 2},
        {IborIborSwapConventions.JPY_LIBOR_3M_LIBOR_6M, 2},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_EUROYEN_6M, 2},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_JAPAN_6M, 2},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M, 2},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M, 2},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M, 2},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M, 2},
    };
  }

  @Test(dataProvider = "spotLag")
  public void test_spot_lag(ImmutableIborIborSwapConvention convention, int lag) {
    assertEquals(convention.getSpotDateOffset().getDays(), lag);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "period")
  static Object[][] data_period() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, Frequency.P6M},
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, Frequency.P3M},
        {IborIborSwapConventions.JPY_LIBOR_1M_LIBOR_6M, Frequency.P1M},
        {IborIborSwapConventions.JPY_LIBOR_3M_LIBOR_6M, Frequency.P3M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_EUROYEN_6M, Frequency.P6M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_JAPAN_6M, Frequency.P6M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M, Frequency.P1M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M, Frequency.P3M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M, Frequency.P1M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M, Frequency.P3M}
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
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, CompoundingMethod.FLAT},
        {IborIborSwapConventions.JPY_LIBOR_1M_LIBOR_6M, CompoundingMethod.NONE},
        {IborIborSwapConventions.JPY_LIBOR_3M_LIBOR_6M, CompoundingMethod.NONE},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_EUROYEN_6M, CompoundingMethod.NONE},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_JAPAN_6M, CompoundingMethod.NONE},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M, CompoundingMethod.NONE},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M, CompoundingMethod.NONE},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M, CompoundingMethod.NONE},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M, CompoundingMethod.NONE}
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
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, IborIndices.USD_LIBOR_1M},
        {IborIborSwapConventions.JPY_LIBOR_1M_LIBOR_6M, IborIndices.JPY_LIBOR_1M},
        {IborIborSwapConventions.JPY_LIBOR_3M_LIBOR_6M, IborIndices.JPY_LIBOR_3M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_EUROYEN_6M, IborIndices.JPY_LIBOR_6M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_JAPAN_6M, IborIndices.JPY_LIBOR_6M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M, IborIndices.JPY_TIBOR_EUROYEN_1M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M, IborIndices.JPY_TIBOR_EUROYEN_3M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M, IborIndices.JPY_TIBOR_JAPAN_1M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M, IborIndices.JPY_TIBOR_JAPAN_3M}
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
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, IborIndices.USD_LIBOR_3M},
        {IborIborSwapConventions.JPY_LIBOR_1M_LIBOR_6M, IborIndices.JPY_LIBOR_6M},
        {IborIborSwapConventions.JPY_LIBOR_3M_LIBOR_6M, IborIndices.JPY_LIBOR_6M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_EUROYEN_6M, IborIndices.JPY_TIBOR_EUROYEN_6M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_JAPAN_6M, IborIndices.JPY_TIBOR_JAPAN_6M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M, IborIndices.JPY_TIBOR_EUROYEN_6M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M, IborIndices.JPY_TIBOR_EUROYEN_6M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M, IborIndices.JPY_TIBOR_JAPAN_6M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M, IborIndices.JPY_TIBOR_JAPAN_6M}
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
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_LIBOR_1M_LIBOR_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_LIBOR_3M_LIBOR_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_EUROYEN_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_JAPAN_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M, BusinessDayConventions.MODIFIED_FOLLOWING}
    };
  }

  @Test(dataProvider = "dayConvention")
  public void test_day_convention(IborIborSwapConvention convention, BusinessDayConvention dayConvention) {
    assertEquals(convention.getSpreadLeg().getAccrualBusinessDayAdjustment().getConvention(), dayConvention);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "stubIbor")
  static Object[][] data_stub_ibor() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_LIBOR_1M_LIBOR_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_LIBOR_3M_LIBOR_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_EUROYEN_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_JAPAN_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M, Tenor.TENOR_8M}
    };
  }

  @Test(dataProvider = "stubIbor")
  public void test_stub_ibor(IborIborSwapConvention convention, Tenor tenor) {
    LocalDate tradeDate = LocalDate.of(2015, 10, 20);
    SwapTrade swap = convention.createTrade(tradeDate, tenor, BuySell.BUY, 1, 0.01, REF_DATA);
    ResolvedSwap swapResolved = swap.getProduct().resolve(REF_DATA);
    LocalDate endDate = swapResolved.getLeg(PayReceive.PAY).get().getEndDate();
    assertTrue(endDate.isAfter(tradeDate.plus(tenor).minusMonths(1)));
    assertTrue(endDate.isBefore(tradeDate.plus(tenor).plusMonths(1)));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(IborIborSwapConventions.class);
    coverPrivateConstructor(StandardIborIborSwapConventions.class);
  }

}
