/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link OvernightIborSwapConventions}.
 */
public class OvernightIborSwapConventionsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public static Object[][] data_spot_lag() {
    return new Object[][] {
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, 2},
        {OvernightIborSwapConventions.GBP_SONIA_OIS_1Y_LIBOR_3M, 0},
    };
  }

  @ParameterizedTest
  @MethodSource("data_spot_lag")
  public void test_spot_lag(ImmutableOvernightIborSwapConvention convention, int lag) {
    assertThat(convention.getSpotDateOffset().getDays()).isEqualTo(lag);
    assertThat(convention.getSpotDateOffset()).isEqualTo(convention.getIborLeg().getIndex().getEffectiveDateOffset());
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_period_on() {
    return new Object[][] {
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, Frequency.P3M},
        {OvernightIborSwapConventions.GBP_SONIA_OIS_1Y_LIBOR_3M, Frequency.P12M},
    };
  }

  @ParameterizedTest
  @MethodSource("data_period_on")
  public void test_accrualPeriod_on(OvernightIborSwapConvention convention, Frequency frequency) {
    assertThat(convention.getOvernightLeg().getAccrualFrequency()).isEqualTo(frequency);
  }

  @ParameterizedTest
  @MethodSource("data_period_on")
  public void test_paymentPeriod_on(OvernightIborSwapConvention convention, Frequency frequency) {
    assertThat(convention.getOvernightLeg().getPaymentFrequency()).isEqualTo(frequency);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_period_ibor() {
    return new Object[][] {
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, Frequency.P3M},
        {OvernightIborSwapConventions.GBP_SONIA_OIS_1Y_LIBOR_3M, Frequency.P3M},
    };
  }

  @ParameterizedTest
  @MethodSource("data_period_ibor")
  public void test_accrualPeriod_ibor(OvernightIborSwapConvention convention, Frequency frequency) {
    assertThat(convention.getIborLeg().getAccrualFrequency()).isEqualTo(frequency);
  }

  @ParameterizedTest
  @MethodSource("data_period_ibor")
  public void test_paymentPeriod_ibor(OvernightIborSwapConvention convention, Frequency frequency) {
    assertThat(convention.getIborLeg().getPaymentFrequency()).isEqualTo(frequency);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_day_count() {
    return new Object[][] {
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, DayCounts.ACT_360},
        {OvernightIborSwapConventions.GBP_SONIA_OIS_1Y_LIBOR_3M, DayCounts.ACT_365F},
    };
  }

  @ParameterizedTest
  @MethodSource("data_day_count")
  public void test_day_count(OvernightIborSwapConvention convention, DayCount dayCount) {
    assertThat(convention.getOvernightLeg().getDayCount()).isEqualTo(dayCount);
    assertThat(convention.getIborLeg().getDayCount()).isEqualTo(dayCount);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_float_leg() {
    return new Object[][] {
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, OvernightIndices.USD_FED_FUND},
        {OvernightIborSwapConventions.GBP_SONIA_OIS_1Y_LIBOR_3M, OvernightIndices.GBP_SONIA},
    };
  }

  @ParameterizedTest
  @MethodSource("data_float_leg")
  public void test_float_leg(OvernightIborSwapConvention convention, OvernightIndex floatLeg) {
    assertThat(convention.getOvernightLeg().getIndex()).isEqualTo(floatLeg);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_ibor_leg() {
    return new Object[][] {
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, IborIndices.USD_LIBOR_3M},
        {OvernightIborSwapConventions.GBP_SONIA_OIS_1Y_LIBOR_3M, IborIndices.GBP_LIBOR_3M},
    };
  }

  @ParameterizedTest
  @MethodSource("data_ibor_leg")
  public void test_ibor_leg(OvernightIborSwapConvention convention, IborIndex iborLeg) {
    assertThat(convention.getIborLeg().getIndex()).isEqualTo(iborLeg);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_day_convention() {
    return new Object[][] {
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {OvernightIborSwapConventions.GBP_SONIA_OIS_1Y_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
    };
  }

  @ParameterizedTest
  @MethodSource("data_day_convention")
  public void test_day_convention(OvernightIborSwapConvention convention, BusinessDayConvention dayConvention) {
    assertThat(convention.getOvernightLeg().getAccrualBusinessDayAdjustment().getConvention()).isEqualTo(dayConvention);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_stub_on() {
    return new Object[][] {
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, Tenor.TENOR_4M},
        {OvernightIborSwapConventions.GBP_SONIA_OIS_1Y_LIBOR_3M, Tenor.of(Period.ofMonths(13))},
    };
  }

  @ParameterizedTest
  @MethodSource("data_stub_on")
  public void test_stub_overnight(OvernightIborSwapConvention convention, Tenor tenor) {
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
    coverPrivateConstructor(OvernightIborSwapConventions.class);
    coverPrivateConstructor(StandardOvernightIborSwapConventions.class);
  }

}
