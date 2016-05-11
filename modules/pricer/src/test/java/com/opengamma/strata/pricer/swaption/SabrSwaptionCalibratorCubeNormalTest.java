/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.DATA_ARRAY_SPARSE;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.EXPIRIES;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.MONEYNESS;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.TENORS;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.ImmutableMarketData;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.id.QuoteId;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.surface.ConstantNodalSurface;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.pricer.calibration.CalibrationMeasures;
import com.opengamma.strata.pricer.calibration.CurveCalibrator;
import com.opengamma.strata.pricer.calibration.RawOptionData;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.impl.option.NormalFormulaRepository;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Tests {@link SabrSwaptionCalibrator} for a cube. Realistic dimension and data.
 */
@Test
public class SabrSwaptionCalibratorCubeNormalTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final LocalDate CALIBRATION_DATE = LocalDate.of(2016, 2, 29);
  private static final ZonedDateTime CALIBRATION_TIME = CALIBRATION_DATE.atTime(10, 0).atZone(ZoneId.of("Europe/Berlin"));

  private static final SabrSwaptionCalibrator SABR_CALIBRATION = SabrSwaptionCalibrator.DEFAULT;

  private static final String BASE_DIR = "src/test/resources/";
  private static final String GROUPS_FILE = "curve-config/EUR-DSCONOIS-E3BS-E6IRS-group.csv";
  private static final String SETTINGS_FILE = "curve-config/EUR-DSCONOIS-E3BS-E6IRS-settings.csv";
  private static final String NODES_FILE = "curve-config/EUR-DSCONOIS-E3BS-E6IRS-nodes.csv";
  private static final String QUOTES_FILE = "quotes/quotes-20160229-eur.csv";
  private static final Map<Index, LocalDateDoubleTimeSeries> TS = new HashMap<>();
  private static final CurveGroupDefinition CONFIGS =
      RatesCalibrationCsvLoader.load(
          ResourceLocator.of(BASE_DIR + GROUPS_FILE),
          ResourceLocator.of(BASE_DIR + SETTINGS_FILE),
          ResourceLocator.of(BASE_DIR + NODES_FILE)).get(0);
  private static final Map<QuoteId, Double> MAP_MQ =
      QuotesCsvLoader.load(CALIBRATION_DATE, ImmutableList.of(ResourceLocator.of(BASE_DIR + QUOTES_FILE)));
  private static final ImmutableMarketData MARKET_QUOTES = ImmutableMarketData.builder(CALIBRATION_DATE)
      .addValuesById(MAP_MQ).build();

  private static final CalibrationMeasures CALIBRATION_MEASURES = CalibrationMeasures.PAR_SPREAD;
  private static final CurveCalibrator CALIBRATOR = CurveCalibrator.of(1e-9, 1e-9, 100, CALIBRATION_MEASURES);
  private static final RatesProvider MULTICURVE =
      CALIBRATOR.calibrate(CONFIGS, CALIBRATION_DATE, MARKET_QUOTES, REF_DATA, TS);

  private static final DiscountingSwapProductPricer SWAP_PRICER = DiscountingSwapProductPricer.DEFAULT;
  private static final List<RawOptionData> DATA_SPARSE = rawData(DATA_ARRAY_SPARSE);
  private static final Interpolator1D LINEAR_FLAT = CombinedInterpolatorExtrapolator.of(
      CurveInterpolators.LINEAR.getName(), CurveExtrapolators.FLAT.getName(), CurveExtrapolators.FLAT.getName());
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);

  private static final double TOLERANCE_PRICE_CALIBRATION_LS = 5.0E-4; // Calibration Least Square; result not exact

  @Test
  public void normal_cube() {
    double beta = 0.50;
    NodalSurface betaSurface = ConstantNodalSurface.of("Beta", beta);
    double shift = 0.0300;
    NodalSurface shiftSurface = ConstantNodalSurface.of("Shift", shift);
    SabrParametersSwaptionVolatilities calibrated = SABR_CALIBRATION.calibrateWithFixedBetaAndShift(
        EUR_FIXED_1Y_EURIBOR_6M, CALIBRATION_TIME, ACT_365F, TENORS, DATA_SPARSE,
        MULTICURVE, betaSurface, shiftSurface, INTERPOLATOR_2D);

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
          if (!Double.isNaN(DATA_ARRAY_SPARSE[looptenor][loopexpiry][loopmoney])) {
            double strike = parRate + MONEYNESS.get(loopmoney);
            double volBlack = calibrated.volatility(expiryDateTime, tenor, strike, parRate);
            double priceComputed = BlackFormulaRepository.price(parRate + shift, parRate + MONEYNESS.get(loopmoney) + shift,
                time, volBlack, true);
            double priceNormal = NormalFormulaRepository.price(parRate, parRate + MONEYNESS.get(loopmoney),
                time, DATA_ARRAY_SPARSE[looptenor][loopexpiry][loopmoney], PutCall.CALL);
            assertEquals(priceComputed, priceNormal, TOLERANCE_PRICE_CALIBRATION_LS);
          }
        }
      }
    }
  }

  private static List<RawOptionData> rawData(double[][][] dataArray) {
    List<RawOptionData> raw = new ArrayList<>();
    for (int looptenor = 0; looptenor < dataArray.length; looptenor++) {
      raw.add(RawOptionData.of(MONEYNESS, ValueType.SIMPLE_MONEYNESS, EXPIRIES,
          DoubleMatrix.ofUnsafe(dataArray[looptenor]), ValueType.NORMAL_VOLATILITY));
    }
    return raw;
  }

}
