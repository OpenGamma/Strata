/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_12M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
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
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link ThreeLegBasisSwapConvention}.
 */
public class ThreeLegBasisSwapConventionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, EUTA);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, EUTA);

  private static final String NAME = "EUR-Swap";
  private static final FixedRateSwapLegConvention FIXED =
      FixedRateSwapLegConvention.of(EUR, THIRTY_U_360, P12M, BDA_FOLLOW);
  private static final IborRateSwapLegConvention IBOR3M = IborRateSwapLegConvention.of(EUR_EURIBOR_3M);
  private static final IborRateSwapLegConvention IBOR6M = IborRateSwapLegConvention.of(EUR_EURIBOR_6M);
  private static final IborRateSwapLegConvention IBOR12M = IborRateSwapLegConvention.of(EUR_EURIBOR_12M);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    ImmutableThreeLegBasisSwapConvention test = ImmutableThreeLegBasisSwapConvention.of(NAME, FIXED, IBOR6M, IBOR12M);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getSpreadLeg()).isEqualTo(FIXED);
    assertThat(test.getSpreadFloatingLeg()).isEqualTo(IBOR6M);
    assertThat(test.getFlatFloatingLeg()).isEqualTo(IBOR12M);
    assertThat(test.getSpotDateOffset()).isEqualTo(EUR_EURIBOR_6M.getEffectiveDateOffset());
  }

  @Test
  public void test_of_spotDateOffset() {
    ImmutableThreeLegBasisSwapConvention test =
        ImmutableThreeLegBasisSwapConvention.of(NAME, FIXED, IBOR6M, IBOR12M, PLUS_ONE_DAY);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getSpreadLeg()).isEqualTo(FIXED);
    assertThat(test.getSpreadFloatingLeg()).isEqualTo(IBOR6M);
    assertThat(test.getFlatFloatingLeg()).isEqualTo(IBOR12M);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_ONE_DAY);
  }

  @Test
  public void test_builder() {
    ImmutableThreeLegBasisSwapConvention test = ImmutableThreeLegBasisSwapConvention.builder()
        .name(NAME)
        .spreadLeg(FIXED)
        .spreadFloatingLeg(IBOR6M)
        .flatFloatingLeg(IBOR12M)
        .spotDateOffset(PLUS_ONE_DAY)
        .build();
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getSpreadLeg()).isEqualTo(FIXED);
    assertThat(test.getSpreadFloatingLeg()).isEqualTo(IBOR6M);
    assertThat(test.getFlatFloatingLeg()).isEqualTo(IBOR12M);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_ONE_DAY);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toTrade_tenor() {
    ThreeLegBasisSwapConvention base = ImmutableThreeLegBasisSwapConvention.of(NAME, FIXED, IBOR6M, IBOR12M);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 5, 7);
    LocalDate endDate = date(2025, 5, 7);
    SwapTrade test = base.createTrade(tradeDate, TENOR_10Y, BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        IBOR6M.toLeg(startDate, endDate, PAY, NOTIONAL_2M),
        IBOR12M.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_toTrade_periodTenor() {
    ThreeLegBasisSwapConvention base = ImmutableThreeLegBasisSwapConvention.of(NAME, FIXED, IBOR6M, IBOR12M);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 7);
    LocalDate endDate = date(2025, 8, 7);
    SwapTrade test = base.createTrade(tradeDate, Period.ofMonths(3), TENOR_10Y, BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        IBOR6M.toLeg(startDate, endDate, PAY, NOTIONAL_2M),
        IBOR12M.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_toTrade_dates() {
    ThreeLegBasisSwapConvention base = ImmutableThreeLegBasisSwapConvention.of(NAME, FIXED, IBOR6M, IBOR12M);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 5);
    LocalDate endDate = date(2015, 11, 5);
    SwapTrade test = base.toTrade(tradeDate, startDate, endDate, BUY, NOTIONAL_2M, 0.25d);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        IBOR6M.toLeg(startDate, endDate, PAY, NOTIONAL_2M),
        IBOR12M.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M, "EUR-FIXED-1Y-EURIBOR-3M-EURIBOR-6M"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(ThreeLegBasisSwapConvention convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(ThreeLegBasisSwapConvention convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(ThreeLegBasisSwapConvention convention, String name) {
    assertThat(ThreeLegBasisSwapConvention.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(ThreeLegBasisSwapConvention convention, String name) {
    ThreeLegBasisSwapConvention.of(name);  // ensures map is populated
    ImmutableMap<String, ThreeLegBasisSwapConvention> map = ThreeLegBasisSwapConvention.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ThreeLegBasisSwapConvention.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ThreeLegBasisSwapConvention.of((String) null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ImmutableThreeLegBasisSwapConvention test = ImmutableThreeLegBasisSwapConvention.of(NAME, FIXED, IBOR6M, IBOR12M);
    coverImmutableBean(test);
    ImmutableThreeLegBasisSwapConvention test2 = ImmutableThreeLegBasisSwapConvention.of("swap", FIXED, IBOR3M, IBOR6M);
    coverBeanEquals(test, test2);
    ImmutableThreeLegBasisSwapConvention test3 = ImmutableThreeLegBasisSwapConvention.of(NAME, FIXED, IBOR3M, IBOR12M);
    coverBeanEquals(test, test3);
  }

  @Test
  public void test_serialization() {
    ThreeLegBasisSwapConvention test = ImmutableThreeLegBasisSwapConvention.of(NAME, FIXED, IBOR6M, IBOR12M);
    assertSerialization(test);
  }

}
