/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.datasets;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * RatesProvider data sets for testing.
 */
public class RatesProviderDataSets {

  /** Wednesday. */
  public static final LocalDate VAL_DATE_2014_01_22 = LocalDate.of(2014, 1, 22);

  public static final DoubleArray TIMES_1 = DoubleArray.of(
      0.01, 0.25, 0.50, 1.0, 2.0, 3.0, 5.0, 7.0, 10.0, 30.0); // 10 nodes
  public static final DoubleArray TIMES_2 = DoubleArray.of(
      0.25, 0.50, 1.0, 2.0, 3.0, 5.0, 7.0, 10.0, 30.0); // 9 nodes
  public static final DoubleArray TIMES_3 = DoubleArray.of(
      0.50, 1.0, 2.0, 3.0, 5.0, 7.0, 10.0, 30.0); // 8 nodes
  public static final DoubleArray RATES_1 = DoubleArray.of(
      0.0100, 0.0110, 0.0120, 0.0130, 0.0140, 0.0150, 0.0160, 0.0170, 0.0180, 0.0190);
  public static final DoubleArray RATES_2 = DoubleArray.of(
      0.0120, 0.0130, 0.0140, 0.0150, 0.0160, 0.0170, 0.0180, 0.0190, 0.0200);
  public static final DoubleArray RATES_3 = DoubleArray.of(
      0.0140, 0.0150, 0.0160, 0.0170, 0.0180, 0.0190, 0.0200, 0.0210);
  public static final DoubleArray RATES_1_1 = DoubleArray.of(
      0.0100, 0.0110, 0.0120, 0.0130, 0.0140, 0.0150, 0.0160, 0.0170, 0.0180, 0.0190);
  public static final DoubleArray RATES_2_1 = DoubleArray.of(
      0.0120, 0.0130, 0.0140, 0.0150, 0.0160, 0.0170, 0.0180, 0.0190, 0.0200);
  public static final DoubleArray RATES_3_1 = DoubleArray.of(
      0.0140, 0.0150, 0.0160, 0.0170, 0.0180, 0.0190, 0.0200, 0.0210);
  public static final DoubleArray RATES_1_2 = DoubleArray.of(
      0.0200, 0.0210, 0.0220, 0.0230, 0.0240, 0.0250, 0.0260, 0.0270, 0.0280, 0.0290);
  public static final DoubleArray RATES_2_2 = DoubleArray.of(
      0.0220, 0.0230, 0.0240, 0.0250, 0.0260, 0.0270, 0.0280, 0.0290, 0.0300);
  public static final DoubleArray RATES_3_2 = DoubleArray.of(
      0.0240, 0.0250, 0.0260, 0.0270, 0.0280, 0.0290, 0.0300, 0.0310);

  //-------------------------------------------------------------------------
  public static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;

  //-------------------------------------------------------------------------
  //     =====     USD     =====     

  private static final FxMatrix FX_MATRIX_USD =
      FxMatrix.builder().addRate(USD, USD, 1.00).build();

  public static final CurveName USD_SINGLE_NAME = CurveName.of("USD-ALL");
  public static final CurveName USD_DSC_NAME = CurveName.of("USD-DSCON");
  public static final CurveName USD_L3_NAME = CurveName.of("USD-LIBOR3M");
  public static final CurveName USD_L6_NAME = CurveName.of("USD-LIBOR6M");
  private static final CurveMetadata USD_SINGLE_METADATA = Curves.zeroRates(USD_SINGLE_NAME, ACT_360);
  private static final CurveMetadata USD_DSC_METADATA = Curves.zeroRates(USD_DSC_NAME, ACT_360);
  private static final CurveMetadata USD_L3_METADATA = Curves.zeroRates(USD_L3_NAME, ACT_360);
  private static final CurveMetadata USD_L6_METADATA = Curves.zeroRates(USD_L6_NAME, ACT_360);

  private static final Curve USD_SINGLE_CURVE =
      InterpolatedNodalCurve.of(USD_SINGLE_METADATA, TIMES_1, RATES_1_1, INTERPOLATOR);
  private static final Map<Currency, Curve> USD_SINGLE_CCY_MAP = ImmutableMap.of(USD, USD_SINGLE_CURVE);
  private static final Map<Index, Curve> USD_SINGLE_IND_MAP = ImmutableMap.of(
      USD_FED_FUND, USD_SINGLE_CURVE,
      USD_LIBOR_3M, USD_SINGLE_CURVE,
      USD_LIBOR_6M, USD_SINGLE_CURVE);

  public static final ImmutableRatesProvider SINGLE_USD = ImmutableRatesProvider.builder()
      .valuationDate(VAL_DATE_2014_01_22)
      .fxRateProvider(FX_MATRIX_USD)
      .discountCurves(USD_SINGLE_CCY_MAP)
      .indexCurves(USD_SINGLE_IND_MAP)
      .build();

  //-------------------------------------------------------------------------
  private static final Curve USD_DSC =
      InterpolatedNodalCurve.of(USD_DSC_METADATA, TIMES_1, RATES_1_1, INTERPOLATOR);
  private static final Curve USD_L3 =
      InterpolatedNodalCurve.of(USD_L3_METADATA, TIMES_2, RATES_2_1, INTERPOLATOR);
  private static final Curve USD_L6 =
      InterpolatedNodalCurve.of(USD_L6_METADATA, TIMES_3, RATES_3_1, INTERPOLATOR);
  private static final Map<Currency, Curve> USD_MULTI_CCY_MAP = ImmutableMap.of(USD, USD_DSC);
  private static final Map<Index, Curve> USD_MULTI_IND_MAP = ImmutableMap.of(
      USD_FED_FUND, USD_DSC,
      USD_LIBOR_3M, USD_L3,
      USD_LIBOR_6M, USD_L6);

  public static final ImmutableRatesProvider MULTI_USD = ImmutableRatesProvider.builder()
      .valuationDate(VAL_DATE_2014_01_22)
      .fxRateProvider(FX_MATRIX_USD)
      .discountCurves(USD_MULTI_CCY_MAP)
      .indexCurves(USD_MULTI_IND_MAP)
      .build();

  //-------------------------------------------------------------------------
  //     =====     GBP     =====     

  private static final FxMatrix FX_MATRIX_GBP =
      FxMatrix.builder().addRate(GBP, GBP, 1.00).build();

  public static final CurveName GBP_DSC_NAME = CurveName.of("USD-DSCON");
  public static final CurveName GBP_L3_NAME = CurveName.of("USD-LIBOR3M");
  public static final CurveName GBP_L6_NAME = CurveName.of("USD-LIBOR6M");
  private static final CurveMetadata GBP_DSC_METADATA = Curves.zeroRates(GBP_DSC_NAME, ACT_360);
  private static final CurveMetadata GBP_L3_METADATA = Curves.zeroRates(GBP_L3_NAME, ACT_360);
  private static final CurveMetadata GBP_L6_METADATA = Curves.zeroRates(GBP_L6_NAME, ACT_360);

  private static final Curve GBP_DSC =
      InterpolatedNodalCurve.of(GBP_DSC_METADATA, TIMES_1, RATES_1_2, INTERPOLATOR);
  private static final Curve GBP_L3 =
      InterpolatedNodalCurve.of(GBP_L3_METADATA, TIMES_2, RATES_2_2, INTERPOLATOR);
  private static final Curve GBP_L6 =
      InterpolatedNodalCurve.of(GBP_L6_METADATA, TIMES_3, RATES_3_2, INTERPOLATOR);
  public static final Map<Currency, Curve> GBP_MULTI_CCY_MAP = ImmutableMap.of(GBP, GBP_DSC);
  public static final Map<Index, Curve> GBP_MULTI_IND_MAP = ImmutableMap.of(
      GBP_SONIA, GBP_DSC,
      GBP_LIBOR_3M, GBP_L3,
      GBP_LIBOR_6M, GBP_L6);

  public static final ImmutableRatesProvider MULTI_GBP = ImmutableRatesProvider.builder()
      .valuationDate(VAL_DATE_2014_01_22)
      .fxRateProvider(FX_MATRIX_GBP)
      .discountCurves(GBP_MULTI_CCY_MAP)
      .indexCurves(GBP_MULTI_IND_MAP)
      .build();

  //-------------------------------------------------------------------------
  //     =====     GBP + USD      =====        

  private static final FxMatrix FX_MATRIX_GBP_USD =
      FxMatrix.builder().addRate(GBP, USD, 1.50).build();

  private static final Map<Currency, Curve> GBP_USD_MULTI_CCY_MAP = new HashMap<>();
  static {
    GBP_USD_MULTI_CCY_MAP.put(GBP, GBP_DSC);
    GBP_USD_MULTI_CCY_MAP.put(USD, USD_DSC);
  }
  private static final Map<Index, Curve> GBP_USD_MULTI_IND_MAP = new HashMap<>();
  static {
    GBP_USD_MULTI_IND_MAP.put(GBP_SONIA, GBP_DSC);
    GBP_USD_MULTI_IND_MAP.put(GBP_LIBOR_3M, GBP_L3);
    GBP_USD_MULTI_IND_MAP.put(GBP_LIBOR_6M, GBP_L6);
    GBP_USD_MULTI_IND_MAP.put(USD_FED_FUND, USD_DSC);
    GBP_USD_MULTI_IND_MAP.put(USD_LIBOR_3M, USD_L3);
    GBP_USD_MULTI_IND_MAP.put(USD_LIBOR_6M, USD_L6);
  }
  public static final ImmutableRatesProvider MULTI_GBP_USD = ImmutableRatesProvider.builder()
      .valuationDate(VAL_DATE_2014_01_22)
      .fxRateProvider(FX_MATRIX_GBP_USD)
      .discountCurves(GBP_USD_MULTI_CCY_MAP)
      .indexCurves(GBP_USD_MULTI_IND_MAP)
      .build();

}
