/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.DAY_COUNT;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
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
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.curve.CalibrationMeasures;
import com.opengamma.strata.pricer.curve.CurveCalibrator;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.option.TenorRawOptionData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Tests {@link SabrSwaptionCalibrator} for a cube. Realistic dimension and data.
 */
@Test
public class SabrSwaptionCalibratorCubeBlackExtremeDataTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final LocalDate CALIBRATION_DATE = LocalDate.of(2016, 2, 29);
  private static final ZonedDateTime CALIBRATION_TIME = CALIBRATION_DATE.atTime(10, 0).atZone(ZoneId.of("Europe/Berlin"));

  private static final SabrSwaptionCalibrator SABR_CALIBRATION = SabrSwaptionCalibrator.DEFAULT;

  private static final String BASE_DIR = "src/test/resources/";
  private static final String GROUPS_FILE = "curve-config/EUR-DSCONOIS-E3BS-E6IRS-group.csv";
  private static final String SETTINGS_FILE = "curve-config/EUR-DSCONOIS-E3BS-E6IRS-settings.csv";
  private static final String NODES_FILE = "curve-config/EUR-DSCONOIS-E3BS-E6IRS-nodes.csv";
  private static final String QUOTES_FILE = "quotes/quotes-20160229-eur.csv";
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
  private static final RatesProvider MULTICURVE = CALIBRATOR.calibrate(CONFIGS, MARKET_QUOTES, REF_DATA);

  private static final DiscountingSwapProductPricer SWAP_PRICER = DiscountingSwapProductPricer.DEFAULT;

  private static final DoubleArray MONEYNESS =
      DoubleArray.of(-0.0100, -0.0050, -0.0025, 0.0000, 0.0025, 0.0050, 0.0100, 0.0200);
  private static final List<Period> EXPIRIES = new ArrayList<>();
  private static final List<Tenor> TENORS = new ArrayList<>();
  static {
    EXPIRIES.add(Period.ofMonths(1));
    EXPIRIES.add(Period.ofMonths(3));
    EXPIRIES.add(Period.ofMonths(6));
    EXPIRIES.add(Period.ofYears(1));
    EXPIRIES.add(Period.ofYears(2));
    EXPIRIES.add(Period.ofYears(5));
    TENORS.add(Tenor.TENOR_1Y);
    TENORS.add(Tenor.TENOR_2Y);
    TENORS.add(Tenor.TENOR_5Y);
    TENORS.add(Tenor.TENOR_10Y);
  }

  private static final double[][][] DATA_LOGNORMAL_SPARSE = {
      {
      {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
      {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
      {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
      {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
      {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
      {Double.NaN, Double.NaN, Double.NaN, Double.NaN, 1.4019, 1.0985, 0.8441, 0.6468}},
      {
      {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
      {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
      {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
      {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
      {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
      {Double.NaN, 1.969, Double.NaN, 1.2894, 1.0152, 0.8718, 0.7142, 0.5712}},
      {
      {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
      {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN},
      {Double.NaN, Double.NaN, Double.NaN, Double.NaN, 6.2852, 4.2545, 2.5079, 1.8524},
      {Double.NaN, Double.NaN, Double.NaN, Double.NaN, 4.0593, 2.2422, 1.5647, 1.2192},
      {Double.NaN, Double.NaN, 3.1613, 2.8434, 1.5934, 1.2644, 0.9856, 0.7885},
      {2.0291, 1.2216, 0.9355, 0.7936, 0.7047, 0.643, 0.5625, 0.4793}},
      {
      {Double.NaN, 6.6443, 1.8125, 1.3324, 1.1296, 1.0273, 0.9392, 0.8961},
      {Double.NaN, 4.3337, 1.6752, 1.2496, 1.0687, 0.9785, 0.9032, 0.8712},
      {Double.NaN, 3.2343, 1.5785, 1.2105, 1.0415, 0.9499, 0.8629, 0.8123},
      {Double.NaN, 2.3148, 1.3951, 1.1006, 0.9474, 0.8544, 0.751, 0.6684},
      {Double.NaN, 1.5092, 1.1069, 0.9209, 0.8095, 0.7348, 0.6416, 0.5518},
      {2.1248, 0.8566, 0.734, 0.6542, 0.5972, 0.5541, 0.4929, 0.4218}}
  };
  private static final TenorRawOptionData DATA_SPARSE = SabrSwaptionCalibratorSmileTestUtils
      .rawData(TENORS, EXPIRIES, ValueType.SIMPLE_MONEYNESS, MONEYNESS, ValueType.BLACK_VOLATILITY, DATA_LOGNORMAL_SPARSE);
  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final SwaptionVolatilitiesName NAME_SABR = SwaptionVolatilitiesName.of("Calibrated-SABR");
  private static final SabrSwaptionDefinition DEFINITION =
      SabrSwaptionDefinition.of(NAME_SABR, EUR_FIXED_1Y_EURIBOR_6M, DAY_COUNT, INTERPOLATOR_2D);

  private static final double TOLERANCE_PRICE_CALIBRATION_LS = 1.0E-3; // Calibration Least Square; result not exact

  @Test
  public void log_normal_cube() {
    double beta = 0.50;
    Surface betaSurface = ConstantSurface.of("Beta", beta)
        .withMetadata(DefaultSurfaceMetadata.builder()
            .xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.YEAR_FRACTION)
            .zValueType(ValueType.SABR_BETA).surfaceName("Beta").build());
    double shift = 0.0300;
    Surface shiftSurface = ConstantSurface.of("Shift", shift)
        .withMetadata(DefaultSurfaceMetadata.builder()
            .xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.YEAR_FRACTION).surfaceName("Shift").build());
    SabrParametersSwaptionVolatilities calibrated = SABR_CALIBRATION.calibrateWithFixedBetaAndShift(
        DEFINITION, CALIBRATION_TIME, DATA_SPARSE, MULTICURVE, betaSurface, shiftSurface);

    for (int looptenor = 0; looptenor < TENORS.size(); looptenor++) {
      double tenor = TENORS.get(looptenor).get(ChronoUnit.YEARS);
      for (int loopexpiry = 0; loopexpiry < EXPIRIES.size(); loopexpiry++) {
        LocalDate expiry = EUR_FIXED_1Y_EURIBOR_6M.getFloatingLeg().getStartDateBusinessDayAdjustment()
            .adjust(CALIBRATION_DATE.plus(EXPIRIES.get(loopexpiry)), REF_DATA);
        LocalDate effectiveDate = EUR_FIXED_1Y_EURIBOR_6M.calculateSpotDateFromTradeDate(expiry, REF_DATA);
        LocalDate endDate = effectiveDate.plus(TENORS.get(looptenor));
        SwapTrade swap = EUR_FIXED_1Y_EURIBOR_6M
            .toTrade(CALIBRATION_DATE, effectiveDate, endDate, BuySell.BUY, 1.0, 0.0);
        double parRate = SWAP_PRICER.parRate(swap.resolve(REF_DATA).getProduct(), MULTICURVE);
        ZonedDateTime expiryDateTime = expiry.atTime(11, 0).atZone(ZoneId.of("Europe/Berlin"));
        double time = calibrated.relativeTime(expiryDateTime);
        for (int loopmoney = 0; loopmoney < MONEYNESS.size(); loopmoney++) {
          if (!Double.isNaN(DATA_LOGNORMAL_SPARSE[looptenor][loopexpiry][loopmoney])) {
            double strike = parRate + MONEYNESS.get(loopmoney);
            double volBlack = calibrated.volatility(expiryDateTime, tenor, strike, parRate);
            double priceComputed = BlackFormulaRepository.price(parRate + shift, parRate + MONEYNESS.get(loopmoney) + shift,
                time, volBlack, true);
            double priceLogNormal = BlackFormulaRepository.price(parRate, parRate + MONEYNESS.get(loopmoney),
                time, DATA_LOGNORMAL_SPARSE[looptenor][loopexpiry][loopmoney], true);
            if (strike > 0.0025) { // Test only for strikes above 25bps
              assertEquals(priceComputed, priceLogNormal, TOLERANCE_PRICE_CALIBRATION_LS);
            }
          }
        }
      }
    }
  }

}
