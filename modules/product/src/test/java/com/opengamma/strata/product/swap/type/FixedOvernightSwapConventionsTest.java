/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
 * https://developers.opengamma.com/quantitative-research/Interest-Rate-Instruments-and-Market-Conventions.pdf
 */
public class FixedOvernightSwapConventionsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public static Object[][] data_spot_lag() {
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

  @ParameterizedTest
  @MethodSource("data_spot_lag")
  public void test_spot_lag(ImmutableFixedOvernightSwapConvention convention, int lag) {
    assertThat(convention.getSpotDateOffset().getDays()).isEqualTo(lag);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_period() {
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

  @ParameterizedTest
  @MethodSource("data_period")
  public void test_accrualPeriod(FixedOvernightSwapConvention convention, Frequency frequency) {
    assertThat(convention.getFixedLeg().getAccrualFrequency()).isEqualTo(frequency);
  }

  @ParameterizedTest
  @MethodSource("data_period")
  public void test_paymentPeriod(FixedOvernightSwapConvention convention, Frequency frequency) {
    assertThat(convention.getFixedLeg().getPaymentFrequency()).isEqualTo(frequency);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_day_count() {
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

  @ParameterizedTest
  @MethodSource("data_day_count")
  public void test_day_count(FixedOvernightSwapConvention convention, DayCount dayCount) {
    assertThat(convention.getFixedLeg().getDayCount()).isEqualTo(dayCount);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_float_leg() {
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

  @ParameterizedTest
  @MethodSource("data_float_leg")
  public void test_float_leg(FixedOvernightSwapConvention convention, OvernightIndex floatLeg) {
    assertThat(convention.getFloatingLeg().getIndex()).isEqualTo(floatLeg);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_day_convention() {
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

  @ParameterizedTest
  @MethodSource("data_day_convention")
  public void test_day_convention(FixedOvernightSwapConvention convention, BusinessDayConvention dayConvention) {
    assertThat(convention.getFixedLeg().getAccrualBusinessDayAdjustment().getConvention()).isEqualTo(dayConvention);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_stub_on() {
    return new Object[][] {
        {FixedOvernightSwapConventions.USD_FIXED_1Y_FED_FUND_OIS, Tenor.TENOR_18M},
        {FixedOvernightSwapConventions.EUR_FIXED_1Y_EONIA_OIS, Tenor.TENOR_18M},
        {FixedOvernightSwapConventions.GBP_FIXED_1Y_SONIA_OIS, Tenor.TENOR_18M},
        {FixedOvernightSwapConventions.JPY_FIXED_1Y_TONAR_OIS, Tenor.TENOR_18M},
    };
  }

  @ParameterizedTest
  @MethodSource("data_stub_on")
  public void test_stub_overnight(FixedOvernightSwapConvention convention, Tenor tenor) {
    LocalDate tradeDate = LocalDate.of(2015, 10, 20);
    SwapTrade swap = convention.createTrade(tradeDate, tenor, BuySell.BUY, 1, 0.01, REF_DATA);
    ResolvedSwap swapResolved = swap.getProduct().resolve(REF_DATA);
    LocalDate endDate = swapResolved.getLeg(PayReceive.PAY).get().getEndDate();
    assertThat(endDate.isAfter(tradeDate.plus(tenor).minusMonths(1))).isTrue();
    assertThat(endDate.isBefore(tradeDate.plus(tenor).plusMonths(1))).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(FixedOvernightSwapConventions.class);
    coverPrivateConstructor(StandardFixedOvernightSwapConventions.class);
  }

}
