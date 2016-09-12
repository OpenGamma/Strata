/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx.type;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.testng.annotations.Test;

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

/**
 * Test {@link FxSwapTemplate}.
 */
@Test
public class FxSwapTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyPair EUR_USD = CurrencyPair.of(Currency.EUR, Currency.USD);
  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, EUTA_USNY);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, EUTA_USNY);
  private static final ImmutableFxSwapConvention CONVENTION = ImmutableFxSwapConvention.of(EUR_USD, PLUS_TWO_DAYS);
  private static final ImmutableFxSwapConvention CONVENTION2 = ImmutableFxSwapConvention.of(EUR_USD, PLUS_ONE_DAY);
  private static final Period NEAR_PERIOD = Period.ofMonths(3);
  private static final Period FAR_PERIOD = Period.ofMonths(6);

  private static final double NOTIONAL_EUR = 2_000_000d;
  private static final double FX_RATE_NEAR = 1.30d;
  private static final double FX_RATE_PTS = 0.0050d;

  public void test_of_far() {
    FxSwapTemplate test = FxSwapTemplate.of(FAR_PERIOD, CONVENTION);
    assertEquals(test.getPeriodToNear(), Period.ZERO);
    assertEquals(test.getPeriodToFar(), FAR_PERIOD);
    assertEquals(test.getConvention(), CONVENTION);
    assertEquals(test.getCurrencyPair(), EUR_USD);
  }

  public void test_of_near_far() {
    FxSwapTemplate test = FxSwapTemplate.of(NEAR_PERIOD, FAR_PERIOD, CONVENTION);
    assertEquals(test.getPeriodToNear(), NEAR_PERIOD);
    assertEquals(test.getPeriodToFar(), FAR_PERIOD);
    assertEquals(test.getConvention(), CONVENTION);
    assertEquals(test.getCurrencyPair(), EUR_USD);
  }

  public void test_builder_insufficientInfo() {
    assertThrowsIllegalArg(() -> FxSwapTemplate.builder().convention(CONVENTION).build());
    assertThrowsIllegalArg(() -> FxSwapTemplate.builder().periodToNear(NEAR_PERIOD).build());
    assertThrowsIllegalArg(() -> FxSwapTemplate.builder().periodToFar(FAR_PERIOD).build());
  }

  //-------------------------------------------------------------------------
  public void test_createTrade() {
    FxSwapTemplate base = FxSwapTemplate.of(NEAR_PERIOD, FAR_PERIOD, CONVENTION);
    LocalDate tradeDate = LocalDate.of(2015, 10, 29);
    FxSwapTrade test = base.createTrade(tradeDate, BUY, NOTIONAL_EUR, FX_RATE_NEAR, FX_RATE_PTS, REF_DATA);
    LocalDate spotDate = PLUS_TWO_DAYS.adjust(tradeDate, REF_DATA);
    LocalDate nearDate = spotDate.plus(NEAR_PERIOD);
    LocalDate farDate = spotDate.plus(FAR_PERIOD);
    BusinessDayAdjustment bda = CONVENTION.getBusinessDayAdjustment();
    FxSwap expected = FxSwap.ofForwardPoints(
        CurrencyAmount.of(EUR, NOTIONAL_EUR), FxRate.of(EUR, USD, FX_RATE_NEAR), FX_RATE_PTS, nearDate, farDate, bda);
    assertEquals(test.getInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxSwapTemplate test = FxSwapTemplate.of(NEAR_PERIOD, FAR_PERIOD, CONVENTION);
    coverImmutableBean(test);
    FxSwapTemplate test2 = FxSwapTemplate.of(Period.ofMonths(4), Period.ofMonths(7), CONVENTION2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FxSwapTemplate test = FxSwapTemplate.of(NEAR_PERIOD, FAR_PERIOD, CONVENTION);
    assertSerialization(test);
  }

}
