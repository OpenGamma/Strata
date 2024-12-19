/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_SOFR;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
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
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link OvernightOvernightSwapConvention}.
 */
public class OvernightOvernightSwapConventionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, GBLO);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);

  private static final String NAME = "USD-SOFR-FF";

  private static final OvernightRateSwapLegConvention SOFR_LEG =
      OvernightRateSwapLegConvention.builder()
          .index(USD_SOFR)
          .accrualFrequency(Frequency.P3M)
          .paymentFrequency(Frequency.P3M)
          .stubConvention(StubConvention.SMART_INITIAL)
          .rateCutOffDays(2)
          .build();

  private static final OvernightRateSwapLegConvention FFUND_LEG =
      OvernightRateSwapLegConvention.builder()
          .index(USD_FED_FUND)
          .accrualFrequency(Frequency.P3M)
          .paymentFrequency(Frequency.P3M)
          .stubConvention(StubConvention.SMART_INITIAL)
          .rateCutOffDays(2)
          .build();

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    OvernightOvernightSwapConvention test =
        ImmutableOvernightOvernightSwapConvention.of(NAME, SOFR_LEG, FFUND_LEG, PLUS_TWO_DAYS);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getSpreadLeg()).isEqualTo(SOFR_LEG);
    assertThat(test.getFlatLeg()).isEqualTo(FFUND_LEG);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_TWO_DAYS);
  }

  @Test
  public void test_builder() {
    ImmutableOvernightOvernightSwapConvention test = ImmutableOvernightOvernightSwapConvention.builder()
        .name(NAME)
        .spreadLeg(SOFR_LEG)
        .flatLeg(FFUND_LEG)
        .spotDateOffset(PLUS_ONE_DAY)
        .build();
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getSpreadLeg()).isEqualTo(SOFR_LEG);
    assertThat(test.getFlatLeg()).isEqualTo(FFUND_LEG);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_ONE_DAY);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toTrade_tenor() {
    OvernightOvernightSwapConvention base =
        ImmutableOvernightOvernightSwapConvention.of(NAME, SOFR_LEG, FFUND_LEG, PLUS_TWO_DAYS);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 5, 7);
    LocalDate endDate = date(2025, 5, 7);
    SwapTrade test = base.createTrade(tradeDate, TENOR_10Y, BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        SOFR_LEG.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        FFUND_LEG.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_toTrade_periodTenor() {
    OvernightOvernightSwapConvention base =
        ImmutableOvernightOvernightSwapConvention.of(NAME, SOFR_LEG, FFUND_LEG, PLUS_TWO_DAYS);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 7);
    LocalDate endDate = date(2025, 8, 7);
    SwapTrade test = base.createTrade(tradeDate, Period.ofMonths(3), TENOR_10Y, BuySell.SELL, NOTIONAL_2M, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        SOFR_LEG.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M, 0.25d),
        FFUND_LEG.toLeg(startDate, endDate, PAY, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_toTrade_dates() {
    OvernightOvernightSwapConvention base =
        ImmutableOvernightOvernightSwapConvention.of(NAME, SOFR_LEG, FFUND_LEG, PLUS_TWO_DAYS);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 5);
    LocalDate endDate = date(2015, 11, 5);
    SwapTrade test = base.toTrade(tradeDate, startDate, endDate, BUY, NOTIONAL_2M, 0.25d);
    Swap expected = Swap.of(
        SOFR_LEG.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        FFUND_LEG.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {OvernightOvernightSwapConventions.USD_SOFR_3M_FED_FUND_3M, "USD-SOFR-3M-FED-FUND-3M"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(OvernightOvernightSwapConvention convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(OvernightOvernightSwapConvention convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(OvernightOvernightSwapConvention convention, String name) {
    assertThat(OvernightOvernightSwapConvention.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(OvernightOvernightSwapConvention convention, String name) {
    OvernightOvernightSwapConvention.of(name);  // ensures map is populated
    ImmutableMap<String, OvernightOvernightSwapConvention> map = OvernightOvernightSwapConvention.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightOvernightSwapConvention.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightOvernightSwapConvention.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ImmutableOvernightOvernightSwapConvention test =
        ImmutableOvernightOvernightSwapConvention.of(NAME, SOFR_LEG, FFUND_LEG, PLUS_TWO_DAYS);
    coverImmutableBean(test);
    ImmutableOvernightOvernightSwapConvention test2 = ImmutableOvernightOvernightSwapConvention.of(
        "EUR-EONIA-ESTR",
        OvernightRateSwapLegConvention.of(OvernightIndices.EUR_EONIA, P3M, 2),
        OvernightRateSwapLegConvention.of(OvernightIndices.EUR_EONIA, P3M, 2),
        PLUS_TWO_DAYS);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    OvernightOvernightSwapConvention test =
        ImmutableOvernightOvernightSwapConvention.of(NAME, SOFR_LEG, FFUND_LEG, PLUS_TWO_DAYS);
    assertSerialization(test);
  }

}
