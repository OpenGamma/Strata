/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.datasets;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.index.FxIndices.ECB_EUR_GBP;
import static com.opengamma.strata.basics.index.FxIndices.ECB_EUR_USD;
import static com.opengamma.strata.basics.index.FxIndices.WM_EUR_USD;
import static com.opengamma.strata.basics.index.FxIndices.WM_GBP_USD;
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
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * RatesProvider data sets for testing.
 */
public class RatesProviderDataSets {

  public static final double[] TIMES_1 = new double[]
  {0.01, 0.25, 0.50, 1.0, 2.0, 3.0, 5.0, 7.0, 10.0, 30.0}; // 10 nodes
  public static final double[] TIMES_2 = new double[]
  {0.25, 0.50, 1.0, 2.0, 3.0, 5.0, 7.0, 10.0, 30.0}; // 9 nodes
  public static final double[] TIMES_3 = new double[]
  {0.50, 1.0, 2.0, 3.0, 5.0, 7.0, 10.0, 30.0}; // 8 nodes
  public static final double[] RATES_1 = new double[]
  {0.0100, 0.0110, 0.0120, 0.0130, 0.0140, 0.0150, 0.0160, 0.0170, 0.0180, 0.0190};
  public static final double[] RATES_2 = new double[]
  {0.0120, 0.0130, 0.0140, 0.0150, 0.0160, 0.0170, 0.0180, 0.0190, 0.0200};
  public static final double[] RATES_3 = new double[]
  {0.0140, 0.0150, 0.0160, 0.0170, 0.0180, 0.0190, 0.0200, 0.0210};
  public static final double[] RATES_1_1 = new double[]
  {0.0100, 0.0110, 0.0120, 0.0130, 0.0140, 0.0150, 0.0160, 0.0170, 0.0180, 0.0190};
  public static final double[] RATES_2_1 = new double[]
  {0.0120, 0.0130, 0.0140, 0.0150, 0.0160, 0.0170, 0.0180, 0.0190, 0.0200};
  public static final double[] RATES_3_1 = new double[]
  {0.0140, 0.0150, 0.0160, 0.0170, 0.0180, 0.0190, 0.0200, 0.0210};
  public static final double[] RATES_1_2 = new double[]
  {0.0200, 0.0210, 0.0220, 0.0230, 0.0240, 0.0250, 0.0260, 0.0270, 0.0280, 0.0290};
  public static final double[] RATES_2_2 = new double[]
  {0.0220, 0.0230, 0.0240, 0.0250, 0.0260, 0.0270, 0.0280, 0.0290, 0.0300};
  public static final double[] RATES_3_2 = new double[]
  {0.0240, 0.0250, 0.0260, 0.0270, 0.0280, 0.0290, 0.0300, 0.0310};

  //-------------------------------------------------------------------------
  private static final Map<Index, LocalDateDoubleTimeSeries> TIME_SERIES =
      ImmutableMap.<Index, LocalDateDoubleTimeSeries>builder()
          .put(USD_FED_FUND, LocalDateDoubleTimeSeries.empty())
          .put(USD_LIBOR_3M, LocalDateDoubleTimeSeries.empty())
          .put(USD_LIBOR_6M, LocalDateDoubleTimeSeries.empty())
          .put(GBP_SONIA, LocalDateDoubleTimeSeries.empty())
          .put(GBP_LIBOR_3M, LocalDateDoubleTimeSeries.empty())
          .put(GBP_LIBOR_6M, LocalDateDoubleTimeSeries.empty())
          .put(WM_GBP_USD, LocalDateDoubleTimeSeries.empty())
          .put(WM_EUR_USD, LocalDateDoubleTimeSeries.empty())
          .put(ECB_EUR_GBP, LocalDateDoubleTimeSeries.empty())
          .put(ECB_EUR_USD, LocalDateDoubleTimeSeries.empty())
          .build();
  private static final CurveInterpolator INTERPOLATOR = Interpolator1DFactory.LINEAR_INSTANCE;

  //-------------------------------------------------------------------------
  //     =====     USD     =====     

  private static final FxMatrix FX_MATRIX_USD =
      FxMatrix.builder().addRate(USD, USD, 1.00).build();

  public static final String USD_SINGLE_NAME = "USD-ALL";
  public static final String USD_DSC_NAME = "USD-DSCON";
  public static final String USD_L3_NAME = "USD-LIBOR3M";
  public static final String USD_L6_NAME = "USD-LIBOR6M";

  private static final Curve USD_SINGLE_CURVE =
      InterpolatedNodalCurve.of(USD_SINGLE_NAME, TIMES_1, RATES_1_1, INTERPOLATOR);
  private static final Map<Currency, Curve> USD_SINGLE_CCY_MAP = ImmutableMap.of(USD, USD_SINGLE_CURVE);
  private static final Map<Index, Curve> USD_SINGLE_IND_MAP = ImmutableMap.of(
      USD_FED_FUND, USD_SINGLE_CURVE,
      USD_LIBOR_3M, USD_SINGLE_CURVE,
      USD_LIBOR_6M, USD_SINGLE_CURVE);

  public static final ImmutableRatesProvider USD_SINGLE = ImmutableRatesProvider.builder()
      .valuationDate(LocalDate.of(2015, 4, 27))
      .dayCount(ACT_360)
      .fxMatrix(FX_MATRIX_USD)
      .discountCurves(USD_SINGLE_CCY_MAP)
      .indexCurves(USD_SINGLE_IND_MAP)
      .timeSeries(TIME_SERIES)
      .build();

  //-------------------------------------------------------------------------
  private static final Curve USD_DSC =
      InterpolatedNodalCurve.of(USD_DSC_NAME, TIMES_1, RATES_1_1, INTERPOLATOR);
  private static final Curve USD_L3 =
      InterpolatedNodalCurve.of(USD_L3_NAME, TIMES_2, RATES_2_1, INTERPOLATOR);
  private static final Curve USD_L6 =
      InterpolatedNodalCurve.of(USD_L6_NAME, TIMES_3, RATES_3_1, INTERPOLATOR);
  private static final Map<Currency, Curve> USD_MULTI_CCY_MAP = ImmutableMap.of(USD, USD_DSC);
  private static final Map<Index, Curve> USD_MULTI_IND_MAP = ImmutableMap.of(
      USD_FED_FUND, USD_DSC,
      USD_LIBOR_3M, USD_L3,
      USD_LIBOR_6M, USD_L6);

  public static final ImmutableRatesProvider MULTI_USD = ImmutableRatesProvider.builder()
      .valuationDate(LocalDate.of(2015, 4, 27))
      .dayCount(ACT_360)
      .fxMatrix(FX_MATRIX_USD)
      .discountCurves(USD_MULTI_CCY_MAP)
      .indexCurves(USD_MULTI_IND_MAP)
      .timeSeries(TIME_SERIES)
      .build();

  //-------------------------------------------------------------------------
  //     =====     GBP     =====     

  private static final FxMatrix FX_MATRIX_GBP =
      FxMatrix.builder().addRate(GBP, GBP, 1.00).build();

  public static final String GBP_DSC_NAME = "GBP-DSCON";
  public static final String GBP_L3_NAME = "GBP-LIBOR3M";
  public static final String GBP_L6_NAME = "GBP-LIBOR6M";

  private static final Curve GBP_DSC =
      InterpolatedNodalCurve.of(GBP_DSC_NAME, TIMES_1, RATES_1_2, INTERPOLATOR);
  private static final Curve GBP_L3 =
      InterpolatedNodalCurve.of(GBP_L3_NAME, TIMES_2, RATES_2_2, INTERPOLATOR);
  private static final Curve GBP_L6 =
      InterpolatedNodalCurve.of(GBP_L6_NAME, TIMES_3, RATES_3_2, INTERPOLATOR);
  private static final Map<Currency, Curve> GBP_MULTI_CCY_MAP = ImmutableMap.of(GBP, GBP_DSC);
  private static final Map<Index, Curve> GBP_MULTI_IND_MAP = ImmutableMap.of(
      GBP_SONIA, GBP_DSC,
      GBP_LIBOR_3M, GBP_L3,
      GBP_LIBOR_6M, GBP_L6);

  public static final ImmutableRatesProvider MULTI_GBP = ImmutableRatesProvider.builder()
      .valuationDate(LocalDate.of(2013, 1, 2))
      .dayCount(ACT_360)
      .fxMatrix(FX_MATRIX_GBP)
      .discountCurves(GBP_MULTI_CCY_MAP)
      .indexCurves(GBP_MULTI_IND_MAP)
      .timeSeries(TIME_SERIES)
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
      .valuationDate(LocalDate.of(2013, 1, 2))
      .dayCount(ACT_360)
      .fxMatrix(FX_MATRIX_GBP_USD)
      .discountCurves(GBP_USD_MULTI_CCY_MAP)
      .indexCurves(GBP_USD_MULTI_IND_MAP)
      .timeSeries(TIME_SERIES)
      .build();

}
