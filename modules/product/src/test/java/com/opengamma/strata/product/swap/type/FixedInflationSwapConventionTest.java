/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPIX;
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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link FixedInflationSwapConvention}.
 */
@Test
public class FixedInflationSwapConventionTest {

  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, GBLO);
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, GBLO);

  private static final String NAME = "GBP-Swap";
  private static final FixedRateSwapLegConvention FIXED =
      FixedRateSwapLegConvention.of(GBP, ACT_360, P6M, BDA_FOLLOW);
  private static final FixedRateSwapLegConvention FIXED2 =
      FixedRateSwapLegConvention.of(GBP, ACT_365F, P3M, BDA_MOD_FOLLOW);
  private static final InflationRateSwapLegConvention INFL = InflationRateSwapLegConvention.of(GB_HICP);
  private static final InflationRateSwapLegConvention INFL2 = InflationRateSwapLegConvention.of(GB_RPI);
  private static final InflationRateSwapLegConvention INFL3 = InflationRateSwapLegConvention.of(GB_RPIX);

  //-------------------------------------------------------------------------
  public void test_of() {
    ImmutableFixedInflationSwapConvention test = ImmutableFixedInflationSwapConvention.of(
        NAME, 
        FIXED, 
        INFL, 
        BDA_FOLLOW, 
        PLUS_ONE_DAY);
    assertEquals(test.getName(), NAME);
    assertEquals(test.getFixedLeg(), FIXED);
    assertEquals(test.getFloatingLeg(), INFL);
    assertEquals(test.getSpotDateOffset(), BDA_FOLLOW);
    assertEquals(test.getPaymentDateOffset(), PLUS_ONE_DAY);
  }

  public void test_of_spotDateOffset() {
    ImmutableFixedInflationSwapConvention test = ImmutableFixedInflationSwapConvention.of(
        NAME, 
        FIXED, 
        INFL, 
        BDA_FOLLOW, 
        PLUS_ONE_DAY);
    assertEquals(test.getName(), NAME);
    assertEquals(test.getFixedLeg(), FIXED);
    assertEquals(test.getFloatingLeg(), INFL);
    assertEquals(test.getSpotDateOffset(), BDA_FOLLOW);
  }

  public void test_builder() {
    ImmutableFixedInflationSwapConvention test = ImmutableFixedInflationSwapConvention.builder()
        .name(NAME)
        .fixedLeg(FIXED)
        .floatingLeg(INFL)
        .spotDateOffset(BDA_FOLLOW)
        .paymentDateOffset(PLUS_ONE_DAY)
        .build();
    assertEquals(test.getName(), NAME);
    assertEquals(test.getFixedLeg(), FIXED);
    assertEquals(test.getFloatingLeg(), INFL);
    assertEquals(test.getSpotDateOffset(), BDA_FOLLOW);
  }

  //-------------------------------------------------------------------------
  public void test_toTrade_dates() {
    ImmutableFixedInflationSwapConvention base = ImmutableFixedInflationSwapConvention.of(
        NAME, 
        FIXED, 
        INFL, 
        BDA_FOLLOW, 
        PLUS_ONE_DAY);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 5);
    LocalDate endDate = date(2015, 11, 5);
    SwapTrade test = base.toTrade(tradeDate, startDate, endDate, Period.ofMonths(3), BUY, NOTIONAL_2M, 0.25d);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        INFL.toLeg(startDate, endDate, RECEIVE, Period.ofMonths(3), BDA_FOLLOW, PLUS_ONE_DAY, NOTIONAL_2M));
    assertEquals(test.getInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {FixedInflationSwapConventions.GBP_FIXED_6M_GB_HCIP, "GBP-FIXED-6M-GB-HCIP"},
        {FixedInflationSwapConventions.USD_FIXED_6M_US_CPI, "USD-FIXED-6M-US-CPI"},
    };
  }

  @Test(dataProvider = "name")
  public void test_name(FixedInflationSwapConvention convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(FixedInflationSwapConvention convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(FixedInflationSwapConvention convention, String name) {
    assertEquals(FixedInflationSwapConvention.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(FixedInflationSwapConvention convention, String name) {
    FixedInflationSwapConvention.of(name);  // ensures map is populated
    ImmutableMap<String, FixedInflationSwapConvention> map = FixedInflationSwapConvention.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> FixedInflationSwapConvention.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> FixedInflationSwapConvention.of((String) null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableFixedInflationSwapConvention test = ImmutableFixedInflationSwapConvention.of(
        NAME, 
        FIXED, 
        INFL, 
        BDA_FOLLOW, 
        PLUS_ONE_DAY);
    coverImmutableBean(test);
    ImmutableFixedInflationSwapConvention test2 = ImmutableFixedInflationSwapConvention.of(
        NAME, 
        FIXED2, 
        INFL2, 
        BDA_FOLLOW, 
        PLUS_ONE_DAY);
    coverBeanEquals(test, test2);
    ImmutableFixedInflationSwapConvention test3 = ImmutableFixedInflationSwapConvention.of(
        NAME, 
        FIXED, 
        INFL3, 
        BDA_FOLLOW, 
        PLUS_ONE_DAY);
    coverBeanEquals(test, test3);
  }

  public void test_serialization() {
    FixedInflationSwapConvention test = ImmutableFixedInflationSwapConvention.of(
        NAME, 
        FIXED, 
        INFL, 
        BDA_FOLLOW, 
        PLUS_ONE_DAY);
    assertSerialization(test);
  }

}
