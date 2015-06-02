/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.engine.calculations.function.FunctionUtils.toCurrencyAmountList;
import static org.assertj.core.api.Assertions.offset;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.jooq.lambda.Seq;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.CalculationRules;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.DefaultCalculationEngine;
import com.opengamma.strata.engine.calculations.DefaultCalculationRunner;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.calculations.function.result.CurrencyAmountList;
import com.opengamma.strata.engine.config.MarketDataRule;
import com.opengamma.strata.engine.config.MarketDataRules;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.engine.config.pricing.DefaultPricingRules;
import com.opengamma.strata.engine.config.pricing.FunctionGroup;
import com.opengamma.strata.engine.config.pricing.PricingRule;
import com.opengamma.strata.engine.config.pricing.PricingRules;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.engine.marketdata.DefaultMarketDataFactory;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.ObservableMarketDataFunction;
import com.opengamma.strata.engine.marketdata.functions.TimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.rate.fra.ExpandedFra;
import com.opengamma.strata.finance.rate.fra.Fra;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.function.marketdata.mapping.FxRateMapping;
import com.opengamma.strata.function.marketdata.mapping.MarketDataMappingsBuilder;
import com.opengamma.strata.function.rate.swap.SwapPvFunction;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.config.CurveGroupConfig;
import com.opengamma.strata.market.curve.config.CurveNode;
import com.opengamma.strata.market.curve.config.InterpolatedCurveConfig;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.MarketDataKeys;
import com.opengamma.strata.pricer.rate.fra.DiscountingFraProductPricer;

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
    InterpolatedCurveConfig curveConfig = CurveTestUtils.fraSwapCurveConfig();
    List<CurveNode> nodes = curveConfig.getNodes();

    Map<ObservableId, Double> parRateData = ImmutableMap.<ObservableId, Double>builder()
        .put(CurveTestUtils.id(nodes.get(0)), 0.0037)
        .put(CurveTestUtils.id(nodes.get(1)), 0.0054)
        .put(CurveTestUtils.id(nodes.get(2)), 0.005)
        .put(CurveTestUtils.id(nodes.get(3)), 0.0087)
        .put(CurveTestUtils.id(nodes.get(4)), 0.012)
        .build();

    LocalDate valuationDate = date(2011, 3, 8);

    // Build the trades from the node instruments
    Map<ObservableKey, Double> quotesMap = Seq.seq(parRateData).toMap(tp -> tp.v1.toObservableKey(), tp -> tp.v2);

    List<Trade> trades = nodes.stream()
        .map(node -> node.trade(valuationDate, quotesMap))
        .collect(toImmutableList());

    CurveGroupName groupName = CurveGroupName.of("Curve Group");

    CurveGroupConfig groupConfig = CurveGroupConfig.builder()
        .name(groupName)
        .addCurve(curveConfig, Currency.USD, IborIndices.USD_LIBOR_3M)
        .build();

    MarketDataConfig marketDataConfig = MarketDataConfig.builder().add(groupName, groupConfig).build();

    // Rules for market data and calculations ---------------------------------

    MarketDataRules marketDataRules = MarketDataRules.of(
        MarketDataRule.anyTarget(
            MarketDataMappingsBuilder.create()
                .curveGroup(groupName)
                .mapping(FxRateMapping.INSTANCE)
                .build()));

    CalculationRules calculationRules = CalculationRules.builder()
        .pricingRules(pricingRules())
        .marketDataRules(marketDataRules)
        .marketDataConfig(marketDataConfig)
        .reportingRules(ReportingRules.fixedCurrency(Currency.USD))
        .build();

    // Market data functions --------------------------------------------------

    ParRatesMarketDataFunction parRatesFunction = new ParRatesMarketDataFunction();
    CurveGroupMarketDataFunction curveGroupFunction = new CurveGroupMarketDataFunction(RootFinderConfig.defaults());
    DiscountingCurveMarketDataFunction discountCurveFunction = new DiscountingCurveMarketDataFunction();
    RateIndexCurveMarketDataFunction forwardCurveFunction = new RateIndexCurveMarketDataFunction();
    ZeroRateDiscountFactorsMarketDataFunction discountFactorsFunction = new ZeroRateDiscountFactorsMarketDataFunction();

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
        discountFactorsFunction);

    CalculationEngine engine = new DefaultCalculationEngine(calculationRunner, factory, LinkResolver.none());

    // Calculate the results and check the PVs for the node instruments are zero ----------------------

    BaseMarketData marketData = BaseMarketData.empty(valuationDate);
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

    private final Map<ObservableId, Double> marketData;

    private MapObservableMarketDataFunction(Map<ObservableId, Double> marketData) {
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
      implements CalculationSingleFunction<FraTrade, CurrencyAmountList> {

    @Override
    public CurrencyAmountList execute(FraTrade trade, CalculationMarketData marketData) {
      ExpandedFra product = trade.getProduct().expand();
      return IntStream.range(0, marketData.getScenarioCount())
          .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
          .map(MarketDataRatesProvider::new)
          .map(provider -> DiscountingFraProductPricer.DEFAULT.presentValue(product, provider))
          .collect(toCurrencyAmountList());
    }

    @Override
    public CalculationRequirements requirements(FraTrade trade) {
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

      return CalculationRequirements.builder()
          .singleValueRequirements(Sets.union(indexCurveKeys, discountCurveKeys))
          .timeSeriesRequirements(indexRateKeys)
          .outputCurrencies(fra.getCurrency())
          .build();
    }
  }
}
