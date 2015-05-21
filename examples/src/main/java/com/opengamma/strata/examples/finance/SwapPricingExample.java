/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;

import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.CalculationRules;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.examples.engine.ExampleEngine;
import com.opengamma.strata.examples.engine.ResultsFormatter;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.rate.swap.CompoundingMethod;
import com.opengamma.strata.finance.rate.swap.FixedRateCalculation;
import com.opengamma.strata.finance.rate.swap.IborRateCalculation;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
import com.opengamma.strata.finance.rate.swap.OvernightAccrualMethod;
import com.opengamma.strata.finance.rate.swap.OvernightRateCalculation;
import com.opengamma.strata.finance.rate.swap.PaymentSchedule;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.StubCalculation;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.function.OpenGammaPricingRules;

/**
 * Example to illustrate using the engine to price a swap.
 * <p>
 * This makes use of the example engine which sources the required market data from
 * JSON resources.
 */
public class SwapPricingExample {

  /**
   * Runs the example, pricing the instruments, producing the output as an ASCII table.
   * 
   * @param args  ignored
   */
  public static void main(String[] args) {
    // the trades that will have measures calculated
    List<Trade> trades = ImmutableList.of(
        createVanillaFixedVsLibor3mSwap(),
        createBasisLibor3mVsLibor6mWithSpreadSwap(),
        createOvernightAveragedWithSpreadVsLibor3mSwap(),
        createFixedVsLibor3mWithFixingSwap(),
        createFixedVsOvernightWithFixingSwap(),
        createStub3mFixedVsLibor3mSwap(),
        createStub1mFixedVsLibor3mSwap(),
        createInterpolatedStub3mFixedVsLibor6mSwap(),
        createInterpolatedStub4mFixedVsLibor6mSwap(),
        createZeroCouponFixedVsLibor3mSwap(),
        createCompoundingFixedVsFedFundsSwap(),
        createCompoundingFedFundsVsLibor3mSwap(),
        createCompoundingLibor6mVsLibor3mSwap(),
        createXCcyGbpLibor3mVsUsdLibor3mSwap(),
        createXCcyUsdFixedVsGbpLibor3mSwap(),
        createNotionalExchangeSwap());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measure.ID),
        Column.of(Measure.COUNTERPARTY),
        Column.of(Measure.SETTLEMENT_DATE),
        Column.of(Measure.MATURITY_DATE),
        Column.of(Measure.NOTIONAL),
        Column.of(Measure.PRESENT_VALUE),
        Column.of(Measure.PRESENT_VALUE_PAY_LEG),
        Column.of(Measure.PRESENT_VALUE_RECEIVE_LEG),
        Column.of(Measure.PV01),
        Column.of(Measure.ACCRUED_INTEREST));

    // the complete set of rules for calculating measures
    CalculationRules rules = CalculationRules.builder()
        .pricingRules(OpenGammaPricingRules.standard())
        .marketDataRules(ExampleMarketData.rules())
        .reportingRules(ReportingRules.fixedCurrency(Currency.USD))
        .build();

    // Use an empty snapshot of market data, indicating only the valuation date.
    // The engine will attempt to source the data for us, which the example engine is
    // configured to load from JSON resources. We could alternatively populate the snapshot
    // with some or all of the required market data here.
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    BaseMarketData baseMarketData = BaseMarketData.empty(valuationDate);

    // create the engine and calculate the results
    CalculationEngine engine = ExampleEngine.create();
    Results results = engine.calculate(trades, columns, rules, baseMarketData);

    // produce an ASCII table of the results
    ResultsFormatter.print(results, columns);
  }

  //-----------------------------------------------------------------------  
  // create a vanilla fixed vs libor 3m swap
  private static Trade createVanillaFixedVsLibor3mSwap() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 100_000_000);

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.of(0.015, DayCounts.THIRTY_U_360))
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_3M))
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "Fixed vs Libor 3m"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 9, 12))
            .build())
        .build();
  }

  // create a libor 3m vs libor 6m basis swap with spread
  private static Trade createBasisLibor3mVsLibor6mWithSpreadSwap() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 100_000_000);

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 8, 27))
            .endDate(LocalDate.of(2024, 8, 27))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_6M))
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 8, 27))
            .endDate(LocalDate.of(2024, 8, 27))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(IborIndices.USD_LIBOR_3M)
            .spread(ValueSchedule.of(0.001))
            .build())
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "Libor 3m + spread vs Libor 6m"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 9, 12))
            .build())
        .build();
  }

  // Create an overnight averaged vs libor 3m swap with spread
  private static Trade createOvernightAveragedWithSpreadVsLibor3mSwap() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 100_000_000);

    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2020, 9, 12))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_3M))
        .build();

    RateCalculationSwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2020, 9, 12))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(OvernightRateCalculation.builder()
            .dayCount(DayCounts.ACT_360)
            .index(OvernightIndices.USD_FED_FUND)
            .accrualMethod(OvernightAccrualMethod.AVERAGED)
            .spread(ValueSchedule.of(0.0025))
            .build())
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "Fed Funds averaged + spread vs Libor 3m"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 9, 12))
            .build())
        .build();
  }

  // Create a vanilla fixed vs libor 3m swap with fixing
  private static Trade createFixedVsLibor3mWithFixingSwap() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 100_000_000);

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2013, 9, 12))
            .endDate(LocalDate.of(2020, 9, 12))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.of(0.015, DayCounts.THIRTY_U_360))
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2013, 9, 12))
            .endDate(LocalDate.of(2020, 9, 12))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_3M))
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "Fixed vs libor 3m (with fixing)"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2013, 9, 12))
            .build())
        .build();
  }

  // Create a fixed vs overnight swap with fixing
  private static Trade createFixedVsOvernightWithFixingSwap() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 100_000_000);

    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 17))
            .endDate(LocalDate.of(2014, 3, 17))
            .frequency(Frequency.TERM)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.TERM)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.of(0.00123, DayCounts.ACT_360))
        .build();

    RateCalculationSwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 17))
            .endDate(LocalDate.of(2014, 3, 17))
            .frequency(Frequency.TERM)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.TERM)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(OvernightRateCalculation.builder()
            .dayCount(DayCounts.ACT_360)
            .index(OvernightIndices.USD_FED_FUND)
            .build())
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "Fixed vs ON (with fixing)"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 1, 17))
            .build())
        .build();
  }

  // Create a fixed vs libor 3m swap
  private static Trade createStub3mFixedVsLibor3mSwap() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 100_000_000);

    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 6, 12))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_3M))
        .build();

    RateCalculationSwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 6, 12))
            .stubConvention(StubConvention.SHORT_INITIAL)
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.of(0.01, DayCounts.THIRTY_U_360))
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "Fixed vs Libor 3m (3m short initial stub)"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 9, 12))
            .build())
        .build();
  }

  // Create a fixed vs libor 3m swap
  private static Trade createStub1mFixedVsLibor3mSwap() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 100_000_000);

    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 7, 12))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_3M))
        .build();

    RateCalculationSwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 7, 12))
            .stubConvention(StubConvention.SHORT_INITIAL)
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.of(0.01, DayCounts.THIRTY_U_360))
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "Fixed vs Libor 3m (1m short initial stub)"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 9, 12))
            .build())
        .build();
  }

  // Create a fixed vs libor 6m swap
  private static Trade createInterpolatedStub3mFixedVsLibor6mSwap() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 100_000_000);

    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 6, 12))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(IborIndices.USD_LIBOR_6M)
            .initialStub(StubCalculation.ofIborInterpolatedRate(IborIndices.USD_LIBOR_3M, IborIndices.USD_LIBOR_6M))
            .build())
        .build();

    RateCalculationSwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 6, 12))
            .stubConvention(StubConvention.SHORT_INITIAL)
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.of(0.01, DayCounts.THIRTY_U_360))
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "Fixed vs Libor 6m (interpolated 3m short initial stub)"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 9, 12))
            .build())
        .build();
  }

  // Create a fixed vs libor 6m swap
  private static Trade createInterpolatedStub4mFixedVsLibor6mSwap() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 100_000_000);

    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 7, 12))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(IborIndices.USD_LIBOR_6M)
            .initialStub(StubCalculation.ofIborInterpolatedRate(IborIndices.USD_LIBOR_3M, IborIndices.USD_LIBOR_6M))
            .build())
        .build();

    RateCalculationSwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 7, 12))
            .stubConvention(StubConvention.SHORT_INITIAL)
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.of(0.01, DayCounts.THIRTY_U_360))
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "Fixed vs Libor 6m (interpolated 4m short initial stub)"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 9, 12))
            .build())
        .build();
  }

  // Create a zero-coupon fixed vs libor 3m swap
  private static Trade createZeroCouponFixedVsLibor3mSwap() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 100_000_000);

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(Frequency.P12M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.TERM)
            .paymentDateOffset(DaysAdjustment.NONE)
            .compoundingMethod(CompoundingMethod.STRAIGHT)
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.of(0.015, DayCounts.THIRTY_U_360))
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.TERM)
            .paymentDateOffset(DaysAdjustment.NONE)
            .compoundingMethod(CompoundingMethod.STRAIGHT)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_3M))
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "Zero-coupon fixed vs libor 3m"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 9, 12))
            .build())
        .build();
  }

  // Create a compounding fixed vs fed funds swap
  private static Trade createCompoundingFixedVsFedFundsSwap() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 100_000_000);

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 2, 5))
            .endDate(LocalDate.of(2014, 4, 7))
            .frequency(Frequency.TERM)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.TERM)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.of(0.00123, DayCounts.ACT_360))
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 2, 5))
            .endDate(LocalDate.of(2014, 4, 7))
            .frequency(Frequency.TERM)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.TERM)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(OvernightRateCalculation.of(OvernightIndices.USD_FED_FUND))
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "Compounding fixed vs fed funds"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 2, 5))
            .build())
        .build();
  }

  // Create a compounding fed funds vs libor 3m swap
  private static Trade createCompoundingFedFundsVsLibor3mSwap() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 100_000_000);

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2020, 9, 12))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_3M))
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2020, 9, 12))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(OvernightRateCalculation.builder()
            .index(OvernightIndices.USD_FED_FUND)
            .accrualMethod(OvernightAccrualMethod.AVERAGED)
            .build())
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "Compounding fed funds vs libor 3m"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 9, 12))
            .build())
        .build();
  }

  // Create a compounding libor 6m vs libor 3m swap
  private static Trade createCompoundingLibor6mVsLibor3mSwap() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 100_000_000);

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 8, 27))
            .endDate(LocalDate.of(2024, 8, 27))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_6M))
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 8, 27))
            .endDate(LocalDate.of(2024, 8, 27))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .compoundingMethod(CompoundingMethod.STRAIGHT)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_3M))
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "Compounding libor 6m vs libor 3m"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 8, 27))
            .build())
        .build();
  }

  // create a cross-currency GBP libor 3m vs USD libor 3m swap with spread
  private static Trade createXCcyGbpLibor3mVsUsdLibor3mSwap() {
    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 24))
            .endDate(LocalDate.of(2021, 1, 24))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(Currency.GBP, 61_600_000))
        .calculation(IborRateCalculation.of(IborIndices.GBP_LIBOR_3M))
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 24))
            .endDate(LocalDate.of(2021, 1, 24))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(Currency.USD, 100_000_000))
        .calculation(IborRateCalculation.builder().index(IborIndices.USD_LIBOR_3M).spread(ValueSchedule.of(0.0091)).build())
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "GBP Libor 3m vs USD Libor 3m"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 1, 24))
            .build())
        .build();
  }

  // create a cross-currency USD fixed vs GBP libor 3m swap
  private static SwapTrade createXCcyUsdFixedVsGbpLibor3mSwap() {
    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 24))
            .endDate(LocalDate.of(2021, 1, 24))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(Currency.USD, 100_000_000))
        .calculation(FixedRateCalculation.of(0.03, DayCounts.THIRTY_U_360))
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 24))
            .endDate(LocalDate.of(2021, 1, 24))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(Currency.GBP, 61_600_000))
        .calculation(IborRateCalculation.of(IborIndices.GBP_LIBOR_3M))
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "USD fixed vs GBP Libor 3m"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 1, 24))
            .build())
        .build();
  }

  // create a cross-currency USD fixed vs GBP libor 3m swap with initial and final notional exchange
  private static SwapTrade createNotionalExchangeSwap() {
    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 24))
            .endDate(LocalDate.of(2021, 1, 24))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .currency(Currency.USD)
            .amount(ValueSchedule.of(100_000_000))
            .initialExchange(true)
            .finalExchange(true)
            .build())
        .calculation(FixedRateCalculation.of(0.03, DayCounts.THIRTY_U_360))
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 1, 24))
            .endDate(LocalDate.of(2021, 1, 24))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.GBLO))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .currency(Currency.GBP)
            .amount(ValueSchedule.of(61_600_000))
            .initialExchange(true)
            .finalExchange(true)
            .build())
        .calculation(IborRateCalculation.of(IborIndices.GBP_LIBOR_3M))
        .build();

    return SwapTrade.builder()
        .standardId(StandardId.of("swap", "USD fixed vs GBP Libor 3m (notional exchange)"))
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 1, 24))
            .build())
        .build();
  }

}
