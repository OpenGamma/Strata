/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx.type;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.HolidayCalendarId;

/**
 * Test {@link FxSwapConventions}.
 */
public class FxSwapConventionsTest {

  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);
  private static final HolidayCalendarId GBLO_EUTA = GBLO.combinedWith(EUTA);
  private static final HolidayCalendarId GBLO_USNY = GBLO.combinedWith(USNY);
  private static final HolidayCalendarId GBLO_JPTO = GBLO.combinedWith(JPTO);

  public static Object[][] data_spotLag() {
    return new Object[][] {
        {FxSwapConventions.EUR_USD, 2},
        {FxSwapConventions.EUR_GBP, 2},
        {FxSwapConventions.GBP_USD, 2},
        {FxSwapConventions.GBP_JPY, 2}
    };
  }

  @ParameterizedTest
  @MethodSource("data_spotLag")
  public void test_spotLag(ImmutableFxSwapConvention convention, int lag) {
    assertThat(convention.getSpotDateOffset().getDays()).isEqualTo(lag);
  }

  public static Object[][] data_currencyPair() {
    return new Object[][] {
        {FxSwapConventions.EUR_USD, CurrencyPair.of(EUR, USD)},
        {FxSwapConventions.EUR_GBP, CurrencyPair.of(EUR, GBP)},
        {FxSwapConventions.GBP_USD, CurrencyPair.of(GBP, USD)},
        {FxSwapConventions.GBP_JPY, CurrencyPair.of(GBP, JPY)}
    };
  }

  @ParameterizedTest
  @MethodSource("data_currencyPair")
  public void test_currencyPair(ImmutableFxSwapConvention convention, CurrencyPair ccys) {
    assertThat(convention.getCurrencyPair()).isEqualTo(ccys);
  }

  public static Object[][] data_calendar() {
    return new Object[][] {
        {FxSwapConventions.EUR_USD, EUTA_USNY},
        {FxSwapConventions.EUR_GBP, GBLO_EUTA},
        {FxSwapConventions.GBP_USD, GBLO_USNY},
        {FxSwapConventions.GBP_JPY, GBLO_JPTO}
    };
  }

  @ParameterizedTest
  @MethodSource("data_calendar")
  public void test_calendar(ImmutableFxSwapConvention convention, HolidayCalendarId cal) {
    assertThat(convention.getSpotDateOffset().getCalendar()).isEqualTo(cal);
    assertThat(convention.getBusinessDayAdjustment().getCalendar()).isEqualTo(cal);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(FxSwapConventions.class);
    coverPrivateConstructor(StandardFxSwapConventions.class);
  }

}
