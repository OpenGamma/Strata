/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.DATA_ARRAY_FULL;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.DATA_ARRAY_SPARSE;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.DAY_COUNT;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.EXPIRIES;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.MONEYNESS;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.TENORS;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
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
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsLegPricer;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsPeriodPricer;
import com.opengamma.strata.pricer.curve.CalibrationMeasures;
import com.opengamma.strata.pricer.curve.CurveCalibrator;
import com.opengamma.strata.pricer.option.TenorRawOptionData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.cms.CmsLeg;
import com.opengamma.strata.product.cms.ResolvedCmsLeg;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;

/**
 * Tests {@link SabrSwaptionRawDataSensitivityCalculator}.
 */
@Test
public class SabrSwaptionRawDataSensitivityCalculatorTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  /* =====     Data     ===== */
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

  private static final TenorRawOptionData DATA_RAW_FULL = SabrSwaptionCalibratorSmileTestUtils
      .rawData(TENORS, EXPIRIES, ValueType.SIMPLE_MONEYNESS, MONEYNESS, ValueType.NORMAL_VOLATILITY, DATA_ARRAY_FULL);
  private static final TenorRawOptionData DATA_RAW_SPARSE = SabrSwaptionCalibratorSmileTestUtils
      .rawData(TENORS, EXPIRIES, ValueType.SIMPLE_MONEYNESS, MONEYNESS, ValueType.NORMAL_VOLATILITY, DATA_ARRAY_SPARSE);
  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final SwaptionVolatilitiesName NAME_SABR = SwaptionVolatilitiesName.of("Calibrated-SABR");
  private static final SabrSwaptionDefinition DEFINITION =
      SabrSwaptionDefinition.of(NAME_SABR, EUR_FIXED_1Y_EURIBOR_6M, DAY_COUNT, INTERPOLATOR_2D);

  private static final double BETA = 0.50;
  private static final Surface BETA_SURFACE = ConstantSurface.of("Beta", BETA)
      .withMetadata(DefaultSurfaceMetadata.builder()
          .xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.YEAR_FRACTION)
          .zValueType(ValueType.SABR_BETA).surfaceName("Beta").build());
  private static final double SHIFT_SABR = 0.0300;
  private static final Surface SHIFT_SABR_SURFACE = ConstantSurface.of("Shift", SHIFT_SABR)
      .withMetadata(DefaultSurfaceMetadata.builder()
          .xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.YEAR_FRACTION).surfaceName("Shift").build());
  private static final SabrParametersSwaptionVolatilities SABR_CALIBRATED_FULL =
      SABR_CALIBRATION.calibrateWithFixedBetaAndShift(
          DEFINITION, CALIBRATION_TIME, DATA_RAW_FULL, MULTICURVE, BETA_SURFACE, SHIFT_SABR_SURFACE);
  private static final SabrParametersSwaptionVolatilities SABR_CALIBRATED_SPARSE =
      SABR_CALIBRATION.calibrateWithFixedBetaAndShift(
          DEFINITION, CALIBRATION_TIME, DATA_RAW_SPARSE, MULTICURVE, BETA_SURFACE, SHIFT_SABR_SURFACE);

  /* =====     Trades     ===== */
  private static final LocalDate START = LocalDate.of(2016, 3, 7);
  private static final LocalDate END = LocalDate.of(2021, 3, 7);
  private static final Frequency FREQUENCY = Frequency.P12M;
  private static final BusinessDayAdjustment BUSS_ADJ_EUR =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA);
  private static final PeriodicSchedule SCHEDULE_EUR =
      PeriodicSchedule.of(START, END, FREQUENCY, BUSS_ADJ_EUR, StubConvention.NONE, RollConventions.NONE);
  private static final SwapIndex INDEX = SwapIndices.EUR_EURIBOR_1100_5Y;
  private static final double FLOOR_VALUE = 0.014;
  private static final ValueSchedule FLOOR_STRIKE = ValueSchedule.of(FLOOR_VALUE);
  private static final double NOTIONAL_VALUE = 100_000_000;
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(NOTIONAL_VALUE);
  private static final ResolvedCmsLeg FLOOR_LEG = CmsLeg.builder()
      .floorSchedule(FLOOR_STRIKE)
      .index(INDEX)
      .notional(NOTIONAL)
      .payReceive(RECEIVE)
      .paymentSchedule(SCHEDULE_EUR)
      .build()
      .resolve(REF_DATA);

  /* =====     Pricers     ===== */
  private static final double CUT_OFF_STRIKE = 0.10;
  private static final double MU = 2.50;
  private static final SabrExtrapolationReplicationCmsPeriodPricer PERIOD_PRICER =
      SabrExtrapolationReplicationCmsPeriodPricer.of(CUT_OFF_STRIKE, MU);
  private static final SabrExtrapolationReplicationCmsLegPricer LEG_PRICER =
      new SabrExtrapolationReplicationCmsLegPricer(PERIOD_PRICER);

  private static final SabrSwaptionRawDataSensitivityCalculator RDSC = SabrSwaptionRawDataSensitivityCalculator.DEFAULT;

  /**
   * Compare the AD version of the sensitivity to a finite difference parallel bump of the smile.
   * Full data set, no missing data.
   */
  public void presentValueSensitivityRawDataParallelSensitivity_full() {
    presentValueSensitivityRawDataParallelSensitivity(SABR_CALIBRATED_FULL, DATA_RAW_FULL);
  }

  /**
   * Compare the AD version of the sensitivity to a finite difference parallel bump of the smile.
   * Sparse data set, some raw data are missing in some smiles.
   */
  public void presentValueSensitivityRawDataParallelSensitivity_sparse() {
    presentValueSensitivityRawDataParallelSensitivity(SABR_CALIBRATED_SPARSE, DATA_RAW_SPARSE);
  }

  private void presentValueSensitivityRawDataParallelSensitivity(
      SabrParametersSwaptionVolatilities sabrCalibrated,
      TenorRawOptionData dataRaw) {

    PointSensitivities points =
        LEG_PRICER.presentValueSensitivityModelParamsSabr(FLOOR_LEG, MULTICURVE, sabrCalibrated).build();
    CurrencyParameterSensitivities sabrParametersSurfaceSensitivities = sabrCalibrated.parameterSensitivity(points);
    CurrencyParameterSensitivity parallelSensitivitiesSurface =
        RDSC.parallelSensitivity(sabrParametersSurfaceSensitivities, sabrCalibrated);
    DoubleArray sensitivityArray = parallelSensitivitiesSurface.getSensitivity();
    double fdShift = 1.0E-6;
    int surfacePointIndex = 0;
    for (int loopexpiry = 0; loopexpiry < EXPIRIES.size(); loopexpiry++) {
      for (int looptenor = 0; looptenor < TENORS.size(); looptenor++) {
        Tenor tenor = TENORS.get(looptenor);
        Pair<DoubleArray, DoubleArray> ds = dataRaw.getData(tenor).availableSmileAtExpiry(EXPIRIES.get(loopexpiry));
        if (!ds.getFirst().isEmpty()) {
          double[] pv = new double[2]; // pv with shift up and down
          for (int loopsign = 0; loopsign < 2; loopsign++) {
            TenorRawOptionData dataShifted = SabrSwaptionCalibratorSmileTestUtils
                .rawDataShiftSmile(
                    TENORS, EXPIRIES, ValueType.SIMPLE_MONEYNESS, MONEYNESS, ValueType.NORMAL_VOLATILITY,
                    DATA_ARRAY_FULL, looptenor, loopexpiry, (2 * loopsign - 1) * fdShift);
            SabrParametersSwaptionVolatilities calibratedShifted = SABR_CALIBRATION.calibrateWithFixedBetaAndShift(
                DEFINITION, CALIBRATION_TIME, dataShifted, MULTICURVE, BETA_SURFACE, SHIFT_SABR_SURFACE);
            pv[loopsign] = LEG_PRICER.presentValue(FLOOR_LEG, MULTICURVE, calibratedShifted).getAmount();
          }
          double sensitivityFd = (pv[1] - pv[0]) / (2 * fdShift); // FD sensitivity computation
          SabrSwaptionCalibratorSmileTestUtils.checkAcceptable(sensitivityFd, sensitivityArray.get(surfacePointIndex),
              0.10, "Tenor/Expiry: " + TENORS.get(looptenor) + " / " + EXPIRIES.get(loopexpiry));
          surfacePointIndex++;
        }
      }
    }
  }

}
