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
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;

/**
 * Test {@link TermDeposit}.
 */
@Test
public class TermDepositTest {

  private static final BuySell SELL = BuySell.SELL;
  private static final LocalDate START_DATE = LocalDate.of(2015, 1, 19);
  private static final LocalDate END_DATE = LocalDate.of(2015, 7, 19);
  private static final double NOTIONAL = 100000000;
  private static final CurrencyAmount PRINCIPAL = CurrencyAmount.of(GBP, NOTIONAL);
  private static final double RATE = 0.0250;
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final double EPS = 1.0e-14;

  @Test
  public void test_builder() {
    TermDeposit test = TermDeposit.builder()
        .buySell(SELL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .dayCount(ACT_365F)
        .principal(PRINCIPAL)
        .rate(RATE)
        .build();
    assertEquals(test.getBuySell(), SELL);
    assertEquals(test.getStartDate(), START_DATE);
    assertEquals(test.getEndDate(), END_DATE);
    assertEquals(test.getBusinessDayAdjustment().get(), BDA_MOD_FOLLOW);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getPrincipal(), PRINCIPAL);
    assertEquals(test.getRate(), RATE);
    assertEquals(test.getCurrency(), GBP);
  }

  public void test_builder_wrongDates() {
    assertThrowsIllegalArg(() -> TermDeposit.builder()
        .buySell(SELL)
        .startDate(START_DATE)
        .endDate(LocalDate.of(2014, 10, 19))
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .dayCount(ACT_365F)
        .principal(PRINCIPAL)
        .rate(RATE)
        .build());
  }

  public void test_expand() {
    TermDeposit base = TermDeposit.builder()
        .buySell(SELL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .dayCount(ACT_365F)
        .principal(PRINCIPAL)
        .rate(RATE)
        .build();
    ExpandedTermDeposit test = base.expand();
    LocalDate expectedEndDate = BDA_MOD_FOLLOW.adjust(END_DATE);
    double expectedYearFraction = ACT_365F.yearFraction(START_DATE, expectedEndDate);
    assertEquals(test.getStartDate(), START_DATE);
    assertEquals(test.getEndDate(), expectedEndDate);
    assertEquals(test.getPrincipal(), -NOTIONAL);
    assertEquals(test.getYearFraction(), expectedYearFraction, EPS);
    assertEquals(test.getInterest(), -RATE * expectedYearFraction * NOTIONAL, NOTIONAL * EPS);
    assertEquals(test.getRate(), RATE);
    assertEquals(test.getCurrency(), GBP);
  }

  public void coverage() {
    TermDeposit test1 = TermDeposit.builder()
        .buySell(SELL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .dayCount(ACT_365F)
        .principal(PRINCIPAL)
        .rate(RATE)
        .build();
    coverImmutableBean(test1);
    TermDeposit test2 = TermDeposit.builder()
        .buySell(BuySell.BUY)
        .startDate(LocalDate.of(2015, 1, 21))
        .endDate(LocalDate.of(2015, 7, 21))
        .dayCount(ACT_360)
        .principal(CurrencyAmount.of(EUR, NOTIONAL))
        .rate(RATE)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    TermDeposit test = TermDeposit.builder()
        .buySell(SELL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .dayCount(ACT_365F)
        .principal(PRINCIPAL)
        .rate(RATE)
        .build();
    assertSerialization(test);
  }
}
