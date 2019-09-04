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
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2Y;
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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link FixedOvernightSwapTemplate}.
 */
public class FixedOvernightSwapTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, GBLO);
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, GBLO);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, USNY);

  private static final FixedRateSwapLegConvention FIXED =
      FixedRateSwapLegConvention.of(USD, ACT_360, P6M, BDA_FOLLOW);
  private static final FixedRateSwapLegConvention FIXED2 =
      FixedRateSwapLegConvention.of(GBP, ACT_365F, P3M, BDA_MOD_FOLLOW);
  private static final OvernightRateSwapLegConvention FFUND_LEG =
      OvernightRateSwapLegConvention.of(USD_FED_FUND, P12M, 2);
  private static final OvernightRateSwapLegConvention FFUND_LEG2 =
      OvernightRateSwapLegConvention.of(GBP_SONIA, P12M, 0);
  private static final FixedOvernightSwapConvention CONV = ImmutableFixedOvernightSwapConvention.of(
      "USD-Swap", FIXED, FFUND_LEG, PLUS_TWO_DAYS);
  private static final FixedOvernightSwapConvention CONV2 = ImmutableFixedOvernightSwapConvention.of(
      "GBP-Swap", FIXED2, FFUND_LEG2, PLUS_ONE_DAY);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    FixedOvernightSwapTemplate test = FixedOvernightSwapTemplate.of(TENOR_10Y, CONV);
    assertThat(test.getPeriodToStart()).isEqualTo(Period.ZERO);
    assertThat(test.getTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getConvention()).isEqualTo(CONV);
  }

  @Test
  public void test_of_period() {
    FixedOvernightSwapTemplate test = FixedOvernightSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    assertThat(test.getPeriodToStart()).isEqualTo(Period.ofMonths(3));
    assertThat(test.getTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getConvention()).isEqualTo(CONV);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_notEnoughData() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedOvernightSwapTemplate.builder()
            .tenor(TENOR_2Y)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade() {
    FixedOvernightSwapTemplate base = FixedOvernightSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 7);
    LocalDate endDate = date(2025, 8, 7);
    SwapTrade test = base.createTrade(tradeDate, BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        FFUND_LEG.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FixedOvernightSwapTemplate test = FixedOvernightSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    coverImmutableBean(test);
    FixedOvernightSwapTemplate test2 = FixedOvernightSwapTemplate.of(Period.ofMonths(2), TENOR_2Y, CONV2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    FixedOvernightSwapTemplate test = FixedOvernightSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    assertSerialization(test);
  }

}
