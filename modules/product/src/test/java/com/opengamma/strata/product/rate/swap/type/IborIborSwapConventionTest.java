/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.swap.type;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.rate.swap.Swap;
import com.opengamma.strata.product.rate.swap.SwapTrade;

/**
 * Test {@link IborIborSwapConvention}.
 */
@Test
public class IborIborSwapConventionTest {

  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, GBLO);
  private static final DaysAdjustment NEXT_SAME_BUS_DAY = DaysAdjustment.ofCalendarDays(0, BDA_FOLLOW);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, GBLO);


  private static final String NAME = "USD-Swap";
  private static final IborRateSwapLegConvention IBOR1M = IborRateSwapLegConvention.of(USD_LIBOR_1M);
  private static final IborRateSwapLegConvention IBOR3M = IborRateSwapLegConvention.of(USD_LIBOR_3M);
  private static final IborRateSwapLegConvention IBOR6M = IborRateSwapLegConvention.of(USD_LIBOR_6M);

  //-------------------------------------------------------------------------
  public void test_of() {
    ImmutableIborIborSwapConvention test = ImmutableIborIborSwapConvention.of(NAME, IBOR3M, IBOR6M);
    assertEquals(test.getName(), NAME);
    assertEquals(test.getSpreadLeg(), IBOR3M);
    assertEquals(test.getFlatLeg(), IBOR6M);
    assertEquals(test.getSpotDateOffset(), USD_LIBOR_3M.getEffectiveDateOffset());
  }

  //-------------------------------------------------------------------------
  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> ImmutableIborIborSwapConvention.builder()
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    ImmutableIborIborSwapConvention test = ImmutableIborIborSwapConvention.of(NAME, IBOR3M, IBOR6M).expand();
    assertEquals(test.getName(), NAME);
    assertEquals(test.getSpreadLeg(), IBOR3M.expand());
    assertEquals(test.getFlatLeg(), IBOR6M.expand());
    assertEquals(test.getSpotDateOffset(), USD_LIBOR_3M.getEffectiveDateOffset());
  }

  public void test_expandAllSpecified() {
    ImmutableIborIborSwapConvention test = ImmutableIborIborSwapConvention.builder()
        .name(NAME)
        .spreadLeg(IBOR3M)
        .flatLeg(IBOR6M)
        .spotDateOffset(PLUS_ONE_DAY)
        .build()
        .expand();
    assertEquals(test.getSpreadLeg(), IBOR3M.expand());
    assertEquals(test.getFlatLeg(), IBOR6M.expand());
    assertEquals(test.getSpotDateOffset(), PLUS_ONE_DAY);
  }

  //-------------------------------------------------------------------------
  public void test_toTrade_tenor() {
    IborIborSwapConvention base = ImmutableIborIborSwapConvention.of(NAME, IBOR3M, IBOR6M);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 5, 7);
    LocalDate endDate = date(2025, 5, 7);
    SwapTrade test = base.toTrade(tradeDate, TENOR_10Y, BUY, NOTIONAL_2M, 0.25d);
    Swap expected = Swap.of(
        IBOR3M.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        IBOR6M.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_toTrade_periodTenor() {
    IborIborSwapConvention base = ImmutableIborIborSwapConvention.of(NAME, IBOR3M, IBOR6M);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 7);
    LocalDate endDate = date(2025, 8, 7);
    SwapTrade test = base.toTrade(tradeDate, Period.ofMonths(3), TENOR_10Y, BUY, NOTIONAL_2M, 0.25d);
    Swap expected = Swap.of(
        IBOR3M.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        IBOR6M.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_toTrade_dates() {
    IborIborSwapConvention base = ImmutableIborIborSwapConvention.of(NAME, IBOR3M, IBOR6M);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 5);
    LocalDate endDate = date(2015, 11, 5);
    SwapTrade test = base.toTrade(tradeDate, startDate, endDate, BUY, NOTIONAL_2M, 0.25d);
    Swap expected = Swap.of(
        IBOR3M.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        IBOR6M.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, "USD-LIBOR-1M-LIBOR-3M"},
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, "USD-LIBOR-3M-LIBOR-6M"},
    };
  }

  @Test(dataProvider = "name")
  public void test_name(IborIborSwapConvention convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(IborIborSwapConvention convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(IborIborSwapConvention convention, String name) {
    assertEquals(IborIborSwapConvention.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(IborIborSwapConvention convention, String name) {
    IborIborSwapConvention.of(name);  // ensures map is populated
    ImmutableMap<String, IborIborSwapConvention> map = IborIborSwapConvention.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> IborIborSwapConvention.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> IborIborSwapConvention.of((String) null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableIborIborSwapConvention test = ImmutableIborIborSwapConvention.of(NAME, IBOR3M, IBOR6M);
    coverImmutableBean(test);
    ImmutableIborIborSwapConvention test2 = ImmutableIborIborSwapConvention.of(NAME, IBOR1M, IBOR6M);
    coverBeanEquals(test, test2);
    ImmutableIborIborSwapConvention test3 = ImmutableIborIborSwapConvention.of(NAME, IBOR1M, IBOR3M);
    coverBeanEquals(test, test3);
  }

  public void test_serialization() {
    IborIborSwapConvention test = ImmutableIborIborSwapConvention.of(NAME, IBOR3M, IBOR6M);
    assertSerialization(test);
  }

}
