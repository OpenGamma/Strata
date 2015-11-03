/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx.type;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.finance.fx.FxSwap;
import com.opengamma.strata.finance.fx.FxSwapTrade;

/**
 * Tests {@link ImmutableFxSwapConvention}.
 */
@Test
public class ImmutableFxSwapConventionTest {
  
  private static final CurrencyPair EUR_USD = CurrencyPair.of(Currency.EUR, Currency.USD);
  private static final String EUR_USD_STR = "EUR/USD";
  private static final HolidayCalendar EUTA_USNY = EUTA.combineWith(USNY);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, EUTA_USNY);
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, GBLO);

  private static final double NOTIONAL_EUR = 2_000_000d;
  private static final double FX_RATE_NEAR = 1.30d;
  private static final double FX_RATE_PTS = 0.0050d;
  

  //-------------------------------------------------------------------------
  public void test_of_nobda() {
    ImmutableFxSwapConvention test = ImmutableFxSwapConvention.of(EUR_USD, PLUS_TWO_DAYS);
    assertEquals(test.getCurrencyPair(), EUR_USD);
    assertEquals(test.getSpotDateOffset(), PLUS_TWO_DAYS);
    assertEquals(test.getBusinessDayAdjustment(), BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY));
  }

  //-------------------------------------------------------------------------
  public void test_of_bda() {
    ImmutableFxSwapConvention test = ImmutableFxSwapConvention.of(EUR_USD_STR, EUR_USD, PLUS_TWO_DAYS, BDA_FOLLOW);
    assertEquals(test.getCurrencyPair(), EUR_USD);
    assertEquals(test.getSpotDateOffset(), PLUS_TWO_DAYS);
    assertEquals(test.getBusinessDayAdjustment(), BDA_FOLLOW);
  }

  //-------------------------------------------------------------------------
  public void test_toTrade_periods() {
    ImmutableFxSwapConvention base = ImmutableFxSwapConvention.of(EUR_USD_STR, EUR_USD, PLUS_TWO_DAYS, BDA_FOLLOW);
    Period startPeriod = Period.ofMonths(3);
    Period endPeriod = Period.ofMonths(6);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate spotDate = PLUS_TWO_DAYS.adjust(tradeDate);
    LocalDate nearDate = BDA_FOLLOW.adjust(spotDate.plus(startPeriod));
    LocalDate farDate = BDA_FOLLOW.adjust(spotDate.plus(endPeriod));
    FxSwapTrade test = 
        base.toTrade(tradeDate, startPeriod, endPeriod, BUY, NOTIONAL_EUR, FX_RATE_NEAR, FX_RATE_PTS);
    FxSwap expected = FxSwap
        .ofForwardPoints(CurrencyAmount.of(EUR, NOTIONAL_EUR), USD, FX_RATE_NEAR, FX_RATE_PTS, nearDate, farDate);
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_toTrade_dates() {
    ImmutableFxSwapConvention base = ImmutableFxSwapConvention.of(EUR_USD_STR, EUR_USD, PLUS_TWO_DAYS, BDA_FOLLOW);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate nearDate = LocalDate.of(2015, 7, 6); // Adjusted: 5 is Sunday
    LocalDate farDate = LocalDate.of(2015, 9, 7); // Adjusted: 5 is Saturday
    FxSwapTrade test = base.toTrade(tradeDate, nearDate, farDate, BUY, NOTIONAL_EUR, FX_RATE_NEAR, FX_RATE_PTS);
    FxSwap expected = FxSwap
        .ofForwardPoints(CurrencyAmount.of(EUR, NOTIONAL_EUR), USD, FX_RATE_NEAR, FX_RATE_PTS, nearDate, farDate);
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_toTemplate_badDateOrder() {
    ImmutableFxSwapConvention base = ImmutableFxSwapConvention.of(EUR_USD_STR, EUR_USD, PLUS_TWO_DAYS, BDA_FOLLOW);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate nearDate = date(2015, 4, 5);
    LocalDate farDate = date(2015, 7, 5);
    assertThrowsIllegalArg(() -> base.toTrade(tradeDate, nearDate, farDate, BUY, NOTIONAL_EUR, FX_RATE_NEAR, FX_RATE_PTS));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableFxSwapConvention test = ImmutableFxSwapConvention.of(EUR_USD_STR, EUR_USD, PLUS_TWO_DAYS, BDA_FOLLOW);
    coverImmutableBean(test);
    ImmutableFxSwapConvention test2 = ImmutableFxSwapConvention.builder()
        .currencyPair(EUR_USD)
        .spotDateOffset(PLUS_TWO_DAYS)
        .businessDayAdjustment(BDA_FOLLOW)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ImmutableFxSwapConvention test = ImmutableFxSwapConvention.of(EUR_USD_STR, EUR_USD, PLUS_TWO_DAYS, BDA_FOLLOW);
    assertSerialization(test);
  }
  
}
