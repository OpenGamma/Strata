/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.measure.StandardComponents.marketDataFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.examples.marketdata.ExampleData;
import com.opengamma.strata.loader.csv.FixingSeriesCsvLoader;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.measure.calc.TradeCounterpartyCalculationParameter;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeAttributeType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Example to illustrate using the engine to price a swap.
 * <p>
 * This makes use of the example engine and the example market data environment.
 */
public class SwapPricingCcpExample {

  /**
   * The valuation date.
   */
  private static final LocalDate VAL_DATE = LocalDate.of(2015, 7, 21);
  /**
   * The curve group name.
   */
  private static final CurveGroupName CURVE_GROUP_NAME_CCP1 = CurveGroupName.of("USD-DSCON-LIBOR3M");
  private static final CurveGroupName CURVE_GROUP_NAME_CCP2 = CurveGroupName.of("USD-DSCON-LIBOR3M-CCP2");
  /**
   * The location of the data files.
   */
  private static final String PATH_CONFIG = "src/main/resources/";
  /**
   * The location of the curve calibration groups file for CCP1 and CCP2.
   */
  private static final ResourceLocator GROUPS_RESOURCE_CCP1 =
      ResourceLocator.ofFile(new File(PATH_CONFIG + "example-calibration/curves/groups.csv"));
  private static final ResourceLocator GROUPS_RESOURCE_CCP2 =
      ResourceLocator.ofFile(new File(PATH_CONFIG + "example-calibration/curves/groups-ccp2.csv"));
  /**
   * The location of the curve calibration settings file for CCP1 and CCP2.
   */
  private static final ResourceLocator SETTINGS_RESOURCE_CCP1 =
      ResourceLocator.ofFile(new File(PATH_CONFIG + "example-calibration/curves/settings.csv"));
  private static final ResourceLocator SETTINGS_RESOURCE_CCP2 =
      ResourceLocator.ofFile(new File(PATH_CONFIG + "example-calibration/curves/settings-ccp2.csv"));
  /**
   * The location of the curve calibration nodes file for CCP1 and CCP2.
   */
  private static final ResourceLocator CALIBRATION_RESOURCE_CCP1 =
      ResourceLocator.ofFile(new File(PATH_CONFIG + "example-calibration/curves/calibrations.csv"));
  private static final ResourceLocator CALIBRATION_RESOURCE_CCP2 =
      ResourceLocator.ofFile(new File(PATH_CONFIG + "example-calibration/curves/calibrations-ccp2.csv"));
  /**
   * The location of the market quotes file for CCP1 and CCP2.
   */
  private static final ResourceLocator QUOTES_RESOURCE_CCP1 =
      ResourceLocator.ofFile(new File(PATH_CONFIG + "example-calibration/quotes/quotes.csv"));
  private static final ResourceLocator QUOTES_RESOURCE_CCP2 =
      ResourceLocator.ofFile(new File(PATH_CONFIG + "example-calibration/quotes/quotes-ccp2.csv"));
  /**
   * The location of the historical fixing file.
   */
  private static final ResourceLocator FIXINGS_RESOURCE =
      ResourceLocator.ofFile(new File(PATH_CONFIG + "example-marketdata/historical-fixings/usd-libor-3m.csv"));

  /**
   * The first counterparty.
   */
  private static final StandardId CCP1_ID = StandardId.of("example", "CCP-1");
  /**
   * The second counterparty.
   */
  private static final StandardId CCP2_ID = StandardId.of("example", "CCP-2");

  /**
   * Runs the example, pricing the instruments, producing the output as an ASCII table.
   * 
   * @param args  ignored
   */
  public static void main(String[] args) {
    // setup calculation runner component, which needs life-cycle management
    // a typical application might use dependency injection to obtain the instance
    try (CalculationRunner runner = CalculationRunner.ofMultiThreaded()) {
      calculate(runner);
    }
  }

  // obtains the data and calculates the grid of results
  private static void calculate(CalculationRunner runner) {
    // the trades that will have measures calculated
    List<Trade> trades = createSwapTrades();

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.PRESENT_VALUE),
        Column.of(Measures.PAR_RATE),
        Column.of(Measures.PV01_MARKET_QUOTE_BUCKETED),
        Column.of(Measures.PV01_CALIBRATED_BUCKETED));

    // load quotes
    ImmutableMap<QuoteId, Double> quotesCcp1 = QuotesCsvLoader.load(VAL_DATE, QUOTES_RESOURCE_CCP1);
    ImmutableMap<QuoteId, Double> quotesCcp2 = QuotesCsvLoader.load(VAL_DATE, QUOTES_RESOURCE_CCP2);

    // load fixings
    ImmutableMap<ObservableId, LocalDateDoubleTimeSeries> fixings = FixingSeriesCsvLoader.load(FIXINGS_RESOURCE);

    // create the market data
    MarketData marketData = ImmutableMarketData.builder(VAL_DATE)
        .addValueMap(quotesCcp1)
        .addValueMap(quotesCcp2)
        .addTimeSeriesMap(fixings)
        .build();

    // the reference data, such as holidays and securities
    ReferenceData refData = ReferenceData.standard();

    // load the curve definition
    Map<CurveGroupName, CurveGroupDefinition> defnsCcp1 =
        RatesCalibrationCsvLoader.load(GROUPS_RESOURCE_CCP1, SETTINGS_RESOURCE_CCP1, CALIBRATION_RESOURCE_CCP1);
    Map<CurveGroupName, CurveGroupDefinition> defnsCcp2 =
        RatesCalibrationCsvLoader.load(GROUPS_RESOURCE_CCP2, SETTINGS_RESOURCE_CCP2, CALIBRATION_RESOURCE_CCP2);
    CurveGroupDefinition curveGroupDefinitionCcp1 = defnsCcp1.get(CURVE_GROUP_NAME_CCP1).filtered(VAL_DATE, refData);
    CurveGroupDefinition curveGroupDefinitionCcp2 = defnsCcp2.get(CURVE_GROUP_NAME_CCP2).filtered(VAL_DATE, refData);

    // the configuration that defines how to create the curves when a curve group is requested
    MarketDataConfig marketDataConfig = MarketDataConfig.builder()
        .add(CURVE_GROUP_NAME_CCP1, curveGroupDefinitionCcp1)
        .add(CURVE_GROUP_NAME_CCP2, curveGroupDefinitionCcp2)
        .build();

    // the complete set of rules for calculating measures
    CalculationFunctions functions = StandardComponents.calculationFunctions();
    RatesMarketDataLookup ratesLookupCcp1 = RatesMarketDataLookup.of(curveGroupDefinitionCcp1);
    RatesMarketDataLookup ratesLookupCcp2 = RatesMarketDataLookup.of(curveGroupDefinitionCcp2);
    // choose RatesMarketDataLookup instance based on counterparty
    TradeCounterpartyCalculationParameter perCounterparty = TradeCounterpartyCalculationParameter.of(
        ImmutableMap.of(CCP1_ID, ratesLookupCcp1, CCP2_ID, ratesLookupCcp2), ratesLookupCcp1);
    CalculationRules rules = CalculationRules.of(functions, perCounterparty);

    // calibrate the curves and calculate the results
    MarketDataRequirements reqs = MarketDataRequirements.of(rules, trades, columns, refData);
    MarketData calibratedMarketData = marketDataFactory().create(reqs, marketDataConfig, marketData, refData);
    Results results = runner.calculate(rules, trades, columns, calibratedMarketData, refData);

    // use the report runner to transform the engine results into a trade report
    ReportCalculationResults calculationResults =
        ReportCalculationResults.of(VAL_DATE, trades, columns, results, functions, refData);
    TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("swap-report-template2");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
    tradeReport.writeAsciiTable(System.out);
  }

  //-----------------------------------------------------------------------  
  // create swap trades
  private static List<Trade> createSwapTrades() {
    return ImmutableList.of(createVanillaFixedVsLibor3mSwap(CCP1_ID), createVanillaFixedVsLibor3mSwap(CCP2_ID));
  }

  //-----------------------------------------------------------------------  
  // create a vanilla fixed vs libor 3m swap
  private static Trade createVanillaFixedVsLibor3mSwap(StandardId ctptyId) {
    TradeInfo tradeInfo = TradeInfo.builder()
        .id(StandardId.of("example", "1"))
        .addAttribute(TradeAttributeType.DESCRIPTION, "Fixed vs Libor 3m")
        .counterparty(ctptyId)
        .settlementDate(LocalDate.of(2014, 9, 12))
        .build();
    return FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M.toTrade(
        tradeInfo,
        LocalDate.of(2014, 9, 12), // the start date
        LocalDate.of(2021, 9, 12), // the end date
        BuySell.BUY,               // indicates whether this trade is a buy or sell
        100_000_000,               // the notional amount  
        0.015);                    // the fixed interest rate
  }

}
