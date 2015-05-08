/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.fra;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.NZD;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.SAT_SUN;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.finance.rate.fra.FraDiscountingMethod.AFMA;
import static com.opengamma.strata.finance.rate.fra.FraDiscountingMethod.ISDA;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.TenorAdjustment;
import com.opengamma.strata.basics.index.ImmutableIborIndex;
import com.opengamma.strata.collect.id.StandardId;

/**
 * Test {@link FraConvention}.
 */
@Test
public class FraConventionTest {

  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, GBLO);
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment NEXT_SAME_BUS_DAY = DaysAdjustment.ofCalendarDays(0, BDA_FOLLOW);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, GBLO);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);
  private static final DaysAdjustment MINUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(-2, GBLO);
  private static final DaysAdjustment MINUS_FIVE_DAYS = DaysAdjustment.ofBusinessDays(-5, GBLO);
  private static final StandardId STANDARD_ID = StandardId.of("A", "B");
  private static final ImmutableIborIndex AUD_INDEX = ImmutableIborIndex.builder()
      .name("AUD_INDEX")
      .currency(AUD)
      .dayCount(ACT_360)
      .fixingDateOffset(MINUS_TWO_DAYS)
      .effectiveDateOffset(PLUS_TWO_DAYS)
      .maturityDateOffset(TenorAdjustment.ofLastDay(TENOR_3M, BDA_MOD_FOLLOW))
      .fixingCalendar(SAT_SUN)
      .build();
  private static final ImmutableIborIndex NZD_INDEX = ImmutableIborIndex.builder()
      .name("NZD")
      .currency(NZD)
      .dayCount(ACT_360)
      .fixingDateOffset(MINUS_TWO_DAYS)
      .effectiveDateOffset(PLUS_TWO_DAYS)
      .maturityDateOffset(TenorAdjustment.ofLastDay(TENOR_3M, BDA_MOD_FOLLOW))
      .fixingCalendar(SAT_SUN)
      .build();

  //-------------------------------------------------------------------------
  public void test_of_index() {
    FraConvention test = FraConvention.of(GBP_LIBOR_3M);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getSpotDateOffset(), GBP_LIBOR_3M.getEffectiveDateOffset());
    assertEquals(test.getBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getPaymentDateOffset(), DaysAdjustment.NONE);
    assertEquals(test.getFixingDateOffset(), GBP_LIBOR_3M.getFixingDateOffset());
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getDiscounting(), ISDA);
  }

  //-------------------------------------------------------------------------
  public void test_builder_noIndex() {
    assertThrowsIllegalArg(() -> FraConvention.builder()
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    FraConvention test = FraConvention.of(GBP_LIBOR_3M).expand();
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getSpotDateOffset(), GBP_LIBOR_3M.getEffectiveDateOffset());
    assertEquals(test.getBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getPaymentDateOffset(), DaysAdjustment.NONE);
    assertEquals(test.getFixingDateOffset(), GBP_LIBOR_3M.getFixingDateOffset());
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getDiscounting(), ISDA);
  }

  public void test_expandAllSpecified() {
    FraConvention test = FraConvention.builder()
        .index(GBP_LIBOR_3M)
        .currency(GBP)
        .spotDateOffset(PLUS_ONE_DAY)
        .businessDayAdjustment(BDA_FOLLOW)
        .paymentDateOffset(PLUS_TWO_DAYS)
        .fixingDateOffset(MINUS_FIVE_DAYS)
        .dayCount(ACT_360)
        .discounting(FraDiscountingMethod.NONE)
        .build()
        .expand();
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getSpotDateOffset(), PLUS_ONE_DAY);
    assertEquals(test.getBusinessDayAdjustment(), BDA_FOLLOW);
    assertEquals(test.getPaymentDateOffset(), PLUS_TWO_DAYS);
    assertEquals(test.getFixingDateOffset(), MINUS_FIVE_DAYS);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getDiscounting(), FraDiscountingMethod.NONE);
  }

  public void test_expand_AUD() {
    FraConvention test = FraConvention.of(AUD_INDEX).expand();
    assertEquals(test.getCurrency(), AUD);
    assertEquals(test.getSpotDateOffset(), PLUS_TWO_DAYS);
    assertEquals(test.getBusinessDayAdjustment(), BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN));
    assertEquals(test.getPaymentDateOffset(), DaysAdjustment.NONE);
    assertEquals(test.getFixingDateOffset(), MINUS_TWO_DAYS);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getDiscounting(), AFMA);
  }

  public void test_expand_NZD() {
    FraConvention test = FraConvention.of(NZD_INDEX).expand();
    assertEquals(test.getCurrency(), NZD);
    assertEquals(test.getSpotDateOffset(), PLUS_TWO_DAYS);
    assertEquals(test.getBusinessDayAdjustment(), BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN));
    assertEquals(test.getPaymentDateOffset(), DaysAdjustment.NONE);
    assertEquals(test.getFixingDateOffset(), MINUS_TWO_DAYS);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getDiscounting(), AFMA);
  }

  //-------------------------------------------------------------------------
  public void test_toTemplate() {
    FraConvention base = FraConvention.builder()
        .index(GBP_LIBOR_3M)
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .build();
    FraTemplate test = base.toTemplate(Period.ofMonths(2));
    FraTemplate expected = FraTemplate.of(Period.ofMonths(2), Period.ofMonths(5), base);
    assertEquals(test, expected);
  }

  public void test_toTemplate_negativePeriod() {
    FraConvention base = FraConvention.of(GBP_LIBOR_3M);
    assertThrowsIllegalArg(() -> base.toTemplate(Period.ofMonths(-1)));
  }

  //-------------------------------------------------------------------------
  public void test_toTrade_periods() {
    FraConvention base = FraConvention.builder()
        .index(GBP_LIBOR_3M)
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .build();
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    FraTrade test = base.toTrade(STANDARD_ID, tradeDate, Period.ofMonths(3), Period.ofMonths(6), BUY, NOTIONAL_2M, 0.25d);
    Fra expected = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_2M)
        .startDate(date(2015, 8, 5))
        .endDate(date(2015, 11, 5))
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build();
    assertEquals(test.getStandardId(), STANDARD_ID);
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_toTrade_dates() {
    FraConvention base = FraConvention.builder()
        .index(GBP_LIBOR_3M)
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .build();
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 5);
    LocalDate endDate = date(2015, 11, 5);
    FraTrade test = base.toTrade(STANDARD_ID, tradeDate, startDate, endDate, BUY, NOTIONAL_2M, 0.25d);
    Fra expected = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_2M)
        .startDate(startDate)
        .endDate(endDate)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build();
    assertEquals(test.getStandardId(), STANDARD_ID);
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_toTrade_dates_paymentOffset() {
    FraConvention base = FraConvention.builder()
        .index(GBP_LIBOR_3M)
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .paymentDateOffset(PLUS_TWO_DAYS)
        .build();
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 5);
    LocalDate endDate = date(2015, 11, 5);
    FraTrade test = base.toTrade(STANDARD_ID, tradeDate, startDate, endDate, BUY, NOTIONAL_2M, 0.25d);
    Fra expected = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_2M)
        .startDate(date(2015, 8, 5))
        .endDate(date(2015, 11, 5))
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .paymentDateOffset(PLUS_TWO_DAYS)
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build();
    assertEquals(test.getStandardId(), STANDARD_ID);
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_toTemplate_badDateOrder() {
    FraConvention base = FraConvention.of(GBP_LIBOR_3M);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 4, 5);
    LocalDate endDate = date(2015, 7, 5);
    assertThrowsIllegalArg(() -> base.toTrade(STANDARD_ID, tradeDate, startDate, endDate, BUY, NOTIONAL_2M, 0.25d));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FraConvention test = FraConvention.builder()
        .index(GBP_LIBOR_3M)
        .build();
    coverImmutableBean(test);
    FraConvention test2 = FraConvention.builder()
        .index(GBP_LIBOR_3M)
        .currency(USD)
        .spotDateOffset(PLUS_ONE_DAY)
        .businessDayAdjustment(BDA_FOLLOW)
        .paymentDateOffset(PLUS_TWO_DAYS)
        .fixingDateOffset(MINUS_FIVE_DAYS)
        .dayCount(ACT_360)
        .discounting(FraDiscountingMethod.NONE)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FraConvention test = FraConvention.builder()
        .index(GBP_LIBOR_3M)
        .build();
    assertSerialization(test);
  }

}
