/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link FixedOvernightSwapConvention}.
 */
public class FixedOvernightSwapConventionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, GBLO);
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, GBLO);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);

  private static final String NAME = "USD-Swap";
  private static final FixedRateSwapLegConvention FIXED =
      FixedRateSwapLegConvention.of(USD, ACT_360, P6M, BDA_FOLLOW);
  private static final FixedRateSwapLegConvention FIXED2 =
      FixedRateSwapLegConvention.of(GBP, ACT_365F, P3M, BDA_MOD_FOLLOW);
  private static final OvernightRateSwapLegConvention FFUND_LEG =
      OvernightRateSwapLegConvention.of(USD_FED_FUND, P12M, 2);
  private static final OvernightRateSwapLegConvention FFUND_LEG2 =
      OvernightRateSwapLegConvention.of(USD_FED_FUND, P12M, 3);
  private static final OvernightRateSwapLegConvention FLOATING_LEG2 =
      OvernightRateSwapLegConvention.of(GBP_SONIA, P12M, 0);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    ImmutableFixedOvernightSwapConvention test =
        ImmutableFixedOvernightSwapConvention.of(NAME, FIXED, FFUND_LEG, PLUS_TWO_DAYS);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getFixedLeg()).isEqualTo(FIXED);
    assertThat(test.getFloatingLeg()).isEqualTo(FFUND_LEG);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_TWO_DAYS);
  }

  @Test
  public void test_builder() {
    ImmutableFixedOvernightSwapConvention test = ImmutableFixedOvernightSwapConvention.builder()
        .name(NAME)
        .fixedLeg(FIXED)
        .floatingLeg(FFUND_LEG)
        .spotDateOffset(PLUS_ONE_DAY)
        .build();
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getFixedLeg()).isEqualTo(FIXED);
    assertThat(test.getFloatingLeg()).isEqualTo(FFUND_LEG);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_ONE_DAY);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toTrade_tenor() {
    FixedOvernightSwapConvention base = ImmutableFixedOvernightSwapConvention.of(NAME, FIXED, FFUND_LEG, PLUS_TWO_DAYS);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 5, 7);
    LocalDate endDate = date(2025, 5, 7);
    SwapTrade test = base.createTrade(tradeDate, TENOR_10Y, BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        FFUND_LEG.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_toTrade_periodTenor() {
    FixedOvernightSwapConvention base = ImmutableFixedOvernightSwapConvention.of(NAME, FIXED, FFUND_LEG, PLUS_TWO_DAYS);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 7);
    LocalDate endDate = date(2025, 8, 7);
    SwapTrade test = base.createTrade(tradeDate, Period.ofMonths(3), TENOR_10Y, BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        FFUND_LEG.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_toTrade_dates() {
    FixedOvernightSwapConvention base = ImmutableFixedOvernightSwapConvention.of(NAME, FIXED, FFUND_LEG, PLUS_TWO_DAYS);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 5);
    LocalDate endDate = date(2015, 11, 5);
    SwapTrade test = base.toTrade(tradeDate, startDate, endDate, BUY, NOTIONAL_2M, 0.25d);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        FFUND_LEG.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {FixedOvernightSwapConventions.USD_FIXED_1Y_FED_FUND_OIS, "USD-FIXED-1Y-FED-FUND-OIS"},
        {FixedOvernightSwapConventions.USD_FIXED_TERM_FED_FUND_OIS, "USD-FIXED-TERM-FED-FUND-OIS"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(FixedOvernightSwapConvention convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(FixedOvernightSwapConvention convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(FixedOvernightSwapConvention convention, String name) {
    assertThat(FixedOvernightSwapConvention.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(FixedOvernightSwapConvention convention, String name) {
    FixedOvernightSwapConvention.of(name);  // ensures map is populated
    ImmutableMap<String, FixedOvernightSwapConvention> map = FixedOvernightSwapConvention.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedOvernightSwapConvention.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedOvernightSwapConvention.of((String) null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ImmutableFixedOvernightSwapConvention test = ImmutableFixedOvernightSwapConvention.of(
        NAME, FIXED, FFUND_LEG, PLUS_TWO_DAYS);
    coverImmutableBean(test);
    ImmutableFixedOvernightSwapConvention test2 = ImmutableFixedOvernightSwapConvention.of(
        "GBP-Swap", FIXED2, FLOATING_LEG2, PLUS_ONE_DAY);
    coverBeanEquals(test, test2);
    ImmutableFixedOvernightSwapConvention test3 = ImmutableFixedOvernightSwapConvention.of(
        "USD-Swap2", FIXED, FFUND_LEG2, PLUS_ONE_DAY);
    coverBeanEquals(test, test3);
  }

  @Test
  public void test_serialization() {
    ImmutableFixedOvernightSwapConvention test = ImmutableFixedOvernightSwapConvention.of(
        NAME, FIXED, FFUND_LEG, PLUS_TWO_DAYS);
    assertSerialization(test);
  }

}
