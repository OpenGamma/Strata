/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.datasets;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_EONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * RatesProvider data sets for testing.
 */
public class RatesProviderDataSets {

  /** Wednesday. */
  public static final LocalDate VAL_DATE_2014_01_22 = LocalDate.of(2014, 1, 22);
  public static final LocalDate VAL_DATE_END_OF_MONTH = LocalDate.of(2014, 1, 31);

  public static final DoubleArray TIMES_1 = DoubleArray.of(
      0.01, 0.25, 0.50, 1.0, 2.0, 3.0, 5.0, 7.0, 10.0, 30.0); // 10 nodes
  public static final DoubleArray TIMES_2 = DoubleArray.of(
      0.25, 0.50, 1.0, 2.0, 3.0, 5.0, 7.0, 10.0, 30.0); // 9 nodes
  public static final DoubleArray TIMES_3 = DoubleArray.of(
      0.50, 1.0, 2.0, 3.0, 5.0, 7.0, 10.0, 30.0); // 8 nodes
  public static final DoubleArray TIMES_4 = DoubleArray.of(
      10.0, 22.0, 34.0, 58.0, 82.0, 118.0, 178.0); // 7 nodes
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
  public static final DoubleArray RATES_1_3 = DoubleArray.of(
      0.0150, 0.0160, 0.0170, 0.0180, 0.0190, 0.0200, 0.0210, 0.0220, 0.0230, 0.0240);
  public static final DoubleArray RATES_2_3 = DoubleArray.of(
      0.0170, 0.0180, 0.0190, 0.0200, 0.0210, 0.0220, 0.0230, 0.0240, 0.0250);
  public static final DoubleArray RATES_3_3 = DoubleArray.of(
      0.0190, 0.0200, 0.0210, 0.0220, 0.0230, 0.0240, 0.0250, 0.0260);
  public static final DoubleArray VALUES_4 = DoubleArray.of(
      1.98, 2.05, 2.14, 2.28, 2.42, 2.57, 2.88); // small values for testing purposes
  public static final DoubleArray RATES_1_1_SIMPLE;
  public static final DoubleArray RATES_2_1_SIMPLE;
  public static final DoubleArray RATES_3_1_SIMPLE;
  public static final DoubleArray RATES_1_2_SIMPLE;
  public static final DoubleArray RATES_2_2_SIMPLE;
  public static final DoubleArray RATES_3_2_SIMPLE;
  public static final DoubleArray RATES_1_3_SIMPLE;
  public static final DoubleArray RATES_2_3_SIMPLE;
  public static final DoubleArray RATES_3_3_SIMPLE;
  static {
    double[] simple11 = new double[TIMES_1.size()];
    double[] simple12 = new double[TIMES_1.size()];
    double[] simple13 = new double[TIMES_1.size()];
    double[] simple21 = new double[TIMES_2.size()];
    double[] simple22 = new double[TIMES_2.size()];
    double[] simple23 = new double[TIMES_2.size()];
    double[] simple31 = new double[TIMES_3.size()];
    double[] simple32 = new double[TIMES_3.size()];
    double[] simple33 = new double[TIMES_3.size()];
    for (int i = 0; i < TIMES_1.size(); ++i) {
      simple11[i] = Math.exp(-TIMES_1.get(i) * RATES_1_1.get(i));
      simple12[i] = Math.exp(-TIMES_1.get(i) * RATES_1_2.get(i));
      simple13[i] = Math.exp(-TIMES_1.get(i) * RATES_1_3.get(i));
    }
    for (int i = 0; i < TIMES_2.size(); ++i) {
      simple21[i] = Math.exp(-TIMES_2.get(i) * RATES_2_1.get(i));
      simple22[i] = Math.exp(-TIMES_2.get(i) * RATES_2_2.get(i));
      simple23[i] = Math.exp(-TIMES_2.get(i) * RATES_2_3.get(i));
    }
    for (int i = 0; i < TIMES_3.size(); ++i) {
      simple31[i] = Math.exp(-TIMES_3.get(i) * RATES_3_1.get(i));
      simple32[i] = Math.exp(-TIMES_3.get(i) * RATES_3_2.get(i));
      simple33[i] = Math.exp(-TIMES_3.get(i) * RATES_3_3.get(i));
    }
    RATES_1_1_SIMPLE = DoubleArray.copyOf(simple11);
    RATES_1_2_SIMPLE = DoubleArray.copyOf(simple12);
    RATES_1_3_SIMPLE = DoubleArray.copyOf(simple13);
    RATES_2_1_SIMPLE = DoubleArray.copyOf(simple21);
    RATES_2_2_SIMPLE = DoubleArray.copyOf(simple22);
    RATES_2_3_SIMPLE = DoubleArray.copyOf(simple23);
    RATES_3_1_SIMPLE = DoubleArray.copyOf(simple31);
    RATES_3_2_SIMPLE = DoubleArray.copyOf(simple32);
    RATES_3_3_SIMPLE = DoubleArray.copyOf(simple33);
  }

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
  public static final CurveName USD_CPI_NAME = CurveName.of("US-CPI-U");
  private static final CurveMetadata USD_SINGLE_METADATA = Curves.zeroRates(USD_SINGLE_NAME, ACT_360);
  private static final CurveMetadata USD_DSC_METADATA = Curves.zeroRates(USD_DSC_NAME, ACT_360);
  private static final CurveMetadata USD_L3_METADATA = Curves.zeroRates(USD_L3_NAME, ACT_360);
  private static final CurveMetadata USD_L6_METADATA = Curves.zeroRates(USD_L6_NAME, ACT_360);
  private static final CurveMetadata USD_DSC_METADATA_SIMPLE = Curves.discountFactors(USD_DSC_NAME, ACT_360);
  private static final CurveMetadata USD_L3_METADATA_SIMPLE = Curves.discountFactors(USD_L3_NAME, ACT_360);
  private static final CurveMetadata USD_L6_METADATA_SIMPLE = Curves.discountFactors(USD_L6_NAME, ACT_360);
  private static final CurveMetadata PRICE_INDEX_METADATA = Curves.prices(USD_CPI_NAME);
  private static final Curve USD_SINGLE_CURVE =
      InterpolatedNodalCurve.of(USD_SINGLE_METADATA, TIMES_1, RATES_1_1, INTERPOLATOR);
  private static final LocalDateDoubleTimeSeries PRICE_INDEX_TS =
      LocalDateDoubleTimeSeries.of(VAL_DATE_END_OF_MONTH, 193.0);

  public static final ImmutableRatesProvider SINGLE_USD = singleUsd(VAL_DATE_2014_01_22);

  public static final ImmutableRatesProvider singleUsd(LocalDate valDate) {
    return ImmutableRatesProvider.builder(valDate)
        .fxRateProvider(FX_MATRIX_USD)
        .discountCurve(USD, USD_SINGLE_CURVE)
        .overnightIndexCurve(USD_FED_FUND, USD_SINGLE_CURVE)
        .iborIndexCurve(USD_LIBOR_3M, USD_SINGLE_CURVE)
        .iborIndexCurve(USD_LIBOR_6M, USD_SINGLE_CURVE)
        .build();
  }

  //-------------------------------------------------------------------------
  private static final Curve USD_DSC =
      InterpolatedNodalCurve.of(USD_DSC_METADATA, TIMES_1, RATES_1_1, INTERPOLATOR);
  private static final Curve USD_L3 =
      InterpolatedNodalCurve.of(USD_L3_METADATA, TIMES_2, RATES_2_1, INTERPOLATOR);
  private static final Curve USD_L6 =
      InterpolatedNodalCurve.of(USD_L6_METADATA, TIMES_3, RATES_3_1, INTERPOLATOR);
  private static final Curve USD_DSC_SIMPLE =
      InterpolatedNodalCurve.of(USD_DSC_METADATA_SIMPLE, TIMES_1, RATES_1_1_SIMPLE, INTERPOLATOR);
  private static final Curve USD_L3_SIMPLE =
      InterpolatedNodalCurve.of(USD_L3_METADATA_SIMPLE, TIMES_2, RATES_2_1_SIMPLE, INTERPOLATOR);
  private static final Curve USD_L6_SIMPLE =
      InterpolatedNodalCurve.of(USD_L6_METADATA_SIMPLE, TIMES_3, RATES_3_1_SIMPLE, INTERPOLATOR);
  private static final InterpolatedNodalCurve US_CPI_U_CURVE =
      InterpolatedNodalCurve.of(PRICE_INDEX_METADATA, TIMES_4, VALUES_4, INTERPOLATOR);

  public static final ImmutableRatesProvider MULTI_USD = multiUsd(VAL_DATE_2014_01_22);

  public static final ImmutableRatesProvider multiUsd(LocalDate valDate) {
    return ImmutableRatesProvider.builder(valDate)
        .fxRateProvider(FX_MATRIX_USD)
        .discountCurve(USD, USD_DSC)
        .overnightIndexCurve(USD_FED_FUND, USD_DSC)
        .iborIndexCurve(USD_LIBOR_3M, USD_L3)
        .iborIndexCurve(USD_LIBOR_6M, USD_L6)
        .build();
  }

  public static final ImmutableRatesProvider MULTI_CPI_USD = ImmutableRatesProvider.builder(VAL_DATE_2014_01_22)
      .fxRateProvider(FX_MATRIX_USD)
      .discountCurve(USD, USD_DSC)
      .overnightIndexCurve(USD_FED_FUND, USD_DSC)
      .iborIndexCurve(USD_LIBOR_3M, USD_L3)
      .iborIndexCurve(USD_LIBOR_6M, USD_L6)
      .priceIndexCurve(US_CPI_U, US_CPI_U_CURVE)
      .timeSeries(US_CPI_U, PRICE_INDEX_TS)
      .build();

  //-------------------------------------------------------------------------
  //     =====     GBP     =====     

  private static final FxMatrix FX_MATRIX_GBP =
      FxMatrix.builder().addRate(GBP, GBP, 1.00).build();

  public static final CurveName GBP_DSC_NAME = CurveName.of("GBP-DSCON");
  public static final CurveName GBP_L3_NAME = CurveName.of("GBP-LIBOR3M");
  public static final CurveName GBP_L6_NAME = CurveName.of("GBP-LIBOR6M");
  private static final CurveMetadata GBP_DSC_METADATA = Curves.zeroRates(GBP_DSC_NAME, ACT_360);
  private static final CurveMetadata GBP_L3_METADATA = Curves.zeroRates(GBP_L3_NAME, ACT_360);
  private static final CurveMetadata GBP_L6_METADATA = Curves.zeroRates(GBP_L6_NAME, ACT_360);
  private static final CurveMetadata GBP_DSC_METADATA_SIMPLE = Curves.discountFactors(GBP_DSC_NAME, ACT_360);
  private static final CurveMetadata GBP_L3_METADATA_SIMPLE = Curves.discountFactors(GBP_L3_NAME, ACT_360);
  private static final CurveMetadata GBP_L6_METADATA_SIMPLE = Curves.discountFactors(GBP_L6_NAME, ACT_360);

  private static final Curve GBP_DSC =
      InterpolatedNodalCurve.of(GBP_DSC_METADATA, TIMES_1, RATES_1_2, INTERPOLATOR);
  private static final Curve GBP_L3 =
      InterpolatedNodalCurve.of(GBP_L3_METADATA, TIMES_2, RATES_2_2, INTERPOLATOR);
  private static final Curve GBP_L6 =
      InterpolatedNodalCurve.of(GBP_L6_METADATA, TIMES_3, RATES_3_2, INTERPOLATOR);
  private static final Curve GBP_DSC_SIMPLE =
      InterpolatedNodalCurve.of(GBP_DSC_METADATA_SIMPLE, TIMES_1, RATES_1_2_SIMPLE, INTERPOLATOR);
  private static final Curve GBP_L3_SIMPLE =
      InterpolatedNodalCurve.of(GBP_L3_METADATA_SIMPLE, TIMES_2, RATES_2_2_SIMPLE, INTERPOLATOR);
  private static final Curve GBP_L6_SIMPLE =
      InterpolatedNodalCurve.of(GBP_L6_METADATA_SIMPLE, TIMES_3, RATES_3_2_SIMPLE, INTERPOLATOR);

  public static final ImmutableRatesProvider MULTI_GBP = multiGbp(VAL_DATE_2014_01_22);

  public static final ImmutableRatesProvider multiGbp(LocalDate valDate) {
    return ImmutableRatesProvider.builder(valDate)
        .fxRateProvider(FX_MATRIX_GBP)
        .discountCurve(GBP, GBP_DSC)
        .overnightIndexCurve(GBP_SONIA, GBP_DSC)
        .iborIndexCurve(GBP_LIBOR_3M, GBP_L3)
        .iborIndexCurve(GBP_LIBOR_6M, GBP_L6)
        .build();
  }

  //-------------------------------------------------------------------------
  //     =====     EUR     =====     

  private static final FxMatrix FX_MATRIX_EUR =
      FxMatrix.builder().addRate(EUR, EUR, 1.00).build();

  public static final CurveName EUR_DSC_NAME = CurveName.of("EUR-DSCON");
  public static final CurveName EUR_L3_NAME = CurveName.of("EUR-LIBOR3M");
  public static final CurveName EUR_L6_NAME = CurveName.of("EUR-LIBOR6M");
  private static final CurveMetadata EUR_DSC_METADATA = Curves.zeroRates(EUR_DSC_NAME, ACT_360);
  private static final CurveMetadata EUR_L3_METADATA = Curves.zeroRates(EUR_L3_NAME, ACT_360);
  private static final CurveMetadata EUR_L6_METADATA = Curves.zeroRates(EUR_L6_NAME, ACT_360);

  private static final Curve EUR_DSC =
      InterpolatedNodalCurve.of(EUR_DSC_METADATA, TIMES_1, RATES_1_2, INTERPOLATOR);
  private static final Curve EUR_L3 =
      InterpolatedNodalCurve.of(EUR_L3_METADATA, TIMES_2, RATES_2_2, INTERPOLATOR);
  private static final Curve EUR_L6 =
      InterpolatedNodalCurve.of(EUR_L6_METADATA, TIMES_3, RATES_3_2, INTERPOLATOR);

  public static final ImmutableRatesProvider MULTI_EUR = multiEur(VAL_DATE_2014_01_22);

  public static final ImmutableRatesProvider multiEur(LocalDate valDate) {
    return ImmutableRatesProvider.builder(valDate)
        .fxRateProvider(FX_MATRIX_EUR)
        .discountCurve(EUR, EUR_DSC)
        .overnightIndexCurve(EUR_EONIA, EUR_DSC)
        .iborIndexCurve(EUR_EURIBOR_3M, EUR_L3)
        .iborIndexCurve(EUR_EURIBOR_6M, EUR_L6)
        .build();
  }

  //-------------------------------------------------------------------------
  //     =====     GBP + USD      =====        

  public static final FxMatrix FX_MATRIX_GBP_USD =
      FxMatrix.builder().addRate(GBP, USD, 1.50).build();

  // zero rate curves
  public static final ImmutableRatesProvider MULTI_GBP_USD = multiGbpUsd(VAL_DATE_2014_01_22);

  public static final ImmutableRatesProvider multiGbpUsd(LocalDate valDate) {
    return ImmutableRatesProvider.builder(valDate)
        .fxRateProvider(FX_MATRIX_GBP_USD)
        .discountCurve(GBP, GBP_DSC)
        .discountCurve(USD, USD_DSC)
        .overnightIndexCurve(GBP_SONIA, GBP_DSC)
        .iborIndexCurve(GBP_LIBOR_3M, GBP_L3)
        .iborIndexCurve(GBP_LIBOR_6M, GBP_L6)
        .overnightIndexCurve(USD_FED_FUND, USD_DSC)
        .iborIndexCurve(USD_LIBOR_3M, USD_L3)
        .iborIndexCurve(USD_LIBOR_6M, USD_L6)
        .build();
  }

  // discount factor curves
  public static final ImmutableRatesProvider MULTI_GBP_USD_SIMPLE = multiGbpUsdSimple(VAL_DATE_2014_01_22);

  public static final ImmutableRatesProvider multiGbpUsdSimple(LocalDate valDate) {
    return ImmutableRatesProvider.builder(valDate)
        .fxRateProvider(FX_MATRIX_GBP_USD)
        .discountCurve(GBP, GBP_DSC_SIMPLE)
        .discountCurve(USD, USD_DSC_SIMPLE)
        .overnightIndexCurve(GBP_SONIA, GBP_DSC_SIMPLE)
        .iborIndexCurve(GBP_LIBOR_3M, GBP_L3_SIMPLE)
        .iborIndexCurve(GBP_LIBOR_6M, GBP_L6_SIMPLE)
        .overnightIndexCurve(USD_FED_FUND, USD_DSC_SIMPLE)
        .iborIndexCurve(USD_LIBOR_3M, USD_L3_SIMPLE)
        .iborIndexCurve(USD_LIBOR_6M, USD_L6_SIMPLE)
        .build();
  }

}
