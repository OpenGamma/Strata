/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link OvernightOvernightSwapConventions}.
 */
public class OvernightOvernightSwapConventionsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public static Object[][] data_spot_lag() {
    return new Object[][] {
        {OvernightOvernightSwapConventions.USD_SOFR_3M_FED_FUND_3M, 2},
    };
  }

  @ParameterizedTest
  @MethodSource("data_spot_lag")
  public void test_spot_lag(OvernightOvernightSwapConvention convention, int lag) {
    assertThat(convention.getSpotDateOffset().getDays()).isEqualTo(lag);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_period() {
    return new Object[][] {
        {OvernightOvernightSwapConventions.USD_SOFR_3M_FED_FUND_3M, Frequency.P3M}
    };
  }

  @ParameterizedTest
  @MethodSource("data_period")
  public void test_accrualPeriod_on(OvernightOvernightSwapConvention convention, Frequency frequency) {
    assertThat(convention.getSpreadLeg().getAccrualFrequency()).isEqualTo(frequency);
    assertThat(convention.getFlatLeg().getAccrualFrequency()).isEqualTo(frequency);
  }

  @ParameterizedTest
  @MethodSource("data_period")
  public void test_paymentPeriod_on(OvernightOvernightSwapConvention convention, Frequency frequency) {
    assertThat(convention.getSpreadLeg().getPaymentFrequency()).isEqualTo(frequency);
    assertThat(convention.getFlatLeg().getPaymentFrequency()).isEqualTo(frequency);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_day_count() {
    return new Object[][] {
        {OvernightOvernightSwapConventions.USD_SOFR_3M_FED_FUND_3M, DayCounts.ACT_360},
    };
  }

  @ParameterizedTest
  @MethodSource("data_day_count")
  public void test_day_count(OvernightOvernightSwapConvention convention, DayCount dayCount) {
    assertThat(convention.getSpreadLeg().getDayCount()).isEqualTo(dayCount);
    assertThat(convention.getFlatLeg().getDayCount()).isEqualTo(dayCount);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_float_index() {
    return new Object[][] {
        {OvernightOvernightSwapConventions.USD_SOFR_3M_FED_FUND_3M, OvernightIndices.USD_SOFR, OvernightIndices.USD_FED_FUND},
    };
  }

  @ParameterizedTest
  @MethodSource("data_float_index")
  public void test_float_leg(
      OvernightOvernightSwapConvention convention,
      OvernightIndex spreadLegIndex,
      OvernightIndex flatLegIndex) {

    assertThat(convention.getSpreadLeg().getIndex()).isEqualTo(spreadLegIndex);
    assertThat(convention.getFlatLeg().getIndex()).isEqualTo(flatLegIndex);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_day_convention() {
    return new Object[][] {
        {OvernightOvernightSwapConventions.USD_SOFR_3M_FED_FUND_3M, BusinessDayConventions.MODIFIED_FOLLOWING}
    };
  }

  @ParameterizedTest
  @MethodSource("data_day_convention")
  public void test_day_convention(OvernightOvernightSwapConvention convention, BusinessDayConvention dayConvention) {
    assertThat(convention.getSpreadLeg().getAccrualBusinessDayAdjustment().getConvention()).isEqualTo(dayConvention);
    assertThat(convention.getFlatLeg().getAccrualBusinessDayAdjustment().getConvention()).isEqualTo(dayConvention);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_stub_on() {
    return new Object[][] {
        {OvernightOvernightSwapConventions.USD_SOFR_3M_FED_FUND_3M, Tenor.TENOR_4M}
    };
  }

  @ParameterizedTest
  @MethodSource("data_stub_on")
  public void test_stub_overnight(OvernightOvernightSwapConvention convention, Tenor tenor) {
    LocalDate tradeDate = LocalDate.of(2015, 10, 20);
    SwapTrade swap = convention.createTrade(tradeDate, tenor, BuySell.BUY, 1, 0.01, REF_DATA);
    ResolvedSwap swapResolved = swap.getProduct().resolve(REF_DATA);
    LocalDate endDate = swapResolved.getLeg(PayReceive.PAY).get().getEndDate();
    assertThat(endDate.isAfter(tradeDate.plus(tenor).minusDays(7))).isTrue();
    assertThat(endDate.isBefore(tradeDate.plus(tenor).plusDays(7))).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(OvernightOvernightSwapConventions.class);
    coverPrivateConstructor(StandardOvernightOvernightSwapConventions.class);
  }
}
