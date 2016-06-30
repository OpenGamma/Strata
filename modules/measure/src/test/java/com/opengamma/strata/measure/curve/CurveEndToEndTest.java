/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.curve;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.measure.StandardComponents.marketDataFactory;
import static com.opengamma.strata.measure.curve.CurveTestUtils.fixedIborSwapNode;
import static com.opengamma.strata.measure.curve.CurveTestUtils.fraNode;
import static com.opengamma.strata.measure.curve.CurveTestUtils.id;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataFactory;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.calc.runner.CalculationTaskRunner;
import com.opengamma.strata.calc.runner.CalculationTasks;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.FraCurveNode;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.fra.FraTradeCalculationFunction;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.swap.SwapTradeCalculationFunction;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test curves.
 */
@Test
public class CurveEndToEndTest {

  /** The maximum allowable PV when round-tripping an instrument used to calibrate a curve. */
  private static final double PV_TOLERANCE = 5e-10;
  /** The reference data. */
  private static final ReferenceData REF_DATA = ReferenceData.standard();

  /**
   * End-to-end test for curve calibration and round-tripping that uses the {@link MarketDataFactory}
   * to calibrate a curve and calculate PVs for the instruments at the curve nodes.
   *
   * This tests the full pipeline of market data functions:
   *   - Par rates
   *   - Curve group (including calibration)
   *   - Individual curves
   *   - Discount factors
   */
  public void roundTripFraAndFixedFloatSwap() {

    // Configuration and market data for the curve ---------------------------------

    String fra3x6 = "fra3x6";
    String fra6x9 = "fra6x9";
    String swap1y = "swap1y";
    String swap2y = "swap2y";
    String swap3y = "swap3y";

    FraCurveNode fra3x6Node = fraNode(3, fra3x6);
    FraCurveNode fra6x9Node = fraNode(6, fra6x9);
    FixedIborSwapCurveNode swap1yNode = fixedIborSwapNode(Tenor.TENOR_1Y, swap1y);
    FixedIborSwapCurveNode swap2yNode = fixedIborSwapNode(Tenor.TENOR_2Y, swap2y);
    FixedIborSwapCurveNode swap3yNode = fixedIborSwapNode(Tenor.TENOR_3Y, swap3y);

    Map<ObservableId, Double> parRateData = ImmutableMap.<ObservableId, Double>builder()
        .put(id(fra3x6), 0.0037)
        .put(id(fra6x9), 0.0054)
        .put(id(swap1y), 0.005)
        .put(id(swap2y), 0.0087)
        .put(id(swap3y), 0.012)
        .build();

    LocalDate valuationDate = date(2011, 3, 8);

    // Build the trades from the node instruments
    MarketData quotes = ImmutableMarketData.of(valuationDate, parRateData);
    Trade fra3x6Trade = fra3x6Node.trade(1d, quotes, REF_DATA);
    Trade fra6x9Trade = fra6x9Node.trade(1d, quotes, REF_DATA);
    Trade swap1yTrade = swap1yNode.trade(1d, quotes, REF_DATA);
    Trade swap2yTrade = swap2yNode.trade(1d, quotes, REF_DATA);
    Trade swap3yTrade = swap3yNode.trade(1d, quotes, REF_DATA);

    List<Trade> trades = ImmutableList.of(fra3x6Trade, fra6x9Trade, swap1yTrade, swap2yTrade, swap3yTrade);

    List<CurveNode> nodes = ImmutableList.of(fra3x6Node, fra6x9Node, swap1yNode, swap2yNode, swap3yNode);
    CurveGroupName groupName = CurveGroupName.of("Curve Group");
    CurveName curveName = CurveName.of("FRA and Fixed-Float Swap Curve");

    InterpolatedNodalCurveDefinition curveDefn = InterpolatedNodalCurveDefinition.builder()
        .name(curveName)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(DayCounts.ACT_ACT_ISDA)
        .nodes(nodes)
        .interpolator(CurveInterpolators.DOUBLE_QUADRATIC)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();

    CurveGroupDefinition groupDefn = CurveGroupDefinition.builder()
        .name(groupName)
        .addCurve(curveDefn, Currency.USD, IborIndices.USD_LIBOR_3M)
        .build();

    MarketDataConfig marketDataConfig = MarketDataConfig.builder().add(groupName, groupDefn).build();

    // Rules for market data and calculations ---------------------------------

    RatesMarketDataLookup ratesLookup = RatesMarketDataLookup.of(groupDefn);
    CalculationRules calculationRules = CalculationRules.of(functions(), Currency.USD, ratesLookup);

    // Calculate the results and check the PVs for the node instruments are zero ----------------------

    List<Column> columns = ImmutableList.of(Column.of(Measures.PRESENT_VALUE));
    MarketData knownMarketData = MarketData.of(date(2011, 3, 8), parRateData);

    // using the direct executor means there is no need to close/shutdown the runner
    CalculationTasks tasks = CalculationTasks.of(calculationRules, trades, columns);
    MarketDataRequirements reqs = tasks.requirements(REF_DATA);
    MarketData enhancedMarketData = marketDataFactory().create(reqs, marketDataConfig, knownMarketData, REF_DATA);
    CalculationTaskRunner runner = CalculationTaskRunner.of(MoreExecutors.newDirectExecutorService());
    Results results = runner.calculate(tasks, enhancedMarketData, REF_DATA);

    results.getCells().stream().forEach(this::checkPvIsZero);
  }

  private void checkPvIsZero(Result<?> result) {
    assertThat(result).isSuccess();
    assertThat(((CurrencyAmount) result.getValue()).getAmount()).isEqualTo(0, offset(PV_TOLERANCE));
  }

  //-----------------------------------------------------------------------------------------------------------
  private static CalculationFunctions functions() {
    return CalculationFunctions.of(ImmutableMap.of(
        SwapTrade.class, new SwapTradeCalculationFunction(),
        FraTrade.class, new FraTradeCalculationFunction()));
  }

}
