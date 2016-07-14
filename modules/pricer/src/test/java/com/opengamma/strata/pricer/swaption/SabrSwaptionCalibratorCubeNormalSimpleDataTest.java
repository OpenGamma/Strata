/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.ATM_NORMAL_SIMPLE;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.DATA_ARRAY_SPARSE;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.DATA_DATE;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.DATA_NORMAL_ATM_SIMPLE;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.DATA_NORMAL_SIMPLE;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.DATA_TIME;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.DAY_COUNT;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.EXPIRIES_SIMPLE;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.EXPIRIES_SIMPLE_2;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.MONEYNESS;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.TENORS_SIMPLE;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.curve.CalibrationMeasures;
import com.opengamma.strata.pricer.curve.CurveCalibrator;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.impl.option.NormalFormulaRepository;
import com.opengamma.strata.pricer.option.TenorRawOptionData;
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

  private static final LocalDate CALIBRATION_DATE = DATA_DATE;
  private static final ZonedDateTime CALIBRATION_TIME = DATA_TIME;

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

  private static final TenorRawOptionData DATA_SIMPLE = SabrSwaptionCalibratorSmileTestUtils.rawData(
      TENORS_SIMPLE,
      EXPIRIES_SIMPLE,
      ValueType.SIMPLE_MONEYNESS,
      MONEYNESS,
      ValueType.NORMAL_VOLATILITY,
      DATA_NORMAL_SIMPLE);
  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final SwaptionVolatilitiesName NAME_SABR = SwaptionVolatilitiesName.of("Calibrated-SABR");
  private static final SabrSwaptionDefinition DEFINITION =
      SabrSwaptionDefinition.of(NAME_SABR, EUR_FIXED_1Y_EURIBOR_6M, DAY_COUNT, INTERPOLATOR_2D);

  private static final double TOLERANCE_PRICE_CALIBRATION_LS = 5.0E-4; // Calibration Least Square; result not exact
  private static final double TOLERANCE_PRICE_CALIBRATION_ROOT = 1.0E-6; // Calibration root finding
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
    SabrParametersSwaptionVolatilities calibrated = SABR_CALIBRATION.calibrateWithFixedBetaAndShift(
        DEFINITION, CALIBRATION_TIME, DATA_SIMPLE, MULTICURVE, betaSurface, shiftSurface);

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

  @SuppressWarnings("unused")
  @Test
  public void normal_atm() {
    double beta = 0.50;
    Surface betaSurface = ConstantSurface.of("Beta", beta)
        .withMetadata(DefaultSurfaceMetadata.builder()
            .xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.YEAR_FRACTION)
            .zValueType(ValueType.SABR_BETA).surfaceName("Beta").build());
    double shift = 0.0300;
    Surface shiftSurface = ConstantSurface.of("Shift", shift)
        .withMetadata(DefaultSurfaceMetadata.builder()
            .xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.YEAR_FRACTION).surfaceName("Shift").build());
    SabrParametersSwaptionVolatilities calibratedSmile = SABR_CALIBRATION.calibrateWithFixedBetaAndShift(
        DEFINITION, CALIBRATION_TIME, DATA_SIMPLE, MULTICURVE, betaSurface, shiftSurface);
    SabrParametersSwaptionVolatilities calibratedAtm =
        SABR_CALIBRATION.calibrateAlphaWithAtm(NAME_SABR, calibratedSmile, MULTICURVE, ATM_NORMAL_SIMPLE, TENORS_SIMPLE,
            EXPIRIES_SIMPLE_2, INTERPOLATOR_2D);
    int nbExp = EXPIRIES_SIMPLE_2.size();
    int nbTenor = TENORS_SIMPLE.size();
    for (int loopexpiry = 0; loopexpiry < nbExp; loopexpiry++) {
      for (int looptenor = 0; looptenor < nbTenor; looptenor++) {
        double tenor = TENORS_SIMPLE.get(looptenor).get(ChronoUnit.YEARS);
        LocalDate expiry = EUR_FIXED_1Y_EURIBOR_6M.getFloatingLeg().getStartDateBusinessDayAdjustment()
            .adjust(CALIBRATION_DATE.plus(EXPIRIES_SIMPLE_2.get(loopexpiry)), REF_DATA);
        LocalDate effectiveDate = EUR_FIXED_1Y_EURIBOR_6M.calculateSpotDateFromTradeDate(expiry, REF_DATA);
        LocalDate endDate = effectiveDate.plus(TENORS_SIMPLE.get(looptenor));
        SwapTrade swap = EUR_FIXED_1Y_EURIBOR_6M
            .toTrade(CALIBRATION_DATE, effectiveDate, endDate, BuySell.BUY, 1.0, 0.0);
        double parRate = SWAP_PRICER.parRate(swap.resolve(REF_DATA).getProduct(), MULTICURVE);
        ZonedDateTime expiryDateTime = expiry.atTime(11, 0).atZone(ZoneId.of("Europe/Berlin"));
        double time = calibratedAtm.relativeTime(expiryDateTime);
        double volBlack = calibratedAtm.volatility(expiryDateTime, tenor, parRate, parRate);
        double priceComputed = BlackFormulaRepository.price(parRate + shift, parRate + shift, time, volBlack, true);
        double priceNormal = NormalFormulaRepository.price(parRate, parRate, time,
            DATA_NORMAL_ATM_SIMPLE[looptenor + loopexpiry * nbTenor], PutCall.CALL);
        assertEquals(priceComputed, priceNormal, TOLERANCE_PRICE_CALIBRATION_ROOT);
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
    SabrParametersSwaptionVolatilities calibrated = SABR_CALIBRATION.calibrateWithFixedBetaAndShift(
        DEFINITION, CALIBRATION_TIME, DATA_SIMPLE, MULTICURVE, betaSurface, shiftSurface);
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
    for (int loopexpiry = 0; loopexpiry < EXPIRIES_SIMPLE.size(); loopexpiry++) {
      for (int looptenor = 0; looptenor < TENORS_SIMPLE.size(); looptenor++) {
        Tenor tenor = TENORS_SIMPLE.get(looptenor);
        double tenorYear = tenor.get(ChronoUnit.YEARS);
        LocalDate expiry = EUR_FIXED_1Y_EURIBOR_6M.getFloatingLeg().getStartDateBusinessDayAdjustment()
            .adjust(CALIBRATION_DATE.plus(EXPIRIES_SIMPLE.get(loopexpiry)), REF_DATA);
        ZonedDateTime expiryDateTime = expiry.atTime(11, 0).atZone(ZoneId.of("Europe/Berlin"));
        double time = calibrated.relativeTime(expiryDateTime);
        Pair<DoubleArray, DoubleArray> ds = DATA_SIMPLE.getData(tenor).availableSmileAtExpiry(EXPIRIES_SIMPLE.get(loopexpiry));
        if (!ds.getFirst().isEmpty()) {
          int availableDataIndex = 0;

          ParameterMetadata alphaPM = alphaParameterMetadata.get(surfacePointIndex);
          assertTrue(alphaPM instanceof SwaptionSurfaceExpiryTenorParameterMetadata);
          SwaptionSurfaceExpiryTenorParameterMetadata pmAlphaSabr = (SwaptionSurfaceExpiryTenorParameterMetadata) alphaPM;
          assertEquals(tenorYear, pmAlphaSabr.getTenor());
          assertEquals(time, pmAlphaSabr.getYearFraction(), TOLERANCE_EXPIRY);
          DoubleArray alphaSensitivityToData = alphaJacobian.get(surfacePointIndex);
          ParameterMetadata rhoPM = rhoParameterMetadata.get(surfacePointIndex);
          assertTrue(rhoPM instanceof SwaptionSurfaceExpiryTenorParameterMetadata);
          SwaptionSurfaceExpiryTenorParameterMetadata pmRhoSabr = (SwaptionSurfaceExpiryTenorParameterMetadata) rhoPM;
          assertEquals(tenorYear, pmRhoSabr.getTenor());
          assertEquals(time, pmRhoSabr.getYearFraction(), TOLERANCE_EXPIRY);
          DoubleArray rhoSensitivityToData = rhoJacobian.get(surfacePointIndex);
          ParameterMetadata nuPM = nuParameterMetadata.get(surfacePointIndex);
          assertTrue(nuPM instanceof SwaptionSurfaceExpiryTenorParameterMetadata);
          SwaptionSurfaceExpiryTenorParameterMetadata pmNuSabr = (SwaptionSurfaceExpiryTenorParameterMetadata) nuPM;
          assertEquals(tenorYear, pmNuSabr.getTenor());
          assertEquals(time, pmNuSabr.getYearFraction(), TOLERANCE_EXPIRY);
          DoubleArray nuSensitivityToData = nuJacobian.get(surfacePointIndex);

          for (int loopmoney = 0; loopmoney < MONEYNESS.size(); loopmoney++) {
            if (!Double.isNaN(DATA_NORMAL_SIMPLE[looptenor][loopexpiry][loopmoney])) {
              double[] alphaShifted = new double[2];
              double[] rhoShifted = new double[2];
              double[] nuShifted = new double[2];
              for (int loopsign = 0; loopsign < 2; loopsign++) {
                TenorRawOptionData dataShifted = SabrSwaptionCalibratorSmileTestUtils.rawDataShiftPoint(
                    TENORS_SIMPLE,
                    EXPIRIES_SIMPLE,
                    ValueType.SIMPLE_MONEYNESS,
                    MONEYNESS,
                    ValueType.NORMAL_VOLATILITY,
                    DATA_NORMAL_SIMPLE,
                    looptenor,
                    loopexpiry,
                    loopmoney,
                    (2 * loopsign - 1) * fdShift);
                SabrParametersSwaptionVolatilities calibratedShifted = SABR_CALIBRATION.calibrateWithFixedBetaAndShift(
                    DEFINITION, CALIBRATION_TIME, dataShifted, MULTICURVE, betaSurface, shiftSurface);
                alphaShifted[loopsign] = calibratedShifted.getParameters().getAlphaSurface().zValue(time, tenorYear);
                rhoShifted[loopsign] = calibratedShifted.getParameters().getRhoSurface().zValue(time, tenorYear);
                nuShifted[loopsign] = calibratedShifted.getParameters().getNuSurface().zValue(time, tenorYear);
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
