/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USGS;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * Test {@link XCcyOvernightOvernightSwapConventions}.
 */
public class XCcyOvernightOvernightSwapConventionsTest {

  private static final HolidayCalendarId EUTA_USGS = EUTA.combinedWith(USGS);
  private static final HolidayCalendarId GBLO_USGS = GBLO.combinedWith(USGS);
  private static final HolidayCalendarId GBLO_EUTA = GBLO.combinedWith(EUTA);
  private static final HolidayCalendarId JPTO_USGS = JPTO.combinedWith(USGS);

  public static Object[][] data_spot_lag() {
    return new Object[][] {
        {XCcyOvernightOvernightSwapConventions.EUR_ESTR_3M_USD_SOFR_3M, 2},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_USD_SOFR_3M, 2},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_EUR_ESTR_3M, 2},
        {XCcyOvernightOvernightSwapConventions.JPY_TONA_3M_USD_SOFR_3M, 2}
    };
  }

  @ParameterizedTest
  @MethodSource("data_spot_lag")
  public void test_spot_lag(XCcyOvernightOvernightSwapConvention convention, int lag) {
    assertThat(convention.getSpotDateOffset().getDays()).isEqualTo(lag);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_period() {
    return new Object[][] {
        {XCcyOvernightOvernightSwapConventions.EUR_ESTR_3M_USD_SOFR_3M, Frequency.P3M},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_USD_SOFR_3M, Frequency.P3M},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_EUR_ESTR_3M, Frequency.P3M},
        {XCcyOvernightOvernightSwapConventions.JPY_TONA_3M_USD_SOFR_3M, Frequency.P3M}
    };
  }

  @ParameterizedTest
  @MethodSource("data_period")
  public void test_period(XCcyOvernightOvernightSwapConvention convention, Frequency frequency) {
    assertThat(convention.getSpreadLeg().getPaymentFrequency()).isEqualTo(frequency);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_spread_leg() {
    return new Object[][] {
        {XCcyOvernightOvernightSwapConventions.EUR_ESTR_3M_USD_SOFR_3M, OvernightIndices.EUR_ESTR},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_USD_SOFR_3M, OvernightIndices.GBP_SONIA},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_EUR_ESTR_3M, OvernightIndices.GBP_SONIA},
        {XCcyOvernightOvernightSwapConventions.JPY_TONA_3M_USD_SOFR_3M, OvernightIndices.JPY_TONAR}
    };
  }

  @ParameterizedTest
  @MethodSource("data_spread_leg")
  public void test_float_leg(XCcyOvernightOvernightSwapConvention convention, OvernightIndex index) {
    assertThat(convention.getSpreadLeg().getIndex()).isEqualTo(index);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_spread_leg_bda() {
    return new Object[][] {
        {XCcyOvernightOvernightSwapConventions.EUR_ESTR_3M_USD_SOFR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USGS)},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_USD_SOFR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USGS)},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_EUR_ESTR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_EUTA)},
        {XCcyOvernightOvernightSwapConventions.JPY_TONA_3M_USD_SOFR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO_USGS)}
    };
  }

  @ParameterizedTest
  @MethodSource("data_spread_leg_bda")
  public void test_spread_leg_bdc(XCcyOvernightOvernightSwapConvention convention, BusinessDayAdjustment bda) {
    assertThat(convention.getSpreadLeg().getAccrualBusinessDayAdjustment()).isEqualTo(bda);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_flat_leg() {
    return new Object[][] {
        {XCcyOvernightOvernightSwapConventions.EUR_ESTR_3M_USD_SOFR_3M, OvernightIndices.USD_SOFR},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_USD_SOFR_3M, OvernightIndices.USD_SOFR},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_EUR_ESTR_3M, OvernightIndices.EUR_ESTR},
        {XCcyOvernightOvernightSwapConventions.JPY_TONA_3M_USD_SOFR_3M, OvernightIndices.USD_SOFR}
    };
  }

  @ParameterizedTest
  @MethodSource("data_flat_leg")
  public void test_flat_leg(XCcyOvernightOvernightSwapConvention convention, OvernightIndex index) {
    assertThat(convention.getFlatLeg().getIndex()).isEqualTo(index);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_flat_leg_bda() {
    return new Object[][] {
        {XCcyOvernightOvernightSwapConventions.EUR_ESTR_3M_USD_SOFR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USGS)},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_USD_SOFR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USGS)},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_EUR_ESTR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_EUTA)},
        {XCcyOvernightOvernightSwapConventions.JPY_TONA_3M_USD_SOFR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO_USGS)}
    };
  }

  @ParameterizedTest
  @MethodSource("data_flat_leg_bda")
  public void test_flat_leg_bdc(XCcyOvernightOvernightSwapConvention convention, BusinessDayAdjustment bda) {
    assertThat(convention.getFlatLeg().getAccrualBusinessDayAdjustment()).isEqualTo(bda);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_day_convention() {
    return new Object[][] {
        {XCcyOvernightOvernightSwapConventions.EUR_ESTR_3M_USD_SOFR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_USD_SOFR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_EUR_ESTR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {XCcyOvernightOvernightSwapConventions.JPY_TONA_3M_USD_SOFR_3M, BusinessDayConventions.MODIFIED_FOLLOWING}
    };
  }

  @ParameterizedTest
  @MethodSource("data_day_convention")
  public void test_day_convention(XCcyOvernightOvernightSwapConvention convention, BusinessDayConvention dayConvention) {
    assertThat(convention.getSpreadLeg().getAccrualBusinessDayAdjustment().getConvention()).isEqualTo(dayConvention);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_paymentlag() {
    return new Object[][] {
        {XCcyOvernightOvernightSwapConventions.EUR_ESTR_3M_USD_SOFR_3M, DaysAdjustment.ofBusinessDays(2, EUTA_USGS)},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_USD_SOFR_3M, DaysAdjustment.ofBusinessDays(2, GBLO_USGS)},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_EUR_ESTR_3M, DaysAdjustment.ofBusinessDays(2, GBLO_EUTA)},
        {XCcyOvernightOvernightSwapConventions.JPY_TONA_3M_USD_SOFR_3M, DaysAdjustment.ofBusinessDays(2, JPTO_USGS)}
    };
  }

  @ParameterizedTest
  @MethodSource("data_paymentlag")
  public void test_paymentlag(XCcyOvernightOvernightSwapConvention convention, DaysAdjustment dadj) {
    assertThat(convention.getSpreadLeg().getPaymentDateOffset()).isEqualTo(dadj);
    assertThat(convention.getFlatLeg().getPaymentDateOffset()).isEqualTo(dadj);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_notional_exchange() {
    return new Object[][] {
        {XCcyOvernightOvernightSwapConventions.EUR_ESTR_3M_USD_SOFR_3M, true},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_USD_SOFR_3M, true},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_EUR_ESTR_3M, true},
        {XCcyOvernightOvernightSwapConventions.JPY_TONA_3M_USD_SOFR_3M, true}
    };
  }

  @ParameterizedTest
  @MethodSource("data_notional_exchange")
  public void test_notional_exchange(XCcyOvernightOvernightSwapConvention convention, boolean notionalExchange) {
    assertThat(convention.getSpreadLeg().isNotionalExchange()).isEqualTo(notionalExchange);
    assertThat(convention.getFlatLeg().isNotionalExchange()).isEqualTo(notionalExchange);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(XCcyOvernightOvernightSwapConventions.class);
    coverPrivateConstructor(StandardXCcyOvernightOvernightSwapConventions.class);
  }

}
