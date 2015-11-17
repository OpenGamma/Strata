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
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
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
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.product.rate.swap.Swap;
import com.opengamma.strata.product.rate.swap.SwapTrade;

/**
 * Test {@link IborIborSwapConvention}.
 */
@Test
public class XCcyIborIborSwapConventionTest {

  private static final HolidayCalendar EUTA_USNY = EUTA.combineWith(USNY);

  private static final String NAME = "EUR/USD-Swap";
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final double FX_EUR_USD = 1.15d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, EUTA_USNY);
  private static final DaysAdjustment NEXT_SAME_BUS_DAY = DaysAdjustment.ofCalendarDays(0, BDA_FOLLOW);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, EUTA_USNY);

  private static final IborRateSwapLegConvention EUR3M = IborRateSwapLegConvention.builder()
      .index(IborIndices.EUR_EURIBOR_3M)
      .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY))
      .build();
  private static final IborRateSwapLegConvention USD3M = IborRateSwapLegConvention.builder()
      .index(IborIndices.USD_LIBOR_3M)
      .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY))
      .build();

  //-------------------------------------------------------------------------
  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> ImmutableXCcyIborIborSwapConvention.builder()
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_expand() {
    ImmutableXCcyIborIborSwapConvention base = ImmutableXCcyIborIborSwapConvention.builder()
        .name(NAME)
        .spreadLeg(EUR3M)
        .flatLeg(USD3M)
        .build();
    ImmutableXCcyIborIborSwapConvention test = base.expand();
    assertEquals(test.getName(), NAME);
    assertEquals(test.getSpreadLeg(), EUR3M.expand());
    assertEquals(test.getFlatLeg(), USD3M.expand());
    assertEquals(test.getSpotDateOffset(), EUR3M.getIndex().getEffectiveDateOffset());
  }

  public void test_expandAllSpecified() {
    ImmutableXCcyIborIborSwapConvention base = ImmutableXCcyIborIborSwapConvention.builder()
        .name(NAME)
        .spreadLeg(EUR3M)
        .flatLeg(USD3M)
        .spotDateOffset(PLUS_TWO_DAYS)
        .build();
    ImmutableXCcyIborIborSwapConvention test = base.expand();
    assertEquals(test.getSpreadLeg(), EUR3M.expand());
    assertEquals(test.getFlatLeg(), USD3M.expand());
    assertEquals(test.getSpotDateOffset(), PLUS_TWO_DAYS);
  }

  //-------------------------------------------------------------------------
  public void test_toTrade_tenor() {
    XCcyIborIborSwapConvention base = ImmutableXCcyIborIborSwapConvention.builder()
        .name("EUR-EURIBOR-3M-USD-LIBOR-3M")
        .spreadLeg(EUR3M)
        .flatLeg(USD3M)
        .spotDateOffset(PLUS_TWO_DAYS)
        .build();
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 5, 7);
    LocalDate endDate = date(2025, 5, 7);
    SwapTrade test = base.toTrade(tradeDate, TENOR_10Y, BUY, NOTIONAL_2M, NOTIONAL_2M * FX_EUR_USD, 0.25d);
    Swap expected = Swap.of(
        EUR3M.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        USD3M.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M * FX_EUR_USD));
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_toTrade_periodTenor() {
    XCcyIborIborSwapConvention base = ImmutableXCcyIborIborSwapConvention.builder()
        .name("EUR-EURIBOR-3M-USD-LIBOR-3M")
        .spreadLeg(EUR3M)
        .flatLeg(USD3M)
        .spotDateOffset(PLUS_TWO_DAYS)
        .build();
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 7);
    LocalDate endDate = date(2025, 8, 7);
    SwapTrade test = base.toTrade(tradeDate, Period.ofMonths(3), TENOR_10Y, BUY, NOTIONAL_2M, NOTIONAL_2M * FX_EUR_USD, 0.25d);
    Swap expected = Swap.of(
        EUR3M.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        USD3M.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M * FX_EUR_USD));
    assertEquals(test.getTradeInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  public void test_toTrade_dates() {
    XCcyIborIborSwapConvention base = ImmutableXCcyIborIborSwapConvention.builder()
        .name("EUR-EURIBOR-3M-USD-LIBOR-3M")
        .spreadLeg(EUR3M)
        .flatLeg(USD3M)
        .spotDateOffset(PLUS_TWO_DAYS)
        .build();
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 5);
    LocalDate endDate = date(2015, 11, 5);
    SwapTrade test = base.toTrade(tradeDate, startDate, endDate, BUY, NOTIONAL_2M, NOTIONAL_2M * FX_EUR_USD, 0.25d);
    Swap expected = Swap.of(
        EUR3M.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        USD3M.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M * FX_EUR_USD));
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
    ImmutableXCcyIborIborSwapConvention test = ImmutableXCcyIborIborSwapConvention.builder()
        .name("EUR-EURIBOR-3M-USD-LIBOR-3M")
        .spreadLeg(EUR3M)
        .flatLeg(USD3M)
        .spotDateOffset(PLUS_TWO_DAYS)
        .build();
    coverImmutableBean(test);
    ImmutableXCcyIborIborSwapConvention test2 = ImmutableXCcyIborIborSwapConvention.builder()
        .name("XXX")
        .spreadLeg(USD3M)
        .flatLeg(EUR3M)
        .spotDateOffset(NEXT_SAME_BUS_DAY)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    XCcyIborIborSwapConvention test = ImmutableXCcyIborIborSwapConvention.builder()
        .name("EUR-EURIBOR-3M-USD-LIBOR-3M")
        .spreadLeg(EUR3M)
        .flatLeg(USD3M)
        .spotDateOffset(PLUS_TWO_DAYS)
        .build();
    assertSerialization(test);
  }

}
