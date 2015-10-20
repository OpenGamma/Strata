/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.market.ImmutableObservableValues;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.CalculationRules;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.DefaultCalculationEngine;
import com.opengamma.strata.engine.calculation.CalculationRunner;
import com.opengamma.strata.engine.calculation.DefaultCalculationRunner;
import com.opengamma.strata.engine.calculation.Results;
import com.opengamma.strata.engine.config.MarketDataRule;
import com.opengamma.strata.engine.config.MarketDataRules;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.marketdata.DefaultMarketDataFactory;
import com.opengamma.strata.engine.marketdata.MarketDataFactory;
import com.opengamma.strata.engine.marketdata.MarketEnvironment;
import com.opengamma.strata.engine.marketdata.MarketEnvironmentBuilder;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.function.ObservableMarketDataFunction;
import com.opengamma.strata.engine.marketdata.function.TimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.examples.marketdata.LoaderUtils;
import com.opengamma.strata.function.StandardComponents;
import com.opengamma.strata.function.marketdata.mapping.MarketDataMappingsBuilder;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.definition.CurveGroupDefinition;
import com.opengamma.strata.market.curve.definition.CurveGroupEntry;
import com.opengamma.strata.market.curve.definition.CurveNode;
import com.opengamma.strata.market.curve.definition.IborFixingDepositCurveNode;
import com.opengamma.strata.market.id.IndexRateId;

/**
 * Test for curve calibration with 2 curves in USD. 
 * <p>
 * One curve is used for Discounting and Fed Fund forward.
 * The other curve is used for Libor 3M forward. 
 * <p>
 * Curve configuration and market data loaded from XML files.
 * Tests that the trades used for calibration have a PV of 0.
 */
public class CalibrationCheckExample {

  private static final LocalDate VALUATION_DATE = LocalDate.of(2015, 7, 21);

  private static final LocalDateDoubleTimeSeries TS_EMTPY = LocalDateDoubleTimeSeries.empty();

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
  private static final String CURVE_GROUP_NAME = "USD-DSCON-LIBOR3M";

  /**
   * The location of the data files.
   */
  private static final String PATH_CONFIG = "src/main/resources/example-marketdata/quotes/";
  /**
   * The location of the curve definition.
   */
  private static final String CURVE_GROUP_CONFIG_FILE_NAME = PATH_CONFIG + "USD-DSCON-LIBOR3M.xml";
  /**
   * The location of the market quotes file.
   */
  private static final String MARKET_QUOTES_FILE_NAME = PATH_CONFIG + "market_quotes_usd.xml";

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
    ImmutableObservableValues marketQuotes =
        LoaderUtils.loadXmlBean(MARKET_QUOTES_FILE_NAME, ImmutableObservableValues.class);

    // create the market data builder and populate with known data
    MarketEnvironmentBuilder snapshotBuilder =
        MarketEnvironment.builder(VALUATION_DATE)
            .addTimeSeries(IndexRateId.of(USD_LIBOR_3M), TS_EMTPY)
            .addTimeSeries(IndexRateId.of(USD_FED_FUND), TS_EMTPY);
    for (ObservableKey k : marketQuotes.getValues().keySet()) {
      snapshotBuilder.addValue(k.toObservableId(MarketDataFeed.NONE), marketQuotes.getValue(k));
    }
    MarketEnvironment snapshot = snapshotBuilder.build();

    // load the curve definition
    CurveGroupDefinition curveGroupDefinition =
        LoaderUtils.loadXmlBean(CURVE_GROUP_CONFIG_FILE_NAME, CurveGroupDefinition.class);

    // extract the trades used for calibration
    List<Trade> trades = new ArrayList<>();
    ImmutableList<CurveGroupEntry> curveGroups = curveGroupDefinition.getEntries();
    for (CurveGroupEntry entry : curveGroups) {
      ImmutableList<CurveNode> nodes = entry.getCurveDefinition().getNodes();
      for (CurveNode node : nodes) {
        if (!(node instanceof IborFixingDepositCurveNode)) {
          // IborFixingDeposit is not a real trade, so there is no appropriate comparison
          trades.add(node.trade(VALUATION_DATE, marketQuotes));
        }
      }
    }

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measure.PRESENT_VALUE));

    // the configuration that defines how to create the curves when a curve group is requested
    MarketDataConfig marketDataConfig = MarketDataConfig.builder()
        .add(CurveGroupName.of(CURVE_GROUP_NAME), curveGroupDefinition)
        .build();

    // the configuration defining the curve group to use when finding a curve
    MarketDataRules marketDataRules = MarketDataRules.of(
        MarketDataRule.anyTarget(MarketDataMappingsBuilder.create()
            .curveGroup(CurveGroupName.of(CURVE_GROUP_NAME))
            .build()));

    // the complete set of rules for calculating measures
    CalculationRules rules = CalculationRules.builder()
        .pricingRules(StandardComponents.pricingRules())
        .marketDataConfig(marketDataConfig)
        .marketDataRules(marketDataRules)
        .build();

    // create the engine and calculate the results
    CalculationEngine engine = create();
    return Pair.of(trades, engine.calculate(trades, columns, rules, snapshot));
  }

  //-------------------------------------------------------------------------
  // Create the calculation engine
  private static CalculationEngine create() {
    // create the calculation runner that calculates the results
    ExecutorService executor = createExecutor();
    CalculationRunner calcRunner = new DefaultCalculationRunner(executor);

    // create the market data factory that builds market data
    MarketDataFactory marketDataFactory = new DefaultMarketDataFactory(
        TimeSeriesProvider.none(),
        ObservableMarketDataFunction.none(),
        FeedIdMapping.identity(),
        StandardComponents.marketDataFunctions());

    // combine the runner and market data factory
    return new DefaultCalculationEngine(calcRunner, marketDataFactory, LinkResolver.none());
  }

  // create an executor with daemon threads
  private static ExecutorService createExecutor() {
    ExecutorService executor = Executors.newFixedThreadPool(NB_THREADS, r -> {
      Thread t = Executors.defaultThreadFactory().newThread(r);
      t.setDaemon(true);
      return t;
    });
    return executor;
  }

}
