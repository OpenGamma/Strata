/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.deposit;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.index.IborIndices.EUR_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.finance.TradeInfo;

/**
 * Test {@link IborFixingDepositConvention}.
 */
@Test
public class IborFixingDepositConventionTest {
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA);
  private static final DaysAdjustment SPOT_ADJ = DaysAdjustment.ofBusinessDays(2, EUTA);
  private static final DaysAdjustment FIXING_ADJ =
      DaysAdjustment.ofBusinessDays(-2, EUTA, BusinessDayAdjustment.of(PRECEDING, GBLO));

  public void test_builder_full() {
    IborFixingDepositConvention test = IborFixingDepositConvention.builder()
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .currency(EUR)
        .dayCount(ACT_365F)
        .fixingDateOffset(FIXING_ADJ)
        .index(EUR_LIBOR_3M)
        .spotDateOffset(SPOT_ADJ)
        .build();
    assertEquals(test.getBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getFixingDateOffset(), FIXING_ADJ);
    assertEquals(test.getIndex(), EUR_LIBOR_3M);
    assertEquals(test.getSpotDateOffset(), SPOT_ADJ);
  }

  public void test_builder_indexOnly() {
    IborFixingDepositConvention test = IborFixingDepositConvention.builder()
        .index(GBP_LIBOR_6M)
        .build();
    assertEquals(test.getBusinessDayAdjustment(),
        BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBP_LIBOR_6M.getFixingCalendar()));
    assertEquals(test.getCurrency(), GBP_LIBOR_6M.getCurrency());
    assertEquals(test.getDayCount(), GBP_LIBOR_6M.getDayCount());
    assertEquals(test.getFixingDateOffset(), GBP_LIBOR_6M.getFixingDateOffset());
    assertEquals(test.getIndex(), GBP_LIBOR_6M);
    assertEquals(test.getSpotDateOffset(), GBP_LIBOR_6M.getEffectiveDateOffset());
  }

  public void test_of_indexOnly() {
    IborFixingDepositConvention test = IborFixingDepositConvention.of(GBP_LIBOR_6M);
    assertEquals(test.getBusinessDayAdjustment(),
        BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBP_LIBOR_6M.getFixingCalendar()));
    assertEquals(test.getCurrency(), GBP_LIBOR_6M.getCurrency());
    assertEquals(test.getDayCount(), GBP_LIBOR_6M.getDayCount());
    assertEquals(test.getFixingDateOffset(), GBP_LIBOR_6M.getFixingDateOffset());
    assertEquals(test.getIndex(), GBP_LIBOR_6M);
    assertEquals(test.getSpotDateOffset(), GBP_LIBOR_6M.getEffectiveDateOffset());
  }

  public void test_expand() {
    IborFixingDepositConvention base = IborFixingDepositConvention.of(EUR_LIBOR_3M);
    IborFixingDepositConvention test = base.expand();
    IborFixingDepositConvention expected = IborFixingDepositConvention.builder()
        .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUR_LIBOR_3M.getFixingCalendar()))
        .currency(EUR_LIBOR_3M.getCurrency())
        .dayCount(EUR_LIBOR_3M.getDayCount())
        .fixingDateOffset(EUR_LIBOR_3M.getFixingDateOffset())
        .index(EUR_LIBOR_3M)
        .spotDateOffset(EUR_LIBOR_3M.getEffectiveDateOffset())
        .build();
    assertTrue(test.equals(expected));
  }

  public void test_toTemplate() {
    IborFixingDepositConvention convention = IborFixingDepositConvention.of(GBP_LIBOR_6M);
    IborFixingDepositTemplate template = convention.toTemplate();
    assertEquals(template.getConvention(), convention);
    assertEquals(template.getDepositPeriod(), GBP_LIBOR_6M.getTenor().getPeriod());
  }

  public void test_toTrade() {
    IborFixingDepositConvention convention = IborFixingDepositConvention.builder()
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .currency(EUR)
        .dayCount(ACT_365F)
        .fixingDateOffset(FIXING_ADJ)
        .index(EUR_LIBOR_3M)
        .spotDateOffset(SPOT_ADJ)
        .build();
    LocalDate tradeDate = LocalDate.of(2015, 1, 22);
    Period depositPeriod = Period.ofMonths(3);
    double notional = 1d;
    double fixedRate = 0.045;
    IborFixingDepositTrade trade = convention.toTrade(tradeDate, depositPeriod, BUY, notional, fixedRate);
    LocalDate startExpected = SPOT_ADJ.adjust(tradeDate);
    LocalDate endExpected = startExpected.plus(depositPeriod);
    IborFixingDeposit productExpected = IborFixingDeposit.builder()
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .buySell(BUY)
        .currency(EUR)
        .dayCount(ACT_365F)
        .startDate(startExpected)
        .endDate(endExpected)
        .fixedRate(fixedRate)
        .fixingDateOffset(FIXING_ADJ)
        .index(EUR_LIBOR_3M)
        .notional(notional)
        .build();
    TradeInfo tradeInfoExpected = TradeInfo.builder()
        .tradeDate(tradeDate)
        .build();
    assertEquals(trade.getProduct(), productExpected);
    assertEquals(trade.getTradeInfo(), tradeInfoExpected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborFixingDepositConvention test1 = IborFixingDepositConvention.of(GBP_LIBOR_6M);
    coverImmutableBean(test1);
    IborFixingDepositConvention test2 = IborFixingDepositConvention.of(EUR_LIBOR_3M);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    IborFixingDepositConvention test = IborFixingDepositConvention.of(GBP_LIBOR_6M);
    assertSerialization(test);
  }

}
