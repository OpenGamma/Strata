/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.basics.StandardSchemes.OG_COUNTERPARTY;
import static com.opengamma.strata.basics.StandardSchemes.OG_SECURITY_SCHEME;
import static com.opengamma.strata.basics.currency.Currency.BRL;
import static com.opengamma.strata.basics.currency.Currency.CAD;
import static com.opengamma.strata.basics.currency.Currency.CLP;
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
import static com.opengamma.strata.collect.Guavate.filtering;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.beans.test.BeanAssert.assertBeanEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.joda.beans.Bean;
import org.junit.jupiter.api.Test;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.ImmutableFxIndex;
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
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.GenericSecurityTrade;
import com.opengamma.strata.product.PortfolioItemInfo;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.capfloor.IborCapFloor;
import com.opengamma.strata.product.capfloor.IborCapFloorLeg;
import com.opengamma.strata.product.capfloor.IborCapFloorTrade;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapFloor;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapFloorLeg;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapFloorTrade;
import com.opengamma.strata.product.common.CcpIds;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsIndex;
import com.opengamma.strata.product.credit.CdsIndexTrade;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.PaymentOnDefault;
import com.opengamma.strata.product.credit.ProtectionStartOfDay;
import com.opengamma.strata.product.credit.type.CdsConventions;
import com.opengamma.strata.product.deposit.TermDeposit;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.deposit.type.TermDepositConventions;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fra.type.FraConventions;
import com.opengamma.strata.product.fx.FxNdf;
import com.opengamma.strata.product.fx.FxNdfTrade;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fx.FxSwap;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.fxopt.FxSingleBarrierOption;
import com.opengamma.strata.product.fxopt.FxSingleBarrierOptionTrade;
import com.opengamma.strata.product.fxopt.FxVanillaOption;
import com.opengamma.strata.product.fxopt.FxVanillaOptionTrade;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;
import com.opengamma.strata.product.payment.BulletPayment;
import com.opengamma.strata.product.payment.BulletPaymentTrade;
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
import com.opengamma.strata.product.swap.KnownAmountSwapLeg;
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
import com.opengamma.strata.product.swaption.CashSwaptionSettlement;
import com.opengamma.strata.product.swaption.CashSwaptionSettlementMethod;
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Test {@link TradeCsvLoader}.
 */
public class TradeCsvLoaderTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final int NUMBER_SWAPS = 8;

  private static final ResourceLocator FILE =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/trades.csv");
  private static final ResourceLocator FILE_CPTY =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/trades-cpty.csv");
  private static final ResourceLocator FILE_CPTY2 =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/trades-cpty2.csv");

  //-------------------------------------------------------------------------
  @Test
  public void test_isKnownFormat() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    assertThat(test.isKnownFormat(FILE.getCharSource())).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_mixed() {
    TradeCsvLoader standard = TradeCsvLoader.standard();
    ResourceLocator locator = ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/mixed-trades-positions.csv");
    ImmutableList<CharSource> charSources = ImmutableList.of(locator.getCharSource());
    ValueWithFailures<List<Trade>> loadedData = standard.parse(charSources);
    assertThat(loadedData.getFailures().size()).as(loadedData.getFailures().toString()).isEqualTo(1);
    assertThat(loadedData.getFailures()).first().hasToString(
        "PARSING: CSV position file 'mixed-trades-positions.csv' contained row with mixed trade/position type 'FX/EtdFuture' at line 6");

    List<Trade> loadedTrades = loadedData.getValue();
    assertThat(loadedTrades).hasSize(2).allMatch(trade -> trade instanceof FxSingleTrade);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_failures() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    assertThat(trades.getFailures().size()).as(trades.getFailures().toString()).isEqualTo(0);
  }

  @Test
  public void test_load_fx_forwards() {
    TradeCsvLoader standard = TradeCsvLoader.standard();
    ResourceLocator locator = ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fxtrades.csv");
    ImmutableList<CharSource> charSources = ImmutableList.of(locator.getCharSource());
    ValueWithFailures<List<FxSingleTrade>> loadedData = standard.parse(charSources, FxSingleTrade.class);
    assertThat(loadedData.getFailures().size()).as(loadedData.getFailures().toString()).isEqualTo(0);

    List<FxSingleTrade> loadedTrades = loadedData.getValue();
    assertThat(loadedTrades).hasSize(2);

    FxSingleTrade expected0 = FxSingleTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2016-12-06"))
            .id(StandardId.of("OG", "tradeId1"))
            .addAttribute(AttributeType.CCP, CcpIds.CME)
            .addAttribute(AttributeType.BUY_SELL, SELL)
            .build())
        .product(FxSingle.of(
            CurrencyAmount.of(USD, -3850000),
            FxRate.of(USD, INR, 67.40),
            LocalDate.parse("2016-12-08"),
            BusinessDayAdjustment.of(FOLLOWING, USNY)))
        .build();
    assertBeanEquals(loadedTrades.get(0), expected0);

    FxSingleTrade expected1 = FxSingleTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2016-12-22"))
            .id(StandardId.of("OG", "tradeId2"))
            .addAttribute(AttributeType.CCP, CcpIds.CME)
            .addAttribute(AttributeType.BUY_SELL, BUY)
            .build())
        .product(FxSingle.of(CurrencyAmount.of(EUR, 1920000), FxRate.of(EUR, CZK, 25.62), LocalDate.parse("2016-12-24")))
        .build();
    assertBeanEquals(loadedTrades.get(1), expected1);

    checkRoundtrip(FxSingleTrade.class, loadedTrades, expected0, expected1);
  }

  @Test
  public void test_load_fx_forwards_with_legs_in_same_direction() {
    TradeCsvLoader standard = TradeCsvLoader.standard();
    ResourceLocator locator = ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fxtrades_legs_same_direction.csv");
    ValueWithFailures<List<Trade>> loadedData = standard.load(locator);
    assertThat(loadedData.getFailures().size()).as(loadedData.getFailures().toString()).isEqualTo(1);
    FailureItem failureItem = loadedData.getFailures().get(0);
    assertThat(failureItem.getReason().toString()).isEqualTo("PARSING");
    assertThat(failureItem.getMessage())
        .isEqualTo("CSV trade file 'fxtrades_legs_same_direction.csv' type 'FX' could not be parsed at line 2: " +
            "FxSingle legs must not have the same direction: Pay, Pay");
    List<Trade> loadedTrades = loadedData.getValue();
    assertThat(loadedTrades).hasSize(0);
  }

  @Test
  public void test_load_fx_forwards_fullFormat() {
    TradeCsvLoader standard = TradeCsvLoader.standard();
    ResourceLocator locator = ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fxtrades2.csv");
    ImmutableList<CharSource> charSources = ImmutableList.of(locator.getCharSource());
    ValueWithFailures<List<FxSingleTrade>> loadedData = standard.parse(charSources, FxSingleTrade.class);
    assertThat(loadedData.getFailures().size()).as(loadedData.getFailures().toString()).isEqualTo(0);

    List<FxSingleTrade> loadedTrades = loadedData.getValue();
    assertThat(loadedTrades).hasSize(5);

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
    assertThat(loadedTrades.get(0)).isEqualTo(expectedTrade1);

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
    assertThat(loadedTrades.get(1)).isEqualTo(expectedTrade2);

    FxSingleTrade expectedTrade3 = FxSingleTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2017-01-25"))
            .tradeTime(LocalTime.of(11, 0))
            .zone(ZoneId.of("Europe/London"))
            .id(StandardId.of("OG", "tradeId6"))
            .build())
        .product(FxSingle.of(
            CurrencyAmount.of(Currency.EUR, -1920000),
            CurrencyAmount.of(Currency.CZK, 12448000),
            LocalDate.parse("2018-01-29"),
            BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA.combinedWith(CZPR))))
        .build();
    assertThat(loadedTrades.get(2)).isEqualTo(expectedTrade3);

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
    assertThat(loadedTrades.get(3)).isEqualTo(expectedTrade4);

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
    assertThat(loadedTrades.get(4)).isEqualTo(expectedTrade5);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_fx_swaps() {
    TradeCsvLoader standard = TradeCsvLoader.standard();
    ResourceLocator locator = ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fxtrades.csv");
    ImmutableList<CharSource> charSources = ImmutableList.of(locator.getCharSource());
    ValueWithFailures<List<FxSwapTrade>> loadedData = standard.parse(charSources, FxSwapTrade.class);
    assertThat(loadedData.getFailures().size()).as(loadedData.getFailures().toString()).isEqualTo(0);

    List<FxSwapTrade> loadedTrades = loadedData.getValue();
    assertThat(loadedTrades).hasSize(2);

    FxSingle near1 = FxSingle.of(CurrencyAmount.of(USD, 120000), FxRate.of(USD, CAD, 1.31), LocalDate.parse("2016-12-08"));
    FxSingle far1 = FxSingle.of(CurrencyAmount.of(USD, -120000), FxRate.of(USD, CAD, 1.34), LocalDate.parse("2017-01-08"));
    FxSwapTrade expected0 = FxSwapTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2016-12-06"))
            .id(StandardId.of("OG", "tradeId11"))
            .addAttribute(AttributeType.CCP, CcpIds.CME)
            .addAttribute(AttributeType.BUY_SELL, BUY)
            .build())
        .product(FxSwap.of(near1, far1))
        .build();
    assertBeanEquals(loadedTrades.get(0), expected0);

    FxSingle near2 = FxSingle.of(CurrencyAmount.of(CAD, -160000), FxRate.of(USD, CAD, 1.32), LocalDate.parse("2016-12-08"));
    FxSingle far2 = FxSingle.of(CurrencyAmount.of(CAD, 160000), FxRate.of(USD, CAD, 1.34), LocalDate.parse("2017-01-08"));
    FxSwapTrade expected1 = FxSwapTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2016-12-06"))
            .id(StandardId.of("OG", "tradeId12"))
            .addAttribute(AttributeType.CCP, CcpIds.CME)
            .addAttribute(AttributeType.BUY_SELL, SELL)
            .build())
        .product(FxSwap.of(near2, far2))
        .build();
    assertBeanEquals(loadedTrades.get(1), expected1);

    checkRoundtrip(FxSwapTrade.class, loadedTrades, expected0, expected1);
  }

  @Test
  public void test_load_fx_swaps_fullFormat() {
    TradeCsvLoader standard = TradeCsvLoader.standard();
    ResourceLocator locator = ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fxtrades2.csv");
    ImmutableList<CharSource> charSources = ImmutableList.of(locator.getCharSource());
    ValueWithFailures<List<FxSwapTrade>> loadedData = standard.parse(charSources, FxSwapTrade.class);
    assertThat(loadedData.getFailures().size()).as(loadedData.getFailures().toString()).isEqualTo(0);

    List<FxSwapTrade> loadedTrades = loadedData.getValue();
    assertThat(loadedTrades).hasSize(1);

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
  @Test
  public void test_load_fx_vanilla_option() {
    TradeCsvLoader standard = TradeCsvLoader.standard();
    ResourceLocator locator = ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fxtrades.csv");
    ImmutableList<CharSource> charSources = ImmutableList.of(locator.getCharSource());
    ValueWithFailures<List<FxVanillaOptionTrade>> loadedData = standard.parse(charSources, FxVanillaOptionTrade.class);
    assertThat(loadedData.getFailures().size()).as(loadedData.getFailures().toString()).isEqualTo(0);

    List<FxVanillaOptionTrade> loadedTrades = loadedData.getValue();
    assertThat(loadedTrades).hasSize(1);

    FxVanillaOptionTrade expectedTrade0 = FxVanillaOptionTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(LocalDate.parse("2016-12-06"))
            .id(StandardId.of("OG", "tradeId31"))
            .addAttribute(AttributeType.CCP, CcpIds.CME)
            .build())
        .product(FxVanillaOption.builder()
            .longShort(LongShort.LONG)
            .expiryDate(LocalDate.of(2017, 1, 8))
            .expiryTime(LocalTime.of(11, 0))
            .expiryZone(ZoneId.of("Europe/London"))
            .underlying(FxSingle.of(
                CurrencyAmount.of(USD, 30000),
                FxRate.of(USD, CAD, 1.31),
                LocalDate.of(2017, 1, 10)))
            .build())
        .premium(AdjustablePayment.of(
            CurrencyAmount.of(GBP, -2000),
            AdjustableDate.of(LocalDate.of(2016, 12, 8))))
        .build();
    assertBeanEquals(loadedTrades.get(0), expectedTrade0);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_fx_ndf() {
    TradeCsvLoader standard = TradeCsvLoader.standard();
    ResourceLocator locator = ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fxtrades2.csv");
    ImmutableList<CharSource> charSources = ImmutableList.of(locator.getCharSource());
    ValueWithFailures<List<FxNdfTrade>> loadedData = standard.parse(charSources, FxNdfTrade.class);
    assertThat(loadedData.getFailures().size()).as(loadedData.getFailures().toString()).isEqualTo(0);

    List<FxNdfTrade> loadedTrades = loadedData.getValue();
    assertThat(loadedTrades).hasSize(3);

    FxNdfTrade expectedTrade0 = FxNdfTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(date(2016, 12, 6))
            .id(StandardId.of("OG", "tradeId11"))
            .build())
        .product(FxNdf.builder()
            .settlementCurrencyNotional(CurrencyAmount.of(USD, 10_000_000))
            .paymentDate(date(2016, 12, 8))
            .agreedFxRate(FxRate.of(CurrencyPair.of(USD, INR), 6.5))
            .index(FxIndex.of("USD/INR-FBIL-INR01"))
            .build())
        .build();
    assertBeanEquals(loadedTrades.get(0), expectedTrade0);

    FxNdfTrade expectedTrade1 = FxNdfTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(date(2016, 12, 6))
            .id(StandardId.of("OG", "tradeId12"))
            .build())
        .product(FxNdf.builder()
            .settlementCurrencyNotional(CurrencyAmount.of(USD, -20_000_000))
            .paymentDate(date(2016, 12, 8))
            .agreedFxRate(FxRate.of(CurrencyPair.of(USD, CLP), 5.8))
            .index(FxIndex.of("USD/CLP-DOLAR-OBS-CLP10"))
            .build())
        .build();
    assertBeanEquals(loadedTrades.get(1), expectedTrade1);

    HolidayCalendarId usdBrlCalId = HolidayCalendarId.defaultByCurrency(USD)
        .combinedWith(HolidayCalendarId.defaultByCurrency(BRL));
    FxNdfTrade expectedTrade2 = FxNdfTrade.builder()
        .info(TradeInfo.builder()
            .tradeDate(date(2016, 12, 6))
            .id(StandardId.of("OG", "tradeId13"))
            .build())
        .product(FxNdf.builder()
            .settlementCurrencyNotional(CurrencyAmount.of(USD, -30_000_000))
            .paymentDate(date(2016, 12, 8))
            .agreedFxRate(FxRate.of(CurrencyPair.of(USD, BRL), 5.5))
            .index(ImmutableFxIndex.builder()
                .name("USD/BRL")
                .currencyPair(CurrencyPair.of(USD, BRL))
                .fixingCalendar(usdBrlCalId)
                .maturityDateOffset(DaysAdjustment.ofBusinessDays(2, usdBrlCalId))
                .build())
            .build())
        .build();
    assertBeanEquals(loadedTrades.get(2), expectedTrade2);

    checkRoundtrip(FxNdfTrade.class, loadedTrades, expectedTrade0, expectedTrade1, expectedTrade2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_fra() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    List<FraTrade> filtered = trades.getValue().stream()
        .flatMap(filtering(FraTrade.class))
        .collect(toImmutableList());
    assertThat(filtered).hasSize(3);

    FraTrade expected0 = FraConventions.of(IborIndices.GBP_LIBOR_3M)
        .createTrade(date(2017, 6, 1), Period.ofMonths(2), BUY, 1_000_000, 0.005, REF_DATA)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123401"))
            .tradeDate(date(2017, 6, 1))
            .tradeTime(LocalTime.of(11, 5))
            .zone(ZoneId.of("Europe/London"))
            .build())
        .build();
    assertBeanEquals(expected0, filtered.get(0));

    FraTrade expected1 = FraConventions.of(IborIndices.GBP_LIBOR_6M)
        .toTrade(date(2017, 6, 1), date(2017, 8, 1), date(2018, 2, 1), date(2017, 8, 1), SELL, 1_000_000, 0.007)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123402"))
            .tradeDate(date(2017, 6, 1))
            .tradeTime(LocalTime.of(12, 35))
            .zone(ZoneId.of("Europe/London"))
            .build())
        .build();
    assertBeanEquals(expected1, filtered.get(1));

    FraTrade expected2 = FraTrade.builder()
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
    assertBeanEquals(expected2, filtered.get(2));

    checkRoundtrip(FraTrade.class, filtered, expected0, expected1, expected2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_fra_bothCounterpartyColumnsPresent() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE_CPTY);

    List<FraTrade> filtered = trades.getValue().stream()
        .flatMap(filtering(FraTrade.class))
        .collect(toImmutableList());
    assertThat(filtered).hasSize(3);

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
            .counterparty(StandardId.of(OG_COUNTERPARTY, "Bank B"))
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
  @Test
  public void test_load_fra_counterpartyColumnPresentNoScheme() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE_CPTY2);

    List<FraTrade> filtered = trades.getValue().stream()
        .flatMap(filtering(FraTrade.class))
        .collect(toImmutableList());
    assertThat(filtered).hasSize(3);

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
  @Test
  public void test_load_swap() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    List<SwapTrade> filtered = trades.getValue().stream()
        .flatMap(filtering(SwapTrade.class))
        .collect(toImmutableList());
    assertThat(filtered).hasSize(NUMBER_SWAPS);

    SwapTrade expected0 = expectedSwap0();
    SwapTrade expected1 = expectedSwap1();
    SwapTrade expected2 = expectedSwap2();
    SwapTrade expected3 = expectedSwap3();
    SwapTrade expected4 = expectedSwap4();
    SwapTrade expected5 = expectedSwap5();
    SwapTrade expected6 = expectedSwap6();
    SwapTrade expected7 = expectedSwap7();

    assertBeanEquals(expected0, filtered.get(0));
    assertBeanEquals(expected1, filtered.get(1));
    assertBeanEquals(expected2, filtered.get(2));
    assertBeanEquals(expected3, filtered.get(3));
    assertBeanEquals(expected4, filtered.get(4));
    assertBeanEquals(expected5, filtered.get(5));
    assertBeanEquals(expected6, filtered.get(6));
    assertBeanEquals(expected7, filtered.get(7));

    checkRoundtrip(
        SwapTrade.class, filtered, expected0, expected1, expected2, expected3, expected4, expected5, expected6, expected7);
  }

  private SwapTrade expectedSwap0() {
    return FixedIborSwapConventions.GBP_FIXED_1Y_LIBOR_3M
        .createTrade(date(2017, 6, 1), Period.ofMonths(1), Tenor.ofYears(5), BUY, 2_000_000, 0.004, REF_DATA)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123411"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .build();
  }

  private SwapTrade expectedSwap1() {
    return FixedIborSwapConventions.GBP_FIXED_6M_LIBOR_6M
        .toTrade(date(2017, 6, 1), date(2017, 8, 1), date(2022, 8, 1), BUY, 3_100_000, -0.0001)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123412"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .build();
  }

  private SwapTrade expectedSwap2() {
    NotionalSchedule notionalSchedule = NotionalSchedule.of(GBP,
        ValueSchedule.of(
            5_000_000,
            ValueStep.of(date(2018, 8, 1), ValueAdjustment.ofReplace(4_000_000)),
            ValueStep.of(date(2019, 8, 1), ValueAdjustment.ofReplace(3_000_000)),
            ValueStep.of(date(2020, 8, 1), ValueAdjustment.ofReplace(2_000_000)),
            ValueStep.of(date(2021, 8, 1), ValueAdjustment.ofReplace(1_000_000))));
    Swap expectedSwap = Swap.builder()
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
    return SwapTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123413"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(expectedSwap)
        .build();
  }

  private SwapTrade expectedSwap3() {
    return XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M
        .createTrade(date(2017, 6, 1), Period.ofMonths(1), Tenor.TENOR_3Y, BUY, 2_000_000, 2_500_000, 0.006, REF_DATA)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123414"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .build();
  }

  private SwapTrade expectedSwap4() {
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
    return SwapTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123415"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(expectedSwap)
        .build();
  }

  private SwapTrade expectedSwap5() {
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
                    .stubConvention(StubConvention.SMART_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P3M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 1_200_000))
                .calculation(IborRateCalculation.of(IborIndices.GBP_LIBOR_3M))
                .build())
        .build();
    return SwapTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123416"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(expectedSwap)
        .build();
  }

  private SwapTrade expectedSwap6() {
    Swap expectedSwap = Swap.builder()
        .legs(
            RateCalculationSwapLeg.builder()
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 8, 8))
                    .endDate(date(2022, 8, 8))
                    .frequency(Frequency.P3M)
                    .businessDayAdjustment(BusinessDayAdjustment.of(PRECEDING, GBLO.combinedWith(USNY)))
                    .stubConvention(StubConvention.SMART_INITIAL)
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
                    .stubConvention(StubConvention.SMART_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P6M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 1_500_000))
                .calculation(OvernightRateCalculation.of(OvernightIndices.GBP_SONIA))
                .build())
        .build();
    return SwapTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123417"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(expectedSwap)
        .build();
  }

  private SwapTrade expectedSwap7() {
    Swap expectedSwap = Swap.builder()
        .legs(
            KnownAmountSwapLeg.builder()
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 8, 8))
                    .endDate(date(2022, 8, 8))
                    .frequency(Frequency.P3M)
                    .businessDayAdjustment(BusinessDayAdjustment.of(PRECEDING, GBLO.combinedWith(USNY)))
                    .stubConvention(StubConvention.SMART_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P3M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .amount(ValueSchedule.of(2_000_000, ValueStep.of(LocalDate.of(2018, 6, 1), ValueAdjustment.ofReplace(2_500_000))))
                .currency(GBP)
                .build(),

            RateCalculationSwapLeg.builder()
                .payReceive(RECEIVE)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 8, 8))
                    .endDate(date(2022, 8, 8))
                    .frequency(Frequency.P6M)
                    .businessDayAdjustment(BusinessDayAdjustment.NONE)
                    .stubConvention(StubConvention.SMART_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P6M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
                .notionalSchedule(NotionalSchedule.of(GBP, 1_500_000))
                .calculation(OvernightRateCalculation.of(OvernightIndices.GBP_SONIA))
                .build())
        .build();
    return SwapTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123418"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(expectedSwap)
        .build();
  }

  @Test
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
    assertThat(result.getFailures().size()).as(result.getFailures().toString()).isEqualTo(0);
    assertThat(result.getValue()).hasSize(1);

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
                    .stubConvention(StubConvention.SMART_INITIAL)
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
                    .stubConvention(StubConvention.SMART_INITIAL)
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
                    .stubConvention(StubConvention.SMART_INITIAL)
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
                    .stubConvention(StubConvention.SMART_INITIAL)
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
                    .stubConvention(StubConvention.SMART_INITIAL)
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
                    .stubConvention(StubConvention.SMART_INITIAL)
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

    checkRoundtrip(SwapTrade.class, result.getValue(), expected);
  }

  @Test
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
    assertThat(result.getFailures().size()).as(result.getFailures().toString()).isEqualTo(0);
    assertThat(result.getValue()).hasSize(1);

    Swap expectedSwap = Swap.builder()
        .legs(
            RateCalculationSwapLeg.builder()  // Fixed fixed stub
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 5, 2))
                    .endDate(date(2022, 5, 2))
                    .frequency(Frequency.P12M)
                    .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
                    .stubConvention(StubConvention.SMART_INITIAL)
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
                    .stubConvention(StubConvention.SMART_INITIAL)
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

  @Test
  public void test_load_swap_knownAmount() {
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
        .put("Leg 1 Known Amount", "1100000")

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
    assertThat(result.getFailures().size()).as(result.getFailures().toString()).isEqualTo(0);
    assertThat(result.getValue()).hasSize(1);

    Swap expectedSwap = Swap.builder()
        .legs(
            KnownAmountSwapLeg.builder()
                .payReceive(PAY)
                .currency(GBP)
                .amount(ValueSchedule.of(1100000d))
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 5, 2))
                    .endDate(date(2022, 5, 2))
                    .frequency(Frequency.P12M)
                    .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
                    .stubConvention(StubConvention.SMART_INITIAL)
                    .build())
                .paymentSchedule(PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P12M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .paymentRelativeTo(PaymentRelativeTo.PERIOD_END)
                    .build())
                .build(),
            RateCalculationSwapLeg.builder()
                .payReceive(PAY)
                .accrualSchedule(PeriodicSchedule.builder()
                    .startDate(date(2017, 5, 2))
                    .endDate(date(2022, 5, 2))
                    .frequency(Frequency.P6M)
                    .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
                    .stubConvention(StubConvention.SMART_INITIAL)
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

  @Test
  public void test_load_swap_knownAmountAndFixedRate() {
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
        .put("Leg 1 Known Amount", "1100000")
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
    assertThat(result.getFailures().size()).as(result.getFailures().toString()).isEqualTo(1);
    FailureItem failure = result.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage())
        .isEqualTo("CSV trade file 'Unknown.txt' type 'Swap' could not be parsed at line 2: " +
            "Swap leg 1 must not define both kinds of fixed leg: 'Leg 1 Fixed Rate' and 'Leg 1 Known Amount'");
  }

  @Test
  public void test_load_swap_knownAmountAndIndex() {
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
        .put("Leg 1 Known Amount", "1100000")
        .put("Leg 1 Index", "GBP-LIBOR-6M")

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
    assertThat(result.getFailures().size()).as(result.getFailures().toString()).isEqualTo(1);
    FailureItem failure = result.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage())
        .isEqualTo("CSV trade file 'Unknown.txt' type 'Swap' could not be parsed at line 2: Swap leg 1 must not define " +
            "both floating and fixed leg columns: 'Leg 1 Index' and 'Leg 1 Fixed Rate' or 'Leg 1 Known Amount'");
  }

  @Test
  public void test_load_swap_fixedRateAndIndex() {
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
        .put("Leg 1 Index", "GBP-LIBOR-6M")

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
    assertThat(result.getFailures().size()).as(result.getFailures().toString()).isEqualTo(1);
    FailureItem failure = result.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage())
        .isEqualTo("CSV trade file 'Unknown.txt' type 'Swap' could not be parsed at line 2: Swap leg 1 must not define " +
            "both floating and fixed leg columns: 'Leg 1 Index' and 'Leg 1 Fixed Rate' or 'Leg 1 Known Amount'");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_swaption() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    List<SwaptionTrade> filtered = trades.getValue().stream()
        .flatMap(filtering(SwaptionTrade.class))
        .collect(toImmutableList());
    assertThat(filtered).hasSize(3);

    SwaptionTrade expected0 = expectedSwaption0();
    SwaptionTrade expected1 = expectedSwaption1();
    SwaptionTrade expected2 = expectedSwaption2();

    assertBeanEquals(expected0, filtered.get(0));
    assertBeanEquals(expected1, filtered.get(1));
    assertBeanEquals(expected2, filtered.get(2));

    checkRoundtrip(
        SwaptionTrade.class, filtered, expected0, expected1, expected2);
  }

  private SwaptionTrade expectedSwaption0() {
    SwapTrade swapTrade = expectedSwap0();
    Swaption swaption = Swaption.builder()
        .longShort(LongShort.LONG)
        .swaptionSettlement(CashSwaptionSettlement.of(date(2017, 7, 3), CashSwaptionSettlementMethod.PAR_YIELD))
        .expiryDate(AdjustableDate.of(date(2017, 6, 30)))
        .expiryTime(LocalTime.of(11, 0))
        .expiryZone(ZoneId.of("Europe/London"))
        .underlying(swapTrade.getProduct())
        .build();
    Payment premium = Payment.of(CurrencyAmount.of(GBP, -1000), date(2017, 6, 3));
    return SwaptionTrade.of(swapTrade.getInfo(), swaption, premium);
  }

  private SwaptionTrade expectedSwaption1() {
    SwapTrade swapTrade = expectedSwap1();
    Swaption swaption = Swaption.builder()
        .longShort(LongShort.SHORT)
        .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT)
        .expiryDate(AdjustableDate.of(date(2017, 6, 30)))
        .expiryTime(LocalTime.of(11, 0))
        .expiryZone(ZoneId.of("Europe/London"))
        .underlying(swapTrade.getProduct())
        .build();
    Payment premium = Payment.of(CurrencyAmount.of(GBP, 1000), date(2017, 6, 3));
    return SwaptionTrade.of(swapTrade.getInfo(), swaption, premium);
  }

  private SwaptionTrade expectedSwaption2() {
    SwapTrade swapTrade = expectedSwap2();
    Swaption swaption = Swaption.builder()
        .longShort(LongShort.SHORT)
        .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT)
        .expiryDate(AdjustableDate.of(date(2017, 6, 30)))
        .expiryTime(LocalTime.of(11, 0))
        .expiryZone(ZoneId.of("Europe/London"))
        .underlying(swapTrade.getProduct())
        .build();
    Payment premium = Payment.of(CurrencyAmount.of(GBP, 0), date(2017, 6, 30));
    return SwaptionTrade.of(swapTrade.getInfo(), swaption, premium);
  }

  private FxSingleTrade expectedFxSingle() {
    double notional = 1.0e6;
    double fxRate = 1.1d;
    return FxSingleTrade.of(
        TradeInfo.empty(),
        FxSingle.of(
            CurrencyAmount.of(EUR, notional),
            CurrencyAmount.of(USD, -notional * fxRate),
            LocalDate.of(2014, 5, 13)));
  }

  private FxVanillaOptionTrade expectedFxVanillaOption() {
    return FxVanillaOptionTrade.builder()
        .product(FxVanillaOption.builder()
            .longShort(SHORT)
            .expiryDate(LocalDate.of(2014, 5, 9))
            .expiryTime(LocalTime.of(13, 10))
            .expiryZone(ZoneId.of("Z"))
            .underlying(expectedFxSingle().getProduct())
            .build())
        .premium(AdjustablePayment.of(Payment.of(CurrencyAmount.of(USD, 230.3), LocalDate.of(2014, 1, 12))))
        .build();
  }

  private FxSingleBarrierOptionTrade expectedFxSingleBarrierOptionWithRebate() {
    return FxSingleBarrierOptionTrade.builder()
        .product(FxSingleBarrierOption.of(
            expectedFxVanillaOption().getProduct(),
            SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, 17.2),
            CurrencyAmount.of(USD, 17.666)))
        .premium(AdjustablePayment.of(Payment.of(CurrencyAmount.of(USD, 230.3), LocalDate.of(2014, 1, 12))))
        .build();
  }

  private FxSingleBarrierOptionTrade expectedFxSingleBarrierOptionWithoutRebate() {
    return FxSingleBarrierOptionTrade.builder()
        .product(FxSingleBarrierOption.of(
            expectedFxVanillaOption().getProduct(),
            SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, 17.2)))
        .premium(AdjustablePayment.of(Payment.of(CurrencyAmount.of(USD, 230.3), LocalDate.of(2014, 1, 12))))
        .build();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_bulletPayment() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ResourceLocator locator = ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fxtrades.csv");
    ImmutableList<CharSource> charSources = ImmutableList.of(locator.getCharSource());
    ValueWithFailures<List<BulletPaymentTrade>> loadedData = test.parse(charSources, BulletPaymentTrade.class);
    assertThat(loadedData.getValue()).hasSize(2);

    BulletPaymentTrade expected0 = BulletPaymentTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "tradeId21"))
            .tradeDate(date(2016, 12, 6))
            .build())
        .product(BulletPayment.builder()
            .payReceive(RECEIVE)
            .value(CurrencyAmount.of(USD, 25000))
            .date(AdjustableDate.of(date(2016, 12, 8), BusinessDayAdjustment.of(FOLLOWING, USNY)))
            .build())
        .build();
    assertBeanEquals(expected0, loadedData.getValue().get(0));

    BulletPaymentTrade expected1 = BulletPaymentTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "tradeId22"))
            .tradeDate(date(2016, 12, 22))
            .build())
        .product(BulletPayment.builder()
            .payReceive(PAY)
            .value(CurrencyAmount.of(GBP, 35000))
            .date(AdjustableDate.of(date(2016, 12, 24), BusinessDayAdjustment.of(FOLLOWING, GBLO)))
            .build())
        .build();
    assertBeanEquals(expected1, loadedData.getValue().get(1));

    checkRoundtrip(BulletPaymentTrade.class, loadedData.getValue(), expected0, expected1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_termDeposit() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    List<TermDepositTrade> filtered = trades.getValue().stream()
        .flatMap(filtering(TermDepositTrade.class))
        .collect(toImmutableList());
    assertThat(filtered).hasSize(3);

    TermDepositTrade expected0 = TermDepositConventions.GBP_SHORT_DEPOSIT_T0
        .createTrade(date(2017, 6, 1), Period.ofWeeks(2), SELL, 400_000, 0.002, REF_DATA)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123421"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .build();
    assertBeanEquals(expected0, filtered.get(0));

    TermDepositTrade expected1 = TermDepositConventions.GBP_SHORT_DEPOSIT_T0
        .toTrade(date(2017, 6, 1), date(2017, 6, 1), date(2017, 6, 15), SELL, 500_000, 0.0022)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123422"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .build();
    assertBeanEquals(expected1, filtered.get(1));

    TermDepositTrade expected2 = TermDepositTrade.builder()
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
    assertBeanEquals(expected2, filtered.get(2));

    checkRoundtrip(TermDepositTrade.class, filtered, expected0, expected1, expected2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_cds() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    List<CdsTrade> filtered = trades.getValue().stream()
        .flatMap(filtering(CdsTrade.class))
        .collect(toImmutableList());
    assertThat(filtered).hasSize(4);

    CdsTrade expected0 = expectedCds0();
    CdsTrade expected1 = expectedCds1();
    CdsTrade expected2 = expectedCds2();
    CdsTrade expected3 = expectedCds3();

    assertBeanEquals(expected0, filtered.get(0));
    assertBeanEquals(expected1, filtered.get(1));
    assertBeanEquals(expected2, filtered.get(2));
    assertBeanEquals(expected3, filtered.get(3));

    checkRoundtrip(CdsTrade.class, filtered, expected0, expected1, expected2, expected3);
  }

  private CdsTrade expectedCds0() {
    StandardId legEnt = StandardId.of("OG-Entity", "FOO");
    return CdsConventions.GBP_STANDARD
        .createTrade(legEnt, date(2017, 6, 1), Tenor.ofYears(5), BUY, 2_000_000, 0.005, REF_DATA)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123441"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .build();
  }

  private CdsTrade expectedCds1() {
    StandardId legEnt = StandardId.of("BLUE", "BAR");
    return CdsConventions.EUR_GB_STANDARD
        .createTrade(legEnt, date(2017, 6, 1), date(2017, 6, 21), date(2019, 6, 19), SELL, 1_500_000, 0.011, REF_DATA)
        .toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123442"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .build();
  }

  private CdsTrade expectedCds2() {
    return CdsTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123443"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(Cds.builder()
            .buySell(BUY)
            .legalEntityId(StandardId.of("OG-Ticker", "CATCDS-CDS-SNRFOR"))
            .fixedRate(0.026)
            .currency(EUR)
            .notional(1_500_000)
            .paymentSchedule(PeriodicSchedule.builder()
                .startDate(date(2017, 6, 21))
                .endDate(date(2019, 6, 19))
                .frequency(Frequency.P3M)
                .stubConvention(StubConvention.SMART_INITIAL)
                .rollConvention(RollConventions.IMM)
                .businessDayAdjustment(BusinessDayAdjustment.NONE)
                .build())
            .build())
        .build();
  }

  private CdsTrade expectedCds3() {
    return CdsTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123444"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(Cds.builder()
            .buySell(BUY)
            .legalEntityId(StandardId.of("OG-Ticker", "CATCDS-CDS-SECDOM"))
            .fixedRate(0.026)
            .currency(EUR)
            .notional(1_500_000)
            .dayCount(DayCounts.ACT_365F)
            .paymentOnDefault(PaymentOnDefault.NONE)
            .protectionStart(ProtectionStartOfDay.NONE)
            .paymentSchedule(PeriodicSchedule.builder()
                .startDate(date(2017, 6, 21))
                .endDate(date(2019, 6, 19))
                .frequency(Frequency.P3M)
                .stubConvention(StubConvention.SMART_FINAL)
                .rollConvention(RollConventions.IMM)
                .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
                .build())
            .stepinDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
            .settlementDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
            .build())
        .upfrontFee(
            AdjustablePayment.of(
                CurrencyAmount.of(GBP, -1000),
                AdjustableDate.of(date(2017, 6, 3), BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))))
        .build();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_cdsIndex() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    List<CdsIndexTrade> filtered = trades.getValue().stream()
        .flatMap(filtering(CdsIndexTrade.class))
        .collect(toImmutableList());
    assertThat(filtered).hasSize(4);

    CdsIndexTrade expected0 = expectedCdsIndex0();
    CdsIndexTrade expected1 = expectedCdsIndex1();
    CdsIndexTrade expected2 = expectedCdsIndex2();
    CdsIndexTrade expected3 = expectedCdsIndex3();

    assertBeanEquals(expected0, filtered.get(0));
    assertBeanEquals(expected1, filtered.get(1));
    assertBeanEquals(expected2, filtered.get(2));
    assertBeanEquals(expected3, filtered.get(3));

    checkRoundtrip(CdsIndexTrade.class, filtered, expected0, expected1, expected2, expected3);
  }

  @Test
  public void test_load_invalidCdsIndexId() {
    ImmutableMap<String, String> csvMap = ImmutableMap.<String, String>builder()
        .put("Strata Trade Type", "CDS Index")
        .put("Id Scheme", "OG")
        .put("Id", "123456")
        .put("Buy Sell", "Buy")
        .put("Fixed Rate", "5")
        .put("Currency", "GBP")
        .put("Notional", "1000000")
        .put("Start Date", "2017-05-02")
        .put("End Date", "2022-05-02")
        .put("Frequency", "12M")
        .put("RED Code", "FOOBAR")
        .build();

    String csv = Joiner.on(',').join(csvMap.keySet()) + "\n" + Joiner.on(',').join(csvMap.values());

    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<CdsIndexTrade>> result = test.parse(ImmutableList.of(CharSource.wrap(csv)), CdsIndexTrade.class);
    assertThat(result.getFailures().size()).as(result.getFailures().toString()).isEqualTo(1);
    FailureItem failure = result.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage())
        .isEqualTo("CSV trade file 'Unknown.txt' type 'CDS Index' could not be parsed at line 2: RED9 must be present " +
            "for CdsIndex but found: RED6");
  }

  private CdsIndexTrade expectedCdsIndex0() {
    StandardId legEnt1 = StandardId.of("OG-Entity", "FOO");
    StandardId legEnt2 = StandardId.of("OG-Entity", "BAR");
    CdsIndex cdsIndex = CdsIndex.builder()
        .buySell(BUY)
        .currency(GBP)
        .notional(1_000_000)
        .fixedRate(0.05)
        .cdsIndexId(StandardId.of("OG-CDS", "IDX"))
        .legalEntityIds(legEnt1, legEnt2)
        .paymentSchedule(PeriodicSchedule.builder()
            .startDate(date(2017, 6, 1))
            .endDate(date(2022, 6, 1))
            .frequency(Frequency.P12M)
            .businessDayAdjustment(BusinessDayAdjustment.NONE)
            .stubConvention(StubConvention.SMART_INITIAL)
            .build())
        .build();
    return CdsIndexTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123451"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(cdsIndex)
        .build();
  }

  private CdsIndexTrade expectedCdsIndex1() {
    StandardId legEnt1 = StandardId.of("OG-Entity", "FOO");
    StandardId legEnt2 = StandardId.of("OG-Entity", "BAR");
    CdsIndex cdsIndex = CdsIndex.builder()
        .buySell(BUY)
        .currency(GBP)
        .notional(1_000_000)
        .fixedRate(0.05)
        .cdsIndexId(StandardId.of("OG-Ticker", "FOOBARCDS-CDX-S1V1"))
        .legalEntityIds(legEnt1, legEnt2)
        .paymentSchedule(PeriodicSchedule.builder()
            .startDate(date(2017, 6, 1))
            .endDate(date(2022, 6, 1))
            .frequency(Frequency.P12M)
            .businessDayAdjustment(BusinessDayAdjustment.NONE)
            .stubConvention(StubConvention.SMART_INITIAL)
            .build())
        .build();
    return CdsIndexTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123451"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(cdsIndex)
        .build();
  }

  private CdsIndexTrade expectedCdsIndex2() {
    CdsIndex cdsIndex = CdsIndex.builder()
        .buySell(BUY)
        .currency(GBP)
        .notional(1_000_000)
        .fixedRate(0.05)
        .cdsIndexId(StandardId.of("OG-Ticker", "FOOBARCDS-CDX-S1V1"))
        .paymentSchedule(PeriodicSchedule.builder()
            .startDate(date(2017, 6, 1))
            .endDate(date(2022, 6, 1))
            .frequency(Frequency.P12M)
            .businessDayAdjustment(BusinessDayAdjustment.NONE)
            .stubConvention(StubConvention.SMART_INITIAL)
            .build())
        .build();
    return CdsIndexTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123454"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(cdsIndex)
        .build();
  }

  private CdsIndexTrade expectedCdsIndex3() {
    CdsIndex cdsIndex = CdsIndex.builder()
        .buySell(BUY)
        .currency(GBP)
        .notional(1_000_000)
        .fixedRate(0.05)
        .cdsIndexId(StandardId.of("OG-Ticker", "FOOBARCDS-CDX-STD"))
        .paymentSchedule(PeriodicSchedule.builder()
            .startDate(date(2017, 6, 1))
            .endDate(date(2022, 6, 1))
            .frequency(Frequency.P12M)
            .businessDayAdjustment(BusinessDayAdjustment.NONE)
            .stubConvention(StubConvention.SMART_INITIAL)
            .build())
        .build();
    return CdsIndexTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123455"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(cdsIndex)
        .build();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_filtered() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.parse(
        ImmutableList.of(FILE.getCharSource()), ImmutableList.of(FraTrade.class, TermDepositTrade.class));

    assertThat(trades.getValue()).hasSize(6);
    assertThat(trades.getFailures()).hasSize(26);
    assertThat(trades.getFailures().get(0).getMessage()).isEqualTo(
        "Trade type not allowed " + SwapTrade.class.getName() + ", only these types are supported: FraTrade, TermDepositTrade");
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(fraCount.get()).isEqualTo(3);
    assertThat(termCount.get()).isEqualTo(3);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_security() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    List<SecurityTrade> filtered = trades.getValue().stream()
        .flatMap(filtering(SecurityTrade.class))
        .collect(toImmutableList());
    assertThat(filtered).hasSize(2);

    SecurityTrade expected0 = SecurityTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123431"))
            .tradeDate(date(2017, 6, 1))
            .settlementDate(date(2017, 6, 3))
            .build())
        .securityId(SecurityId.of(OG_SECURITY_SCHEME, "AAPL"))
        .quantity(12)
        .price(14.5)
        .build();
    assertBeanEquals(expected0, filtered.get(0));

    SecurityTrade expected1 = SecurityTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123432"))
            .tradeDate(date(2017, 6, 1))
            .settlementDate(date(2017, 6, 3))
            .build())
        .securityId(SecurityId.of("BBG", "MSFT"))
        .quantity(-20)
        .price(17.8)
        .build();
    assertBeanEquals(expected1, filtered.get(1));

    checkRoundtrip(SecurityTrade.class, filtered, expected0, expected1);
  }

  @Test
  public void test_load_security_explicit() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<SecurityTrade>> trades =
        test.parse(ImmutableList.of(FILE.getCharSource()), SecurityTrade.class);

    List<SecurityTrade> filtered = trades.getValue().stream()
        .collect(toImmutableList());
    assertThat(filtered).hasSize(3);

    SecurityTrade expected0 = SecurityTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123431"))
            .tradeDate(date(2017, 6, 1))
            .settlementDate(date(2017, 6, 3))
            .build())
        .securityId(SecurityId.of(OG_SECURITY_SCHEME, "AAPL"))
        .quantity(12)
        .price(14.5)
        .build();
    assertBeanEquals(expected0, filtered.get(0));

    SecurityTrade expected1 = SecurityTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123432"))
            .tradeDate(date(2017, 6, 1))
            .settlementDate(date(2017, 6, 3))
            .build())
        .securityId(SecurityId.of("BBG", "MSFT"))
        .quantity(-20)
        .price(17.8)
        .build();
    assertBeanEquals(expected1, filtered.get(1));

    SecurityTrade expected2 = SecurityTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123433"))
            .tradeDate(date(2017, 6, 1))
            .settlementDate(date(2017, 6, 3))
            .build())
        .securityId(SecurityId.of(OG_SECURITY_SCHEME, "AAPL"))
        .quantity(12)
        .price(14.5)
        .build();
    assertBeanEquals(expected1, filtered.get(1));

    checkRoundtrip(SecurityTrade.class, filtered, expected0, expected1, expected2);
  }

  @Test
  public void test_load_genericSecurity() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    List<GenericSecurityTrade> filtered = trades.getValue().stream()
        .flatMap(filtering(GenericSecurityTrade.class))
        .collect(toImmutableList());
    assertThat(filtered).hasSize(1);

    GenericSecurityTrade expected0 = GenericSecurityTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123433"))
            .tradeDate(date(2017, 6, 1))
            .settlementDate(date(2017, 6, 3))
            .build())
        .security(
            GenericSecurity.of(
                SecurityInfo.of(
                    SecurityId.of(OG_SECURITY_SCHEME, "AAPL"),
                    SecurityPriceInfo.of(5, CurrencyAmount.of(USD, 0.01), 10))))
        .quantity(12)
        .price(14.5)
        .build();
    assertBeanEquals(expected0, filtered.get(0));

    checkRoundtrip(GenericSecurityTrade.class, filtered, expected0);
  }

  //-------------------------------------------------------------------------

  @Test
  public void test_FxSingleBarrierOption() {
    ResourceLocator file = ResourceLocator.of(
        "classpath:com/opengamma/strata/loader/csv/fx_single_barrier_option_trade.csv");

    ValueWithFailures<List<FxSingleBarrierOptionTrade>> trades = TradeCsvLoader.standard().parse(
        ImmutableList.of(file.getCharSource()), FxSingleBarrierOptionTrade.class);

    checkRoundtrip(
        FxSingleBarrierOptionTrade.class,
        trades.getValue(),
        expectedFxSingleBarrierOptionWithRebate(),
        expectedFxSingleBarrierOptionWithoutRebate());
  }

  @Test
  public void test_load_CapFloor() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    List<IborCapFloorTrade> filtered = trades.getValue().stream()
        .flatMap(filtering(IborCapFloorTrade.class))
        .collect(toImmutableList());
    assertThat(filtered).hasSize(2);

    IborCapFloorTrade expected0 = IborCapFloorTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123452"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(IborCapFloor.of(IborCapFloorLeg.builder()
            .payReceive(RECEIVE)
            .paymentSchedule(PeriodicSchedule.of(
                date(2020, 3, 10),
                date(2025, 3, 10),
                Frequency.P3M,
                BusinessDayAdjustment.NONE,
                StubConvention.NONE,
                false))
            .currency(USD)
            .notional(ValueSchedule.of(10_000_000))
            .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_3M))
            .capSchedule(ValueSchedule.of(0.021))
            .build()))
        .build();
    assertBeanEquals(expected0, filtered.get(0));

    IborCapFloorTrade expected1 = IborCapFloorTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123453"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(IborCapFloor.of(IborCapFloorLeg.builder()
            .payReceive(PAY)
            .paymentSchedule(PeriodicSchedule.of(
                date(2020, 3, 10),
                date(2030, 3, 10),
                Frequency.P6M,
                BusinessDayAdjustment.NONE,
                StubConvention.NONE,
                false))
            .currency(EUR)
            .notional(ValueSchedule.of(15_000_000))
            .calculation(IborRateCalculation.of(IborIndices.EUR_EURIBOR_6M))
            .floorSchedule(ValueSchedule.of(0.005))
            .build()))
        .premium(AdjustablePayment.ofReceive(CurrencyAmount.of(EUR, 5000), date(2020, 3, 10)))
        .build();
    assertBeanEquals(expected1, filtered.get(1));

    checkRoundtrip(IborCapFloorTrade.class, filtered, expected0, expected1);
  }

  @Test
  public void test_load_OvernightCapFloor() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.load(FILE);

    List<OvernightInArrearsCapFloorTrade> filtered = trades.getValue().stream()
        .flatMap(filtering(OvernightInArrearsCapFloorTrade.class))
        .collect(toImmutableList());
    assertThat(filtered).hasSize(2);

    OvernightInArrearsCapFloorTrade expected0 = OvernightInArrearsCapFloorTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123456"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(OvernightInArrearsCapFloor.of(OvernightInArrearsCapFloorLeg.builder()
            .payReceive(RECEIVE)
            .paymentSchedule(PeriodicSchedule.of(
                date(2020, 3, 10),
                date(2025, 3, 10),
                Frequency.P3M,
                BusinessDayAdjustment.NONE,
                StubConvention.NONE,
                false))
            .currency(USD)
            .notional(ValueSchedule.of(10_000_000))
            .calculation(OvernightRateCalculation.of(OvernightIndices.USD_SOFR))
            .capSchedule(ValueSchedule.of(0.021))
            .build()))
        .build();
    assertBeanEquals(expected0, filtered.get(0));

    OvernightInArrearsCapFloorTrade expected1 = OvernightInArrearsCapFloorTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("OG", "123457"))
            .tradeDate(date(2017, 6, 1))
            .build())
        .product(OvernightInArrearsCapFloor.of(OvernightInArrearsCapFloorLeg.builder()
            .payReceive(PAY)
            .paymentSchedule(PeriodicSchedule.of(
                date(2020, 3, 10),
                date(2030, 3, 10),
                Frequency.P6M,
                BusinessDayAdjustment.NONE,
                StubConvention.NONE,
                false))
            .currency(EUR)
            .notional(ValueSchedule.of(15_000_000))
            .calculation(OvernightRateCalculation.of(OvernightIndices.EUR_ESTR))
            .floorSchedule(ValueSchedule.of(0.005))
            .build()))
        .premium(AdjustablePayment.ofReceive(CurrencyAmount.of(EUR, 5000), date(2020, 3, 10)))
        .build();
    assertBeanEquals(expected1, filtered.get(1));

    checkRoundtrip(OvernightInArrearsCapFloorTrade.class, filtered, expected0, expected1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_invalidNoHeader() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.parse(ImmutableList.of(CharSource.wrap("")));

    assertThat(trades.getFailures()).hasSize(1);
    FailureItem failure = trades.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage()).startsWith("CSV trade file 'Unknown.txt' could not be parsed");
  }

  @Test
  public void test_load_invalidNoType() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.parse(ImmutableList.of(CharSource.wrap("Id")));

    assertThat(trades.getFailures()).hasSize(1);
    FailureItem failure = trades.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage()).startsWith("CSV trade file 'Unknown.txt' does not contain 'Strata Trade Type' header");
  }

  @Test
  public void test_load_invalidUnknownType() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.parse(ImmutableList.of(CharSource.wrap("Strata Trade Type\nFoo")));

    assertThat(trades.getFailures()).hasSize(1);
    FailureItem failure = trades.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage()).isEqualTo("CSV trade file 'Unknown.txt' contained unknown trade type 'Foo' at line 2");
  }

  @Test
  public void test_load_unknownTypeNotFixedViaResolver() {
    AtomicReference<String> foundType = new AtomicReference<>();
    TradeCsvLoader test = TradeCsvLoader.of(new TradeCsvInfoResolver() {
      @Override
      public ReferenceData getReferenceData() {
        return ReferenceData.standard();
      }

      @Override
      public Optional<Trade> parseOtherTrade(String typeUpper, CsvRow row, TradeInfo info) {
        foundType.set(typeUpper);
        return Optional.empty();
      }
    });
    ValueWithFailures<List<Trade>> trades = test.parse(ImmutableList.of(CharSource.wrap("Strata Trade Type\nFoo")));

    assertThat(foundType.get()).isEqualTo("FOO");
    assertThat(trades.getFailures()).hasSize(1);
    FailureItem failure = trades.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage()).isEqualTo("CSV trade file 'Unknown.txt' contained unknown trade type 'Foo' at line 2");
  }

  @Test
  public void test_load_unknownTypeFixedViaResolver() {
    Trade trade = new Trade() {
      @Override
      public Trade withInfo(PortfolioItemInfo info) {
        throw new UnsupportedOperationException();
      }

      @Override
      public TradeInfo getInfo() {
        return TradeInfo.empty();
      }
    };
    TradeCsvLoader test = TradeCsvLoader.of(new TradeCsvInfoResolver() {
      @Override
      public ReferenceData getReferenceData() {
        return ReferenceData.standard();
      }

      @Override
      public Optional<Trade> parseOtherTrade(String typeUpper, CsvRow row, TradeInfo info) {
        return Optional.of(trade);
      }
    });
    ValueWithFailures<List<Trade>> trades = test.parse(ImmutableList.of(CharSource.wrap("Strata Trade Type\nFoo")));

    assertThat(trades.getFailures()).hasSize(0);
    assertThat(trades.getValue()).hasSize(1);
  }

  @Test
  public void test_load_overrideTypeViaResolver() {
    Trade trade = new Trade() {
      @Override
      public Trade withInfo(PortfolioItemInfo info) {
        throw new UnsupportedOperationException();
      }

      @Override
      public TradeInfo getInfo() {
        return TradeInfo.empty();
      }
    };
    TradeCsvLoader test = TradeCsvLoader.of(new TradeCsvInfoResolver() {
      @Override
      public ReferenceData getReferenceData() {
        return ReferenceData.standard();
      }

      @Override
      public Optional<Trade> overrideParseTrade(String typeUpper, CsvRow row, TradeInfo info) {
        return Optional.of(trade);
      }
    });
    ValueWithFailures<List<Trade>> trades = test.parse(ImmutableList.of(CharSource.wrap("Strata Trade Type\nFRA")));

    assertThat(trades.getFailures()).hasSize(0);
    assertThat(trades.getValue()).hasSize(1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_invalidFra() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.parse(ImmutableList.of(CharSource.wrap("Strata Trade Type,Buy Sell\nFra,Buy")));

    assertThat(trades.getFailures()).hasSize(1);
    FailureItem failure = trades.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage())
        .isEqualTo("CSV trade file 'Unknown.txt' type 'Fra' could not be parsed at line 2: Header not found: 'Notional'");
  }

  @Test
  public void test_load_invalidSwap() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades = test.parse(ImmutableList.of(CharSource.wrap("Strata Trade Type,Buy Sell\nSwap,Buy")));

    assertThat(trades.getFailures()).hasSize(1);
    FailureItem failure = trades.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage())
        .isEqualTo(
            "CSV trade file 'Unknown.txt' type 'Swap' could not be parsed at line 2: Swap trade had invalid combination of fields. " +
                "Must include either 'Convention' or '" + "Leg 1 Direction'");
  }

  @Test
  public void test_load_invalidBulletPayment() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades =
        test.parse(ImmutableList.of(CharSource.wrap("Strata Trade Type,Direction\nBulletPayment,Pay")));

    assertThat(trades.getFailures()).hasSize(1);
    FailureItem failure = trades.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage())
        .isEqualTo("CSV trade file 'Unknown.txt' type 'BulletPayment' could not be parsed at line 2: Header not found: 'Currency'");
  }

  @Test
  public void test_load_invalidTermDeposit() {
    TradeCsvLoader test = TradeCsvLoader.standard();
    ValueWithFailures<List<Trade>> trades =
        test.parse(ImmutableList.of(CharSource.wrap("Strata Trade Type,Buy Sell\nTermDeposit,Buy")));

    assertThat(trades.getFailures()).hasSize(1);
    FailureItem failure = trades.getFailures().get(0);
    assertThat(failure.getReason()).isEqualTo(FailureReason.PARSING);
    assertThat(failure.getMessage())
        .isEqualTo("CSV trade file 'Unknown.txt' type 'TermDeposit' could not be parsed at line 2: Header not found: 'Notional'");
  }

  //-------------------------------------------------------------------------
  @SafeVarargs
  private final <T extends Trade & Bean> void checkRoundtrip(
      Class<T> type,
      List<T> loadedTrades,
      T... expectedTrades) {

    StringBuilder buf = new StringBuilder(1024);
    TradeCsvWriter.standard().write(loadedTrades, buf);
    List<CharSource> writtenCsv = ImmutableList.of(CharSource.wrap(buf.toString()));
    ValueWithFailures<List<T>> roundtrip = TradeCsvLoader.standard().parse(writtenCsv, type);
    assertThat(roundtrip.getFailures().size()).as(roundtrip.getFailures().toString()).isEqualTo(0);
    List<T> roundtripTrades = roundtrip.getValue();
    assertThat(roundtripTrades).hasSize(expectedTrades.length);
    for (int i = 0; i < roundtripTrades.size(); i++) {
      assertBeanEquals(expectedTrades[i], roundtripTrades.get(i));
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(BulletPaymentTradeCsvPlugin.class);
    coverPrivateConstructor(FraTradeCsvPlugin.class);
    coverPrivateConstructor(FxSingleTradeCsvPlugin.class);
    coverPrivateConstructor(FxSwapTradeCsvPlugin.class);
    coverPrivateConstructor(SecurityTradeCsvPlugin.class);
    coverPrivateConstructor(SwapTradeCsvPlugin.class);
    coverPrivateConstructor(TermDepositTradeCsvPlugin.class);
    coverPrivateConstructor(FullSwapTradeCsvPlugin.class);
    coverPrivateConstructor(FxSingleBarrierOptionTradeCsvPlugin.class);
  }

}
