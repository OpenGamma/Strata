/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
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
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link ImmutableXCcyOvernightOvernightSwapConvention}.
 */
public class ImmutableXCcyOvernightOvernightSwapConventionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);

  private static final String NAME = "EUR-ESTR-3M-USD-SOFR-3M";
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final CurrencyPair EUR_USD = CurrencyPair.of(Currency.EUR, Currency.USD);
  private static final double FX_EUR_USD = 1.15d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, EUTA_USNY);
  private static final DaysAdjustment NEXT_SAME_BUS_DAY = DaysAdjustment.ofCalendarDays(0, BDA_FOLLOW);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, EUTA_USNY);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, EUTA_USNY);

  private static final OvernightRateSwapLegConvention EUR_ON_Q =
      OvernightRateSwapLegConvention.builder()
          .index(OvernightIndices.EUR_ESTR)
          .paymentFrequency(Frequency.P3M)
          .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA_USNY))
          .stubConvention(StubConvention.SMART_INITIAL)
          .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY))
          .notionalExchange(true)
          .build();
  private static final OvernightRateSwapLegConvention USD_ON_Q =
      OvernightRateSwapLegConvention.builder()
          .index(OvernightIndices.USD_SOFR)
          .paymentFrequency(Frequency.P3M)
          .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA_USNY))
          .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY))
          .notionalExchange(true)
          .build();

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    ImmutableXCcyOvernightOvernightSwapConvention test =
        ImmutableXCcyOvernightOvernightSwapConvention.of(NAME, EUR_ON_Q, USD_ON_Q, PLUS_TWO_DAYS);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getSpreadLeg()).isEqualTo(EUR_ON_Q);
    assertThat(test.getFlatLeg()).isEqualTo(USD_ON_Q);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_TWO_DAYS);
    assertThat(test.getCurrencyPair()).isEqualTo(EUR_USD);
  }

  @Test
  public void test_builder() {
    ImmutableXCcyOvernightOvernightSwapConvention test =
        ImmutableXCcyOvernightOvernightSwapConvention.builder()
            .name(NAME)
            .spreadLeg(EUR_ON_Q)
            .flatLeg(USD_ON_Q)
            .spotDateOffset(PLUS_ONE_DAY)
            .build();
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getSpreadLeg()).isEqualTo(EUR_ON_Q);
    assertThat(test.getFlatLeg()).isEqualTo(USD_ON_Q);
    assertThat(test.getSpotDateOffset()).isEqualTo(PLUS_ONE_DAY);
    assertThat(test.getCurrencyPair()).isEqualTo(EUR_USD);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toTrade_tenor() {
    ImmutableXCcyOvernightOvernightSwapConvention base =
        ImmutableXCcyOvernightOvernightSwapConvention.of(NAME, EUR_ON_Q, USD_ON_Q, PLUS_TWO_DAYS);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 5, 7);
    LocalDate endDate = date(2025, 5, 7);
    SwapTrade test = base.createTrade(tradeDate, TENOR_10Y, BUY, NOTIONAL_2M, NOTIONAL_2M * FX_EUR_USD, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        EUR_ON_Q.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        USD_ON_Q.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M * FX_EUR_USD));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_toTrade_periodTenor() {
    ImmutableXCcyOvernightOvernightSwapConvention base =
        ImmutableXCcyOvernightOvernightSwapConvention.of(NAME, EUR_ON_Q, USD_ON_Q, PLUS_TWO_DAYS);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 7);
    LocalDate endDate = date(2025, 8, 7);
    SwapTrade test = base.createTrade(
        tradeDate, Period.ofMonths(3), TENOR_10Y, BUY, NOTIONAL_2M, NOTIONAL_2M * FX_EUR_USD, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        EUR_ON_Q.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        USD_ON_Q.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M * FX_EUR_USD));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_toTrade_dates() {
    ImmutableXCcyOvernightOvernightSwapConvention base =
        ImmutableXCcyOvernightOvernightSwapConvention.of(NAME, EUR_ON_Q, USD_ON_Q, PLUS_TWO_DAYS);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 5);
    LocalDate endDate = date(2015, 11, 5);
    SwapTrade test = base.toTrade(tradeDate, startDate, endDate, BUY, NOTIONAL_2M, NOTIONAL_2M * FX_EUR_USD, 0.25d);
    Swap expected = Swap.of(
        EUR_ON_Q.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        USD_ON_Q.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M * FX_EUR_USD));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {XCcyOvernightOvernightSwapConventions.EUR_ESTR_3M_USD_SOFR_3M, "EUR-ESTR-3M-USD-SOFR-3M"},
        {XCcyOvernightOvernightSwapConventions.GBP_SONIA_3M_USD_SOFR_3M, "GBP-SONIA-3M-USD-SOFR-3M"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(ImmutableXCcyOvernightOvernightSwapConvention convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(ImmutableXCcyOvernightOvernightSwapConvention convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(ImmutableXCcyOvernightOvernightSwapConvention convention, String name) {
    assertThat(XCcyOvernightOvernightSwapConvention.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(ImmutableXCcyOvernightOvernightSwapConvention convention, String name) {
    XCcyOvernightOvernightSwapConvention.of(name);  // ensures map is populated
    ImmutableMap<String, XCcyOvernightOvernightSwapConvention> map = 
        XCcyOvernightOvernightSwapConvention.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> XCcyOvernightOvernightSwapConvention.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> XCcyOvernightOvernightSwapConvention.of((String) null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ImmutableXCcyOvernightOvernightSwapConvention test =
        ImmutableXCcyOvernightOvernightSwapConvention.of(NAME, EUR_ON_Q, USD_ON_Q, PLUS_TWO_DAYS);
    coverImmutableBean(test);
    ImmutableXCcyOvernightOvernightSwapConvention test2 =
        ImmutableXCcyOvernightOvernightSwapConvention.of("XXX", USD_ON_Q, EUR_ON_Q, NEXT_SAME_BUS_DAY);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    ImmutableXCcyOvernightOvernightSwapConvention test =
        ImmutableXCcyOvernightOvernightSwapConvention.of(NAME, EUR_ON_Q, USD_ON_Q, PLUS_TWO_DAYS);
    assertSerialization(test);
  }

}
