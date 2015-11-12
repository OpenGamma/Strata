/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toFxConvertibleList;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.function.marketdata.curve.CurveTestUtils.fixedIborSwapNode;
import static com.opengamma.strata.function.marketdata.curve.CurveTestUtils.fraNode;
import static com.opengamma.strata.function.marketdata.curve.CurveTestUtils.id;
import static org.assertj.core.api.Assertions.offset;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

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
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.ObservableValues;
import com.opengamma.strata.calc.CalculationEngine;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.DefaultCalculationEngine;
import com.opengamma.strata.calc.config.MarketDataRule;
import com.opengamma.strata.calc.config.MarketDataRules;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.ReportingRules;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.DefaultPricingRules;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.config.pricing.PricingRule;
import com.opengamma.strata.calc.config.pricing.PricingRules;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.DefaultMarketDataFactory;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.ObservableMarketDataFunction;
import com.opengamma.strata.calc.marketdata.function.TimeSeriesProvider;
import com.opengamma.strata.calc.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.calc.runner.DefaultCalculationRunner;
import com.opengamma.strata.calc.runner.DefaultSingleCalculationMarketData;
import com.opengamma.strata.calc.runner.Results;
import com.opengamma.strata.calc.runner.function.CalculationSingleFunction;
import com.opengamma.strata.calc.runner.function.result.FxConvertibleList;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.function.calculation.rate.swap.SwapPvFunction;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.function.marketdata.mapping.MarketDataMappingsBuilder;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.definition.CurveGroupDefinition;
import com.opengamma.strata.market.curve.definition.CurveNode;
import com.opengamma.strata.market.curve.definition.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.definition.FraCurveNode;
import com.opengamma.strata.market.curve.definition.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.MarketDataKeys;
import com.opengamma.strata.market.value.ValueType;
import com.opengamma.strata.pricer.calibration.CalibrationMeasures;
import com.opengamma.strata.pricer.rate.fra.DiscountingFraProductPricer;
import com.opengamma.strata.product.rate.fra.ExpandedFra;
import com.opengamma.strata.product.rate.fra.Fra;
import com.opengamma.strata.product.rate.fra.FraTrade;
import com.opengamma.strata.product.rate.swap.SwapTrade;

/**
 * Test curves.
 */
@Test
public class CurveEndToEndTest {

  /** The maximum allowable PV when round-tripping an instrument used to calibrate a curve. */
  private static final double PV_TOLERANCE = 5e-10;

  /**
   * End-to-end test for curve calibration and round-tripping that uses the {@link CalculationEngine} to calibrate
   * a curve and calculate PVs for the instruments at the curve nodes.
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
    ObservableValues quotesMap = ObservableValues.ofIdMap(parRateData);
    Trade fra3x6Trade = fra3x6Node.trade(valuationDate, quotesMap);
    Trade fra6x9Trade = fra6x9Node.trade(valuationDate, quotesMap);
    Trade swap1yTrade = swap1yNode.trade(valuationDate, quotesMap);
    Trade swap2yTrade = swap2yNode.trade(valuationDate, quotesMap);
    Trade swap3yTrade = swap3yNode.trade(valuationDate, quotesMap);

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

    MarketDataRules marketDataRules = MarketDataRules.of(
        MarketDataRule.anyTarget(
            MarketDataMappingsBuilder.create()
                .curveGroup(groupName)
                .build()));

    CalculationRules calculationRules = CalculationRules.builder()
        .pricingRules(pricingRules())
        .marketDataRules(marketDataRules)
        .marketDataConfig(marketDataConfig)
        .reportingRules(ReportingRules.fixedCurrency(Currency.USD))
        .build();

    // Market data functions --------------------------------------------------

    ParRatesMarketDataFunction parRatesFunction = new ParRatesMarketDataFunction();
    CurveGroupMarketDataFunction curveGroupFunction = new CurveGroupMarketDataFunction(
        RootFinderConfig.defaults(), CalibrationMeasures.DEFAULT);
    DiscountCurveMarketDataFunction discountCurveFunction = new DiscountCurveMarketDataFunction();
    RateIndexCurveMarketDataFunction forwardCurveFunction = new RateIndexCurveMarketDataFunction();
    DiscountFactorsMarketDataFunction discountFactorsFunction = new DiscountFactorsMarketDataFunction();
    IborIndexRatesMarketDataFunction iborIndexRatesFunction = new IborIndexRatesMarketDataFunction();

    // Calculation engine ------------------------------------------------------

    DefaultCalculationRunner calculationRunner = new DefaultCalculationRunner(MoreExecutors.newDirectExecutorService());

    DefaultMarketDataFactory factory = new DefaultMarketDataFactory(
        EmptyTimeSeriesProvider.INSTANCE,
        new MapObservableMarketDataFunction(parRateData),
        FeedIdMapping.identity(),
        parRatesFunction,
        curveGroupFunction,
        discountCurveFunction,
        forwardCurveFunction,
        discountFactorsFunction,
        iborIndexRatesFunction);

    CalculationEngine engine = new DefaultCalculationEngine(calculationRunner, factory, LinkResolver.none());

    // Calculate the results and check the PVs for the node instruments are zero ----------------------

    MarketEnvironment marketData = MarketEnvironment.builder().valuationDate(date(2011, 3, 8)).build();
    List<Column> columns = ImmutableList.of(Column.of(Measure.PRESENT_VALUE));
    Results results = engine.calculate(trades, columns, calculationRules, marketData);
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
        .addFunction(Measure.PRESENT_VALUE, SwapPvFunction.class)
        .build();

    FunctionGroup<FraTrade> fraGroup = DefaultFunctionGroup.builder(FraTrade.class)
        .name("Fra")
        .addFunction(Measure.PRESENT_VALUE, TestFraPresentValueFunction.class)
        .build();

    return DefaultPricingRules.of(
        PricingRule.builder(FraTrade.class).functionGroup(fraGroup).build(),
        PricingRule.builder(SwapTrade.class).functionGroup(swapGroup).build());
  }

  /**
   * Time series provider that returns an empty time series for any ID.
   */
  private static final class EmptyTimeSeriesProvider implements TimeSeriesProvider {

    private static final TimeSeriesProvider INSTANCE = new EmptyTimeSeriesProvider();

    @Override
    public Result<LocalDateDoubleTimeSeries> timeSeries(ObservableId id) {
      return Result.success(LocalDateDoubleTimeSeries.empty());
    }
  }

  /**
   * Returns observable market data from a map.
   */
  private static final class MapObservableMarketDataFunction implements ObservableMarketDataFunction {

    private final Map<? extends ObservableId, Double> marketData;

    private MapObservableMarketDataFunction(Map<? extends ObservableId, Double> marketData) {
      this.marketData = marketData;
    }

    @Override
    public Map<ObservableId, Result<Double>> build(Set<? extends ObservableId> requirements) {
      return requirements.stream()
          .filter(marketData::containsKey)
          .collect(toImmutableMap(id -> id, (ObservableId id) -> Result.success(marketData.get(id))));
    }
  }

  /**
   * PV function for a FRA. There is an equivalent in the function-beta module. This should be replaced with that
   * function once it is promoted to the function module.
   */
  public static final class TestFraPresentValueFunction
      implements CalculationSingleFunction<FraTrade, FxConvertibleList> {

    @Override
    public FxConvertibleList execute(FraTrade trade, CalculationMarketData marketData) {
      ExpandedFra product = trade.getProduct().expand();
      return IntStream.range(0, marketData.getScenarioCount())
          .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
          .map(MarketDataRatesProvider::new)
          .map(provider -> DiscountingFraProductPricer.DEFAULT.presentValue(product, provider))
          .collect(toFxConvertibleList());
    }

    @Override
    public FunctionRequirements requirements(FraTrade trade) {
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

      Set<DiscountFactorsKey> discountCurveKeys =
          ImmutableSet.of(DiscountFactorsKey.of(fra.getCurrency()));

      return FunctionRequirements.builder()
          .singleValueRequirements(Sets.union(indexCurveKeys, discountCurveKeys))
          .timeSeriesRequirements(indexRateKeys)
          .outputCurrencies(fra.getCurrency())
          .build();
    }
  }
}
