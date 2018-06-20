/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.basics.currency.Currency.CAD;
import static com.opengamma.strata.basics.currency.Currency.CZK;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.INR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.CZPR;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.joda.beans.test.BeanAssert.assertBeanEquals;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.result.FailureItem;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.ValueWithFailures;
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.GenericSecurityTrade;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.deposit.TermDeposit;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.deposit.type.TermDepositConventions;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fra.type.FraConventions;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fx.FxSwap;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.FixedRateStubCalculation;
import com.opengamma.strata.product.swap.FixingRelativeTo;
import com.opengamma.strata.product.swap.FxResetCalculation;
import com.opengamma.strata.product.swap.FxResetFixingRelativeTo;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.IborRateResetMethod;
import com.opengamma.strata.product.swap.IborRateStubCalculation;
import com.opengamma.strata.product.swap.InflationRateCalculation;
import com.opengamma.strata.product.swap.NegativeRateMethod;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;
import com.opengamma.strata.product.swap.OvernightRateCalculation;
import com.opengamma.strata.product.swap.PaymentRelativeTo;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.PriceIndexCalculationMethod;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.ResetSchedule;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapConventions;

/**
 * Test {@link TradeCsvLoader}.
 */
@Test
public class TradeCsvLoaderTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final int NUMBER_SWAPS = 7;

  private static final ResourceLocator FILE =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/trades.csv");
  private static final ResourceLocator FILE_CPTY =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/trades-cpty.csv");
  private static final ResourceLocator FILE_CPTY2 =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/trades-cpty2.csv");

  //-------------------------------------------------------------------------
  public void test_isKnownFormat() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    assertEquals(test.isKnownFormat(FILE.getCharSource()), true);
  }

  //-------------------------------------------------------------------------
  public void test_load_failures() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    assertEquals(trades.getFailures().size(), 0, trades.getFailures().toString());
  }

  @Test
  public void test_load_fx_forwards() throws Exception {
    TradeCsvLoader standard = TradeCsvLoader.standard();
    ResourceLocator locator = ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fxtrades.csv");
    ImmutableList<CharSource> charSources = ImmutableList.of(locator.getCharSource());
    ValueWithFailures<List<FxSingleTrade>> loadedData = standard.parse(charSources, FxSingleTrade.class);
    assertEquals(loadedData.getFailures().size(), 0, loadedData.getFailures().toString());

    List<FxSingleTrade> loadedTrades = loadedData.getValue();
    assertEquals(loadedTrades.size(), 2);

    FxSingleTrade expectedTrade1 = FxSingleTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2016-12-06"))
            .id(StandardId.of("OG", "tradeId1"))
            .build())
        .product(FxSingle.of(CurrencyAmount.of(USD, -3850000), FxRate.of(USD, INR, 67.40), LocalDate.parse("2016-12-08")))
        .build();
    assertEquals(loadedTrades.get(0), expectedTrade1);

    FxSingleTrade expectedTrade2 = FxSingleTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2016-12-22"))
            .id(StandardId.of("OG", "tradeId2"))
            .build())
        .product(FxSingle.of(CurrencyAmount.of(EUR, 1920000), FxRate.of(EUR, CZK, 25.62), LocalDate.parse("2016-12-24")))
        .build();
    assertEquals(loadedTrades.get(1), expectedTrade2);
  }

  @Test
  public void test_load_fx_forwards_with_legs_in_same_direction() throws Exception {
    TradeCsvLoader standard = TradeCsvLoader.standard();
    ResourceLocator locator = ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fxtrades_legs_same_direction.csv");
    ValueWithFailures<List<Trade>> loadedData = standard.load(locator);
    assertEquals(loadedData.getFailures().size(), 1, loadedData.getFailures().toString());
    FailureItem failureItem = loadedData.getFailures().get(0);
    assertEquals(failureItem.getReason().toString(), "PARSING");
    assertEquals(
        failureItem.getMessage(),
        "CSV file trade could not be parsed at line 2: FxSingle legs must not have the same direction: Pay, Pay");
    List<Trade> loadedTrades = loadedData.getValue();
    assertEquals(loadedTrades.size(), 0);
  }

  public void test_load_fx_forwards_fullFormat() throws Exception {
    TradeCsvLoader standard = TradeCsvLoader.standard();
    ResourceLocator locator = ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fxtrades2.csv");
    ImmutableList<CharSource> charSources = ImmutableList.of(locator.getCharSource());
    ValueWithFailures<List<FxSingleTrade>> loadedData = standard.parse(charSources, FxSingleTrade.class);
    assertEquals(loadedData.getFailures().size(), 0, loadedData.getFailures().toString());

    List<FxSingleTrade> loadedTrades = loadedData.getValue();
    assertEquals(loadedTrades.size(), 5);

    FxSingleTrade expectedTrade1 = FxSingleTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2016-12-06"))
            .id(StandardId.of("OG", "tradeId1"))
            .build())
        .product(FxSingle.of(
            CurrencyAmount.of(Currency.USD, -3850000),
            CurrencyAmount.of(Currency.INR, 715405000),
            LocalDate.parse("2017-12-08")))
        .build();
    assertEquals(loadedTrades.get(0), expectedTrade1);

    FxSingleTrade expectedTrade2 = FxSingleTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2017-01-11"))
            .id(StandardId.of("OG", "tradeId5"))
            .build())
        .product(FxSingle.of(
            CurrencyAmount.of(Currency.USD, -6608000),
            CurrencyAmount.of(Currency.TWD, 95703040),
            LocalDate.parse("2017-07-13")))
        .build();
    assertEquals(loadedTrades.get(1), expectedTrade2);

    FxSingleTrade expectedTrade3 = FxSingleTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2017-01-25"))
            .tradeTime(LocalTime.of(11, 00))
            .zone(ZoneId.of("Europe/London"))
            .id(StandardId.of("OG", "tradeId6"))
            .build())
        .product(FxSingle.of(
            CurrencyAmount.of(Currency.EUR, -1920000),
            CurrencyAmount.of(Currency.CZK, 12448000),
            LocalDate.parse("2018-01-29"),
            BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA.combinedWith(CZPR))))
        .build();
    assertEquals(loadedTrades.get(2), expectedTrade3);

    FxSingleTrade expectedTrade4 = FxSingleTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2017-01-25"))
            .id(StandardId.of("OG", "tradeId7"))
            .build())
        .product(FxSingle.of(
            CurrencyAmount.of(Currency.EUR, -1920000),
            CurrencyAmount.of(Currency.CZK, 12256000),
            LocalDate.parse("2018-01-29")))
        .build();
    assertEquals(loadedTrades.get(3), expectedTrade4);

    FxSingleTrade expectedTrade5 = FxSingleTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2017-01-25"))
            .id(StandardId.of("OG", "tradeId8"))
            .build())
        .product(FxSingle.of(
            Payment.of(Currency.EUR, 1920000, LocalDate.parse("2018-01-29")),
            Payment.of(Currency.CZK, -12256000, LocalDate.parse("2018-01-30")),
            BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUTA.combinedWith(CZPR))))
        .build();
    assertEquals(loadedTrades.get(4), expectedTrade5);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_fx_swaps() throws Exception {
    TradeCsvLoader standard = TradeCsvLoader.standard();
    ResourceLocator locator = ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fxtrades.csv");
    ImmutableList<CharSource> charSources = ImmutableList.of(locator.getCharSource());
    ValueWithFailures<List<FxSwapTrade>> loadedData = standard.parse(charSources, FxSwapTrade.class);
    assertEquals(loadedData.getFailures().size(), 0, loadedData.getFailures().toString());

    List<FxSwapTrade> loadedTrades = loadedData.getValue();
    assertEquals(loadedTrades.size(), 2);

    FxSingle near1 = FxSingle.of(CurrencyAmount.of(USD, 120000), FxRate.of(USD, CAD, 1.31), LocalDate.parse("2016-12-08"));
    FxSingle far1 = FxSingle.of(CurrencyAmount.of(USD, -120000), FxRate.of(USD, CAD, 1.34), LocalDate.parse("2017-01-08"));
    FxSwapTrade expectedTrade1 = FxSwapTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2016-12-06"))
            .id(StandardId.of("OG", "tradeId11"))
            .build())
        .product(FxSwap.of(near1, far1))
        .build();
    assertBeanEquals(loadedTrades.get(0), expectedTrade1);

    FxSingle near2 = FxSingle.of(CurrencyAmount.of(CAD, -160000), FxRate.of(USD, CAD, 1.32), LocalDate.parse("2016-12-08"));
    FxSingle far2 = FxSingle.of(CurrencyAmount.of(CAD, 160000), FxRate.of(USD, CAD, 1.34), LocalDate.parse("2017-01-08"));
    FxSwapTrade expectedTrade2 = FxSwapTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2016-12-06"))
            .id(StandardId.of("OG", "tradeId12"))
            .build())
        .product(FxSwap.of(near2, far2))
        .build();
    assertBeanEquals(loadedTrades.get(1), expectedTrade2);
  }

  public void test_load_fx_swaps_fullFormat() throws Exception {
    TradeCsvLoader standard = TradeCsvLoader.standard();
    ResourceLocator locator = ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fxtrades2.csv");
    ImmutableList<CharSource> charSources = ImmutableList.of(locator.getCharSource());
    ValueWithFailures<List<FxSwapTrade>> loadedData = standard.parse(charSources, FxSwapTrade.class);
    assertEquals(loadedData.getFailures().size(), 0, loadedData.getFailures().toString());

    List<FxSwapTrade> loadedTrades = loadedData.getValue();
    assertEquals(loadedTrades.size(), 1);

    FxSingle near1 = FxSingle.of(
        Payment.of(EUR, 1920000, LocalDate.parse("2018-01-29")),
        Payment.of(CZK, -12256000, LocalDate.parse("2018-01-30")),
        BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA.combinedWith(CZPR)));
    FxSingle far1 = FxSingle.of(
        Payment.of(EUR, -1920000, LocalDate.parse("2018-04-29")),
        Payment.of(CZK, 12258000, LocalDate.parse("2018-04-30")),
        BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA.combinedWith(CZPR)));
    FxSwapTrade expectedTrade1 = FxSwapTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2017-01-25"))
            .id(StandardId.of("OG", "tradeId9"))
            .build())
        .product(FxSwap.of(near1, far1))
        .build();
    assertBeanEquals(loadedTrades.get(0), expectedTrade1);
  }
  
  //-------------------------------------------------------------------------
  public void test_load_fra() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    List<FraTrade> filtered = trades.getValue().stream()
        .filter(FraTrade.class::isInstance)
        .map(FraTrade.class::cast)
        .collect(toImmutableList());
    assertEquals(filtered.size(), 3);

    FraTrade expected1 = FraConventions.of(IborIndices.GBP_LIBOR_3M)
        .createTrade(date(2017, 6, 1), Period.ofMonths(2), BUY, 1_000_000, 0.005, REF_DATA)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123401"))
            .tradeDate(date(2017, 6, 1))
            .tradeTime(LocalTime.of(11, 5))
            .zone(ZoneId.of("Europe/London"))
            .build())
        .build();
    assertBeanEquals(expected1, filtered.get(0));

    FraTrade expected2 = FraConventions.of(IborIndices.GBP_LIBOR_6M)
        .toTrade(date(2017, 6, 1), date(2017, 8, 1), date(2018, 2, 1), date(2017, 8, 1), SELL, 1_000_000, 0.007)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123402"))
            .tradeDate(date(2017, 6, 1))
            .tradeTime(LocalTime.of(12, 35))
            .zone(ZoneId.of("Europe/London"))
            .build())
        .build();
    assertBeanEquals(expected2, filtered.get(1));

    FraTrade expected3 = FraTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123403"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(Fra.builder()
            .buySell(SELL)
            .startDate(date(2017, 8, 1))
            .endDate(date(2018, 1, 15))
            .notional(1_000_000)
            .fixedRate(0.0055)
            .index(IborIndices.GBP_LIBOR_3M)
            .indexInterpolated(IborIndices.GBP_LIBOR_6M)
            .dayCount(DayCounts.ACT_360)
            .build())
        .build();
    assertBeanEquals(expected3, filtered.get(2));
  }

  //-------------------------------------------------------------------------
  public void test_load_fra_bothCounterpartyColumnsPresent() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE_CPTY);

    List<FraTrade> filtered = trades.getValue().stream()
        .filter(FraTrade.class::isInstance)
        .map(FraTrade.class::cast)
        .collect(toImmutableList());
    assertEquals(filtered.size(), 3);

    FraTrade expected1 = FraConventions.of(IborIndices.GBP_LIBOR_3M)
        .createTrade(date(2017, 6, 1), Period.ofMonths(2), BUY, 1_000_000, 0.005, REF_DATA)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123401"))
            .tradeDate(date(2017, 6, 1))
            .tradeTime(LocalTime.of(11, 5))
            .zone(ZoneId.of("Europe/London"))
            .counterparty(StandardId.of("CPTY", "Bank A"))
            .build())
        .build();
    assertBeanEquals(expected1, filtered.get(0));

    FraTrade expected2 = FraConventions.of(IborIndices.GBP_LIBOR_6M)
        .toTrade(date(2017, 6, 1), date(2017, 8, 1), date(2018, 2, 1), date(2017, 8, 1), SELL, 1_000_000, 0.007)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123402"))
            .tradeDate(date(2017, 6, 1))
            .tradeTime(LocalTime.of(12, 35))
            .zone(ZoneId.of("Europe/London"))
            .counterparty(StandardId.of("OG-Counterparty", "Bank B"))
            .build())
        .build();
    assertBeanEquals(expected2, filtered.get(1));

    FraTrade expected3 = FraTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123403"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(Fra.builder()
            .buySell(SELL)
            .startDate(date(2017, 8, 1))
            .endDate(date(2018, 1, 15))
            .notional(1_000_000)
            .fixedRate(0.0055)
            .index(IborIndices.GBP_LIBOR_3M)
            .indexInterpolated(IborIndices.GBP_LIBOR_6M)
            .dayCount(DayCounts.ACT_360)
            .build())
        .build();
    assertBeanEquals(expected3, filtered.get(2));
  }

  //-------------------------------------------------------------------------
  public void test_load_fra_counterpartyColumnPresentNoScheme() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE_CPTY2);

    List<FraTrade> filtered = trades.getValue().stream()
        .filter(FraTrade.class::isInstance)
        .map(FraTrade.class::cast)
        .collect(toImmutableList());
    assertEquals(filtered.size(), 3);

    FraTrade expected1 = FraConventions.of(IborIndices.GBP_LIBOR_3M)
        .createTrade(date(2017, 6, 1), Period.ofMonths(2), BUY, 1_000_000, 0.005, REF_DATA)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123401"))
            .tradeDate(date(2017, 6, 1))
            .tradeTime(LocalTime.of(11, 5))
            .zone(ZoneId.of("Europe/London"))
            .counterparty(StandardId.of("OG-Counterparty", "Bank A"))
            .build())
        .build();
    assertBeanEquals(expected1, filtered.get(0));

    FraTrade expected2 = FraConventions.of(IborIndices.GBP_LIBOR_6M)
        .toTrade(date(2017, 6, 1), date(2017, 8, 1), date(2018, 2, 1), date(2017, 8, 1), SELL, 1_000_000, 0.007)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123402"))
            .tradeDate(date(2017, 6, 1))
            .tradeTime(LocalTime.of(12, 35))
            .zone(ZoneId.of("Europe/London"))
            .counterparty(StandardId.of("OG-Counterparty", "Bank B"))
            .build())
        .build();
    assertBeanEquals(expected2, filtered.get(1));

    FraTrade expected3 = FraTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123403"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(Fra.builder()
            .buySell(SELL)
            .startDate(date(2017, 8, 1))
            .endDate(date(2018, 1, 15))
            .notional(1_000_000)
            .fixedRate(0.0055)
            .index(IborIndices.GBP_LIBOR_3M)
            .indexInterpolated(IborIndices.GBP_LIBOR_6M)
            .dayCount(DayCounts.ACT_360)
            .build())
        .build();
    assertBeanEquals(expected3, filtered.get(2));
  }

  //-------------------------------------------------------------------------
  public void test_load_swap() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    List<SwapTrade> filtered = trades.getValue().stream()
        .filter(SwapTrade.class::isInstance)
        .map(SwapTrade.class::cast)
        .collect(toImmutableList());
    assertEquals(filtered.size(), NUMBER_SWAPS);

    SwapTrade expected1 = FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M
        .createTrade(date(2017, 6, 1), Period.ofMonths(1), Tenor.ofYears(5), BUY, 2_000_000, 0.004, REF_DATA)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123411"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .build();
    assertBeanEquals(expected1, filtered.get(0));

    SwapTrade expected2 = FixedIborSwapConventions.GBP_FIXED_6M_LIBOR_6M
        .toTrade(date(2017, 6, 1), date(2017, 8, 1), date(2022, 8, 1), BUY, 3_100_000, -0.0001)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123412"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .build();
    assertBeanEquals(expected2, filtered.get(1));

    NotionalSchedule notionalSchedule = NotionalSchedule.of(GBP,
        ValueSchedule.of(
            5_000_000,
            ValueStep.of(date(2018, 8, 1), ValueAdjustment.ofReplace(4_000_000)),
            ValueStep.of(date(2019, 8, 1), ValueAdjustment.ofReplace(3_000_000)),
            ValueStep.of(date(2020, 8, 1), ValueAdjustment.ofReplace(2_000_000)),
            ValueStep.of(date(2021, 8, 1), ValueAdjustment.ofReplace(1_000_000))));
    Swap expectedSwap3 = Swap.builder()
        .legs(
            RateCalculationSwapLeg.builder()
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 8, 1))
                    .endDate(date(2022, 9, 1))
                    .frequency(Frequency.P6M)
                    .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
                    .stubConvention(StubConvention.LONG_FINAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P6M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(notionalSchedule)
                .calculation(FixedRateCalculation.builder()
                    .rate(ValueSchedule.of(
                        0.005,
                        ValueStep.of(date(2018, 8, 1), ValueAdjustment.ofReplace(0.006)),
                        ValueStep.of(date(2020, 8, 1), ValueAdjustment.ofReplace(0.007))))
                    .dayCount(DayCounts.ACT_365F)
                    .build())
                .build(),
            RateCalculationSwapLeg.builder()
                .payReceive(RECEIVE)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 8, 1))
                    .endDate(date(2022, 9, 1))
                    .frequency(Frequency.P6M)
                    .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
                    .stubConvention(StubConvention.LONG_FINAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P6M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(notionalSchedule)
                .calculation(IborRateCalculation.of(IborIndices.GBP_LIBOR_6M))
                .build())
        .build();
    SwapTrade expected3 = SwapTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123413"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(expectedSwap3)
        .build();
    assertBeanEquals(expected3, filtered.get(2));

    SwapTrade expected4 = XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M
        .createTrade(date(2017, 6, 1), Period.ofMonths(1), Tenor.TENOR_3Y, BUY, 2_000_000, 2_500_000, 0.006, REF_DATA)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123414"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .build();
    assertBeanEquals(expected4, filtered.get(3));
  }

  public void test_load_swap_full5() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<SwapTrade>> result = test.parse(ImmutableList.of(FILE.getCharSource()), SwapTrade.class);
    assertEquals(result.getFailures().size(), 0);
    assertEquals(result.getValue().size(), NUMBER_SWAPS);

    Swap expectedSwap = Swap.builder()
        .legs(
            RateCalculationSwapLeg.builder()
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 8, 1))
                    .endDate(date(2022, 8, 1))
                    .frequency(Frequency.P3M)
                    .businessDayAdjustment(BusinessDayAdjustment.NONE)
                    .stubConvention(StubConvention.SHORT_FINAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P3M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 2_500_000))
                .calculation(FixedRateCalculation.of(0.011, DayCounts.THIRTY_360_ISDA))
                .build(),
            RateCalculationSwapLeg.builder()
                .payReceive(RECEIVE)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 8, 1))
                    .endDate(date(2022, 8, 1))
                    .frequency(Frequency.P3M)
                    .businessDayAdjustment(BusinessDayAdjustment.NONE)
                    .stubConvention(StubConvention.SHORT_FINAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P3M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 2_500_000))
                .calculation(IborRateCalculation.of(IborIndices.GBP_LIBOR_3M))
                .build())
        .build();
    SwapTrade expected = SwapTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123415"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(expectedSwap)
        .build();
    assertBeanEquals(expected, result.getValue().get(4));
  }

  public void test_load_swap_full6() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<SwapTrade>> result = test.parse(ImmutableList.of(FILE.getCharSource()), SwapTrade.class);
    assertEquals(result.getFailures().size(), 0);
    assertEquals(result.getValue().size(), NUMBER_SWAPS);

    Swap expectedSwap = Swap.builder()
        .legs(
            RateCalculationSwapLeg.builder()
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 8, 1))
                    .endDate(date(2022, 8, 8))
                    .frequency(Frequency.P3M)
                    .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO.combinedWith(EUTA)))
                    .stubConvention(StubConvention.LONG_INITIAL)
                    .rollConvention(RollConventions.DAY_8)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P3M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 1_200_000))
                .calculation(FixedRateCalculation.of(0.012, DayCounts.THIRTY_360_ISDA))
                .build(),
            RateCalculationSwapLeg.builder()
                .payReceive(RECEIVE)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 8, 8))
                    .endDate(date(2022, 8, 8))
                    .frequency(Frequency.P3M)
                    .businessDayAdjustment(BusinessDayAdjustment.NONE)
                    .stubConvention(StubConvention.SHORT_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P3M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 1_200_000))
                .calculation(IborRateCalculation.of(IborIndices.GBP_LIBOR_3M))
                .build())
        .build();
    SwapTrade expected = SwapTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123416"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(expectedSwap)
        .build();
    assertBeanEquals(expected, result.getValue().get(5));
  }

  public void test_load_swap_full7() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<SwapTrade>> result = test.parse(ImmutableList.of(FILE.getCharSource()), SwapTrade.class);
    assertEquals(result.getFailures().size(), 0);
    assertEquals(result.getValue().size(), NUMBER_SWAPS);

    Swap expectedSwap = Swap.builder()
        .legs(
            RateCalculationSwapLeg.builder()
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 8, 8))
                    .endDate(date(2022, 8, 8))
                    .frequency(Frequency.P3M)
                    .businessDayAdjustment(BusinessDayAdjustment.of(PRECEDING, GBLO.combinedWith(USNY)))
                    .stubConvention(StubConvention.SHORT_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P3M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 1_500_000))
                .calculation(FixedRateCalculation.of(0.013, DayCounts.ACT_365F))
                .build(),
            RateCalculationSwapLeg.builder()
                .payReceive(RECEIVE)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 8, 8))
                    .endDate(date(2022, 8, 8))
                    .frequency(Frequency.P6M)
                    .businessDayAdjustment(BusinessDayAdjustment.NONE)
                    .stubConvention(StubConvention.SHORT_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P6M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 1_500_000))
                .calculation(OvernightRateCalculation.of(OvernightIndices.GBP_SONIA))
                .build())
        .build();
    SwapTrade expected = SwapTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123417"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(expectedSwap)
        .build();
    assertBeanEquals(expected, result.getValue().get(6));
  }

  public void test_load_swap_all() {
    ImmutableMap<String, String> csvMap = ImmutableMap.<String, String>builder()
        .put("Strata Trade Type", "Swap")
        .put("Id Scheme", "OG")
        .put("Id", "1234")
        .put("Trade Date", "20170101")
        .put("Trade Time", "12:30")
        .put("Trade Zone", "Europe/Paris")

        .put("Leg 1 Direction", "Pay")
        .put("Leg 1 Start Date", "2-May-2017")
        .put("Leg 1 End Date", "22May2022")
        .put("Leg 1 First Regular Start Date", "10/05/17")
        .put("Leg 1 Last Regular End Date", "2022-05-10")
        .put("Leg 1 Start Date Convention", "NoAdjust")
        .put("Leg 1 Start Date Calendar", "NoHolidays")
        .put("Leg 1 Date Convention", "Following")
        .put("Leg 1 Date Calendar", "GBLO")
        .put("Leg 1 End Date Convention", "NoAdjust")
        .put("Leg 1 End Date Calendar", "NoHolidays")
        .put("Leg 1 Roll Convention", "Day10")
        .put("Leg 1 Stub Convention", "Both")
        .put("Leg 1 Frequency", "12M")
        .put("Leg 1 Override Start Date", "2017/04/01")
        .put("Leg 1 Override Start Date Convention", "Following")
        .put("Leg 1 Override Start Date Calendar", "USNY")
        .put("Leg 1 Payment Frequency", "P12M")
        .put("Leg 1 Payment Offset Days", "3")
        .put("Leg 1 Payment Offset Calendar", "GBLO")
        .put("Leg 1 Payment Offset Adjustment Convention", "Following")
        .put("Leg 1 Payment Offset Adjustment Calendar", "USNY")
        .put("Leg 1 Payment Relative To", "PeriodStart")
        .put("Leg 1 Compounding Method", "Flat")
        .put("Leg 1 Payment First Regular Start Date", "2017-05-10")
        .put("Leg 1 Payment Last Regular End Date", "2022-05-10")
        .put("Leg 1 Currency", "GBP")
        .put("Leg 1 Notional Currency", "USD")
        .put("Leg 1 Notional", "1000000")
        .put("Leg 1 FX Reset Index", "GBP/USD-WM")
        .put("Leg 1 FX Reset Relative To", "PeriodEnd")
        .put("Leg 1 FX Reset Offset Days", "2")
        .put("Leg 1 FX Reset Offset Calendar", "GBLO")
        .put("Leg 1 FX Reset Offset Adjustment Convention", "Following")
        .put("Leg 1 FX Reset Offset Adjustment Calendar", "USNY")
        .put("Leg 1 Notional Initial Exchange", "true")
        .put("Leg 1 Notional Intermediate Exchange", "true")
        .put("Leg 1 Notional Final Exchange", "true")
        .put("Leg 1 Day Count", "Act/365F")
        .put("Leg 1 Fixed Rate", "1.1")
        .put("Leg 1 Initial Stub Rate", "0.6")
        .put("Leg 1 Final Stub Rate", "0.7")

        .put("Leg 2 Direction", "Pay")
        .put("Leg 2 Start Date", "2017-05-02")
        .put("Leg 2 End Date", "2022-05-22")
        .put("Leg 2 Frequency", "12M")
        .put("Leg 2 Currency", "GBP")
        .put("Leg 2 Notional", "1000000")
        .put("Leg 2 Day Count", "Act/365F")
        .put("Leg 2 Fixed Rate", "1.1")
        .put("Leg 2 Initial Stub Amount", "4000")
        .put("Leg 2 Final Stub Amount", "5000")

        .put("Leg 3 Direction", "Pay")
        .put("Leg 3 Start Date", "2017-05-02")
        .put("Leg 3 End Date", "2022-05-22")
        .put("Leg 3 Frequency", "12M")
        .put("Leg 3 Currency", "GBP")
        .put("Leg 3 Notional", "1000000")
        .put("Leg 3 Day Count", "Act/360")
        .put("Leg 3 Index", "GBP-LIBOR-6M")
        .put("Leg 3 Reset Frequency", "3M")
        .put("Leg 3 Reset Method", "Weighted")
        .put("Leg 3 Reset Date Convention", "Following")
        .put("Leg 3 Reset Date Calendar", "GBLO+USNY")
        .put("Leg 3 Fixing Relative To", "PeriodEnd")
        .put("Leg 3 Fixing Offset Days", "3")
        .put("Leg 3 Fixing Offset Calendar", "GBLO")
        .put("Leg 3 Fixing Offset Adjustment Convention", "Following")
        .put("Leg 3 Fixing Offset Adjustment Calendar", "USNY")
        .put("Leg 3 Negative Rate Method", "NotNegative")
        .put("Leg 3 First Rate", "0.5")
        .put("Leg 3 Gearing", "2")
        .put("Leg 3 Spread", "3")
        .put("Leg 3 Initial Stub Rate", "0.6")
        .put("Leg 3 Final Stub Rate", "0.7")

        .put("Leg 4 Direction", "Pay")
        .put("Leg 4 Start Date", "2017-05-02")
        .put("Leg 4 End Date", "2022-05-22")
        .put("Leg 4 Frequency", "12M")
        .put("Leg 4 Currency", "GBP")
        .put("Leg 4 Notional", "1000000")
        .put("Leg 4 Index", "GBP-LIBOR-6M")
        .put("Leg 4 Initial Stub Amount", "4000")
        .put("Leg 4 Final Stub Amount", "5000")

        .put("Leg 5 Direction", "Pay")
        .put("Leg 5 Start Date", "2017-05-02")
        .put("Leg 5 End Date", "2022-05-22")
        .put("Leg 5 Frequency", "6M")
        .put("Leg 5 Currency", "GBP")
        .put("Leg 5 Notional", "1000000")
        .put("Leg 5 Index", "GBP-LIBOR-6M")
        .put("Leg 5 Initial Stub Index", "GBP-LIBOR-3M")
        .put("Leg 5 Initial Stub Interpolated Index", "GBP-LIBOR-6M")
        .put("Leg 5 Final Stub Index", "GBP-LIBOR-3M")
        .put("Leg 5 Final Stub Interpolated Index", "GBP-LIBOR-6M")

        .put("Leg 6 Direction", "Pay")
        .put("Leg 6 Start Date", "2017-05-02")
        .put("Leg 6 End Date", "2022-05-22")
        .put("Leg 6 Frequency", "6M")
        .put("Leg 6 Currency", "GBP")
        .put("Leg 6 Notional", "1000000")
        .put("Leg 6 Day Count", "Act/360")
        .put("Leg 6 Index", "GBP-SONIA")
        .put("Leg 6 Accrual Method", "Averaged")
        .put("Leg 6 Rate Cut Off Days", "3")
        .put("Leg 6 Negative Rate Method", "NotNegative")
        .put("Leg 6 Gearing", "2")
        .put("Leg 6 Spread", "3")

        .put("Leg 7 Direction", "Pay")
        .put("Leg 7 Start Date", "2017-05-02")
        .put("Leg 7 End Date", "2022-05-22")
        .put("Leg 7 Frequency", "6M")
        .put("Leg 7 Currency", "GBP")
        .put("Leg 7 Notional", "1000000")
        .put("Leg 7 Day Count", "Act/360")
        .put("Leg 7 Index", "GB-RPI")
        .put("Leg 7 Inflation Lag", "2")
        .put("Leg 7 Inflation Method", "Interpolated")
        .put("Leg 7 Inflation First Index Value", "121")
        .put("Leg 7 Gearing", "2")
        .build();
    String csv = Joiner.on(',').join(csvMap.keySet()) + "\n" + Joiner.on(',').join(csvMap.values());

    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<SwapTrade>> result = test.parse(ImmutableList.of(CharSource.wrap(csv)), SwapTrade.class);
    assertEquals(result.getFailures().size(), 0, result.getFailures().toString());
    assertEquals(result.getValue().size(), 1);

    Swap expectedSwap = Swap.builder()
        .legs(
            RateCalculationSwapLeg.builder()  // Fixed fixed stub
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 5, 2))
                    .endDate(date(2022, 5, 22))
                    .firstRegularStartDate(date(2017, 5, 10))
                    .lastRegularEndDate(date(2022, 5, 10))
                    .overrideStartDate(AdjustableDate.of(date(2017, 4, 1), BusinessDayAdjustment.of(FOLLOWING, USNY)))
                    .frequency(Frequency.P12M)
                    .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
                    .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
                    .endDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
                    .rollConvention(RollConventions.DAY_10)
                    .stubConvention(StubConvention.BOTH)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P12M)
                    .paymentDateOffset(DaysAdjustment.ofBusinessDays(3, GBLO, BusinessDayAdjustment.of(FOLLOWING, USNY)))
                    .paymentRelativeTo(PaymentRelativeTo.PERIOD_START)
                    .compoundingMethod(CompoundingMethod.FLAT)
                    .firstRegularStartDate(date(2017, 5, 10))
                    .lastRegularEndDate(date(2022, 5, 10))
                    .build())
                .notionalSchedule(NotionalSchedule.builder()
                    .currency(GBP)
                    .amount(ValueSchedule.of(1_000_000))
                    .fxReset(FxResetCalculation.builder()
                        .referenceCurrency(USD)
                        .index(FxIndices.GBP_USD_WM)
                        .fixingRelativeTo(FxResetFixingRelativeTo.PERIOD_END)
                        .fixingDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO, BusinessDayAdjustment.of(FOLLOWING, USNY)))
                        .build())
                    .initialExchange(true)
                    .intermediateExchange(true)
                    .finalExchange(true)
                    .build())
                .calculation(FixedRateCalculation.builder()
                    .dayCount(DayCounts.ACT_365F)
                    .rate(ValueSchedule.of(0.011))
                    .initialStub(FixedRateStubCalculation.ofFixedRate(0.006))
                    .finalStub(FixedRateStubCalculation.ofFixedRate(0.007))
                    .build())
                .build(),
            RateCalculationSwapLeg.builder()  // Fixed known amount stub
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 5, 2))
                    .endDate(date(2022, 5, 22))
                    .frequency(Frequency.P12M)
                    .businessDayAdjustment(BusinessDayAdjustment.NONE)
                    .stubConvention(StubConvention.SHORT_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P12M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 1_000_000))
                .calculation(FixedRateCalculation.builder()
                    .dayCount(DayCounts.ACT_365F)
                    .rate(ValueSchedule.of(0.011))
                    .initialStub(FixedRateStubCalculation.ofKnownAmount(CurrencyAmount.of(GBP, 4000)))
                    .finalStub(FixedRateStubCalculation.ofKnownAmount(CurrencyAmount.of(GBP, 5000)))
                    .build())
                .build(),
            RateCalculationSwapLeg.builder()  // Ibor fixed rate stub
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 5, 2))
                    .endDate(date(2022, 5, 22))
                    .frequency(Frequency.P12M)
                    .businessDayAdjustment(BusinessDayAdjustment.NONE)
                    .stubConvention(StubConvention.SHORT_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P12M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 1_000_000))
                .calculation(IborRateCalculation.builder()
                    .dayCount(DayCounts.ACT_360)
                    .index(IborIndices.GBP_LIBOR_6M)
                    .resetPeriods(ResetSchedule.builder()
                        .resetFrequency(Frequency.P3M)
                        .resetMethod(IborRateResetMethod.WEIGHTED)
                        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO.combinedWith(USNY)))
                        .build())
                    .fixingRelativeTo(FixingRelativeTo.PERIOD_END)
                    .fixingDateOffset(DaysAdjustment.ofBusinessDays(3, GBLO, BusinessDayAdjustment.of(FOLLOWING, USNY)))
                    .negativeRateMethod(NegativeRateMethod.NOT_NEGATIVE)
                    .firstRate(0.005)
                    .gearing(ValueSchedule.of(2))
                    .spread(ValueSchedule.of(0.03))
                    .initialStub(IborRateStubCalculation.ofFixedRate(0.006))
                    .finalStub(IborRateStubCalculation.ofFixedRate(0.007))
                    .build())
                .build(),
            RateCalculationSwapLeg.builder()  // Ibor known amount stub
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 5, 2))
                    .endDate(date(2022, 5, 22))
                    .frequency(Frequency.P12M)
                    .businessDayAdjustment(BusinessDayAdjustment.NONE)
                    .stubConvention(StubConvention.SHORT_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P12M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 1_000_000))
                .calculation(IborRateCalculation.builder()
                    .dayCount(DayCounts.ACT_365F)
                    .index(IborIndices.GBP_LIBOR_6M)
                    .initialStub(IborRateStubCalculation.ofKnownAmount(CurrencyAmount.of(GBP, 4000)))
                    .finalStub(IborRateStubCalculation.ofKnownAmount(CurrencyAmount.of(GBP, 5000)))
                    .build())
                .build(),
            RateCalculationSwapLeg.builder()  // Ibor interpolated stub
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 5, 2))
                    .endDate(date(2022, 5, 22))
                    .frequency(Frequency.P6M)
                    .businessDayAdjustment(BusinessDayAdjustment.NONE)
                    .stubConvention(StubConvention.SHORT_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P6M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 1_000_000))
                .calculation(IborRateCalculation.builder()
                    .dayCount(DayCounts.ACT_365F)
                    .index(IborIndices.GBP_LIBOR_6M)
                    .initialStub(
                        IborRateStubCalculation.ofIborInterpolatedRate(IborIndices.GBP_LIBOR_3M, IborIndices.GBP_LIBOR_6M))
                    .finalStub(
                        IborRateStubCalculation.ofIborInterpolatedRate(IborIndices.GBP_LIBOR_3M, IborIndices.GBP_LIBOR_6M))
                    .build())
                .build(),
            RateCalculationSwapLeg.builder()  // overnight
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 5, 2))
                    .endDate(date(2022, 5, 22))
                    .frequency(Frequency.P6M)
                    .businessDayAdjustment(BusinessDayAdjustment.NONE)
                    .stubConvention(StubConvention.SHORT_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P6M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 1_000_000))
                .calculation(OvernightRateCalculation.builder()
                    .dayCount(DayCounts.ACT_360)
                    .index(OvernightIndices.GBP_SONIA)
                    .accrualMethod(OvernightAccrualMethod.AVERAGED)
                    .rateCutOffDays(3)
                    .negativeRateMethod(NegativeRateMethod.NOT_NEGATIVE)
                    .gearing(ValueSchedule.of(2))
                    .spread(ValueSchedule.of(0.03))
                    .build())
                .build(),
            RateCalculationSwapLeg.builder()  // inflation
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 5, 2))
                    .endDate(date(2022, 5, 22))
                    .frequency(Frequency.P6M)
                    .businessDayAdjustment(BusinessDayAdjustment.NONE)
                    .stubConvention(StubConvention.SHORT_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P6M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 1_000_000))
                .calculation(InflationRateCalculation.builder()
                    .index(PriceIndices.GB_RPI)
                    .lag(Period.ofMonths(2))
                    .indexCalculationMethod(PriceIndexCalculationMethod.INTERPOLATED)
                    .firstIndexValue(121d)
                    .gearing(ValueSchedule.of(2))
                    .build())
                .build())
        .build();
    SwapTrade expected = SwapTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "1234"))
            .tradeDate(date(2017, 1, 1))
            .tradeTime(LocalTime.of(12, 30))
            .zone(ZoneId.of("Europe/Paris"))
            .build())
        .product(expectedSwap)
        .build();
    assertBeanEquals(expected, result.getValue().get(0));
  }

  public void test_load_swap_defaultFixedLegDayCount() {
    ImmutableMap<String, String> csvMap = ImmutableMap.<String, String>builder()
        .put("Strata Trade Type", "Swap")
        .put("Id Scheme", "OG")
        .put("Id", "1234")
        .put("Trade Date", "20170101")
        .put("Trade Time", "12:30")
        .put("Trade Zone", "Europe/Paris")

        .put("Leg 1 Direction", "Pay")
        .put("Leg 1 Start Date", "2017-05-02")
        .put("Leg 1 End Date", "2022-05-02")
        .put("Leg 1 Date Convention", "Following")
        .put("Leg 1 Date Calendar", "GBLO")
        .put("Leg 1 Frequency", "12M")
        .put("Leg 1 Currency", "GBP")
        .put("Leg 1 Notional", "1000000")
        .put("Leg 1 Fixed Rate", "1.1")

        .put("Leg 2 Direction", "Pay")
        .put("Leg 2 Start Date", "2017-05-02")
        .put("Leg 2 End Date", "2022-05-02")
        .put("Leg 2 Date Convention", "Following")
        .put("Leg 2 Date Calendar", "GBLO")
        .put("Leg 2 Frequency", "6M")
        .put("Leg 2 Currency", "GBP")
        .put("Leg 2 Notional", "1000000")
        .put("Leg 2 Index", "CHF-LIBOR-6M")
        .build();
    String csv = Joiner.on(',').join(csvMap.keySet()) + "\n" + Joiner.on(',').join(csvMap.values());

    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<SwapTrade>> result = test.parse(ImmutableList.of(CharSource.wrap(csv)), SwapTrade.class);
    assertEquals(result.getFailures().size(), 0, result.getFailures().toString());
    assertEquals(result.getValue().size(), 1);

    Swap expectedSwap = Swap.builder()
        .legs(
            RateCalculationSwapLeg.builder()  // Fixed fixed stub
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 5, 2))
                    .endDate(date(2022, 5, 2))
                    .frequency(Frequency.P12M)
                    .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
                    .stubConvention(StubConvention.SHORT_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P12M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .paymentRelativeTo(PaymentRelativeTo.PERIOD_END)
                    .build())
                .notionalSchedule(NotionalSchedule.builder()
                    .currency(GBP)
                    .amount(ValueSchedule.of(1_000_000))
                    .build())
                .calculation(FixedRateCalculation.builder()
                    .dayCount(DayCounts.THIRTY_U_360)  // defaulted from CHF-LIBOR-6M
                    .rate(ValueSchedule.of(0.011))
                    .build())
                .build(),
            RateCalculationSwapLeg.builder()
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 5, 2))
                    .endDate(date(2022, 5, 2))
                    .frequency(Frequency.P6M)
                    .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
                    .stubConvention(StubConvention.SHORT_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P6M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 1_000_000))
                .calculation(IborRateCalculation.builder()
                    .dayCount(DayCounts.ACT_360)
                    .index(IborIndices.CHF_LIBOR_6M)
                    .build())
                .build())
        .build();
    SwapTrade expected = SwapTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "1234"))
            .tradeDate(date(2017, 1, 1))
            .tradeTime(LocalTime.of(12, 30))
            .zone(ZoneId.of("Europe/Paris"))
            .build())
        .product(expectedSwap)
        .build();
    assertBeanEquals(expected, result.getValue().get(0));
  }

  //-------------------------------------------------------------------------
  public void test_load_termDeposit() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    List<TermDepositTrade> filtered = trades.getValue().stream()
        .filter(TermDepositTrade.class::isInstance)
        .map(TermDepositTrade.class::cast)
        .collect(toImmutableList());
    assertEquals(filtered.size(), 3);

    TermDepositTrade expected1 = TermDepositConventions.GBP_SHORT_DEPOSIT_T0
        .createTrade(date(2017, 6, 1), Period.ofWeeks(2), SELL, 400_000, 0.002, REF_DATA)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123421"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .build();
    assertBeanEquals(expected1, filtered.get(0));

    TermDepositTrade expected2 = TermDepositConventions.GBP_SHORT_DEPOSIT_T0
        .toTrade(date(2017, 6, 1), date(2017, 6, 1), date(2017, 6, 15), SELL, 500_000, 0.0022)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123422"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .build();
    assertBeanEquals(expected2, filtered.get(1));

    TermDepositTrade expected3 = TermDepositTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123423"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(TermDeposit.builder()
            .buySell(BUY)
            .currency(GBP)
            .notional(600_000)
            .startDate(date(2017, 6, 1))
            .endDate(date(2017, 6, 22))
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
            .dayCount(DayCounts.ACT_365F)
            .rate(0.0023)
            .build())
        .build();
    assertBeanEquals(expected3, filtered.get(2));
  }

  //-------------------------------------------------------------------------
  public void test_load_filtered() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.parse(
        ImmutableList.of(FILE.getCharSource()), ImmutableList.of(FraTrade.class, TermDepositTrade.class));

    assertEquals(trades.getValue().size(), 6);
    assertEquals(trades.getFailures().size(), 10);
    assertEquals(trades.getFailures().get(0).getMessage(),
        "Trade type not allowed " + SwapTrade.class.getName() + ", only these types are supported: FraTrade, TermDepositTrade");
  }

  //-------------------------------------------------------------------------
  public void test_load_resolver() {
    AtomicInteger fraCount = new AtomicInteger();
    AtomicInteger termCount = new AtomicInteger();
    TradeCsvInfoResolver resolver = new TradeCsvInfoResolver() {
      @Override
      public FraTrade completeTrade(CsvRow row, FraTrade trade) {
        fraCount.incrementAndGet();
        return trade;
      }

      @Override
      public TermDepositTrade completeTrade(CsvRow row, TermDepositTrade trade) {
        termCount.incrementAndGet();
        return trade;
      }

      @Override
      public ReferenceData getReferenceData() {
        return ReferenceData.standard();
      }
    };
    TradeCsvLoader test = TradeCsvLoader.of(resolver);
    test.parse(ImmutableList.of(FILE.getCharSource()));
    assertEquals(fraCount.get(), 3);
    assertEquals(termCount.get(), 3);
  }

  //-------------------------------------------------------------------------
  public void test_load_security() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    List<SecurityTrade> filtered = trades.getValue().stream()
        .filter(SecurityTrade.class::isInstance)
        .map(SecurityTrade.class::cast)
        .collect(toImmutableList());
    assertEquals(filtered.size(), 2);

    SecurityTrade expected1 = SecurityTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123431"))
            .tradeDate(date(2017, 6, 1))
            .settlementDate(date(2017, 6, 3))
            .build())
        .securityId(SecurityId.of("OG-Security", "AAPL"))
        .quantity(12)
        .price(14.5)
        .build();
    assertBeanEquals(expected1, filtered.get(0));

    SecurityTrade expected2 = SecurityTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123432"))
            .tradeDate(date(2017, 6, 1))
            .settlementDate(date(2017, 6, 3))
            .build())
        .securityId(SecurityId.of("BBG", "MSFT"))
        .quantity(-20)
        .price(17.8)
        .build();
    assertBeanEquals(expected2, filtered.get(1));
  }

  public void test_load_genericSecurity() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    List<GenericSecurityTrade> filtered = trades.getValue().stream()
        .filter(GenericSecurityTrade.class::isInstance)
        .map(GenericSecurityTrade.class::cast)
        .collect(toImmutableList());
    assertEquals(filtered.size(), 1);

    GenericSecurityTrade expected1 = GenericSecurityTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123433"))
            .tradeDate(date(2017, 6, 1))
            .settlementDate(date(2017, 6, 3))
            .build())
        .security(
            GenericSecurity.of(
                SecurityInfo.of(
                    SecurityId.of("OG-Security", "AAPL"),
                    SecurityPriceInfo.of(5, CurrencyAmount.of(USD, 0.01), 10))))
        .quantity(12)
        .price(14.5)
        .build();
    assertBeanEquals(expected1, filtered.get(0));
  }

  //-------------------------------------------------------------------------
  public void test_load_invalidNoHeader() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.parse(ImmutableList.of(CharSource.wrap("")));

    assertEquals(trades.getFailures().size(), 1);
    FailureItem failure = trades.getFailures().get(0);
    assertEquals(failure.getReason(), FailureReason.PARSING);
    assertEquals(failure.getMessage().contains("CSV file could not be parsed"), true);
  }

  public void test_load_invalidNoType() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.parse(ImmutableList.of(CharSource.wrap("Id")));

    assertEquals(trades.getFailures().size(), 1);
    FailureItem failure = trades.getFailures().get(0);
    assertEquals(failure.getReason(), FailureReason.PARSING);
    assertEquals(failure.getMessage().contains("CSV file does not contain 'Strata Trade Type' header"), true);
  }

  public void test_load_invalidUnknownType() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.parse(ImmutableList.of(CharSource.wrap("Strata Trade Type\nFoo")));

    assertEquals(trades.getFailures().size(), 1);
    FailureItem failure = trades.getFailures().get(0);
    assertEquals(failure.getReason(), FailureReason.PARSING);
    assertEquals(failure.getMessage(), "CSV file trade type 'Foo' is not known at line 2");
  }

  public void test_load_invalidFra() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.parse(ImmutableList.of(CharSource.wrap("Strata Trade Type,Buy Sell\nFra,Buy")));

    assertEquals(trades.getFailures().size(), 1);
    FailureItem failure = trades.getFailures().get(0);
    assertEquals(failure.getReason(), FailureReason.PARSING);
    assertEquals(failure.getMessage(), "CSV file trade could not be parsed at line 2: Header not found: 'Notional'");
  }

  public void test_load_invalidSwap() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.parse(ImmutableList.of(CharSource.wrap("Strata Trade Type,Buy Sell\nSwap,Buy")));

    assertEquals(trades.getFailures().size(), 1);
    FailureItem failure = trades.getFailures().get(0);
    assertEquals(failure.getReason(), FailureReason.PARSING);
    assertEquals(failure.getMessage(),
        "CSV file trade could not be parsed at line 2: Swap trade had invalid combination of fields. " +
            "Must include either 'Convention' or '" + "Leg 1 Direction'");
  }

  public void test_load_invalidTermDeposit() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades =
        test.parse(ImmutableList.of(CharSource.wrap("Strata Trade Type,Buy Sell\nTermDeposit,Buy")));

    assertEquals(trades.getFailures().size(), 1);
    FailureItem failure = trades.getFailures().get(0);
    assertEquals(failure.getReason(), FailureReason.PARSING);
    assertEquals(failure.getMessage(), "CSV file trade could not be parsed at line 2: Header not found: 'Notional'");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FraTradeCsvLoader.class);
    coverPrivateConstructor(SecurityCsvLoader.class);
    coverPrivateConstructor(SwapTradeCsvLoader.class);
    coverPrivateConstructor(TermDepositTradeCsvLoader.class);
    coverPrivateConstructor(FullSwapTradeCsvLoader.class);
  }

}
