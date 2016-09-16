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
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
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
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.curve.CalibrationMeasures;
import com.opengamma.strata.pricer.curve.CurveCalibrator;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.pricer.option.TenorRawOptionData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.SabrParametersSwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SabrSwaptionCalibrator;
import com.opengamma.strata.pricer.swaption.SabrSwaptionDefinition;
import com.opengamma.strata.pricer.swaption.SabrSwaptionPhysicalProductPricer;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesName;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.ResolvedSwaption;
import com.opengamma.strata.product.swaption.Swaption;

/**
 * Analysis of the pricing and risk of a swaption with calibrated SABR parameters.
 */
public class SabrSwaptionCubePvRiskExample {

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

  private static final SabrSwaptionPhysicalProductPricer SWAPTION_PRICER = SabrSwaptionPhysicalProductPricer.DEFAULT;

  private static final TenorRawOptionData DATA_FULL = rawData(DATA_ARRAY_FULL);
  private static final TenorRawOptionData DATA_SPARSE = rawData(DATA_ARRAY_SPARSE);
  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final SwaptionVolatilitiesName NAME_SABR = SwaptionVolatilitiesName.of("Calibrated-SABR");
  private static final SabrSwaptionDefinition DEFINITION =
      SabrSwaptionDefinition.of(NAME_SABR, EUR_FIXED_1Y_EURIBOR_6M, ACT_365F, INTERPOLATOR_2D);

  //-------------------------------------------------------------------------
  /**
   * Runs the calibration of SABR on swaptions and print on the console the present value, bucketed PV01 and 
   * the bucketed Vega of a 18M x 4Y swaption.
   * 
   * @param args  -s to use the spares data
   */
  public static void main(String[] args) {

    long start, end;

    // Swaption description
    BuySell payer = BuySell.BUY;
    Period expiry = Period.ofMonths(18);
    double notional = 1_000_000;
    double strike = 0.0100;
    Tenor tenor = Tenor.TENOR_4Y;
    LocalDate expiryDate = EUR_FIXED_1Y_EURIBOR_6M.getFloatingLeg().getStartDateBusinessDayAdjustment()
        .adjust(CALIBRATION_DATE.plus(expiry), REF_DATA);
    SwapTrade underlying = EUR_FIXED_1Y_EURIBOR_6M.createTrade(expiryDate, tenor, payer, notional, strike, REF_DATA);
    Swaption swaption = Swaption.builder().expiryDate(AdjustableDate.of(expiryDate)).expiryTime(LocalTime.of(11, 00))
        .expiryZone(ZoneId.of("Europe/Berlin")).underlying(underlying.getProduct()).longShort(LongShort.LONG)
        .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT).build();
    ResolvedSwaption resolvedSwaption = swaption.resolve(REF_DATA);

    // select data
    TenorRawOptionData data = DATA_FULL;
    if (args.length > 0) {
      if (args[0].equals("-s")) {
        data = DATA_SPARSE;
      }
    }

    start = System.currentTimeMillis();
    // Curve calibration 
    RatesProvider multicurve = CALIBRATOR.calibrate(CONFIGS, MARKET_QUOTES, REF_DATA);
    end = System.currentTimeMillis();
    System.out.println("Curve calibration time: " + (end - start) + " ms.");

    // SABR calibration 
    start = System.currentTimeMillis();
    double beta = 0.50;
    SurfaceMetadata betaMetadata = DefaultSurfaceMetadata.builder()
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.SABR_BETA)
        .surfaceName("Beta").build();
    Surface betaSurface = ConstantSurface.of(betaMetadata, beta);
    double shift = 0.0300;
    Surface shiftSurface = ConstantSurface.of("SABR-Shift", shift);
    SabrParametersSwaptionVolatilities sabr = SABR_CALIBRATION.calibrateWithFixedBetaAndShift(
        DEFINITION, CALIBRATION_TIME, data, multicurve, betaSurface, shiftSurface);
    end = System.currentTimeMillis();
    System.out.println("SABR calibration time: " + (end - start) + " ms.");

    // Price and risk
    System.out.println("Risk measures: ");
    start = System.currentTimeMillis();
    CurrencyAmount pv = SWAPTION_PRICER.presentValue(resolvedSwaption, multicurve, sabr);
    System.out.println("  |-> PV: " + pv.toString());

    PointSensitivities deltaPts =
        SWAPTION_PRICER.presentValueSensitivityRatesStickyModel(resolvedSwaption, multicurve, sabr).build();
    CurrencyParameterSensitivities deltaBucketed = multicurve.parameterSensitivity(deltaPts);
    System.out.println("  |-> Delta bucketed: " + deltaBucketed.toString());

    PointSensitivities vegaPts =
        SWAPTION_PRICER.presentValueSensitivityModelParamsSabr(resolvedSwaption, multicurve, sabr).build();
    System.out.println("  |-> Vega point: " + vegaPts.toString());

    CurrencyParameterSensitivities vegaBucketed = sabr.parameterSensitivity(vegaPts);
    for (int i = 0; i < vegaBucketed.size(); i++) {
      System.out.println("  |-> Vega bucketed: " + vegaBucketed.getSensitivities().get(i));
    }

    end = System.currentTimeMillis();
    System.out.println("PV and risk time: " + (end - start) + " ms.");
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
