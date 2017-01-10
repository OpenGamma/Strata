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
 * Test {@link ThreeLegBasisSwapConventions}.
 */
@Test
public class ThreeLegBasisSwapConventionsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  @DataProvider(name = "spotLag")
  static Object[][] data_spot_lag() {
    return new Object[][] {
        {ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M, 2}
    };
  }

  @Test(dataProvider = "spotLag")
  public void test_spot_lag(ImmutableThreeLegBasisSwapConvention convention, int lag) {
    assertEquals(convention.getSpotDateOffset().getDays(), lag);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "period")
  static Object[][] data_period() {
    return new Object[][] {
        {ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M, Frequency.P3M},
    };
  }

  @Test(dataProvider = "period")
  public void test_period(ThreeLegBasisSwapConvention convention, Frequency frequency) {
    assertEquals(convention.getSpreadFloatingLeg().getPaymentFrequency(), frequency);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "dayCount")
  static Object[][] data_day_count() {
    return new Object[][] {
        {ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M, CompoundingMethod.NONE}
    };
  }

  @Test(dataProvider = "dayCount")
  public void test_composition(ThreeLegBasisSwapConvention convention, CompoundingMethod comp) {
    assertEquals(convention.getSpreadLeg().getCompoundingMethod(), comp);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "spreadFloatingLeg")
  static Object[][] data_spread_floating_leg() {
    return new Object[][] {
        {ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M, IborIndices.EUR_EURIBOR_3M}
    };
  }

  @Test(dataProvider = "spreadFloatingLeg")
  public void test_spread_floating_leg(ThreeLegBasisSwapConvention convention, IborIndex index) {
    assertEquals(convention.getSpreadFloatingLeg().getIndex(), index);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "flatFloatingLeg")
  static Object[][] data_flat_floating_leg() {
    return new Object[][] {
        {ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M, IborIndices.EUR_EURIBOR_6M}
    };
  }

  @Test(dataProvider = "flatFloatingLeg")
  public void test_flat_floating_leg(ThreeLegBasisSwapConvention convention, IborIndex index) {
    assertEquals(convention.getFlatFloatingLeg().getIndex(), index);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "dayConvention")
  static Object[][] data_day_convention() {
    return new Object[][] {
        {ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M, BusinessDayConventions.MODIFIED_FOLLOWING}
    };
  }

  @Test(dataProvider = "dayConvention")
  public void test_day_convention(ThreeLegBasisSwapConvention convention, BusinessDayConvention dayConvention) {
    assertEquals(convention.getSpreadLeg().getAccrualBusinessDayAdjustment().getConvention(), dayConvention);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "stubIbor")
  static Object[][] data_stub_ibor() {
    return new Object[][] {
        {ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M, Tenor.TENOR_8M}
    };
  }

  @Test(dataProvider = "stubIbor")
  public void test_stub_ibor(ThreeLegBasisSwapConvention convention, Tenor tenor) {
    LocalDate tradeDate = LocalDate.of(2015, 10, 20);
    SwapTrade swap = convention.createTrade(tradeDate, tenor, BuySell.BUY, 1, 0.01, REF_DATA);
    ResolvedSwap swapResolved = swap.getProduct().resolve(REF_DATA);
    LocalDate endDate = swapResolved.getLeg(PayReceive.PAY).get().getEndDate();
    assertTrue(endDate.isAfter(tradeDate.plus(tenor).minusMonths(1)));
    assertTrue(endDate.isBefore(tradeDate.plus(tenor).plusMonths(1)));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(ThreeLegBasisSwapConventions.class);
    coverPrivateConstructor(StandardThreeLegBasisSwapConventions.class);
  }

}
