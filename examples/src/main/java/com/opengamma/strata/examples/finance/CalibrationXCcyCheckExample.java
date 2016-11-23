/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.measure.StandardComponents.marketDataFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.loader.csv.FxRatesCsvLoader;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.node.IborFixingDepositCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.product.Trade;

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
  private static final LocalDate VAL_DATE = LocalDate.of(2015, 11, 2);
  /**
   * The tolerance to use.
   */
  private static final double TOLERANCE_PV = 1.0E-8;
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
      ResourceLocator.ofFile(new File(PATH_CONFIG + "curves/groups-xccy.csv"));
  /**
   * The location of the curve calibration settings file.
   */
  private static final ResourceLocator SETTINGS_RESOURCE =
      ResourceLocator.ofFile(new File(PATH_CONFIG + "curves/settings-xccy.csv"));
  /**
   * The location of the curve calibration nodes file.
   */
  private static final ResourceLocator CALIBRATION_RESOURCE =
      ResourceLocator.ofFile(new File(PATH_CONFIG + "curves/calibrations-xccy.csv"));
  /**
   * The location of the market quotes file.
   */
  private static final ResourceLocator QUOTES_RESOURCE =
      ResourceLocator.ofFile(new File(PATH_CONFIG + "quotes/quotes-xccy.csv"));
  /**
   * The location of the FX rates file.
   */
  private static final ResourceLocator FX_RATES_RESOURCE =
      ResourceLocator.ofFile(new File(PATH_CONFIG + "quotes/fx-rates-xccy.csv"));

  //-------------------------------------------------------------------------
  /**
   * Runs the calibration and checks that all the trades used in the curve calibration have a PV of 0.
   * 
   * @param args  -p to run the performance estimate
   */
  public static void main(String[] args) {

    System.out.println("Starting curve calibration: configuration and data loaded from files");
    Pair<List<Trade>, Results> results = calculate();
    System.out.println("Computed PV for all instruments used in the calibration set");

    // check that all trades have a PV of near 0
    for (int i = 0; i < results.getFirst().size(); i++) {
      Trade trade = results.getFirst().get(i);
      Result<?> pv = results.getSecond().getCells().get(i);
      String output = "  |--> PV for " + trade.getClass().getSimpleName() + " computed: " + pv.isSuccess();
      Object pvValue = pv.getValue();
      ArgChecker.isTrue(pvValue instanceof CurrencyAmount, "result type");
      CurrencyAmount ca = (CurrencyAmount) pvValue;
      output += " with value: " + ca;
      System.out.println(output);
      ArgChecker.isTrue(Math.abs(ca.getAmount()) < TOLERANCE_PV, "PV should be small");
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
  private static void performance_calibration_pricing() {
    int nbTests = 10;
    int nbRep = 3;
    int count = 0;

    for (int i = 0; i < nbRep; i++) {
      long startTime = System.currentTimeMillis();
      for (int looprep = 0; looprep < nbTests; looprep++) {
        Results r = calculate().getSecond();
        count += r.getColumnCount() + r.getRowCount();
      }
      long endTime = System.currentTimeMillis();
      System.out.println("Performance: " + nbTests + " config load + curve calibrations + pv check (1 thread) in " +
          (endTime - startTime) + " ms");
      // Previous run: 400 ms for 10 cycles
    }
    if (count == 0) {
      System.out.println("Avoiding hotspot: " + count);
    }
  }

  //-------------------------------------------------------------------------
  // setup calculation runner component, which needs life-cycle management
  // a typical application might use dependency injection to obtain the instance
  private static Pair<List<Trade>, Results> calculate() {
    try (CalculationRunner runner = CalculationRunner.ofMultiThreaded()) {
      return calculate(runner);
    }
  }

  // calculates the PV results for the instruments used in calibration from the config
  private static Pair<List<Trade>, Results> calculate(CalculationRunner runner) {
    // the reference data, such as holidays and securities
    ReferenceData refData = ReferenceData.standard();

    // load quotes and FX rates
    Map<QuoteId, Double> quotes = QuotesCsvLoader.load(VAL_DATE, QUOTES_RESOURCE);
    Map<FxRateId, FxRate> fxRates = FxRatesCsvLoader.load(VAL_DATE, FX_RATES_RESOURCE);

    // create the market data
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE)
        .addValueMap(quotes)
        .addValueMap(fxRates)
        .build();

    // load the curve definition
    Map<CurveGroupName, CurveGroupDefinition> defns =
        RatesCalibrationCsvLoader.load(GROUPS_RESOURCE, SETTINGS_RESOURCE, CALIBRATION_RESOURCE);
    CurveGroupDefinition curveGroupDefinition = defns.get(CURVE_GROUP_NAME).filtered(VAL_DATE, refData);

    // extract the trades used for calibration
    List<Trade> trades = curveGroupDefinition.getCurveDefinitions().stream()
        .flatMap(defn -> defn.getNodes().stream())
        // IborFixingDeposit is not a real trade, so there is no appropriate comparison
        .filter(node -> !(node instanceof IborFixingDepositCurveNode))
        .map(node -> node.trade(1d, marketData, refData))
        .collect(toImmutableList());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.PRESENT_VALUE));

    // the configuration that defines how to create the curves when a curve group is requested
    MarketDataConfig marketDataConfig = MarketDataConfig.builder()
        .add(CURVE_GROUP_NAME, curveGroupDefinition)
        .build();

    // the complete set of rules for calculating measures
    CalculationFunctions functions = StandardComponents.calculationFunctions();
    RatesMarketDataLookup ratesLookup = RatesMarketDataLookup.of(curveGroupDefinition);
    CalculationRules rules = CalculationRules.of(functions, ratesLookup);

    // calibrate the curves and calculate the results
    MarketDataRequirements reqs = MarketDataRequirements.of(rules, trades, columns, refData);
    MarketData calibratedMarketData = marketDataFactory().create(reqs, marketDataConfig, marketData, refData);
    Results results = runner.calculate(rules, trades, columns, calibratedMarketData, refData);
    return Pair.of(trades, results);
  }

}
