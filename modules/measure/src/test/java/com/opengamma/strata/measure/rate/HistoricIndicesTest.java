/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.rate;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_2M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.ReportingCurrency;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.IborRateStubCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test historic indices.
 */
@Test
public class HistoricIndicesTest {

  @SuppressWarnings("deprecation")
  public void testHistoricIndex() {
    
    // trade with interpolated front stub using 2M EURIBOR
    PeriodicSchedule schedule = PeriodicSchedule.builder()
        .startDate(LocalDate.of(2014, 9, 12))
        .endDate(LocalDate.of(2016, 7, 12))
        .frequency(P3M)
        .businessDayAdjustment(BusinessDayAdjustment.NONE)
        .stubConvention(StubConvention.SHORT_INITIAL)
        .build();

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(schedule)
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(EUR, 10_000_000))
        .calculation(IborRateCalculation.builder()
            .index(EUR_EURIBOR_3M)
            .initialStub(
                IborRateStubCalculation.ofIborInterpolatedRate(EUR_EURIBOR_2M, EUR_EURIBOR_3M))
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, HolidayCalendarIds.EUTA, BusinessDayAdjustment.NONE))
            .build())
        .build();

    SwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(schedule)
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(EUR, 10_000_000))
        .calculation(FixedRateCalculation.of(0.0123d, DayCounts.ACT_365F))
        .build();

    SwapTrade trade = SwapTrade.of(TradeInfo.empty(), Swap.of(payLeg, recLeg));

    // construct market data; 3M EURIBOR forward curve and fixings for both 2M and 3M EURIBOR
    CurveGroupName curveGroupName = CurveGroupName.of("ALL");
    CurveName curveName = CurveName.of(EUR_EURIBOR_3M.getName());
    CurveId curveId = CurveId.of(curveGroupName, curveName);
    DefaultCurveMetadata metadata = DefaultCurveMetadata.builder()
        .curveName(CurveName.of(EUR_EURIBOR_3M.getName()))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.DISCOUNT_FACTOR)
        .addInfo(CurveInfoType.DAY_COUNT, DayCounts.ACT_360)
        .build();

    ConstantNodalCurve curve = ConstantNodalCurve.of(metadata, 1, 1);

    IndexQuoteId euribor2m = IndexQuoteId.of(EUR_EURIBOR_2M);
    LocalDateDoubleTimeSeries euribor2mFixings = LocalDateDoubleTimeSeries.of(LocalDate.of(2014, 9, 10), 0.018);
    
    IndexQuoteId euribor3m = IndexQuoteId.of(EUR_EURIBOR_3M);
    LocalDateDoubleTimeSeries euribor3mFixings = LocalDateDoubleTimeSeries.builder()
        .put(LocalDate.of(2014, 9, 10), 0.019)
        .put(LocalDate.of(2014, 10, 9), 0.02)
        .build();
    
    Map<MarketDataId<?>, Object> curves = ImmutableMap.of(curveId, curve);
    ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> timeSeries =
        ImmutableMap.of(euribor2m, euribor2mFixings, euribor3m, euribor3mFixings);

    // valuation date during initial stub period; 2M EURIBOR fixing should be available so no forward curve required
    LocalDate valuationDate = LocalDate.of(2014, 10, 12);
    MarketData marketData = MarketData.of(valuationDate, curves, timeSeries);

    RatesCurveGroupDefinition definition = RatesCurveGroupDefinition.builder()
        .name(curveGroupName)
        .addCurve(curveName, EUR, EUR_EURIBOR_3M)
        .build();

    RatesMarketDataLookup lookup = RatesMarketDataLookup.of(definition);
    CalculationParameters params = CalculationParameters.of(lookup);
    CalculationFunctions calcFunctions = StandardComponents.calculationFunctions();
    CalculationRules rules = CalculationRules.of(calcFunctions, ReportingCurrency.NATURAL, params);

    // perform the calculation
    List<Column> columns = ImmutableList.of(Column.of(Measures.PRESENT_VALUE));
    Results results;
    try (CalculationRunner runner = CalculationRunner.ofMultiThreaded()) {
      results = runner.calculate(rules, ImmutableList.of(trade), columns, marketData, ReferenceData.standard());
    }
    
    // assert result is succes
    assertTrue(results.get(0, 0).isSuccess());
  }

}
