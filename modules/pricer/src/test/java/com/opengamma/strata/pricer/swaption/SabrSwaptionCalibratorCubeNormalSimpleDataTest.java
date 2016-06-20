/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.DATA_ARRAY_SPARSE;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.DATA_NORMAL_SIMPLE;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.EXPIRIES_SIMPLE;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.MONEYNESS;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.TENORS_SIMPLE;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.pricer.curve.CalibrationMeasures;
import com.opengamma.strata.pricer.curve.CurveCalibrator;
import com.opengamma.strata.pricer.curve.RawOptionData;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.impl.option.NormalFormulaRepository;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Tests {@link SabrSwaptionCalibrator} for a cube. Realistic dimension and data.
 */
@Test
public class SabrSwaptionCalibratorCubeNormalSimpleDataTest {

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
  private static final ImmutableMarketData MARKET_QUOTES = ImmutableMarketData.of(CALIBRATION_DATE, MAP_MQ);

  private static final CalibrationMeasures CALIBRATION_MEASURES = CalibrationMeasures.PAR_SPREAD;
  private static final CurveCalibrator CALIBRATOR = CurveCalibrator.of(1e-9, 1e-9, 100, CALIBRATION_MEASURES);
  private static final RatesProvider MULTICURVE =
      CALIBRATOR.calibrate(CONFIGS, CALIBRATION_DATE, MARKET_QUOTES, REF_DATA, TS);

  private static final DiscountingSwapProductPricer SWAP_PRICER = DiscountingSwapProductPricer.DEFAULT;

  private static final List<RawOptionData> DATA_SIMPLE = SabrSwaptionCalibratorSmileTestUtils
      .rawData(ValueType.SIMPLE_MONEYNESS, MONEYNESS, EXPIRIES_SIMPLE, ValueType.NORMAL_VOLATILITY, DATA_NORMAL_SIMPLE);
  private static final Interpolator1D LINEAR_FLAT = CombinedInterpolatorExtrapolator.of(
      CurveInterpolators.LINEAR.getName(), CurveExtrapolators.FLAT.getName(), CurveExtrapolators.FLAT.getName());
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  private static final SwaptionVolatilitiesName NAME_SABR = SwaptionVolatilitiesName.of("Calibrated-SABR");

  private static final double TOLERANCE_PRICE_CALIBRATION_LS = 5.0E-4; // Calibration Least Square; result not exact
  private static final double TOLERANCE_PARAM_SENSITIVITY = 3.0E-2;
  private static final double TOLERANCE_PARAM_SENSITIVITY_NU = 9.0E-2;
  private static final double TOLERANCE_EXPIRY = 1.0E-6;

  @Test
  public void normal_cube() {
    double beta = 0.50;
    Surface betaSurface = ConstantSurface.of("Beta", beta)
        .withMetadata(DefaultSurfaceMetadata.builder()
            .xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.YEAR_FRACTION)
            .zValueType(ValueType.SABR_BETA).surfaceName("Beta").build());
    double shift = 0.0300;
    Surface shiftSurface = ConstantSurface.of("Shift", shift)
        .withMetadata(DefaultSurfaceMetadata.builder()
            .xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.YEAR_FRACTION).surfaceName("Shift").build());
    SabrParametersSwaptionVolatilities calibrated = SABR_CALIBRATION.calibrateWithFixedBetaAndShift(NAME_SABR,
        EUR_FIXED_1Y_EURIBOR_6M, CALIBRATION_TIME, ACT_365F, TENORS_SIMPLE, DATA_SIMPLE,
        MULTICURVE, betaSurface, shiftSurface, INTERPOLATOR_2D);

    for (int looptenor = 0; looptenor < TENORS_SIMPLE.size(); looptenor++) {
      double tenor = TENORS_SIMPLE.get(looptenor).get(ChronoUnit.YEARS);
      for (int loopexpiry = 0; loopexpiry < EXPIRIES_SIMPLE.size(); loopexpiry++) {
        LocalDate expiry = EUR_FIXED_1Y_EURIBOR_6M.getFloatingLeg().getStartDateBusinessDayAdjustment()
            .adjust(CALIBRATION_DATE.plus(EXPIRIES_SIMPLE.get(loopexpiry)), REF_DATA);
        LocalDate effectiveDate = EUR_FIXED_1Y_EURIBOR_6M.calculateSpotDateFromTradeDate(expiry, REF_DATA);
        LocalDate endDate = effectiveDate.plus(TENORS_SIMPLE.get(looptenor));
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

  /**
   * Check that the sensitivities of parameters with respect to data is stored in the metadata.
   * Compare the sensitivities to a finite difference approximation.
   * This test is relatively slow as it calibrates the full surface multiple times.
   */
  @Test
  public void normal_cube_sensitivity() {
    double beta = 0.50;
    Surface betaSurface = ConstantSurface.of("Beta", beta)
        .withMetadata(DefaultSurfaceMetadata.builder()
            .xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.YEAR_FRACTION)
            .zValueType(ValueType.SABR_BETA).surfaceName("Beta").build());
    double shift = 0.0300;
    Surface shiftSurface = ConstantSurface.of("Shift", shift)
        .withMetadata(DefaultSurfaceMetadata.builder()
            .xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.YEAR_FRACTION).surfaceName("Shift").build());
    SabrParametersSwaptionVolatilities calibrated = SABR_CALIBRATION.calibrateWithFixedBetaAndShift(NAME_SABR,
        EUR_FIXED_1Y_EURIBOR_6M, CALIBRATION_TIME, ACT_365F, TENORS_SIMPLE, DATA_SIMPLE,
        MULTICURVE, betaSurface, shiftSurface, INTERPOLATOR_2D);
    double fdShift = 1.0E-5;

    SurfaceMetadata alphaMetadata = calibrated.getParameters().getAlphaSurface().getMetadata();
    Optional<List<ParameterMetadata>> alphaParameterMetadataOption = alphaMetadata.getParameterMetadata();
    assertTrue(alphaParameterMetadataOption.isPresent());
    List<ParameterMetadata> alphaParameterMetadata = alphaParameterMetadataOption.get();
    List<DoubleArray> alphaJacobian = calibrated.getDataSensitivityAlpha().get();
    SurfaceMetadata rhoMetadata = calibrated.getParameters().getRhoSurface().getMetadata();
    Optional<List<ParameterMetadata>> rhoParameterMetadataOption = rhoMetadata.getParameterMetadata();
    assertTrue(rhoParameterMetadataOption.isPresent());
    List<ParameterMetadata> rhoParameterMetadata = rhoParameterMetadataOption.get();
    List<DoubleArray> rhoJacobian = calibrated.getDataSensitivityRho().get();
    SurfaceMetadata nuMetadata = calibrated.getParameters().getNuSurface().getMetadata();
    Optional<List<ParameterMetadata>> nuParameterMetadataOption = nuMetadata.getParameterMetadata();
    assertTrue(nuParameterMetadataOption.isPresent());
    List<ParameterMetadata> nuParameterMetadata = nuParameterMetadataOption.get();
    List<DoubleArray> nuJacobian = calibrated.getDataSensitivityNu().get();

    int surfacePointIndex = 0;
    for (int looptenor = 0; looptenor < TENORS_SIMPLE.size(); looptenor++) {
      double tenor = TENORS_SIMPLE.get(looptenor).get(ChronoUnit.YEARS);
      for (int loopexpiry = 0; loopexpiry < EXPIRIES_SIMPLE.size(); loopexpiry++) {
        LocalDate expiry = EUR_FIXED_1Y_EURIBOR_6M.getFloatingLeg().getStartDateBusinessDayAdjustment()
            .adjust(CALIBRATION_DATE.plus(EXPIRIES_SIMPLE.get(loopexpiry)), REF_DATA);
        ZonedDateTime expiryDateTime = expiry.atTime(11, 0).atZone(ZoneId.of("Europe/Berlin"));
        double time = calibrated.relativeTime(expiryDateTime);
        Pair<DoubleArray, DoubleArray> ds = DATA_SIMPLE.get(looptenor).availableSmileAtExpiry(EXPIRIES_SIMPLE.get(loopexpiry));
        if (!ds.getFirst().isEmpty()) {
          int availableDataIndex = 0;

          ParameterMetadata alphaPM = alphaParameterMetadata.get(surfacePointIndex);
          assertTrue(alphaPM instanceof SwaptionSurfaceExpiryTenorParameterMetadata);
          SwaptionSurfaceExpiryTenorParameterMetadata pmAlphaSabr = (SwaptionSurfaceExpiryTenorParameterMetadata) alphaPM;
          assertEquals(tenor, pmAlphaSabr.getTenor());
          assertEquals(time, pmAlphaSabr.getYearFraction(), TOLERANCE_EXPIRY);
          DoubleArray alphaSensitivityToData = alphaJacobian.get(surfacePointIndex);
          ParameterMetadata rhoPM = rhoParameterMetadata.get(surfacePointIndex);
          assertTrue(rhoPM instanceof SwaptionSurfaceExpiryTenorParameterMetadata);
          SwaptionSurfaceExpiryTenorParameterMetadata pmRhoSabr = (SwaptionSurfaceExpiryTenorParameterMetadata) rhoPM;
          assertEquals(tenor, pmRhoSabr.getTenor());
          assertEquals(time, pmRhoSabr.getYearFraction(), TOLERANCE_EXPIRY);
          DoubleArray rhoSensitivityToData = rhoJacobian.get(surfacePointIndex);
          ParameterMetadata nuPM = nuParameterMetadata.get(surfacePointIndex);
          assertTrue(nuPM instanceof SwaptionSurfaceExpiryTenorParameterMetadata);
          SwaptionSurfaceExpiryTenorParameterMetadata pmNuSabr = (SwaptionSurfaceExpiryTenorParameterMetadata) nuPM;
          assertEquals(tenor, pmNuSabr.getTenor());
          assertEquals(time, pmNuSabr.getYearFraction(), TOLERANCE_EXPIRY);
          DoubleArray nuSensitivityToData = nuJacobian.get(surfacePointIndex);

          for (int loopmoney = 0; loopmoney < MONEYNESS.size(); loopmoney++) {
            if (!Double.isNaN(DATA_NORMAL_SIMPLE[looptenor][loopexpiry][loopmoney])) {
              double[] alphaShifted = new double[2];
              double[] rhoShifted = new double[2];
              double[] nuShifted = new double[2];
              for (int loopsign = 0; loopsign < 2; loopsign++) {
                List<RawOptionData> dataShifted = SabrSwaptionCalibratorSmileTestUtils
                    .rawDataShiftPoint(ValueType.SIMPLE_MONEYNESS, MONEYNESS, EXPIRIES_SIMPLE, ValueType.NORMAL_VOLATILITY, 
                        DATA_NORMAL_SIMPLE, looptenor, loopexpiry, loopmoney, (2 * loopsign - 1) * fdShift);
                SabrParametersSwaptionVolatilities calibratedShifted = SABR_CALIBRATION.calibrateWithFixedBetaAndShift(
                    SwaptionVolatilitiesName.of("Calibrated-SABR-Shifted"),
                    EUR_FIXED_1Y_EURIBOR_6M, CALIBRATION_TIME, ACT_365F, TENORS_SIMPLE, dataShifted,
                    MULTICURVE, betaSurface, shiftSurface, INTERPOLATOR_2D);
                alphaShifted[loopsign] = calibratedShifted.getParameters().getAlphaSurface().zValue(time, tenor);
                rhoShifted[loopsign] = calibratedShifted.getParameters().getRhoSurface().zValue(time, tenor);
                nuShifted[loopsign] = calibratedShifted.getParameters().getNuSurface().zValue(time, tenor);
              }
              double alphaSensitivityComputed = alphaSensitivityToData.get(availableDataIndex);
              double alphaSensitivityExpected = (alphaShifted[1] - alphaShifted[0]) / (2 * fdShift);
              SabrSwaptionCalibratorSmileTestUtils
                  .checkAcceptable(alphaSensitivityComputed, alphaSensitivityExpected, TOLERANCE_PARAM_SENSITIVITY,
                      "Alpha: " + looptenor + " / " + loopexpiry + " / " + loopmoney);
              double rhoSensitivityComputed = rhoSensitivityToData.get(availableDataIndex);
              double rhoSensitivityExpected = (rhoShifted[1] - rhoShifted[0]) / (2 * fdShift);
              SabrSwaptionCalibratorSmileTestUtils
                  .checkAcceptable(rhoSensitivityComputed, rhoSensitivityExpected, TOLERANCE_PARAM_SENSITIVITY,
                      "Rho: " + looptenor + " / " + loopexpiry + " / " + loopmoney);
              double nuSensitivityComputed = nuSensitivityToData.get(availableDataIndex);
              double nuSensitivityExpected = (nuShifted[1] - nuShifted[0]) / (2 * fdShift);
              SabrSwaptionCalibratorSmileTestUtils
                  .checkAcceptable(nuSensitivityComputed, nuSensitivityExpected, TOLERANCE_PARAM_SENSITIVITY_NU,
                      "Nu: " + looptenor + " / " + loopexpiry + " / " + loopmoney);
              availableDataIndex++;
            }
          }
          surfacePointIndex++;
        }
      }
    }
  }

}
