/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra.type;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.NZD;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.fra.FraDiscountingMethod.AFMA;
import static com.opengamma.strata.product.fra.FraDiscountingMethod.ISDA;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Optional;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.TenorAdjustment;
import com.opengamma.strata.basics.index.ImmutableIborIndex;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraDiscountingMethod;
import com.opengamma.strata.product.fra.FraTrade;

/**
 * Test {@link FraConvention}.
 */
@Test
public class FraConventionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, GBLO);
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment NEXT_SAME_BUS_DAY = DaysAdjustment.ofCalendarDays(0, BDA_FOLLOW);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, GBLO);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);
  private static final DaysAdjustment MINUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(-2, GBLO);
  private static final DaysAdjustment MINUS_FIVE_DAYS = DaysAdjustment.ofBusinessDays(-5, GBLO);
  private static final ImmutableIborIndex AUD_INDEX = ImmutableIborIndex.builder()
      .name("AUD-INDEX-3M")
      .currency(AUD)
      .dayCount(ACT_360)
      .fixingDateOffset(MINUS_TWO_DAYS)
      .effectiveDateOffset(PLUS_TWO_DAYS)
      .maturityDateOffset(TenorAdjustment.ofLastDay(TENOR_3M, BDA_MOD_FOLLOW))
      .fixingCalendar(SAT_SUN)
      .fixingTime(LocalTime.NOON)
      .fixingZone(ZoneId.of("Australia/Sydney"))
      .build();
  private static final ImmutableIborIndex NZD_INDEX = ImmutableIborIndex.builder()
      .name("NZD-INDEX-3M")
      .currency(NZD)
      .dayCount(ACT_360)
      .fixingDateOffset(MINUS_TWO_DAYS)
      .effectiveDateOffset(PLUS_TWO_DAYS)
      .maturityDateOffset(TenorAdjustment.ofLastDay(TENOR_3M, BDA_MOD_FOLLOW))
      .fixingCalendar(SAT_SUN)
      .fixingTime(LocalTime.NOON)
      .fixingZone(ZoneId.of("NZ"))
      .build();

  //-------------------------------------------------------------------------
  public void test_of_index() {
    ImmutableFraConvention test = ImmutableFraConvention.of(GBP_LIBOR_3M);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getName(), GBP_LIBOR_3M.getName());
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getSpotDateOffset(), GBP_LIBOR_3M.getEffectiveDateOffset());
    assertEquals(test.getBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getPaymentDateOffset(), DaysAdjustment.NONE);
    assertEquals(test.getFixingDateOffset(), GBP_LIBOR_3M.getFixingDateOffset());
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getDiscounting(), ISDA);
    // ensure other factories match
    assertEquals(FraConvention.of(GBP_LIBOR_3M), test);
    assertEquals(FraConventions.of(GBP_LIBOR_3M), test);
  }

  //-------------------------------------------------------------------------
  public void test_builder_noIndex() {
    assertThrowsIllegalArg(() -> ImmutableFraConvention.builder()
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_builder_minSpecified() {
    ImmutableFraConvention test = ImmutableFraConvention.builder()
        .index(GBP_LIBOR_3M)
        .build();
    assertEquals(test.getName(), GBP_LIBOR_3M.getName());
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getSpotDateOffset(), GBP_LIBOR_3M.getEffectiveDateOffset());
    assertEquals(test.getBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getPaymentDateOffset(), DaysAdjustment.NONE);
    assertEquals(test.getFixingDateOffset(), GBP_LIBOR_3M.getFixingDateOffset());
    assertEquals(test.getDayCount(), GBP_LIBOR_3M.getDayCount());
    assertEquals(test.getDiscounting(), ISDA);
  }

  public void test_builder_allSpecified() {
    ImmutableFraConvention test = ImmutableFraConvention.builder()
        .name(GBP_LIBOR_3M.getName())
        .index(GBP_LIBOR_3M)
        .currency(GBP)
        .spotDateOffset(PLUS_ONE_DAY)
        .businessDayAdjustment(BDA_FOLLOW)
        .paymentDateOffset(PLUS_TWO_DAYS)
        .fixingDateOffset(MINUS_FIVE_DAYS)
        .dayCount(ACT_360)
        .discounting(FraDiscountingMethod.NONE)
        .build();
    assertEquals(test.getName(), GBP_LIBOR_3M.getName());
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getSpotDateOffset(), PLUS_ONE_DAY);
    assertEquals(test.getBusinessDayAdjustment(), BDA_FOLLOW);
    assertEquals(test.getPaymentDateOffset(), PLUS_TWO_DAYS);
    assertEquals(test.getFixingDateOffset(), MINUS_FIVE_DAYS);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getDiscounting(), FraDiscountingMethod.NONE);
  }

  public void test_builder_AUD() {
    ImmutableFraConvention test = ImmutableFraConvention.of(AUD_INDEX);
    assertEquals(test.getIndex(), AUD_INDEX);
    assertEquals(test.getDiscounting(), AFMA);
  }

  public void test_builder_NZD() {
    ImmutableFraConvention test = ImmutableFraConvention.of(NZD_INDEX);
    assertEquals(test.getIndex(), NZD_INDEX);
    assertEquals(test.getDiscounting(), AFMA);
  }

  //-------------------------------------------------------------------------
  public void test_createTrade_period() {
    FraConvention base = ImmutableFraConvention.builder()
        .index(GBP_LIBOR_3M)
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .build();
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    FraTrade test = base.createTrade(tradeDate, Period.ofMonths(3), BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Fra expected = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_2M)
        .startDate(date(2015, 8, 5))
        .endDate(date(2015, 11, 5))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build();
    assertEquals(test.getInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_createTrade_periods() {
    FraConvention base = ImmutableFraConvention.builder()
        .index(GBP_LIBOR_3M)
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .build();
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    FraTrade test = base.createTrade(tradeDate, Period.ofMonths(3), Period.ofMonths(6), BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Fra expected = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_2M)
        .startDate(date(2015, 8, 5))
        .endDate(date(2015, 11, 5))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build();
    assertEquals(test.getInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_createTrade_periods_adjust() {
    FraConvention base = ImmutableFraConvention.builder()
        .index(GBP_LIBOR_3M)
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BDA_FOLLOW))
        .build();
    LocalDate tradeDate = LocalDate.of(2016, 8, 11);
    FraTrade test = base.createTrade(tradeDate, Period.ofMonths(1), Period.ofMonths(4), BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Fra expected = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_2M)
        .startDate(date(2016, 9, 12))
        .endDate(date(2016, 12, 12))
        .paymentDate(AdjustableDate.of(date(2016, 9, 12), BDA_FOLLOW))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build();
    assertEquals(test.getInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_createTrade_periods_adjust_payOffset() {
    FraConvention base = ImmutableFraConvention.builder()
        .index(GBP_LIBOR_3M)
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .paymentDateOffset(PLUS_TWO_DAYS)
        .build();
    LocalDate tradeDate = LocalDate.of(2016, 8, 11);
    FraTrade test = base.createTrade(tradeDate, Period.ofMonths(1), Period.ofMonths(4), BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Fra expected = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_2M)
        .startDate(date(2016, 9, 12))
        .endDate(date(2016, 12, 12))
        .paymentDate(AdjustableDate.of(date(2016, 9, 14), PLUS_TWO_DAYS.getAdjustment()))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build();
    assertEquals(test.getInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_toTrade_dates() {
    FraConvention base = ImmutableFraConvention.builder()
        .index(GBP_LIBOR_3M)
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .build();
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 5);
    LocalDate endDate = date(2015, 11, 5);
    LocalDate paymentDate = startDate;
    FraTrade test = base.toTrade(tradeDate, startDate, endDate, startDate, BUY, NOTIONAL_2M, 0.25d);
    Fra expected = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_2M)
        .startDate(startDate)
        .endDate(endDate)
        .paymentDate(AdjustableDate.of(paymentDate))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build();
    assertEquals(test.getInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_toTrade_dates_paymentOffset() {
    FraConvention base = ImmutableFraConvention.builder()
        .index(GBP_LIBOR_3M)
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .paymentDateOffset(PLUS_TWO_DAYS)
        .build();
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 5);
    LocalDate endDate = date(2015, 11, 5);
    LocalDate paymentDate = date(2015, 8, 7);
    FraTrade test = base.toTrade(tradeDate, startDate, endDate, paymentDate, BUY, NOTIONAL_2M, 0.25d);
    Fra expected = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_2M)
        .startDate(date(2015, 8, 5))
        .endDate(date(2015, 11, 5))
        .paymentDate(AdjustableDate.of(paymentDate, PLUS_TWO_DAYS.getAdjustment()))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build();
    assertEquals(test.getInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_unknownIndex() {
    assertThrowsIllegalArg(() -> FraConvention.of("Rubbish"));
  }

  public void test_toTemplate_badDateOrder() {
    FraConvention base = FraConvention.of(GBP_LIBOR_3M);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 4, 5);
    LocalDate endDate = date(2015, 7, 5);
    LocalDate paymentDate = date(2015, 8, 7);
    assertThrowsIllegalArg(() -> base.toTrade(tradeDate, startDate, endDate, paymentDate, BUY, NOTIONAL_2M, 0.25d));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {ImmutableFraConvention.of(GBP_LIBOR_3M), "GBP-LIBOR-3M"},
        {ImmutableFraConvention.of(USD_LIBOR_3M), "USD-LIBOR-3M"},
    };
  }

  @Test(dataProvider = "name")
  public void test_name(FraConvention convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(FraConvention convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(FraConvention convention, String name) {
    assertEquals(FraConvention.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(FraConvention convention, String name) {
    FraConvention.of(name);  // ensures map is populated
    ImmutableMap<String, FraConvention> map = FraConvention.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> FraConvention.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> FraConvention.of((String) null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableFraConvention test = ImmutableFraConvention.builder()
        .index(GBP_LIBOR_3M)
        .build();
    coverImmutableBean(test);
    ImmutableFraConvention test2 = ImmutableFraConvention.builder()
        .index(GBP_LIBOR_3M)
        .name("Test")
        .currency(USD)
        .spotDateOffset(PLUS_ONE_DAY)
        .businessDayAdjustment(BDA_FOLLOW)
        .paymentDateOffset(PLUS_TWO_DAYS)
        .fixingDateOffset(MINUS_FIVE_DAYS)
        .dayCount(ACT_360)
        .discounting(FraDiscountingMethod.NONE)
        .build();
    coverBeanEquals(test, test2);

    coverPrivateConstructor(FraConventions.class);
    coverPrivateConstructor(FraConventionLookup.class);
  }

  public void test_serialization() {
    ImmutableFraConvention test = ImmutableFraConvention.builder()
        .index(GBP_LIBOR_3M)
        .build();
    assertSerialization(test);
  }

}
