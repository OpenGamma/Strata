/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.DATA_NORMAL_SIMPLE;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.EXPIRIES_SIMPLE;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.MONEYNESS;
import static com.opengamma.strata.pricer.swaption.SwaptionCubeData.TENORS_SIMPLE;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_6M;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.id.QuoteId;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivities;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.pricer.calibration.CalibrationMeasures;
import com.opengamma.strata.pricer.calibration.CurveCalibrator;
import com.opengamma.strata.pricer.calibration.RawOptionData;
import com.opengamma.strata.pricer.cms.SabrExtrapolationReplicationCmsLegPricer;
import com.opengamma.strata.pricer.impl.cms.SabrExtrapolationReplicationCmsPeriodPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.cms.CmsLeg;
import com.opengamma.strata.product.cms.ResolvedCmsLeg;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;

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
  private static final Map<Index, LocalDateDoubleTimeSeries> TS = new HashMap<>();
  private static final CurveGroupDefinition CONFIGS =
      RatesCalibrationCsvLoader.load(
          ResourceLocator.of(BASE_DIR + GROUPS_FILE),
          ResourceLocator.of(BASE_DIR + SETTINGS_FILE),
          ResourceLocator.of(BASE_DIR + NODES_FILE)).get(0);
  private static final Map<QuoteId, Double> MAP_MQ =
      QuotesCsvLoader.load(CALIBRATION_DATE, ImmutableList.of(ResourceLocator.of(BASE_DIR + QUOTES_FILE)));
  private static final ImmutableMarketData MARKET_QUOTES = ImmutableMarketData.builder(CALIBRATION_DATE)
      .addValues(MAP_MQ).build();

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

  private static final double BETA = 0.50;
  private static final Surface BETA_SURFACE = ConstantSurface.of("Beta", BETA)
      .withMetadata(DefaultSurfaceMetadata.builder()
          .xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.YEAR_FRACTION)
          .zValueType(ValueType.SABR_BETA).surfaceName("Beta").build());
  private static final double SHIFT_SABR = 0.0300;
  private static final Surface SHIFT_SABR_SURFACE = ConstantSurface.of("Shift", SHIFT_SABR)
      .withMetadata(DefaultSurfaceMetadata.builder()
          .xValueType(ValueType.YEAR_FRACTION).yValueType(ValueType.YEAR_FRACTION).surfaceName("Shift").build());
  private static final SabrParametersSwaptionVolatilities SABR_CALIBRATED = SABR_CALIBRATION.calibrateWithFixedBetaAndShift(
      EUR_FIXED_1Y_EURIBOR_6M, CALIBRATION_TIME, ACT_365F, TENORS_SIMPLE, DATA_SIMPLE,
      MULTICURVE, BETA_SURFACE, SHIFT_SABR_SURFACE, INTERPOLATOR_2D);
  
 /* =====     Trades     ===== */
  private static final LocalDate START = LocalDate.of(2021, 3, 7);
  private static final LocalDate END = LocalDate.of(2026, 3, 7);
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


  @SuppressWarnings("unused")
  public void presentValueSensitivitySabrParameterSurface() {
    SwaptionSabrSensitivities points =
        LEG_PRICER.presentValueSensitivitySabrParameter(FLOOR_LEG, MULTICURVE, SABR_CALIBRATED);
    CurrencyParameterSensitivities sabrParametersSurfaceSensitivities = SABR_CALIBRATED.parameterSensitivity(points);
    
    CurrencyParameterSensitivity parallelSensitivitiesSurface =
        RDSC.parallelSensitivity(sabrParametersSurfaceSensitivities, SABR_CALIBRATED);
        
    int t = 0;
  }

}
