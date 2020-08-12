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
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.MarketTenor;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.fx.FxSwapTrade;

/**
 * Test {@link FxSwapConventions}.
 */
public class FxSwapConventionsTest {

  private static final HolidayCalendarId EUTA_JPTO = EUTA.combinedWith(JPTO);
  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);
  private static final HolidayCalendarId EUTA_GBLO = EUTA.combinedWith(GBLO);
  private static final HolidayCalendarId GBLO_USNY = GBLO.combinedWith(USNY);
  private static final HolidayCalendarId GBLO_JPTO = GBLO.combinedWith(JPTO);
  private static final HolidayCalendarId JPTO_USNY = JPTO.combinedWith(USNY);

  public static Object[][] data_spotLag() {
    return new Object[][] {
        {FxSwapConventions.EUR_USD, 2},
        {FxSwapConventions.EUR_GBP, 2},
        {FxSwapConventions.EUR_JPY, 2},
        {FxSwapConventions.GBP_USD, 2},
        {FxSwapConventions.GBP_JPY, 2},
        {FxSwapConventions.USD_JPY, 2}
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
        {FxSwapConventions.EUR_JPY, CurrencyPair.of(EUR, JPY)},
        {FxSwapConventions.GBP_USD, CurrencyPair.of(GBP, USD)},
        {FxSwapConventions.GBP_JPY, CurrencyPair.of(GBP, JPY)},
        {FxSwapConventions.USD_JPY, CurrencyPair.of(USD, JPY)}
    };
  }

  @ParameterizedTest
  @MethodSource("data_currencyPair")
  public void test_currencyPair(ImmutableFxSwapConvention convention, CurrencyPair pair) {
    assertThat(convention.getCurrencyPair()).isEqualTo(pair);
  }

  @ParameterizedTest
  @MethodSource("data_currencyPair")
  public void test_lookup(ImmutableFxSwapConvention convention, CurrencyPair pair) {
    assertThat(FxSwapConvention.of(pair)).isEqualTo(convention);
    assertThat(FxSwapConvention.of(pair.inverse())).isEqualTo(convention);
  }

  public static Object[][] data_calendar() {
    return new Object[][] {
        {FxSwapConventions.EUR_USD, EUTA_USNY},
        {FxSwapConventions.EUR_GBP, EUTA_GBLO},
        {FxSwapConventions.EUR_JPY, EUTA_JPTO},
        {FxSwapConventions.GBP_USD, GBLO_USNY},
        {FxSwapConventions.GBP_JPY, GBLO_JPTO},
        {FxSwapConventions.USD_JPY, JPTO_USNY}
    };
  }

  @ParameterizedTest
  @MethodSource("data_calendar")
  public void test_calendar(ImmutableFxSwapConvention convention, HolidayCalendarId cal) {
    assertThat(convention.getSpotDateOffset().getCalendar()).isEqualTo(cal);
    assertThat(convention.getBusinessDayAdjustment().getCalendar()).isEqualTo(cal);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_dates() {
    return new Object[][] {
        {MarketTenor.ON, date(2020, 6, 22), date(2020, 6, 23)},
        {MarketTenor.TN, date(2020, 6, 23), date(2020, 6, 24)},
        {MarketTenor.SN, date(2020, 6, 24), date(2020, 6, 25)},
        {MarketTenor.SW, date(2020, 6, 24), date(2020, 7, 1)},
    };
  }

  @ParameterizedTest
  @MethodSource("data_dates")
  public void test_dates(MarketTenor marketTenor, LocalDate startDate, LocalDate endDate) {
    FxSwapTrade trade = FxSwapConventions.EUR_USD.createTrade(
        date(2020, 6, 22), marketTenor, BuySell.BUY, 1_000_000, 1.1, 0.01, ReferenceData.standard());
    assertThat(trade.getProduct().getNearLeg().getPaymentDate()).isEqualTo(startDate);
    assertThat(trade.getProduct().getFarLeg().getPaymentDate()).isEqualTo(endDate);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(FxSwapConventions.class);
    coverPrivateConstructor(StandardFxSwapConventions.class);
  }

}
