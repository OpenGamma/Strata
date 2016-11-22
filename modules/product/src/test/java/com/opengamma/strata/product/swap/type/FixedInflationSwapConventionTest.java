/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.ONE_ONE;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPIX;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link FixedInflationSwapConvention}.
 */
@Test
public class FixedInflationSwapConventionTest {

  private static final Period LAG_3M = Period.ofMonths(3);
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, GBLO);

  private static final String NAME = "GBP-Swap";
  private static final FixedRateSwapLegConvention FIXED = fixedLegZcConvention(GBP, GBLO);
  private static final FixedRateSwapLegConvention FIXED2 =
      FixedRateSwapLegConvention.of(GBP, ACT_365F, P3M, BDA_MOD_FOLLOW);
  private static final InflationRateSwapLegConvention INFL = InflationRateSwapLegConvention.of(GB_HICP, LAG_3M, BDA_MOD_FOLLOW);
  private static final InflationRateSwapLegConvention INFL2 = InflationRateSwapLegConvention.of(GB_RPI, LAG_3M, BDA_MOD_FOLLOW);
  private static final InflationRateSwapLegConvention INFL3 = InflationRateSwapLegConvention.of(GB_RPIX, LAG_3M, BDA_MOD_FOLLOW);

  //-------------------------------------------------------------------------
  public void test_of() {
    ImmutableFixedInflationSwapConvention test = ImmutableFixedInflationSwapConvention.of(
        NAME,
        FIXED,
        INFL,
        PLUS_ONE_DAY);
    assertEquals(test.getName(), NAME);
    assertEquals(test.getFixedLeg(), FIXED);
    assertEquals(test.getFloatingLeg(), INFL);
    assertEquals(test.getSpotDateOffset(), PLUS_ONE_DAY);
  }

  public void test_of_spotDateOffset() {
    ImmutableFixedInflationSwapConvention test = ImmutableFixedInflationSwapConvention.of(
        NAME,
        FIXED,
        INFL,
        PLUS_ONE_DAY);
    assertEquals(test.getName(), NAME);
    assertEquals(test.getFixedLeg(), FIXED);
    assertEquals(test.getFloatingLeg(), INFL);
    assertEquals(test.getSpotDateOffset(), PLUS_ONE_DAY);
  }

  public void test_builder() {
    ImmutableFixedInflationSwapConvention test = ImmutableFixedInflationSwapConvention.builder()
        .name(NAME)
        .fixedLeg(FIXED)
        .floatingLeg(INFL)
        .spotDateOffset(PLUS_ONE_DAY)
        .build();
    assertEquals(test.getName(), NAME);
    assertEquals(test.getFixedLeg(), FIXED);
    assertEquals(test.getFloatingLeg(), INFL);
    assertEquals(test.getSpotDateOffset(), PLUS_ONE_DAY);
  }

  //-------------------------------------------------------------------------
  public void test_toTrade_dates() {
    ImmutableFixedInflationSwapConvention base = ImmutableFixedInflationSwapConvention.of(
        NAME,
        FIXED,
        INFL,
        PLUS_ONE_DAY);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 5);
    LocalDate endDate = date(2017, 8, 5);
    SwapTrade test = base.toTrade(tradeDate, startDate, endDate, BUY, NOTIONAL_2M, 0.25d);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        INFL.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertEquals(test.getInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {FixedInflationSwapConventions.GBP_FIXED_ZC_GB_HCIP, "GBP-FIXED-ZC-GB-HCIP"},
        {FixedInflationSwapConventions.USD_FIXED_ZC_US_CPI, "USD-FIXED-ZC-US-CPI"},
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
        PLUS_ONE_DAY);
    coverImmutableBean(test);
    ImmutableFixedInflationSwapConvention test2 = ImmutableFixedInflationSwapConvention.of(
        NAME,
        FIXED2,
        INFL2,
        PLUS_ONE_DAY);
    coverBeanEquals(test, test2);
    ImmutableFixedInflationSwapConvention test3 = ImmutableFixedInflationSwapConvention.of(
        NAME,
        FIXED,
        INFL3,
        PLUS_ONE_DAY);
    coverBeanEquals(test, test3);
  }

  public void test_serialization() {
    FixedInflationSwapConvention test = ImmutableFixedInflationSwapConvention.of(
        NAME,
        FIXED,
        INFL,
        PLUS_ONE_DAY);
    assertSerialization(test);
  }

  // Create a zero-coupon fixed leg convention
  private static FixedRateSwapLegConvention fixedLegZcConvention(Currency ccy, HolidayCalendarId cal) {
    return FixedRateSwapLegConvention.builder()
        .paymentFrequency(Frequency.TERM)
        .accrualFrequency(Frequency.P12M)
        .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, cal))
        .startDateBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, cal))
        .endDateBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, cal))
        .compoundingMethod(CompoundingMethod.STRAIGHT)
        .dayCount(ONE_ONE)
        .currency(ccy)
        .build();
  }

}
