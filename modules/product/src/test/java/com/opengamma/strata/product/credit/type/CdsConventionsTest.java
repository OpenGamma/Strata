/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * Test {@link CdsConventions}.
 */
public class CdsConventionsTest {

  private static final HolidayCalendarId GBLO_USNY = GBLO.combinedWith(USNY);
  private static final HolidayCalendarId GBLO_USNY_JPTO = JPTO.combinedWith(GBLO_USNY);
  private static final HolidayCalendarId GBLO_EUTA = GBLO.combinedWith(EUTA);

  public static Object[][] data_currency() {
    return new Object[][] {
        {CdsConventions.EUR_GB_STANDARD, EUR},
        {CdsConventions.EUR_STANDARD, EUR},
        {CdsConventions.GBP_STANDARD, GBP},
        {CdsConventions.GBP_US_STANDARD, GBP},
        {CdsConventions.JPY_STANDARD, JPY},
        {CdsConventions.JPY_US_GB_STANDARD, JPY},
        {CdsConventions.USD_STANDARD, USD}
    };
  }

  @ParameterizedTest
  @MethodSource("data_currency")
  public void test_spot_lag(ImmutableCdsConvention convention, Currency currency) {
    assertThat(convention.getCurrency()).isEqualTo(currency);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_common() {
    return new Object[][] {
        {CdsConventions.EUR_GB_STANDARD,},
        {CdsConventions.EUR_STANDARD,},
        {CdsConventions.GBP_STANDARD,},
        {CdsConventions.GBP_US_STANDARD,},
        {CdsConventions.JPY_STANDARD,},
        {CdsConventions.JPY_US_GB_STANDARD,},
        {CdsConventions.USD_STANDARD}
    };
  }

  @ParameterizedTest
  @MethodSource("data_common")
  public void test_period(ImmutableCdsConvention convention) {
    assertThat(convention.getPaymentFrequency()).isEqualTo(Frequency.P3M);
  }

  @ParameterizedTest
  @MethodSource("data_common")
  public void test_day_count(ImmutableCdsConvention convention) {
    assertThat(convention.getDayCount()).isEqualTo(ACT_360);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_businessDayAdjustment() {
    return new Object[][] {
        {CdsConventions.EUR_GB_STANDARD, BusinessDayAdjustment.of(FOLLOWING, GBLO_EUTA)},
        {CdsConventions.EUR_STANDARD, BusinessDayAdjustment.of(FOLLOWING, EUTA)},
        {CdsConventions.GBP_STANDARD, BusinessDayAdjustment.of(FOLLOWING, GBLO)},
        {CdsConventions.GBP_US_STANDARD, BusinessDayAdjustment.of(FOLLOWING, GBLO_USNY)},
        {CdsConventions.JPY_STANDARD, BusinessDayAdjustment.of(FOLLOWING, JPTO)},
        {CdsConventions.JPY_US_GB_STANDARD, BusinessDayAdjustment.of(FOLLOWING, GBLO_USNY_JPTO)},
        {CdsConventions.USD_STANDARD, BusinessDayAdjustment.of(FOLLOWING, USNY)}
    };
  }

  @ParameterizedTest
  @MethodSource("data_businessDayAdjustment")
  public void test_businessDayAdjustment(ImmutableCdsConvention convention, BusinessDayAdjustment adj) {
    assertThat(convention.getBusinessDayAdjustment()).isEqualTo(adj);
    assertThat(convention.getStartDateBusinessDayAdjustment()).isEqualTo(adj);
    assertThat(convention.getEndDateBusinessDayAdjustment()).isEqualTo(BusinessDayAdjustment.NONE);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_string() {
    return new Object[][] {
        {CdsConventions.EUR_GB_STANDARD, "EUR-GB-STANDARD"},
        {CdsConventions.EUR_STANDARD, "EUR-STANDARD"},
        {CdsConventions.GBP_STANDARD, "GBP-STANDARD"},
        {CdsConventions.GBP_US_STANDARD, "GBP-US-STANDARD"},
        {CdsConventions.JPY_STANDARD, "JPY-STANDARD"},
        {CdsConventions.JPY_US_GB_STANDARD, "JPY-US-GB-STANDARD"},
        {CdsConventions.USD_STANDARD, "USD-STANDARD"}
    };
  }

  @ParameterizedTest
  @MethodSource("data_string")
  public void test_string(ImmutableCdsConvention convention, String string) {
    assertThat(convention.toString()).isEqualTo(string);
    assertThat(convention.getName()).isEqualTo(string);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(CdsConventions.class);
    coverPrivateConstructor(StandardCdsConventions.class);
  }

}
