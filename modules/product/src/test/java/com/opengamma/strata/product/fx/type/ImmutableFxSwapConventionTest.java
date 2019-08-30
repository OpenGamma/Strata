/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx.type;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.product.fx.FxSwap;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.fx.ResolvedFxSwap;

/**
 * Tests {@link ImmutableFxSwapConvention}.
 */
public class ImmutableFxSwapConventionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyPair EUR_USD = CurrencyPair.of(Currency.EUR, Currency.USD);
  private static final CurrencyPair GBP_USD = CurrencyPair.of(Currency.GBP, Currency.USD);
  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, EUTA_USNY);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, EUTA_USNY);
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, GBLO);
  private static final BusinessDayAdjustment BDA_MODFOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);

  private static final double NOTIONAL_EUR = 2_000_000d;
  private static final double FX_RATE_NEAR = 1.30d;
  private static final double FX_RATE_PTS = 0.0050d;

  //-------------------------------------------------------------------------
  @Test
  public void test_of_nobda() {
    ImmutableFxSwapConvention test = ImmutableFxSwapConvention.of(EUR_USD, PLUS_TWO_DAYS);
    assertThat(test.getName()).isEqualTo(EUR_USD.toString());
    assertThat(test.getCurrencyPair()).isEqualTo(EUR_USD);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_TWO_DAYS);
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_bda() {
    ImmutableFxSwapConvention test = ImmutableFxSwapConvention.of(EUR_USD, PLUS_TWO_DAYS, BDA_FOLLOW);
    assertThat(test.getName()).isEqualTo(EUR_USD.toString());
    assertThat(test.getCurrencyPair()).isEqualTo(EUR_USD);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_TWO_DAYS);
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BDA_FOLLOW);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    ImmutableFxSwapConvention test = ImmutableFxSwapConvention.builder()
        .currencyPair(EUR_USD)
        .name("EUR::USD")
        .spotDateOffset(PLUS_TWO_DAYS)
        .businessDayAdjustment(BDA_FOLLOW)
        .build();
    assertThat(test.getName()).isEqualTo("EUR::USD");
    assertThat(test.getCurrencyPair()).isEqualTo(EUR_USD);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_TWO_DAYS);
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BDA_FOLLOW);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toTrade_periods() {
    ImmutableFxSwapConvention base = ImmutableFxSwapConvention.of(EUR_USD, PLUS_TWO_DAYS, BDA_FOLLOW);
    Period startPeriod = Period.ofMonths(3);
    Period endPeriod = Period.ofMonths(6);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate spotDate = PLUS_TWO_DAYS.adjust(tradeDate, REF_DATA);
    LocalDate nearDate = spotDate.plus(startPeriod);
    LocalDate farDate = spotDate.plus(endPeriod);
    FxSwapTrade test =
        base.createTrade(tradeDate, startPeriod, endPeriod, BUY, NOTIONAL_EUR, FX_RATE_NEAR, FX_RATE_PTS, REF_DATA);
    FxSwap expected = FxSwap.ofForwardPoints(
        CurrencyAmount.of(EUR, NOTIONAL_EUR), FxRate.of(EUR, USD, FX_RATE_NEAR), FX_RATE_PTS, nearDate, farDate, BDA_FOLLOW);
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_toTrade_dates() {
    ImmutableFxSwapConvention base = ImmutableFxSwapConvention.of(EUR_USD, PLUS_TWO_DAYS, BDA_FOLLOW);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate nearDate = LocalDate.of(2015, 7, 5);
    LocalDate nearDateAdj = LocalDate.of(2015, 7, 6); // Adjusted: 5 is Sunday
    LocalDate farDate = LocalDate.of(2015, 9, 5);
    LocalDate farDateAdj = LocalDate.of(2015, 9, 7); // Adjusted: 5 is Saturday
    FxSwapTrade test = base.toTrade(tradeDate, nearDate, farDate, BUY, NOTIONAL_EUR, FX_RATE_NEAR, FX_RATE_PTS);
    FxSwap expected = FxSwap.ofForwardPoints(
        CurrencyAmount.of(EUR, NOTIONAL_EUR), FxRate.of(EUR, USD, FX_RATE_NEAR), FX_RATE_PTS, nearDate, farDate, BDA_FOLLOW);
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
    ResolvedFxSwap resolvedExpected = ResolvedFxSwap.ofForwardPoints(
        CurrencyAmount.of(EUR, NOTIONAL_EUR), USD, FX_RATE_NEAR, FX_RATE_PTS, nearDateAdj, farDateAdj);
    assertThat(test.getProduct().resolve(REF_DATA)).isEqualTo(resolvedExpected);
  }

  @Test
  public void test_toTemplate_badDateOrder() {
    ImmutableFxSwapConvention base = ImmutableFxSwapConvention.of(EUR_USD, PLUS_TWO_DAYS, BDA_FOLLOW);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate nearDate = date(2015, 4, 5);
    LocalDate farDate = date(2015, 7, 5);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.toTrade(tradeDate, nearDate, farDate, BUY, NOTIONAL_EUR, FX_RATE_NEAR, FX_RATE_PTS));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ImmutableFxSwapConvention test = ImmutableFxSwapConvention.of(EUR_USD, PLUS_TWO_DAYS, BDA_FOLLOW);
    coverImmutableBean(test);
    ImmutableFxSwapConvention test2 = ImmutableFxSwapConvention.builder()
        .name("GBP/USD")
        .currencyPair(GBP_USD)
        .spotDateOffset(PLUS_ONE_DAY)
        .businessDayAdjustment(BDA_MODFOLLOW)
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    ImmutableFxSwapConvention test = ImmutableFxSwapConvention.of(EUR_USD, PLUS_TWO_DAYS, BDA_FOLLOW);
    assertSerialization(test);
  }

}
