/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.deposit;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.finance.rate.IborRateObservation;

/**
 * Test {@link IborFixingDeposit}.
 */
@Test
public class IborFixingDepositTest {

  private static final BuySell SELL = BuySell.SELL;
  private static final LocalDate START_DATE = LocalDate.of(2015, 1, 19);
  private static final LocalDate END_DATE = LocalDate.of(2015, 7, 19);
  private static final double NOTIONAL = 100000000d;
  private static final double RATE = 0.0250;
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment DAY_ADJ = DaysAdjustment.ofBusinessDays(1, GBLO);
  private static final double EPS = 1.0e-14;

  public void test_builder_full() {
    IborFixingDeposit test = IborFixingDeposit.builder()
        .buySell(SELL)
        .notional(NOTIONAL)
        .currency(GBP)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .fixingDateOffset(DAY_ADJ)
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_6M)
        .rate(RATE)
        .build();
    assertEquals(test.getBusinessDayAdjustment().get(), BDA_MOD_FOLLOW);
    assertEquals(test.getBuySell(), SELL);
    assertEquals(test.getFixingDateOffset(), DAY_ADJ);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getStartDate(), START_DATE);
    assertEquals(test.getEndDate(), END_DATE);
    assertEquals(test.getIndex(), GBP_LIBOR_6M);
    assertEquals(test.getRate(), RATE);
  }

  public void test_builder_minimum() {
    IborFixingDeposit test = IborFixingDeposit.builder()
        .buySell(SELL)
        .notional(NOTIONAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .index(GBP_LIBOR_6M)
        .rate(RATE)
        .build();
    assertEquals(test.getBusinessDayAdjustment().get(), BDA_MOD_FOLLOW);
    assertEquals(test.getBuySell(), SELL);
    assertEquals(test.getFixingDateOffset(), GBP_LIBOR_6M.getFixingDateOffset());
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getStartDate(), START_DATE);
    assertEquals(test.getEndDate(), END_DATE);
    assertEquals(test.getIndex(), GBP_LIBOR_6M);
    assertEquals(test.getRate(), RATE);
  }

  public void test_builder_wrongDates() {
    assertThrowsIllegalArg(() -> IborFixingDeposit.builder()
        .buySell(SELL)
        .notional(NOTIONAL)
        .startDate(LocalDate.of(2015, 9, 19))
        .endDate(END_DATE)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .index(GBP_LIBOR_6M)
        .rate(RATE)
        .build());
  }

  public void test_expand() {
    IborFixingDeposit base = IborFixingDeposit.builder()
        .buySell(SELL)
        .notional(NOTIONAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .index(GBP_LIBOR_6M)
        .rate(RATE)
        .build();
    ExpandedIborFixingDeposit test = base.expand();
    LocalDate expectedEndDate = BDA_MOD_FOLLOW.adjust(END_DATE);
    double expectedYearFraction = ACT_365F.yearFraction(START_DATE, expectedEndDate);
    IborRateObservation expectedObservation = IborRateObservation.of(
        GBP_LIBOR_6M, GBP_LIBOR_6M.getFixingDateOffset().adjust(START_DATE));
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getStartDate(), START_DATE);
    assertEquals(test.getEndDate(), expectedEndDate);
    assertEquals(test.getFloatingRate(), expectedObservation);
    assertEquals(test.getNotional(), -NOTIONAL);
    assertEquals(test.getRate(), RATE);
    assertEquals(test.getYearFraction(), expectedYearFraction);
  }

  public void coverage() {
    IborFixingDeposit test1 = IborFixingDeposit.builder()
        .buySell(SELL)
        .notional(NOTIONAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .index(GBP_LIBOR_6M)
        .rate(RATE)
        .build();
    coverImmutableBean(test1);
    IborFixingDeposit test2 = IborFixingDeposit.builder()
        .buySell(BuySell.BUY)
        .notional(NOTIONAL)
        .startDate(LocalDate.of(2015, 1, 19))
        .endDate(LocalDate.of(2015, 4, 19))
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .index(GBP_LIBOR_3M)
        .rate(0.015)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    IborFixingDeposit test = IborFixingDeposit.builder()
        .buySell(SELL)
        .notional(NOTIONAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .index(GBP_LIBOR_6M)
        .rate(RATE)
        .build();
    assertSerialization(test);
  }
}
