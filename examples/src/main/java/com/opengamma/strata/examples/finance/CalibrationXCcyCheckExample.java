/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.SimpleMarketDataKey;
import com.opengamma.strata.calc.CalculationEngine;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.DefaultCalculationEngine;
import com.opengamma.strata.calc.config.MarketDataRule;
import com.opengamma.strata.calc.config.MarketDataRules;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.marketdata.DefaultMarketDataFactory;
import com.opengamma.strata.calc.marketdata.MarketDataFactory;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.ObservableMarketDataFunction;
import com.opengamma.strata.calc.marketdata.function.TimeSeriesProvider;
import com.opengamma.strata.calc.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.calc.runner.CalculationRunner;
import com.opengamma.strata.calc.runner.DefaultCalculationRunner;
import com.opengamma.strata.calc.runner.Results;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.function.StandardComponents;
import com.opengamma.strata.function.marketdata.fx.FxRateConfig;
import com.opengamma.strata.function.marketdata.mapping.MarketDataMappingsBuilder;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupEntry;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.node.IborFixingDepositCurveNode;
import com.opengamma.strata.market.id.QuoteId;
import com.opengamma.strata.market.key.QuoteKey;

/**
 * Test for multi-currency curve calibration with 4 curves (2 in USD and 2 in EUR). 
 * <p>
 * The curves are
 *  - Discounting and Fed Fund forward in USD
 *  - USD Libor 3M forward. 
 *  - Discounting and EONIA forward in EUR
 *  - EUR Euribor 3M forward. 
 * <p>
 * Curve configuration and market data loaded from csv files.
 * Tests that the trades used for calibration have a total converted PV of 0.
 */
public class CalibrationXCcyCheckExample {

  /**
   * The valuation date.
   */
  private static final LocalDate VALUATION_DATE = LocalDate.of(2015, 11, 2);
  /**
   * The tolerance to use.
   */
  private static final double TOLERANCE_PV = 1.0E-8;
  /**
   * The number of threads to use.
   */
  private static final int NB_THREADS = 1;
  /**
   * The curve group name.
   */
  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("USD-EUR-XCCY");

  /**
   * The location of the data files.
   */
  private static final String PATH_CONFIG = "src/main/resources/example-calibration/";
  /**
   * The location of the curve calibration groups file.
   */
  private static final ResourceLocator GROUPS_RESOURCE =
      ResourceLocator.of(ResourceLocator.FILE_URL_PREFIX + PATH_CONFIG + "curves/groups-xccy.csv");
  /**
   * The location of the curve calibration settings file.
   */
  private static final ResourceLocator SETTINGS_RESOURCE =
      ResourceLocator.of(ResourceLocator.FILE_URL_PREFIX + PATH_CONFIG + "curves/settings-xccy.csv");
  /**
   * The location of the curve calibration nodes file.
   */
  private static final ResourceLocator CALIBRATION_RESOURCE =
      ResourceLocator.of(ResourceLocator.FILE_URL_PREFIX + PATH_CONFIG + "curves/calibrations-xccy.csv");
  /**
   * The location of the market quotes file.
   */
  private static final ResourceLocator QUOTES_RESOURCE =
      ResourceLocator.of(ResourceLocator.FILE_URL_PREFIX + PATH_CONFIG + "quotes/quotes-xccy.csv");

  //-------------------------------------------------------------------------
  /** 
   * Runs the calibration and checks that all the trades used in the curve calibration have a PV of 0.
   * 
   * @param args  -p to run the performance estimate
   */
  public static void main(String[] args) {

    System.out.println("Starting curve calibration: configuration and data loaded from files");
    Pair<List<Trade>, Results> results = getResults();
    System.out.println("Computed PV for all instruments used in the calibration set");

    // check that all trades have a PV of near 0
    for (int i = 0; i < results.getFirst().size(); i++) {
      Trade trade = results.getFirst().get(i);
      Result<?> pv = results.getSecond().getItems().get(i);
      String output = "  |--> PV for " + trade.getClass().getSimpleName() + " computed: " + pv.isSuccess();
      Object pvValue = pv.getValue();
      ArgChecker.isTrue((pvValue instanceof MultiCurrencyAmount) || (pvValue instanceof CurrencyAmount), "result type");
      if (pvValue instanceof CurrencyAmount) {
        CurrencyAmount ca = (CurrencyAmount) pvValue;
        ArgChecker.isTrue(Math.abs(ca.getAmount()) < TOLERANCE_PV, "PV should be small");
        output = output + " with value: " + ca;
      } else {
        MultiCurrencyAmount pvMCA = (MultiCurrencyAmount) pvValue;
        double pvConverted = pvMCA.convertedTo(USD, null).getAmount();
        ArgChecker.isTrue(Math.abs(pvConverted) < TOLERANCE_PV, "PV should be small");
        output = output + " with values: " + pvMCA;
      }
      System.out.println(output);
    }

    // optionally test performance
    if (args.length > 0) {
      if (args[0].equals("-p")) {
        performance_calibration_pricing();
      }
    }
    System.out.println("Checked PV for all instruments used in the calibration set are near to zero");
  }

  // Example of performance: loading data from file, calibration and PV
  public static void performance_calibration_pricing() {
    int nbTests = 10;
    int nbRep = 3;
    int count = 0;

    for (int i = 0; i < nbRep; i++) {
      long startTime = System.currentTimeMillis();
      for (int looprep = 0; looprep < nbTests; looprep++) {
        Results r = getResults().getSecond();
        count += r.getColumnCount() + r.getRowCount();
      }
      long endTime = System.currentTimeMillis();
      System.out.println("Performance: " + nbTests + " config load + curve calibrations + pv check (1 thread) in "
          + (endTime - startTime) + " ms");
      // Previous run: 400 ms for 10 cycles
    }
    if (count == 0) {
      System.out.println("Avoiding hotspot: " + count);
    }
  }

  //-------------------------------------------------------------------------
  // Compute the PV results for the instruments used in calibration from the config
  private static Pair<List<Trade>, Results> getResults() {
    // load quotes
    Map<QuoteId, Double> quotes = QuotesCsvLoader.load(VALUATION_DATE, QUOTES_RESOURCE);

    // load the curve definition
    ImmutableMap<CurveGroupName, CurveGroupDefinition> defns =
        RatesCalibrationCsvLoader.load(GROUPS_RESOURCE, SETTINGS_RESOURCE, CALIBRATION_RESOURCE);
    CurveGroupDefinition curveGroupDefinition = defns.get(CURVE_GROUP_NAME);

    FxRateConfig fxRateConfig =
        FxRateConfig.of(ImmutableMap.of(CurrencyPair.of(EUR, USD), QuoteKey.of(StandardId.of("OG-Ticker", "EUR-USD"))));

    // the configuration that defines how to create the curves when a curve group is requested
    MarketDataConfig marketDataConfig = MarketDataConfig.builder()
        .add(CURVE_GROUP_NAME, curveGroupDefinition)
        .addDefault(fxRateConfig)
        .build();

    // the configuration defining the curve group to use when finding a curve
    MarketDataFactory marketDataFactory = createMarketDataFactory();
    MarketDataRequirements requirements = MarketDataRequirements.builder()
        .addValues(FxRateId.of(EUR, USD))
        .addValues(quotes.keySet())
        .build();
    // create the market data environment and populate with known data
    MarketEnvironment suppliedData = MarketEnvironment.builder().valuationDate(VALUATION_DATE).addValues(quotes).build();
    // build the remaining data using the market data factory
    MarketEnvironment marketEnvironment =
        marketDataFactory.buildMarketData(requirements, suppliedData, marketDataConfig, false).getMarketEnvironment();
    MarketData marketData = new MarketEnvironmentData(marketEnvironment);

    // extract the trades used for calibration
    List<Trade> trades = new ArrayList<>();
    ImmutableList<CurveGroupEntry> curveGroups = curveGroupDefinition.getEntries();
    for (CurveGroupEntry entry : curveGroups) {
      ImmutableList<CurveNode> nodes = entry.getCurveDefinition().getNodes();
      for (CurveNode node : nodes) {
        if (!(node instanceof IborFixingDepositCurveNode)) {
          // IborFixingDeposit is not a real trade, so there is no appropriate comparison
          trades.add(node.trade(VALUATION_DATE, marketData));
        }
      }
    }

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measure.PRESENT_VALUE));

    MarketDataMappings marketDataMappings = MarketDataMappingsBuilder.create().curveGroup(CURVE_GROUP_NAME).build();
    MarketDataRules marketDataRules = MarketDataRules.of(MarketDataRule.anyTarget(marketDataMappings));

    // the complete set of rules for calculating measures
    CalculationRules rules = CalculationRules.builder()
        .pricingRules(StandardComponents.pricingRules())
        .marketDataConfig(marketDataConfig)
        .marketDataRules(marketDataRules)
        .build();

    // create the engine and calculate the results
    CalculationEngine engine = createEngine(marketDataFactory);
    return Pair.of(trades, engine.calculate(trades, columns, rules, marketEnvironment));
  }

  //-------------------------------------------------------------------------
  // Create the calculation engine
  private static CalculationEngine createEngine(MarketDataFactory marketDataFactory) {
    // create the calculation runner that calculates the results
    ExecutorService executor = createExecutor();
    CalculationRunner calcRunner = new DefaultCalculationRunner(executor);

    // combine the runner and market data factory
    return new DefaultCalculationEngine(calcRunner, marketDataFactory, LinkResolver.none());
  }

  private static MarketDataFactory createMarketDataFactory() {
    // create the market data factory that builds market data
    return new DefaultMarketDataFactory(
        TimeSeriesProvider.empty(),
        ObservableMarketDataFunction.none(),
        FeedIdMapping.identity(),
        StandardComponents.marketDataFunctions());
  }

  // create an executor with daemon threads
  private static ExecutorService createExecutor() {
    return Executors.newFixedThreadPool(NB_THREADS, r -> {
      Thread t = Executors.defaultThreadFactory().newThread(r);
      t.setDaemon(true);
      return t;
    });
  }

  /**
   * {@link MarketData} implementation backed by a {@link MarketEnvironment}.
   * <p>
   * The market data is taken from the first scenario in the market environment.
   * <p>
   * This class only supports requests for keys that implement {@link SimpleMarketDataKey} as these keys do
   * not require any market data mappings. That is sufficient in this case because the market data is known
   * to only contain quotes and FX rates.
   */
  private static class MarketEnvironmentData implements MarketData {

    private final MarketEnvironment marketEnvironment;

    MarketEnvironmentData(MarketEnvironment marketEnvironment) {
      this.marketEnvironment = marketEnvironment;
    }

    @Override
    public boolean containsValue(MarketDataKey<?> key) {
      MarketDataId<?> id = MarketDataMappings.empty().getIdForKey(key);
      return marketEnvironment.containsValue(id);
    }

    @Override
    public boolean containsTimeSeries(ObservableKey key) {
      ObservableId id = MarketDataMappings.empty().getIdForObservableKey(key);
      return marketEnvironment.containsTimeSeries(id);
    }

    @Override
    public <T> T getValue(MarketDataKey<T> key) {
      MarketDataId<T> id = MarketDataMappings.empty().getIdForKey(key);
      return marketEnvironment.getValue(id).getValue(0);
    }

    @Override
    public LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key) {
      ObservableId id = MarketDataMappings.empty().getIdForObservableKey(key);
      return marketEnvironment.getTimeSeries(id);
    }
  }
}
