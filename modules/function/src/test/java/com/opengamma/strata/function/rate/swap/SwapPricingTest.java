/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.swap;

import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.analytics.financial.interestrate.datasets.StandardDataSetsMulticurveUSD;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.ImmutableIborIndex;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.calculations.CalculationRunner;
import com.opengamma.strata.engine.calculations.CalculationTasks;
import com.opengamma.strata.engine.calculations.DefaultCalculationRunner;
import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.engine.config.CalculationTasksConfig;
import com.opengamma.strata.engine.config.MarketDataRule;
import com.opengamma.strata.engine.config.MarketDataRules;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.engine.config.pricing.DefaultPricingRules;
import com.opengamma.strata.engine.config.pricing.FunctionGroup;
import com.opengamma.strata.engine.config.pricing.PricingRule;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.BaseMarketDataResult;
import com.opengamma.strata.engine.marketdata.DefaultMarketDataFactory;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.ObservableMarketDataFunction;
import com.opengamma.strata.engine.marketdata.functions.TimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.rate.swap.FixedRateCalculation;
import com.opengamma.strata.finance.rate.swap.IborRateCalculation;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
import com.opengamma.strata.finance.rate.swap.PaymentSchedule;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.function.marketdata.curve.DiscountingCurveMarketDataFunction;
import com.opengamma.strata.function.marketdata.curve.RateIndexCurveMarketDataFunction;
import com.opengamma.strata.function.marketdata.mapping.MarketDataMappingsBuilder;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.pricer.rate.e2e.CalendarUSD;

@Test
public class SwapPricingTest {

  private static final IborIndex USD_LIBOR_1M = lockIndexCalendar(IborIndices.USD_LIBOR_1M);
  private static final NotionalSchedule NOTIONAL = NotionalSchedule.of(USD, 100_000_000);
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(
      BusinessDayConventions.MODIFIED_FOLLOWING,
      CalendarUSD.NYC);
  private static final BusinessDayAdjustment BDA_P = BusinessDayAdjustment.of(
      BusinessDayConventions.PRECEDING,
      CalendarUSD.NYC);

  // curve providers
  private static final Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> MULTICURVE_OIS_PAIR =
      StandardDataSetsMulticurveUSD.getCurvesUSDOisL1L3L6();
  private static final MulticurveProviderDiscount MULTICURVE_OIS = MULTICURVE_OIS_PAIR.getFirst();

  // tolerance
  private static final double TOLERANCE_PV = 1.0E-4;

  public void presentValueVanillaFixedVsLibor1mSwap() {
    RateCalculationSwapLeg payLeg = fixedLeg(
        LocalDate.of(2014, 9, 12), LocalDate.of(2016, 9, 12), Frequency.P6M, PayReceive.PAY, NOTIONAL, 0.0125, null);

    RateCalculationSwapLeg receiveLeg =
        RateCalculationSwapLeg.builder()
            .payReceive(RECEIVE)
            .accrualSchedule(
                PeriodicSchedule.builder()
                    .startDate(LocalDate.of(2014, 9, 12))
                    .endDate(LocalDate.of(2016, 9, 12))
                    .frequency(Frequency.P1M)
                    .businessDayAdjustment(BDA_MF)
                    .build())
            .paymentSchedule(
                PaymentSchedule.builder()
                    .paymentFrequency(Frequency.P1M)
                    .paymentDateOffset(DaysAdjustment.NONE)
                    .build())
            .notionalSchedule(NOTIONAL)
            .calculation(
                IborRateCalculation.builder()
                    .dayCount(DayCounts.ACT_360)
                    .index(USD_LIBOR_1M)
                    .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CalendarUSD.NYC, BDA_P))
                    .build())
            .build();

    SwapTrade trade = SwapTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeInfo(TradeInfo.builder().tradeDate(LocalDate.of(2014, 9, 10)).build())
        .product(Swap.of(payLeg, receiveLeg)).build();

    String curveGroupName = "The Curve Group";
    CurveGroup curveGroup = new CurveGroup(MULTICURVE_OIS, new CurveBuildingBlockBundle());

    BaseMarketData suppliedData =
        BaseMarketData.builder(LocalDate.of(2014, 1, 22))
            .addValue(CurveGroupId.of(curveGroupName), curveGroup)
            .build();

    FunctionGroup<SwapTrade> functionGroup =
        DefaultFunctionGroup.builder(SwapTrade.class)
            .addFunction(Measure.PRESENT_VALUE, SwapPvFunction.class)
            .name("FunctionGroup")
            .build();

    PricingRule<SwapTrade> pricingRule =
        PricingRule.builder(SwapTrade.class)
            .addMeasures(Measure.PRESENT_VALUE)
            .functionGroup(functionGroup)
            .build();

    DefaultPricingRules pricingRules = DefaultPricingRules.of(pricingRule);

    MarketDataMappings marketDataMappings =
        MarketDataMappingsBuilder.create()
            .curveGroup(curveGroupName)
            .build();

    MarketDataRules marketDataRules = MarketDataRules.of(MarketDataRule.of(marketDataMappings, SwapTrade.class));

    DefaultMarketDataFactory marketDataFactory =
        new DefaultMarketDataFactory(
            new EmptyTimeSeriesProvider(),
            ObservableMarketDataFunction.none(),
            FeedIdMapping.identity(),
            new DiscountingCurveMarketDataFunction(),
            new RateIndexCurveMarketDataFunction());

    List<SwapTrade> trades = ImmutableList.of(trade);
    Column pvColumn = Column.of(Measure.PRESENT_VALUE);
    List<Column> columns = ImmutableList.of(pvColumn);
    CalculationRunner calculationRunner = new DefaultCalculationRunner(Executors.newSingleThreadExecutor());
    ReportingRules reportingCurrency = ReportingRules.fixedCurrency(USD);
    CalculationTasksConfig calculationConfig =
        calculationRunner.createCalculationConfig(trades, columns, pricingRules, marketDataRules, reportingCurrency);
    CalculationTasks calculationTasks = calculationRunner.createCalculationTasks(calculationConfig);

    BaseMarketDataResult marketDataResult =
        marketDataFactory.buildBaseMarketData(
            calculationTasks.getMarketDataRequirements(),
            suppliedData,
            MarketDataConfig.empty());

    BaseMarketData marketData = marketDataResult.getMarketData();
    Results results = calculationRunner.calculate(calculationTasks, marketData);
    Result<?> result = results.get(0, 0);
    assertThat(result).isSuccess();

    List<?> scenarioResults = (List<?>) result.getValue();
    assertThat(scenarioResults.size()).isEqualTo(1);

    MultiCurrencyAmount pv = (MultiCurrencyAmount) scenarioResults.get(0);
    assertThat(pv.getAmount(USD).getAmount()).isCloseTo(-1003684.8402, offset(TOLERANCE_PV));
  }

  private static RateCalculationSwapLeg fixedLeg(
      LocalDate start, LocalDate end, Frequency frequency,
      PayReceive payReceive, NotionalSchedule notional, double fixedRate, StubConvention stubConvention) {

    return RateCalculationSwapLeg.builder()
        .payReceive(payReceive)
        .accrualSchedule(
            PeriodicSchedule.builder()
                .startDate(start)
                .endDate(end)
                .frequency(frequency)
                .businessDayAdjustment(BDA_MF)
                .stubConvention(stubConvention)
                .build())
        .paymentSchedule(
            PaymentSchedule.builder()
                .paymentFrequency(frequency)
                .paymentDateOffset(DaysAdjustment.NONE)
                .build())
        .notionalSchedule(notional)
        .calculation(
            FixedRateCalculation.builder()
                .dayCount(THIRTY_U_360)
                .rate(ValueSchedule.of(fixedRate))
                .build())
        .build();
  }

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

  private static class EmptyTimeSeriesProvider implements TimeSeriesProvider {

    @Override
    public Result<LocalDateDoubleTimeSeries> timeSeries(ObservableId id) {
      return Result.success(LocalDateDoubleTimeSeries.empty());
    }
  }

}
