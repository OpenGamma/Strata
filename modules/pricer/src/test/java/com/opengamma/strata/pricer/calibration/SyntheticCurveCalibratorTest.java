/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.ImmutableMarketData;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.NodalCurveDefinition;
import com.opengamma.strata.market.id.QuoteId;
import com.opengamma.strata.pricer.calibration.CalibrationMeasures;
import com.opengamma.strata.pricer.calibration.CurveCalibrator;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Tests {@link SyntheticCurveCalibrator}.
 */
@Test 
public class SyntheticCurveCalibratorTest {

  private static final LocalDate VALUATION_DATE = LocalDate.of(2015, 11, 20);

  // Configuration and data stored in csv to avoid long code description of the input data
  private static final String CONFIG_PATH = "src/test/resources/curve-config/";
  private static final String QUOTES_PATH = "src/test/resources/quotes/";
  // Group input based on FRA and basis swaps for EURIBOR3M  
  private static final String GROUPS_IN_FILE = "EUR-DSCONOIS-E3BS-E6IRS-group.csv";
  private static final String SETTINGS_IN_FILE = "EUR-DSCONOIS-E3BS-E6IRS-settings.csv";
  private static final String NODES_IN_FILE = "EUR-DSCONOIS-E3BS-E6IRS-nodes.csv";
  private static final CurveGroupDefinition GROUPS_IN =
      RatesCalibrationCsvLoader.load(
          ResourceLocator.of(CONFIG_PATH + GROUPS_IN_FILE),
          ResourceLocator.of(CONFIG_PATH + SETTINGS_IN_FILE),
          ResourceLocator.of(CONFIG_PATH + NODES_IN_FILE)).get(0);
  // Group with synthetic curves, all nodes based on deposit or Fixed v Floating swaps
  private static final String GROUPS_SY_FILE = "FRTB-EUR-group.csv";
  private static final String SETTINGS_SY_FILE = "FRTB-EUR-settings.csv";
  private static final String NODES_SY_FILE = "FRTB-EUR-nodes.csv";
  private static final CurveGroupDefinition GROUPS_SYN =
      RatesCalibrationCsvLoader.load(
          ResourceLocator.of(CONFIG_PATH + GROUPS_SY_FILE),
          ResourceLocator.of(CONFIG_PATH + SETTINGS_SY_FILE),
          ResourceLocator.of(CONFIG_PATH + NODES_SY_FILE)).get(0);
  private static final String QUOTES_FILE = "quotes-20151120-eur.csv";
  private static final Map<QuoteId, Double> MQ_INPUT = 
      QuotesCsvLoader.load(VALUATION_DATE, ImmutableList.of(ResourceLocator.of(QUOTES_PATH + QUOTES_FILE)));
  private static final ImmutableMarketData MARKET_QUOTES_INPUT = 
      ImmutableMarketData.builder(VALUATION_DATE).addValuesById(MQ_INPUT).build();
  private static final Map<Index, LocalDateDoubleTimeSeries> TS_EMPTY = new HashMap<>();
  private static final Map<Index, LocalDateDoubleTimeSeries> TS_LARGE = new HashMap<>();
  static { // Fixing unnaturally high to see the difference in the calibration
    LocalDateDoubleTimeSeries tsEur3 = LocalDateDoubleTimeSeries.builder().put(VALUATION_DATE, 0.0200).build();
    LocalDateDoubleTimeSeries tsEur6 = LocalDateDoubleTimeSeries.builder().put(VALUATION_DATE, 0.0250).build();
    TS_LARGE.put(EUR_EURIBOR_3M, tsEur3);
    TS_LARGE.put(EUR_EURIBOR_6M, tsEur6);
  }
  private static final CurveCalibrator CALIBRATOR = StandardCurveCalibrator.DEFAULT;
  private static final CalibrationMeasures MQ_MEASURES = CalibrationMeasures.of(
      MarketQuoteMeasure.FRA_MQ,
      MarketQuoteMeasure.IBOR_FIXING_DEPOSIT_MQ,
      MarketQuoteMeasure.IBOR_FUTURE_MQ,
      MarketQuoteMeasure.SWAP_MQ,
      MarketQuoteMeasure.TERM_DEPOSIT_MQ);
  private static final SyntheticCurveCalibrator CALIBRATOR_SYNTHETIC = 
      SyntheticCurveCalibrator.of(MQ_MEASURES, CALIBRATOR);
  
  private static final ImmutableRatesProvider MULTICURVE_INPUT_TSEMPTY =
      CALIBRATOR.calibrate(GROUPS_IN, VALUATION_DATE, MARKET_QUOTES_INPUT, TS_EMPTY);
  private static final ImmutableRatesProvider MULTICURVE_INPUT_TSLARGE =
      CALIBRATOR.calibrate(GROUPS_IN, VALUATION_DATE, MARKET_QUOTES_INPUT, TS_LARGE);
  
  private static final double TOLERANCE_MQ = 1.0E-8;

  public void test_of() {
    SyntheticCurveCalibrator test = SyntheticCurveCalibrator.of(MQ_MEASURES, CALIBRATOR);
    assertEquals(test.getMarketQuotesMeasures(), MQ_MEASURES);
    assertEquals(test.getCalibrator(), CALIBRATOR);
  }

  // Check market data computation
  public void market_data() {
    CurveGroupDefinition group = GROUPS_SYN;
    ImmutableRatesProvider multicurveTsLarge = MULTICURVE_INPUT_TSEMPTY.toBuilder().timeSeries(TS_LARGE).build();
    MarketData madTsEmpty = CALIBRATOR_SYNTHETIC.marketData(MULTICURVE_INPUT_TSEMPTY, group);
    MarketData madTsLarge = CALIBRATOR_SYNTHETIC.marketData(multicurveTsLarge, group);
    assertEquals(madTsEmpty.getValuationDate(), VALUATION_DATE);
    for (NodalCurveDefinition entry : group.getCurveDefinitions()) {
      ImmutableList<CurveNode> nodes = entry.getNodes();
      for (CurveNode node : nodes) {
        Trade tradeTsEmpty = node.trade(VALUATION_DATE, madTsEmpty);
        double mqTsEmpty = MQ_MEASURES.value(tradeTsEmpty, MULTICURVE_INPUT_TSEMPTY);
        assertEquals(mqTsEmpty, (Double) madTsEmpty.getValue(node.requirements().iterator().next()), TOLERANCE_MQ);
        Trade tradeTsLarge = node.trade(VALUATION_DATE, madTsLarge);
        double mqTsLarge = MQ_MEASURES.value(tradeTsLarge, multicurveTsLarge);
        assertEquals(mqTsLarge, (Double) madTsLarge.getValue(node.requirements().iterator().next()), TOLERANCE_MQ);
        // Market Quote for Fixed v ibor swaps should have changed with the fixing
        if ((tradeTsLarge instanceof SwapTrade) && // Swap Fixed v Ibor
            (((SwapTrade) tradeTsLarge)).getProduct().getLegs(SwapLegType.IBOR).size() == 1) {
          assertTrue(Math.abs(mqTsEmpty - mqTsLarge) > TOLERANCE_MQ);
        }
      }
    }
  }

  // Check synthetic calibration in case no time-series is present
  public void calibrate_ts_empty() {
    MarketData mad = CALIBRATOR_SYNTHETIC.marketData(MULTICURVE_INPUT_TSEMPTY, GROUPS_SYN);
    ImmutableRatesProvider multicurveSyn = CALIBRATOR_SYNTHETIC.calibrate(GROUPS_SYN, MULTICURVE_INPUT_TSEMPTY);
    for (NodalCurveDefinition entry : GROUPS_SYN.getCurveDefinitions()) {
      ImmutableList<CurveNode> nodes = entry.getNodes();
      for (CurveNode node : nodes) {
        Trade trade = node.trade(VALUATION_DATE, mad);
        double mqIn = MQ_MEASURES.value(trade, MULTICURVE_INPUT_TSEMPTY);
        double mqSy = MQ_MEASURES.value(trade, multicurveSyn);
        assertEquals(mqIn, mqSy, TOLERANCE_MQ);
      }
    }
  }

  // Check synthetic calibration in the case of existing time-series with fixing on the valuation date
  public void calibrate_ts_vd() {
    SyntheticCurveCalibrator calibratorDefault = SyntheticCurveCalibrator.DEFAULT;
    MarketData mad = calibratorDefault.marketData(MULTICURVE_INPUT_TSLARGE, GROUPS_SYN);
    ImmutableRatesProvider multicurveSyn = CALIBRATOR_SYNTHETIC.calibrate(GROUPS_SYN, MULTICURVE_INPUT_TSLARGE);
    multicurveSyn = multicurveSyn.toBuilder().timeSeries(TS_LARGE).build(); // To ensure TS are present
    for (NodalCurveDefinition entry : GROUPS_SYN.getCurveDefinitions()) {
      ImmutableList<CurveNode> nodes = entry.getNodes();
      for (CurveNode node : nodes) {
        Trade trade = node.trade(VALUATION_DATE, mad);
        double mqIn = MQ_MEASURES.value(trade, MULTICURVE_INPUT_TSLARGE);
        double mqSy = MQ_MEASURES.value(trade, multicurveSyn);
        assertEquals(mqIn, mqSy, TOLERANCE_MQ);
      }
    }
  }

  @Test(enabled = false) // enabled = false for standard testing. Used only to assess the performance
  public void performance() {
    long start, end;
    int nbReps = 4;
    int nbTests = 100;
    for (int looprep = 0; looprep < nbReps; looprep++) {
      start = System.currentTimeMillis();
      int hs = 0;
      for (int looptest = 0; looptest < nbTests; looptest++) {
        ImmutableRatesProvider multicurve =
            CALIBRATOR.calibrate(GROUPS_IN, VALUATION_DATE, MARKET_QUOTES_INPUT, TS_LARGE);
        hs += multicurve.getIndexCurves().size();
      }
      end = System.currentTimeMillis();
      System.out.println("Initial curve calibration time: " + (end-start) + 
          " ms for " + nbTests + " calibrations.  " + hs);
    }    
    for (int looprep = 0; looprep < nbReps; looprep++) {
      start = System.currentTimeMillis();
      int hs = 0;
      for (int looptest = 0; looptest < nbTests; looptest++) {
        ImmutableRatesProvider multicurve1 =
            CALIBRATOR.calibrate(GROUPS_IN, VALUATION_DATE, MARKET_QUOTES_INPUT, TS_LARGE);
        ImmutableRatesProvider multicurve2 = CALIBRATOR_SYNTHETIC.calibrate(GROUPS_SYN, multicurve1);
        hs += multicurve2.getIndexCurves().size();
      }
      end = System.currentTimeMillis();
      System.out.println("Initial + synthetic curve calibration time: " + (end-start) + 
          " ms for " + nbTests + " calibrations.  " + hs);
    }
    // Calibration time of the (initial + synthetic) curves is roughly twice as long as the initial calibration on its
    // own. There is almost no overhead to compute the synthetic quotes used as input to the second calibration.
  }
  
}
