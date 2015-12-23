/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static java.util.stream.Collectors.toMap;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.ImmutableMarketData;
import com.opengamma.strata.calc.CalculationEngine;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.config.MarketDataRule;
import com.opengamma.strata.calc.config.MarketDataRules;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.runner.Results;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.function.StandardComponents;
import com.opengamma.strata.function.marketdata.mapping.MarketDataMappingsBuilder;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.node.IborFixingDepositCurveNode;
import com.opengamma.strata.market.id.CurveGroupId;
import com.opengamma.strata.market.id.QuoteId;
import com.opengamma.strata.pricer.calibration.CalibrationMeasures;
import com.opengamma.strata.pricer.calibration.CurveCalibrator;
import com.opengamma.strata.pricer.calibration.TradeCalibrationMeasure;
import com.opengamma.strata.pricer.impl.rate.model.HullWhiteOneFactorPiecewiseConstantParameters;
import com.opengamma.strata.pricer.index.HullWhiteIborFutureTradePricer;
import com.opengamma.strata.pricer.index.HullWhiteOneFactorPiecewiseConstantParametersProvider;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * Test for curve calibration with 2 curves in USD.
 * One curve is Discounting and Fed Fund forward and the other one is Libor 3M forward.
 */
@Test
public class CalibrationZeroRateUsd2OisFuturesHWIrsTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2015, 7, 21);

  /** Data locations */
  private static final String PATH_CONFIG = "src/main/resources/example-calibration/";
  private static final ResourceLocator GROUPS_RESOURCE =
      ResourceLocator.of(ResourceLocator.FILE_URL_PREFIX + PATH_CONFIG + "curves/groups-fut-usd.csv");
  private static final ResourceLocator SETTINGS_RESOURCE =
      ResourceLocator.of(ResourceLocator.FILE_URL_PREFIX + PATH_CONFIG + "curves/settings-fut-usd.csv");
  private static final ResourceLocator CALIBRATION_RESOURCE =
      ResourceLocator.of(ResourceLocator.FILE_URL_PREFIX + PATH_CONFIG + "curves/nodes-fut-usd.csv");
  private static final ResourceLocator QUOTES_RESOURCE =
      ResourceLocator.of(ResourceLocator.FILE_URL_PREFIX + PATH_CONFIG + "quotes/quotes-usd.csv");
  
  private static final Map<Index, LocalDateDoubleTimeSeries> TS = new HashMap<>();

  // load the curve definition
  private static final List<CurveGroupDefinition> DEFINITIONS =
      RatesCalibrationCsvLoader.load(GROUPS_RESOURCE, SETTINGS_RESOURCE, CALIBRATION_RESOURCE);
  private static final Map<CurveGroupName, CurveGroupDefinition> DEFINITIONS_MAP = 
      DEFINITIONS.stream().collect(toMap(def -> def.getName(), def -> def));
  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("USD-DSCON-LIBOR3M");
  private static final CurveGroupDefinition CURVE_GROUP_DEF = DEFINITIONS_MAP.get(CURVE_GROUP_NAME);
  // load quotes
  private static final ImmutableMap<QuoteId, Double> QUOTES_MAP = QuotesCsvLoader.load(VAL_DATE, QUOTES_RESOURCE);
  /** All quotes for the curve calibration */
  private static final ImmutableMarketData MARKET_DATA = 
      ImmutableMarketData.builder(VAL_DATE).addValuesById(QUOTES_MAP).build();
  /** The number of threads to use. */
  private static final int NB_THREADS = 1;

  private static final CalibrationMeasures CALIBRATION_MEASURES_DISC = CalibrationMeasures.DEFAULT;
  private static final CurveCalibrator CALIBRATOR_DISC = CurveCalibrator.of(1e-9, 1e-9, 100, CALIBRATION_MEASURES_DISC);
  

  // Hull-White model parameters
  private static final double MEAN_REVERSION = 0.01;
  private static final DoubleArray VOLATILITY = DoubleArray.of(0.01, 0.011, 0.012, 0.013, 0.014);
  private static final DoubleArray VOLATILITY_TIME = DoubleArray.of(0.5, 1.0, 2.0, 5.0);
  private static final HullWhiteOneFactorPiecewiseConstantParameters MODEL_PARAMETERS =
      HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
  private static final HullWhiteOneFactorPiecewiseConstantParametersProvider HW_PROVIDER =
      HullWhiteOneFactorPiecewiseConstantParametersProvider.of(MODEL_PARAMETERS, ACT_ACT_ISDA, VAL_DATE);

  public static final TradeCalibrationMeasure<IborFutureTrade> IBOR_FUTURE_PAR_SPREAD_HW =
      TradeCalibrationMeasure.of(
          "IborFutureParSpreadHullWhite",
          IborFutureTrade.class,
          (trade, p) -> HullWhiteIborFutureTradePricer.DEFAULT.parSpread(trade, p, HW_PROVIDER, 0.0),
          (trade, p) -> HullWhiteIborFutureTradePricer.DEFAULT.parSpreadSensitivity(trade, p, HW_PROVIDER));
  public static final CalibrationMeasures CALIBRATION_MEASURES_HW = CalibrationMeasures.of(
      TradeCalibrationMeasure.TERM_DEPOSIT_PAR_SPREAD,
      TradeCalibrationMeasure.IBOR_FIXING_DEPOSIT_PAR_SPREAD,
      TradeCalibrationMeasure.FRA_PAR_SPREAD,
      IBOR_FUTURE_PAR_SPREAD_HW,
      TradeCalibrationMeasure.SWAP_PAR_SPREAD,
      TradeCalibrationMeasure.FX_SWAP_PAR_SPREAD);
  private static final CurveCalibrator CALIBRATOR_HW = CurveCalibrator.of(1e-9, 1e-9, 100, CALIBRATION_MEASURES_HW);

  // Constants
  private static final double TOLERANCE_PV = 1.0E-6;


  //-------------------------------------------------------------------------
  public void calibration_present_value_dsc() {
    ImmutableRatesProvider multicurve=
        CALIBRATOR_DISC.calibrate(CURVE_GROUP_DEF, VAL_DATE, MARKET_DATA, TS);
    assertPresentValue(multicurve);
  }
  
  public void calibration_present_value_hw() {
    ImmutableRatesProvider multicurve=
        CALIBRATOR_HW.calibrate(CURVE_GROUP_DEF, VAL_DATE, MARKET_DATA, TS);
    assertPresentValue(multicurve);
  }

  private void assertPresentValue(ImmutableRatesProvider multicurve) {

    // extract the trades used for calibration
    List<Trade> trades = CURVE_GROUP_DEF.getCurveDefinitions().stream()
        .flatMap(defn -> defn.getNodes().stream())
        // IborFixingDeposit is not a real trade, so there is no appropriate comparison
        .filter(node -> !(node instanceof IborFixingDepositCurveNode))
        .map(node -> node.trade(VAL_DATE, ImmutableMarketData.builder(VAL_DATE).addValuesById(QUOTES_MAP).build()))
        .collect(toImmutableList());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measure.PRESENT_VALUE));

    // the configuration defining the curve group to use when finding a curve
    MarketDataRules marketDataRules = MarketDataRules.of(
        MarketDataRule.anyTarget(MarketDataMappingsBuilder.create()
            .curveGroup(CURVE_GROUP_NAME)
            .build()));

    // the complete set of rules for calculating measures
    CalculationRules rules = CalculationRules.builder()
        .pricingRules(StandardComponents.pricingRules())
        .marketDataRules(marketDataRules)
        .build();
    
    MarketEnvironment snapshot = MarketEnvironment.builder().valuationDate(VAL_DATE)
        .addValue(CurveGroupId.of(CURVE_GROUP_NAME),
            CurveGroup.of(CURVE_GROUP_NAME, multicurve.getDiscountCurves(), multicurve.getIndexCurves()))
        .addValues(QUOTES_MAP)
        .build();

    // create the engine and calculate the results
    CalculationEngine engine = CalibrationCheckUtils.create(NB_THREADS);
    Results results = engine.calculate(trades, columns, rules, snapshot);
    
    CalibrationCheckUtils.checkPv(trades, results, multicurve, TOLERANCE_PV);
    
  }

  //-------------------------------------------------------------------------
  @SuppressWarnings("unused")
  @Test(enabled = false)
  void performance() {
    long startTime, endTime;
    int nbTests = 100;
    int nbRep = 3;
    int count = 0;

    for (int i = 0; i < nbRep; i++) {
      startTime = System.currentTimeMillis();
      for (int looprep = 0; looprep < nbTests; looprep++) {
        ImmutableRatesProvider result =
            CALIBRATOR_DISC.calibrate(CURVE_GROUP_DEF, VAL_DATE, MARKET_DATA, TS);
        count += result.getDiscountCurves().size() + result.getIndexCurves().size();
      }
      endTime = System.currentTimeMillis();
      System.out.println("Performance: " + nbTests + " calibrations for 2 curves with 35 nodes in "
          + (endTime - startTime) + " ms.");
    }
    System.out.println("Avoiding hotspot: " + count);
    // Previous run: 1050 ms for 100 calibrations (2 curves simultaneous - 35 nodes)
  }

}
