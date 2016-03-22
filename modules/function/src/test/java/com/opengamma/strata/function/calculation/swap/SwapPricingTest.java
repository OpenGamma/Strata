/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.function.StandardComponents.marketDataFactory;
import static org.assertj.core.api.Assertions.offset;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.ImmutableIborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.ImmutableReferenceData;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.config.MarketDataRules;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.ReportingCurrency;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.DefaultPricingRules;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.config.pricing.PricingRule;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.calc.runner.CalculationTaskRunner;
import com.opengamma.strata.calc.runner.CalculationTasks;
import com.opengamma.strata.calc.runner.Results;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.marketdata.mapping.MarketDataMappingsBuilder;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.pricer.datasets.StandardDataSets;
import com.opengamma.strata.pricer.swap.e2e.CalendarUSD;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;

@Test
public class SwapPricingTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard()
      .combinedWith(ImmutableReferenceData.of(CalendarUSD.NYC, CalendarUSD.NYC_CALENDAR));
  private static final IborIndex USD_LIBOR_1M = lockIndexCalendar(IborIndices.USD_LIBOR_1M);
  private static final IborIndex USD_LIBOR_3M = lockIndexCalendar(IborIndices.USD_LIBOR_3M);
  private static final IborIndex USD_LIBOR_6M = lockIndexCalendar(IborIndices.USD_LIBOR_6M);
  private static final NotionalSchedule NOTIONAL = NotionalSchedule.of(USD, 100_000_000);
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(
      BusinessDayConventions.MODIFIED_FOLLOWING,
      CalendarUSD.NYC);
  private static final BusinessDayAdjustment BDA_P = BusinessDayAdjustment.of(
      BusinessDayConventions.PRECEDING,
      CalendarUSD.NYC);

  private static final LocalDate VAL_DATE = StandardDataSets.VAL_DATE_2014_01_22;
  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("CurveGroup");
  private static final CurveGroup CURVE_GROUP = curveGroup();

  // tolerance
  private static final double TOLERANCE_PV = 1.0E-4;

  //-------------------------------------------------------------------------
  public void presentValueVanillaFixedVsLibor1mSwap() {
    SwapLeg payLeg = fixedLeg(
        LocalDate.of(2014, 9, 12), LocalDate.of(2016, 9, 12), Frequency.P6M, PayReceive.PAY, NOTIONAL, 0.0125, null);

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 9, 12))
            .frequency(Frequency.P1M)
            .businessDayAdjustment(BDA_MF)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P1M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NOTIONAL)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_1M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
            .build())
        .build();

    SwapTrade trade = SwapTrade.builder()
        .info(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
        .product(Swap.of(payLeg, receiveLeg)).build();

    MarketEnvironment suppliedData = MarketEnvironment.builder(VAL_DATE)
        .addValue(CurveGroupId.of(CURVE_GROUP_NAME), CURVE_GROUP)
        .build();

    FunctionGroup<SwapTrade> functionGroup = DefaultFunctionGroup.builder(SwapTrade.class)
        .addFunction(Measures.PRESENT_VALUE, SwapCalculationFunction.class)
        .name("FunctionGroup")
        .build();

    PricingRule<SwapTrade> pricingRule = PricingRule.builder(SwapTrade.class)
        .addMeasures(Measures.PRESENT_VALUE)
        .functionGroup(functionGroup)
        .build();

    DefaultPricingRules pricingRules = DefaultPricingRules.of(pricingRule);

    MarketDataMappings marketDataMappings = MarketDataMappingsBuilder.create()
        .curveGroup(CURVE_GROUP_NAME)
        .build();

    MarketDataRules marketDataRules = MarketDataRules.ofTargetTypes(marketDataMappings, SwapTrade.class);

    // create the calculation runner
    List<SwapTrade> trades = ImmutableList.of(trade);
    List<Column> columns = ImmutableList.of(Column.of(Measures.PRESENT_VALUE));
    CalculationRules rules = CalculationRules.of(pricingRules, marketDataRules, ReportingCurrency.of(USD));

    // calculate results using the runner
    CalculationTasks tasks = CalculationTasks.of(rules, trades, columns);
    MarketDataRequirements reqs = tasks.getRequirements(REF_DATA);
    MarketEnvironment enhancedMarketData = marketDataFactory()
        .buildMarketData(reqs, MarketDataConfig.empty(), suppliedData, REF_DATA);
    // using the direct executor means there is no need to close/shutdown the runner
    CalculationTaskRunner runner = CalculationTaskRunner.of(MoreExecutors.newDirectExecutorService());
    Results results = runner.calculateSingleScenario(tasks, enhancedMarketData, REF_DATA);

    Result<?> result = results.get(0, 0);
    assertThat(result).isSuccess();

    CurrencyAmount pv = (CurrencyAmount) result.getValue();
    assertThat(pv.getAmount()).isCloseTo(-1003684.8402, offset(TOLERANCE_PV));
  }

  private static SwapLeg fixedLeg(
      LocalDate start, LocalDate end, Frequency frequency,
      PayReceive payReceive, NotionalSchedule notional, double fixedRate, StubConvention stubConvention) {

    return RateCalculationSwapLeg.builder()
        .payReceive(payReceive)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(start)
            .endDate(end)
            .frequency(frequency)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(stubConvention)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(frequency)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.of(fixedRate, THIRTY_U_360))
        .build();
  }

  private static CurveGroup curveGroup() {
    Map<Currency, Curve> discountCurves = ImmutableMap.of(
        USD, StandardDataSets.GROUP1_USD_DSC);
    Map<Index, Curve> forwardCurves = ImmutableMap.of(
        USD_FED_FUND, StandardDataSets.GROUP1_USD_ON,
        USD_LIBOR_1M, StandardDataSets.GROUP1_USD_L1M,
        USD_LIBOR_3M, StandardDataSets.GROUP1_USD_L3M,
        USD_LIBOR_6M, StandardDataSets.GROUP1_USD_L6M);

    return CurveGroup.of(CURVE_GROUP_NAME, discountCurves, forwardCurves);
  }

  //-------------------------------------------------------------------------
  // use a fixed known set of holiday dates to ensure tests produce same numbers
  private static IborIndex lockIndexCalendar(IborIndex index) {
    return ((ImmutableIborIndex) index).toBuilder()
        .fixingCalendar(CalendarUSD.NYC)
        .effectiveDateOffset(
            index.getEffectiveDateOffset().toBuilder()
                .calendar(CalendarUSD.NYC)
                .adjustment(
                    index.getEffectiveDateOffset().getAdjustment().toBuilder()
                        .calendar(CalendarUSD.NYC)
                        .build())
                .build())
        .maturityDateOffset(
            index.getMaturityDateOffset().toBuilder()
                .adjustment(
                    index.getMaturityDateOffset().getAdjustment().toBuilder()
                        .calendar(CalendarUSD.NYC)
                        .build())
                .build())
        .build();
  }
}
