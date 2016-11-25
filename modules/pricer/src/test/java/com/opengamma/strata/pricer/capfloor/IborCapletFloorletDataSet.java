/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Data set of Ibor caplet/floorlet.
 */
public class IborCapletFloorletDataSet {

  // Rates provider
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final DoubleArray DSC_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
  private static final DoubleArray DSC_RATE = DoubleArray.of(0.0150, 0.0125, 0.0150, 0.0175, 0.0150, 0.0150);
  /** discounting curve name */
  public static final CurveName DSC_NAME = CurveName.of("EUR Dsc");
  private static final CurveMetadata META_DSC = Curves.zeroRates(DSC_NAME, ACT_ACT_ISDA);
  private static final InterpolatedNodalCurve DSC_CURVE =
      InterpolatedNodalCurve.of(META_DSC, DSC_TIME, DSC_RATE, INTERPOLATOR);
  private static final DoubleArray FWD3_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0);
  private static final DoubleArray FWD3_RATE =
      DoubleArray.of(0.0150, 0.0125, 0.0150, 0.0175, 0.0175, 0.0190, 0.0200, 0.0210);
  /** Forward curve name */
  public static final CurveName FWD3_NAME = CurveName.of("EUR EURIBOR 3M");
  private static final CurveMetadata META_FWD3 = Curves.zeroRates(FWD3_NAME, ACT_ACT_ISDA);
  private static final InterpolatedNodalCurve FWD3_CURVE =
      InterpolatedNodalCurve.of(META_FWD3, FWD3_TIME, FWD3_RATE, INTERPOLATOR);
  private static final DoubleArray FWD6_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
  private static final DoubleArray FWD6_RATE = DoubleArray.of(0.0150, 0.0125, 0.0150, 0.0175, 0.0150, 0.0150);
  /** Forward curve name */
  public static final CurveName FWD6_NAME = CurveName.of("EUR EURIBOR 6M");
  private static final CurveMetadata META_FWD6 = Curves.zeroRates(FWD6_NAME, ACT_ACT_ISDA);
  private static final InterpolatedNodalCurve FWD6_CURVE =
      InterpolatedNodalCurve.of(META_FWD6, FWD6_TIME, FWD6_RATE, INTERPOLATOR);

  /**
   * Creates rates provider with specified valuation date.
   * 
   * @param valuationDate  the valuation date
   * @return  the rates provider
   */
  public static ImmutableRatesProvider createRatesProvider(LocalDate valuationDate) {
    return ImmutableRatesProvider.builder(valuationDate)
        .discountCurves(ImmutableMap.of(EUR, DSC_CURVE))
        .indexCurves(ImmutableMap.of(EUR_EURIBOR_3M, FWD3_CURVE, EUR_EURIBOR_6M, FWD6_CURVE))
        .fxRateProvider(FxMatrix.empty())
        .build();
  }

  /**
   * Creates rates provider with specified valuation date and time series of the index.
   * 
   * @param valuationDate  the valuation date
   * @param index  the index
   * @param timeSeries  the time series
   * @return  the rates provider
   */
  public static ImmutableRatesProvider createRatesProvider(
      LocalDate valuationDate,
      IborIndex index,
      LocalDateDoubleTimeSeries timeSeries) {
    return ImmutableRatesProvider.builder(valuationDate)
        .discountCurves(ImmutableMap.of(EUR, DSC_CURVE))
        .indexCurves(ImmutableMap.of(EUR_EURIBOR_3M, FWD3_CURVE, EUR_EURIBOR_6M, FWD6_CURVE))
        .fxRateProvider(FxMatrix.empty())
        .timeSeries(index, timeSeries)
        .build();
  }

  // Black volatilities provider
  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final DoubleArray EXPIRIES = DoubleArray.of(0.5, 0.5, 0.5, 1.0, 1.0, 1.0, 5.0, 5.0, 5.0);
  private static final DoubleArray STRIKES = DoubleArray.of(0.01, 0.02, 0.03, 0.01, 0.02, 0.03, 0.01, 0.02, 0.03);
  private static final DoubleArray BLACK_VOLS = DoubleArray.of(0.35, 0.30, 0.28, 0.34, 0.25, 0.23, 0.25, 0.20, 0.18);
  private static final SurfaceMetadata BLACK_METADATA =
      Surfaces.blackVolatilityByExpiryStrike("Black Vol", ACT_ACT_ISDA);
  private static final Surface BLACK_SURFACE_EXP_STR =
      InterpolatedNodalSurface.of(BLACK_METADATA, EXPIRIES, STRIKES, BLACK_VOLS, INTERPOLATOR_2D);

  // Black volatilities provider with shift
  /** constant shift */
  public static final double SHIFT = 5.0e-2;
  private static final DoubleArray SHIFTED_STRIKES = DoubleArray.of(STRIKES.size(), i -> STRIKES.get(i) + SHIFT);
  private static final SurfaceMetadata SHIFTED_BLACK_METADATA =
      Surfaces.blackVolatilityByExpiryStrike("Shifted Black vol", ACT_ACT_ISDA);
  private static final Surface SHIFTED_BLACK_SURFACE_EXP_STR =
      InterpolatedNodalSurface.of(SHIFTED_BLACK_METADATA, EXPIRIES, SHIFTED_STRIKES, BLACK_VOLS, INTERPOLATOR_2D);
  private static final Curve SHIFT_CURVE = ConstantCurve.of("const shift", SHIFT);

  /**
   * Creates volatilities provider with specified date and index.
   * 
   * @param valuationDate  the valuation date
   * @param index  the index
   * @return  the volatilities provider
   */
  public static BlackIborCapletFloorletExpiryStrikeVolatilities createBlackVolatilities(
      ZonedDateTime valuationDate,
      IborIndex index) {
    return BlackIborCapletFloorletExpiryStrikeVolatilities.of(index, valuationDate, BLACK_SURFACE_EXP_STR);
  }

  /**
   * Creates shifted Black volatilities provider with specified date and index.
   * 
   * @param valuationDate  the valuation date
   * @param index  the index
   * @return  the volatilities provider
   */
  public static ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities createShiftedBlackVolatilities(
      ZonedDateTime valuationDate,
      IborIndex index) {
    return ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities.of(
        index, valuationDate, SHIFTED_BLACK_SURFACE_EXP_STR, SHIFT_CURVE);
  }

  // Normal volatilities provider
  private static final DoubleArray NORMAL_VOLS = DoubleArray.of(0.09, 0.08, 0.05, 0.07, 0.05, 0.04, 0.06, 0.05, 0.03);
  private static final SurfaceMetadata NORMAL_METADATA =
      Surfaces.normalVolatilityByExpiryStrike("Normal Vol", ACT_ACT_ISDA);
  private static final Surface NORMAL_SURFACE_EXP_STR =
      InterpolatedNodalSurface.of(NORMAL_METADATA, EXPIRIES, STRIKES, NORMAL_VOLS, INTERPOLATOR_2D);

  /**
   * Creates volatilities provider with specified date and index.
   * 
   * @param valuationDate  the valuation date
   * @param index  the index
   * @return  the volatilities provider
   */
  public static NormalIborCapletFloorletExpiryStrikeVolatilities createNormalVolatilities(
      ZonedDateTime valuationDate,
      IborIndex index) {
    return NormalIborCapletFloorletExpiryStrikeVolatilities.of(index, valuationDate, NORMAL_SURFACE_EXP_STR);
  }

}
