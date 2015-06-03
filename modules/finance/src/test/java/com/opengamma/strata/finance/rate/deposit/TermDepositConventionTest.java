/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.deposit;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.finance.TradeInfo;

/**
 * Test {@link TermDepositConvention}.
 */
@Test
public class TermDepositConventionTest {
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, EUTA);

  public void test_builder_full() {
    TermDepositConvention test = TermDepositConvention.builder()
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .currency(EUR)
        .dayCount(ACT_360)
        .spotDateOffset(PLUS_TWO_DAYS)
        .build();
    assertEquals(test.getBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getSpotDateOffset(), PLUS_TWO_DAYS);
  }

  public void test_of() {
    TermDepositConvention test = TermDepositConvention.of(EUR, BDA_MOD_FOLLOW, ACT_360, PLUS_TWO_DAYS);
    assertEquals(test.getBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getSpotDateOffset(), PLUS_TWO_DAYS);
  }

  public void test_toTemplate() {
    TermDepositConvention convention = TermDepositConvention.builder()
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .currency(EUR)
        .dayCount(ACT_360)
        .spotDateOffset(PLUS_TWO_DAYS)
        .build();
    Period period3M = Period.ofMonths(3);
    TermDepositTemplate template = convention.toTemplate(period3M);
    assertEquals(template.getConvention(), convention);
    assertEquals(template.getDepositPeriod(), period3M);
  }

  public void test_toTrade() {
    TermDepositConvention convention = TermDepositConvention.builder()
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .currency(EUR)
        .dayCount(ACT_360)
        .spotDateOffset(PLUS_TWO_DAYS)
        .build();
    LocalDate tradeDate = LocalDate.of(2015, 1, 22);
    Period period3M = Period.ofMonths(3);
    BuySell buy = BuySell.BUY;
    double notional = 2_000_000d;
    double rate = 0.0125;
    TermDepositTrade trade = convention.toTrade(tradeDate, period3M, buy, notional, rate);
    LocalDate startDateExpected = PLUS_TWO_DAYS.adjust(tradeDate);
    LocalDate endDateExpected = startDateExpected.plus(period3M);
    TermDeposit termDepositExpected = TermDeposit.builder()
        .buySell(buy)
        .currency(EUR)
        .notional(notional)
        .startDate(startDateExpected)
        .endDate(endDateExpected)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .rate(rate)
        .dayCount(ACT_360)
        .build();
    TradeInfo tradeInfoExpected = TradeInfo.builder().tradeDate(tradeDate).build();
    assertEquals(trade.getProduct(), termDepositExpected);
    assertEquals(trade.getTradeInfo(), tradeInfoExpected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TermDepositConvention test1 = TermDepositConvention.of(EUR, BDA_MOD_FOLLOW, ACT_360, PLUS_TWO_DAYS);
    coverImmutableBean(test1);
    TermDepositConvention test2 =
        TermDepositConvention.of(GBP, BDA_MOD_FOLLOW, ACT_365F, DaysAdjustment.ofBusinessDays(0, GBLO));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    TermDepositConvention test = TermDepositConvention.of(EUR, BDA_MOD_FOLLOW, ACT_360, PLUS_TWO_DAYS);
    assertSerialization(test);
  }

}
