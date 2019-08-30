/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.swap.OvernightAccrualMethod.AVERAGED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link OvernightIborSwapConvention}.
 */
public class OvernightIborSwapConventionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, GBLO);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);

  private static final String NAME = "USD-FF";
  private static final OvernightRateSwapLegConvention FFUND_LEG =
      OvernightRateSwapLegConvention.builder()
          .index(USD_FED_FUND)
          .accrualMethod(AVERAGED)
          .accrualFrequency(Frequency.P3M)
          .paymentFrequency(Frequency.P3M)
          .stubConvention(StubConvention.SMART_INITIAL)
          .rateCutOffDays(2)
          .build();
  private static final OvernightRateSwapLegConvention FFUND_LEG2 =
      OvernightRateSwapLegConvention.of(USD_FED_FUND, P12M, 3);
  private static final OvernightRateSwapLegConvention FLOATING_LEG2 =
      OvernightRateSwapLegConvention.of(GBP_SONIA, P12M, 0);
  private static final IborRateSwapLegConvention USD_LIBOR_3M_LEG = IborRateSwapLegConvention.of(USD_LIBOR_3M);
  private static final IborRateSwapLegConvention GBP_LIBOR_3M_LEG = IborRateSwapLegConvention.of(GBP_LIBOR_3M);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    ImmutableOvernightIborSwapConvention test =
        ImmutableOvernightIborSwapConvention.of(NAME, FFUND_LEG, USD_LIBOR_3M_LEG, PLUS_TWO_DAYS);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getOvernightLeg()).isEqualTo(FFUND_LEG);
    assertThat(test.getIborLeg()).isEqualTo(USD_LIBOR_3M_LEG);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_TWO_DAYS);
  }

  @Test
  public void test_builder() {
    ImmutableOvernightIborSwapConvention test = ImmutableOvernightIborSwapConvention.builder()
        .name(NAME)
        .overnightLeg(FFUND_LEG)
        .iborLeg(USD_LIBOR_3M_LEG)
        .spotDateOffset(PLUS_ONE_DAY)
        .build();
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getOvernightLeg()).isEqualTo(FFUND_LEG);
    assertThat(test.getIborLeg()).isEqualTo(USD_LIBOR_3M_LEG);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_ONE_DAY);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toTrade_tenor() {
    OvernightIborSwapConvention base = ImmutableOvernightIborSwapConvention.of(NAME, FFUND_LEG, USD_LIBOR_3M_LEG, PLUS_TWO_DAYS);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 5, 7);
    LocalDate endDate = date(2025, 5, 7);
    SwapTrade test = base.createTrade(tradeDate, TENOR_10Y, BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        FFUND_LEG.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        USD_LIBOR_3M_LEG.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_toTrade_periodTenor() {
    OvernightIborSwapConvention base = ImmutableOvernightIborSwapConvention.of(NAME, FFUND_LEG, USD_LIBOR_3M_LEG, PLUS_TWO_DAYS);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 7);
    LocalDate endDate = date(2025, 8, 7);
    SwapTrade test = base.createTrade(tradeDate, Period.ofMonths(3), TENOR_10Y, BuySell.SELL, NOTIONAL_2M, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        FFUND_LEG.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M, 0.25d),
        USD_LIBOR_3M_LEG.toLeg(startDate, endDate, PAY, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_toTrade_dates() {
    OvernightIborSwapConvention base = ImmutableOvernightIborSwapConvention.of(NAME, FFUND_LEG, USD_LIBOR_3M_LEG, PLUS_TWO_DAYS);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 5);
    LocalDate endDate = date(2015, 11, 5);
    SwapTrade test = base.toTrade(tradeDate, startDate, endDate, BUY, NOTIONAL_2M, 0.25d);
    Swap expected = Swap.of(
        FFUND_LEG.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        USD_LIBOR_3M_LEG.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {OvernightIborSwapConventions.USD_FED_FUND_AA_LIBOR_3M, "USD-FED-FUND-AA-LIBOR-3M"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(OvernightIborSwapConvention convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(OvernightIborSwapConvention convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(OvernightIborSwapConvention convention, String name) {
    assertThat(OvernightIborSwapConvention.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(OvernightIborSwapConvention convention, String name) {
    OvernightIborSwapConvention.of(name);  // ensures map is populated
    ImmutableMap<String, OvernightIborSwapConvention> map = OvernightIborSwapConvention.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightIborSwapConvention.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightIborSwapConvention.of((String) null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ImmutableOvernightIborSwapConvention test = ImmutableOvernightIborSwapConvention.of(
        NAME, FFUND_LEG, USD_LIBOR_3M_LEG, PLUS_TWO_DAYS);
    coverImmutableBean(test);
    ImmutableOvernightIborSwapConvention test2 = ImmutableOvernightIborSwapConvention.of(
        "GBP-Swap", FLOATING_LEG2, GBP_LIBOR_3M_LEG, PLUS_ONE_DAY);
    coverBeanEquals(test, test2);
    ImmutableOvernightIborSwapConvention test3 = ImmutableOvernightIborSwapConvention.of(
        "USD-Swap2", FFUND_LEG2, USD_LIBOR_3M_LEG, PLUS_ONE_DAY);
    coverBeanEquals(test, test3);
  }

  @Test
  public void test_serialization() {
    ImmutableOvernightIborSwapConvention test = ImmutableOvernightIborSwapConvention.of(
        NAME, FFUND_LEG, USD_LIBOR_3M_LEG, PLUS_TWO_DAYS);
    assertSerialization(test);
  }

}
