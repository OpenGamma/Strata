/*
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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.fra.FraDiscountingMethod.AFMA;
import static com.opengamma.strata.product.fra.FraDiscountingMethod.ISDA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
  @Test
  public void test_of_index() {
    ImmutableFraConvention test = ImmutableFraConvention.of(GBP_LIBOR_3M);
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_3M);
    assertThat(test.getName()).isEqualTo(GBP_LIBOR_3M.getName());
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getSpotDateOffset()).isEqualTo(GBP_LIBOR_3M.getEffectiveDateOffset());
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getPaymentDateOffset()).isEqualTo(DaysAdjustment.NONE);
    assertThat(test.getFixingDateOffset()).isEqualTo(GBP_LIBOR_3M.getFixingDateOffset());
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDiscounting()).isEqualTo(ISDA);
    // ensure other factories match
    assertThat(FraConvention.of(GBP_LIBOR_3M)).isEqualTo(test);
    assertThat(FraConventions.of(GBP_LIBOR_3M)).isEqualTo(test);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_noIndex() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ImmutableFraConvention.builder()
            .spotDateOffset(NEXT_SAME_BUS_DAY)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_minSpecified() {
    ImmutableFraConvention test = ImmutableFraConvention.builder()
        .index(GBP_LIBOR_3M)
        .build();
    assertThat(test.getName()).isEqualTo(GBP_LIBOR_3M.getName());
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_3M);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getSpotDateOffset()).isEqualTo(GBP_LIBOR_3M.getEffectiveDateOffset());
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getPaymentDateOffset()).isEqualTo(DaysAdjustment.NONE);
    assertThat(test.getFixingDateOffset()).isEqualTo(GBP_LIBOR_3M.getFixingDateOffset());
    assertThat(test.getDayCount()).isEqualTo(GBP_LIBOR_3M.getDayCount());
    assertThat(test.getDiscounting()).isEqualTo(ISDA);
  }

  @Test
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
    assertThat(test.getName()).isEqualTo(GBP_LIBOR_3M.getName());
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_3M);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_ONE_DAY);
    assertThat(test.getBusinessDayAdjustment()).isEqualTo(BDA_FOLLOW);
    assertThat(test.getPaymentDateOffset()).isEqualTo(PLUS_TWO_DAYS);
    assertThat(test.getFixingDateOffset()).isEqualTo(MINUS_FIVE_DAYS);
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getDiscounting()).isEqualTo(FraDiscountingMethod.NONE);
  }

  @Test
  public void test_builder_AUD() {
    ImmutableFraConvention test = ImmutableFraConvention.of(AUD_INDEX);
    assertThat(test.getIndex()).isEqualTo(AUD_INDEX);
    assertThat(test.getDiscounting()).isEqualTo(AFMA);
  }

  @Test
  public void test_builder_NZD() {
    ImmutableFraConvention test = ImmutableFraConvention.of(NZD_INDEX);
    assertThat(test.getIndex()).isEqualTo(NZD_INDEX);
    assertThat(test.getDiscounting()).isEqualTo(AFMA);
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
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
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
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
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
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
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_unknownIndex() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FraConvention.of("Rubbish"));
  }

  @Test
  public void test_toTemplate_badDateOrder() {
    FraConvention base = FraConvention.of(GBP_LIBOR_3M);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 4, 5);
    LocalDate endDate = date(2015, 7, 5);
    LocalDate paymentDate = date(2015, 8, 7);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.toTrade(tradeDate, startDate, endDate, paymentDate, BUY, NOTIONAL_2M, 0.25d));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {ImmutableFraConvention.of(GBP_LIBOR_3M), "GBP-LIBOR-3M"},
        {ImmutableFraConvention.of(USD_LIBOR_3M), "USD-LIBOR-3M"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(FraConvention convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(FraConvention convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(FraConvention convention, String name) {
    assertThat(FraConvention.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(FraConvention convention, String name) {
    FraConvention.of(name);  // ensures map is populated
    ImmutableMap<String, FraConvention> map = FraConvention.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FraConvention.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FraConvention.of((String) null));
  }

  //-------------------------------------------------------------------------
  @Test
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

  @Test
  public void test_serialization() {
    ImmutableFraConvention test = ImmutableFraConvention.builder()
        .index(GBP_LIBOR_3M)
        .build();
    assertSerialization(test);
  }

}
