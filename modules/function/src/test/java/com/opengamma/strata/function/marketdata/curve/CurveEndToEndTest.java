/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.function.StandardComponents.marketDataFactory;
import static com.opengamma.strata.function.marketdata.curve.CurveTestUtils.fixedIborSwapNode;
import static com.opengamma.strata.function.marketdata.curve.CurveTestUtils.fraNode;
import static com.opengamma.strata.function.marketdata.curve.CurveTestUtils.id;
import static org.assertj.core.api.Assertions.offset;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.ImmutableMarketData;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.config.MarketDataRules;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.ReportingCurrency;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.DefaultPricingRules;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.config.pricing.PricingRule;
import com.opengamma.strata.calc.config.pricing.PricingRules;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.marketdata.MarketDataFactory;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.runner.CalculationTaskRunner;
import com.opengamma.strata.calc.runner.CalculationTasks;
import com.opengamma.strata.calc.runner.Results;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.FunctionUtils;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.calculation.swap.SwapCalculationFunction;
import com.opengamma.strata.function.marketdata.mapping.MarketDataMappingsBuilder;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.FraCurveNode;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.MarketDataKeys;
import com.opengamma.strata.pricer.fra.DiscountingFraProductPricer;
import com.opengamma.strata.pricer.rate.MarketDataRatesProvider;
import com.opengamma.strata.product.fra.ExpandedFra;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test curves.
 */
@Test
public class CurveEndToEndTest {

  /** The maximum allowable PV when round-tripping an instrument used to calibrate a curve. */
  private static final double PV_TOLERANCE = 5e-10;

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
    MarketData quotes = ImmutableMarketData.builder(valuationDate).addValuesById(parRateData).build();
    Trade fra3x6Trade = fra3x6Node.trade(valuationDate, quotes);
    Trade fra6x9Trade = fra6x9Node.trade(valuationDate, quotes);
    Trade swap1yTrade = swap1yNode.trade(valuationDate, quotes);
    Trade swap2yTrade = swap2yNode.trade(valuationDate, quotes);
    Trade swap3yTrade = swap3yNode.trade(valuationDate, quotes);

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

    MarketDataRules marketDataRules = MarketDataRules.anyTarget(
        MarketDataMappingsBuilder.create()
            .curveGroup(groupName)
            .build());

    CalculationRules calculationRules = CalculationRules.builder()
        .pricingRules(pricingRules())
        .marketDataRules(marketDataRules)
        .reportingCurrency(ReportingCurrency.of(Currency.USD))
        .build();

    // Calculate the results and check the PVs for the node instruments are zero ----------------------

    List<Column> columns = ImmutableList.of(Column.of(Measures.PRESENT_VALUE));
    MarketEnvironment knownMarketData = MarketEnvironment.builder()
        .valuationDate(date(2011, 3, 8))
        .addValues(parRateData)
        .build();

    // using the direct executor means there is no need to close/shutdown the runner
    CalculationTasks tasks = CalculationTasks.of(calculationRules, trades, columns);
    MarketDataRequirements reqs = tasks.getRequirements();
    MarketEnvironment enhancedMarketData = marketDataFactory().buildMarketData(reqs, knownMarketData, marketDataConfig);
    CalculationTaskRunner runner = CalculationTaskRunner.of(MoreExecutors.newDirectExecutorService());
    Results results = runner.calculateSingleScenario(tasks, enhancedMarketData);

    results.getItems().stream().forEach(this::checkPvIsZero);
  }

  private void checkPvIsZero(Result<?> result) {
    assertThat(result).isSuccess();
    assertThat(((CurrencyAmount) result.getValue()).getAmount()).isEqualTo(0, offset(PV_TOLERANCE));
  }

  //-----------------------------------------------------------------------------------------------------------

  private static PricingRules pricingRules() {
    FunctionGroup<SwapTrade> swapGroup = DefaultFunctionGroup.builder(SwapTrade.class)
        .name("Swap")
        .addFunction(Measures.PRESENT_VALUE, SwapCalculationFunction.class)
        .build();

    FunctionGroup<FraTrade> fraGroup = DefaultFunctionGroup.builder(FraTrade.class)
        .name("Fra")
        .addFunction(Measures.PRESENT_VALUE, TestFraPresentValueFunction.class)
        .build();

    return DefaultPricingRules.of(
        PricingRule.builder(FraTrade.class).functionGroup(fraGroup).build(),
        PricingRule.builder(SwapTrade.class).functionGroup(swapGroup).build());
  }

  /**
   * PV function for a FRA. There is an equivalent in the function-beta module. This should be replaced with that
   * function once it is promoted to the function module.
   */
  public static final class TestFraPresentValueFunction
      implements CalculationFunction<FraTrade> {

    @Override
    public Set<Measure> supportedMeasures() {
      return ImmutableSet.of(Measures.PRESENT_VALUE);
    }

    @Override
    public Currency naturalCurrency(FraTrade trade) {
      return trade.getProduct().getCurrency();
    }

    @Override
    public FunctionRequirements requirements(FraTrade trade, Set<Measure> measures) {
      Fra fra = trade.getProduct();

      Set<Index> indices = new HashSet<>();
      indices.add(fra.getIndex());
      fra.getIndexInterpolated().ifPresent(indices::add);

      Set<ObservableKey> indexRateKeys =
          indices.stream()
              .map(IndexRateKey::of)
              .collect(toImmutableSet());

      Set<MarketDataKey<?>> indexCurveKeys =
          indices.stream()
              .map(MarketDataKeys::indexCurve)
              .collect(toImmutableSet());

      Set<DiscountCurveKey> discountCurveKeys =
          ImmutableSet.of(DiscountCurveKey.of(fra.getCurrency()));

      return FunctionRequirements.builder()
          .singleValueRequirements(Sets.union(indexCurveKeys, discountCurveKeys))
          .timeSeriesRequirements(indexRateKeys)
          .outputCurrencies(fra.getCurrency())
          .build();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        FraTrade trade,
        Set<Measure> measures,
        CalculationMarketData marketData) {

      ExpandedFra product = trade.getProduct().expand();
      CurrencyValuesArray pv = marketData.scenarios()
          .map(MarketDataRatesProvider::of)
          .map(provider -> DiscountingFraProductPricer.DEFAULT.presentValue(product, provider))
          .collect(FunctionUtils.toCurrencyValuesArray());
      return ImmutableMap.of(Measures.PRESENT_VALUE, Result.success(pv));
    }
  }

}
