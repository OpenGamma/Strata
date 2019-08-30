/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * Test {@link XCcyIborIborSwapConventions}.
 */
public class XCcyIborIborSwapConventionsTest {

  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);
  private static final HolidayCalendarId GBLO_USNY = GBLO.combinedWith(USNY);
  private static final HolidayCalendarId EUTA_GBLO = EUTA.combinedWith(GBLO);
  private static final HolidayCalendarId GBLO_JPTO = GBLO.combinedWith(JPTO);

  public static Object[][] data_spot_lag() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, 2},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, 2},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_EUR_EURIBOR_3M, 2}
    };
  }

  @ParameterizedTest
  @MethodSource("data_spot_lag")
  public void test_spot_lag(ImmutableXCcyIborIborSwapConvention convention, int lag) {
    assertThat(convention.getSpotDateOffset().getDays()).isEqualTo(lag);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_period() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, Frequency.P3M},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, Frequency.P3M},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_EUR_EURIBOR_3M, Frequency.P3M},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_JPY_LIBOR_3M, Frequency.P3M}
    };
  }

  @ParameterizedTest
  @MethodSource("data_period")
  public void test_period(XCcyIborIborSwapConvention convention, Frequency frequency) {
    assertThat(convention.getSpreadLeg().getPaymentFrequency()).isEqualTo(frequency);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_spread_leg() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, IborIndices.EUR_EURIBOR_3M},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, IborIndices.GBP_LIBOR_3M},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_EUR_EURIBOR_3M, IborIndices.GBP_LIBOR_3M},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_JPY_LIBOR_3M, IborIndices.GBP_LIBOR_3M}
    };
  }

  @ParameterizedTest
  @MethodSource("data_spread_leg")
  public void test_float_leg(XCcyIborIborSwapConvention convention, IborIndex index) {
    assertThat(convention.getSpreadLeg().getIndex()).isEqualTo(index);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_spread_leg_bda() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY)},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY)},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_EUR_EURIBOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_GBLO)},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_JPY_LIBOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_JPTO)}
    };
  }

  @ParameterizedTest
  @MethodSource("data_spread_leg_bda")
  public void test_spread_leg_bdc(XCcyIborIborSwapConvention convention, BusinessDayAdjustment bda) {
    assertThat(convention.getSpreadLeg().getAccrualBusinessDayAdjustment()).isEqualTo(bda);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_flat_leg() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, IborIndices.USD_LIBOR_3M},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, IborIndices.USD_LIBOR_3M},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_EUR_EURIBOR_3M, IborIndices.EUR_EURIBOR_3M},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_JPY_LIBOR_3M, IborIndices.JPY_LIBOR_3M}
    };
  }

  @ParameterizedTest
  @MethodSource("data_flat_leg")
  public void test_flat_leg(XCcyIborIborSwapConvention convention, IborIndex index) {
    assertThat(convention.getFlatLeg().getIndex()).isEqualTo(index);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_flat_leg_bda() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY)},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY)},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_EUR_EURIBOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_GBLO)},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_JPY_LIBOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_JPTO)}
    };
  }

  @ParameterizedTest
  @MethodSource("data_flat_leg_bda")
  public void test_flat_leg_bdc(XCcyIborIborSwapConvention convention, BusinessDayAdjustment bda) {
    assertThat(convention.getFlatLeg().getAccrualBusinessDayAdjustment()).isEqualTo(bda);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_day_convention() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_EUR_EURIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_JPY_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING}
    };
  }

  @ParameterizedTest
  @MethodSource("data_day_convention")
  public void test_day_convention(XCcyIborIborSwapConvention convention, BusinessDayConvention dayConvention) {
    assertThat(convention.getSpreadLeg().getAccrualBusinessDayAdjustment().getConvention()).isEqualTo(dayConvention);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_notional_exchange() {
    return new Object[][] {
        {XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M, true},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M, true},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_EUR_EURIBOR_3M, true},
        {XCcyIborIborSwapConventions.GBP_LIBOR_3M_EUR_EURIBOR_3M, true}
    };
  }

  @ParameterizedTest
  @MethodSource("data_notional_exchange")
  public void test_notional_exchange(XCcyIborIborSwapConvention convention, boolean notionalExchange) {
    assertThat(convention.getSpreadLeg().isNotionalExchange()).isEqualTo(notionalExchange);
    assertThat(convention.getFlatLeg().isNotionalExchange()).isEqualTo(notionalExchange);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(XCcyIborIborSwapConventions.class);
    coverPrivateConstructor(StandardXCcyIborIborSwapConventions.class);
  }

}
