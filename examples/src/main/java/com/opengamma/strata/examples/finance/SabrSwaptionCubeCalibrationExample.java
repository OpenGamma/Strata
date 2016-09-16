/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.examples.finance.SwaptionCubeData.DATA_ARRAY_FULL;
import static com.opengamma.strata.examples.finance.SwaptionCubeData.DATA_ARRAY_SPARSE;
import static com.opengamma.strata.examples.finance.SwaptionCubeData.EXPIRIES;
import static com.opengamma.strata.examples.finance.SwaptionCubeData.MONEYNESS;
import static com.opengamma.strata.examples.finance.SwaptionCubeData.TENORS;
import static com.opengamma.strata.market.ValueType.NORMAL_VOLATILITY;
import static com.opengamma.strata.market.ValueType.SIMPLE_MONEYNESS;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.curve.CalibrationMeasures;
import com.opengamma.strata.pricer.curve.CurveCalibrator;
import com.opengamma.strata.pricer.impl.option.NormalFormulaRepository;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.pricer.option.TenorRawOptionData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.swaption.SabrParametersSwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SabrSwaptionCalibrator;
import com.opengamma.strata.pricer.swaption.SabrSwaptionDefinition;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesName;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Analysis of swaption cube calibration with shifted SABR smile function.
 */
public class SabrSwaptionCubeCalibrationExample {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final LocalDate CALIBRATION_DATE = LocalDate.of(2016, 2, 29);
  private static final ZonedDateTime CALIBRATION_TIME = CALIBRATION_DATE.atTime(10, 0).atZone(ZoneId.of("Europe/Berlin"));

  private static final SabrSwaptionCalibrator SABR_CALIBRATION = SabrSwaptionCalibrator.DEFAULT;

  private static final String BASE_DIR = "src/main/resources/";
  private static final String GROUPS_FILE = "example-calibration/curves/EUR-DSCONOIS-E3BS-E6IRS-group.csv";
  private static final String SETTINGS_FILE = "example-calibration/curves/EUR-DSCONOIS-E3BS-E6IRS-settings.csv";
  private static final String NODES_FILE = "example-calibration/curves/EUR-DSCONOIS-E3BS-E6IRS-nodes.csv";
  private static final String QUOTES_FILE = "example-calibration/quotes/quotes-20160229-eur.csv";
  private static final CurveGroupDefinition CONFIGS =
      RatesCalibrationCsvLoader.load(
          ResourceLocator.of(BASE_DIR + GROUPS_FILE),
          ResourceLocator.of(BASE_DIR + SETTINGS_FILE),
          ResourceLocator.of(BASE_DIR + NODES_FILE)).get(CurveGroupName.of("EUR-DSCONOIS-E3BS-E6IRS"));
  private static final Map<QuoteId, Double> MAP_MQ =
      QuotesCsvLoader.load(CALIBRATION_DATE, ImmutableList.of(ResourceLocator.of(BASE_DIR + QUOTES_FILE)));
  private static final ImmutableMarketData MARKET_QUOTES = ImmutableMarketData.of(CALIBRATION_DATE, MAP_MQ);

  private static final CalibrationMeasures CALIBRATION_MEASURES = CalibrationMeasures.PAR_SPREAD;
  private static final CurveCalibrator CALIBRATOR = CurveCalibrator.of(1e-9, 1e-9, 100, CALIBRATION_MEASURES);
  private static final RatesProvider MULTICURVE =
      CALIBRATOR.calibrate(CONFIGS, MARKET_QUOTES, REF_DATA);

  private static final DiscountingSwapProductPricer SWAP_PRICER = DiscountingSwapProductPricer.DEFAULT;

  private static final int NB_EXPIRIES = EXPIRIES.size();
  private static final int NB_TENORS = TENORS.size();
  private static final TenorRawOptionData DATA_FULL = rawData(DATA_ARRAY_FULL);
  private static final TenorRawOptionData DATA_SPARSE = rawData(DATA_ARRAY_SPARSE);
  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final SwaptionVolatilitiesName NAME_SABR = SwaptionVolatilitiesName.of("Calibrated-SABR");
  private static final SabrSwaptionDefinition DEFINITION =
      SabrSwaptionDefinition.of(NAME_SABR, EUR_FIXED_1Y_EURIBOR_6M, ACT_365F, INTERPOLATOR_2D);

  //-------------------------------------------------------------------------
  /**
   * Runs the calibration of swaptions and print the calibrated smile results on the console.
   * 
   * @param args  -s to use the sparse data, i.e. a cube with missing data points
   */
  public static void main(String[] args) {

    // select data
    TenorRawOptionData data = DATA_FULL;
    if (args.length > 0) {
      if (args[0].equals("-s")) {
        data = DATA_SPARSE;
      }
    }
    System.out.println("Start calibration");
    double beta = 0.50;
    SurfaceMetadata betaMetadata = DefaultSurfaceMetadata.builder()
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.SABR_BETA)
        .surfaceName("Beta").build();
    Surface betaSurface = ConstantSurface.of(betaMetadata, beta);
    double shift = 0.0300;
    Surface shiftSurface = ConstantSurface.of("Shift", shift);
    SabrParametersSwaptionVolatilities calibrated = SABR_CALIBRATION.calibrateWithFixedBetaAndShift(
        DEFINITION, CALIBRATION_TIME, data, MULTICURVE, betaSurface, shiftSurface);
    System.out.println("End calibration");
    /* Graph calibration */
    int nbStrikesGraph = 50;
    double moneyMin = -0.0250;
    double moneyMax = +0.0300;
    double[] moneyGraph = new double[nbStrikesGraph + 1];
    for (int i = 0; i < nbStrikesGraph + 1; i++) {
      moneyGraph[i] = moneyMin + i * (moneyMax - moneyMin) / nbStrikesGraph;
    }
    double[][][] strikesGraph = new double[NB_TENORS][NB_EXPIRIES][nbStrikesGraph + 1];
    double[][][] volLNGraph = new double[NB_TENORS][NB_EXPIRIES][nbStrikesGraph + 1];
    double[][][] volNGraph = new double[NB_TENORS][NB_EXPIRIES][nbStrikesGraph + 1];
    double[][] parRate = new double[NB_TENORS][NB_EXPIRIES];
    for (int looptenor = 0; looptenor < TENORS.size(); looptenor++) {
      double tenor = TENORS.get(looptenor).get(ChronoUnit.YEARS);
      for (int loopexpiry = 0; loopexpiry < EXPIRIES.size(); loopexpiry++) {
        LocalDate expiry = EUR_FIXED_1Y_EURIBOR_6M.getFloatingLeg().getStartDateBusinessDayAdjustment()
            .adjust(CALIBRATION_DATE.plus(EXPIRIES.get(loopexpiry)), REF_DATA);
        LocalDate effectiveDate = EUR_FIXED_1Y_EURIBOR_6M.calculateSpotDateFromTradeDate(expiry, REF_DATA);
        LocalDate endDate = effectiveDate.plus(TENORS.get(looptenor));
        SwapTrade swap = EUR_FIXED_1Y_EURIBOR_6M
            .toTrade(CALIBRATION_DATE, effectiveDate, endDate, BuySell.BUY, 1.0, 0.0);
        parRate[looptenor][loopexpiry] = SWAP_PRICER.parRate(swap.resolve(REF_DATA).getProduct(), MULTICURVE);
        ZonedDateTime expiryDateTime = expiry.atTime(11, 0).atZone(ZoneId.of("Europe/Berlin"));
        double time = calibrated.relativeTime(expiryDateTime);
        for (int i = 0; i < nbStrikesGraph + 1; i++) {
          strikesGraph[looptenor][loopexpiry][i] = parRate[looptenor][loopexpiry] + moneyGraph[i];
          volLNGraph[looptenor][loopexpiry][i] = calibrated.volatility(
              expiryDateTime,
              tenor,
              strikesGraph[looptenor][loopexpiry][i],
              parRate[looptenor][loopexpiry]);
          volNGraph[looptenor][loopexpiry][i] = NormalFormulaRepository.impliedVolatilityFromBlackApproximated(
              parRate[looptenor][loopexpiry] + shift,
              strikesGraph[looptenor][loopexpiry][i] + shift,
              time,
              volLNGraph[looptenor][loopexpiry][i]);
        }
      }
    }

    /* Graph export */
    String svn = "Moneyness";
    for (int looptenor = 0; looptenor < TENORS.size(); looptenor++) {
      for (int loopexpiry = 0; loopexpiry < EXPIRIES.size(); loopexpiry++) {
        svn = svn + ", Strike_" + EXPIRIES.get(loopexpiry).toString() + "x" + TENORS.get(looptenor).toString() + ", NormalVol_" +
            EXPIRIES.get(loopexpiry).toString() + "x" + TENORS.get(looptenor).toString();
      }
    }
    svn = svn + "\n";
    for (int i = 0; i < nbStrikesGraph + 1; i++) {
      svn = svn + moneyGraph[i];
      for (int looptenor = 0; looptenor < TENORS.size(); looptenor++) {
        for (int loopexpiry = 0; loopexpiry < EXPIRIES.size(); loopexpiry++) {
          svn = svn + ", " + strikesGraph[looptenor][loopexpiry][i];
          svn = svn + ", " + volNGraph[looptenor][loopexpiry][i];
        }
      }
      svn = svn + "\n";
    }
    System.out.println(svn);
  }

  private static TenorRawOptionData rawData(double[][][] dataArray) {
    Map<Tenor, RawOptionData> raw = new TreeMap<>();
    for (int looptenor = 0; looptenor < dataArray.length; looptenor++) {
      DoubleMatrix matrix = DoubleMatrix.ofUnsafe(dataArray[looptenor]);
      raw.put(TENORS.get(looptenor), RawOptionData.of(EXPIRIES, MONEYNESS, SIMPLE_MONEYNESS, matrix, NORMAL_VOLATILITY));
    }
    return TenorRawOptionData.of(raw);
  }

}
