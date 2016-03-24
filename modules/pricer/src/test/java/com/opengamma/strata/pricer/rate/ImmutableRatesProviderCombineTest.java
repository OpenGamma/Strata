/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.interpolator.CurveInterpolator;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.view.DiscountFactors;
import com.opengamma.strata.market.view.ForwardPriceIndexValues;
import com.opengamma.strata.market.view.IborIndexRates;
import com.opengamma.strata.market.view.OvernightIndexRates;
import com.opengamma.strata.market.view.PriceIndexValues;

/**
 * Tests {@link ImmutableRatesProvider}.
 */
@Test
public class ImmutableRatesProviderCombineTest {

  private static final LocalDate PREV_DATE = LocalDate.of(2014, 6, 27);
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 6, 30);
  private static final double FX_GBP_USD = 1.6d;
  private static final FxMatrix FX_MATRIX = FxMatrix.of(GBP, USD, FX_GBP_USD);
  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;

  private static final double GBP_DSC = 0.99d;
  private static final double USD_DSC = 0.95d;
  private static final Curve DISCOUNT_CURVE_GBP = ConstantNodalCurve.of(
      Curves.zeroRates("GBP-DSCON", ACT_ACT_ISDA), GBP_DSC);
  private static final Curve DISCOUNT_CURVE_USD = ConstantNodalCurve.of(
      Curves.zeroRates("USD-DSCON", ACT_ACT_ISDA), USD_DSC);
  private static final Curve USD_LIBOR_CURVE = ConstantNodalCurve.of(
      Curves.zeroRates("USD-LIBOR-3M", ACT_ACT_ISDA), 0.96d);
  private static final Curve FED_FUND_CURVE = ConstantNodalCurve.of(
      Curves.zeroRates("USD-FED_FUND", ACT_ACT_ISDA), 0.97d);
  private static final PriceIndexValues GBPRI_CURVE = ForwardPriceIndexValues.of(
      GB_RPI,
      VAL_DATE,
      InterpolatedNodalCurve.of(
          Curves.prices("GB-RPI"), DoubleArray.of(1d, 10d), DoubleArray.of(252d, 252d), INTERPOLATOR),
      LocalDateDoubleTimeSeries.of(date(2013, 11, 30), 252));
  private static final LocalDateDoubleTimeSeries TS = LocalDateDoubleTimeSeries.of(PREV_DATE, 0.62d);

  public void merge_content_2() {
    ImmutableRatesProvider test1 = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .timeSeries(GBP_USD_WM, TS)
        .build();
    ImmutableRatesProvider test2 = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .iborIndexCurve(USD_LIBOR_3M, USD_LIBOR_CURVE)
        .overnightIndexCurve(USD_FED_FUND, FED_FUND_CURVE)
        .priceIndexValues(GBPRI_CURVE)
        .build();
    ImmutableRatesProvider merged = ImmutableRatesProvider.combined(FX_MATRIX, test1, test2);
    assertEquals(merged.getValuationDate(), VAL_DATE);
    assertEquals(merged.discountFactors(USD), DiscountFactors.of(USD, VAL_DATE, DISCOUNT_CURVE_USD));
    assertEquals(merged.discountFactors(GBP), DiscountFactors.of(GBP, VAL_DATE, DISCOUNT_CURVE_GBP));
    assertEquals(merged.iborIndexRates(USD_LIBOR_3M), IborIndexRates.of(USD_LIBOR_3M, VAL_DATE, USD_LIBOR_CURVE));
    assertEquals(merged.overnightIndexRates(USD_FED_FUND), OvernightIndexRates.of(USD_FED_FUND, VAL_DATE, FED_FUND_CURVE));
    assertEquals(merged.priceIndexValues(GB_RPI), GBPRI_CURVE);
    assertEquals(merged.timeSeries(GBP_USD_WM), TS);
    assertEquals(merged.getFxRateProvider(), FX_MATRIX);
  }

  public void merge_content_3() {
    ImmutableRatesProvider test1 = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .timeSeries(GBP_USD_WM, TS)
        .build();
    ImmutableRatesProvider test2 = ImmutableRatesProvider.builder(VAL_DATE)
        .iborIndexCurve(USD_LIBOR_3M, USD_LIBOR_CURVE)
        .overnightIndexCurve(USD_FED_FUND, FED_FUND_CURVE)
        .build();
    ImmutableRatesProvider test3 = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .priceIndexValues(GBPRI_CURVE)
        .build();
    ImmutableRatesProvider merged = ImmutableRatesProvider.combined(FX_MATRIX, test1, test2, test3);
    assertEquals(merged.getValuationDate(), VAL_DATE);
    assertEquals(merged.discountFactors(USD), DiscountFactors.of(USD, VAL_DATE, DISCOUNT_CURVE_USD));
    assertEquals(merged.discountFactors(GBP), DiscountFactors.of(GBP, VAL_DATE, DISCOUNT_CURVE_GBP));
    assertEquals(merged.iborIndexRates(USD_LIBOR_3M), IborIndexRates.of(USD_LIBOR_3M, VAL_DATE, USD_LIBOR_CURVE));
    assertEquals(merged.overnightIndexRates(USD_FED_FUND), OvernightIndexRates.of(USD_FED_FUND, VAL_DATE, FED_FUND_CURVE));
    assertEquals(merged.priceIndexValues(GB_RPI), GBPRI_CURVE);
    assertEquals(merged.timeSeries(GBP_USD_WM), TS);
    assertEquals(merged.getFxRateProvider(), FX_MATRIX);
  }

  public void merge_illegal_arguments() {
    ImmutableRatesProvider test_dsc = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .build();
    ImmutableRatesProvider test_ts = ImmutableRatesProvider.builder(VAL_DATE)
        .timeSeries(GBP_USD_WM, TS)
        .build();
    ImmutableRatesProvider test_ibor = ImmutableRatesProvider.builder(VAL_DATE)
        .iborIndexCurve(USD_LIBOR_3M, USD_LIBOR_CURVE)
        .build();
    ImmutableRatesProvider test_on = ImmutableRatesProvider.builder(VAL_DATE)
        .overnightIndexCurve(USD_FED_FUND, FED_FUND_CURVE)
        .build();
    ImmutableRatesProvider test_pi = ImmutableRatesProvider.builder(VAL_DATE)
        .priceIndexValues(GBPRI_CURVE)
        .build();
    assertThrowsIllegalArg(() -> ImmutableRatesProvider.combined(FX_MATRIX, test_dsc, test_dsc));
    assertThrowsIllegalArg(() -> ImmutableRatesProvider.combined(FX_MATRIX, test_ts, test_ts));
    assertThrowsIllegalArg(() -> ImmutableRatesProvider.combined(FX_MATRIX, test_ibor, test_ibor));
    assertThrowsIllegalArg(() -> ImmutableRatesProvider.combined(FX_MATRIX, test_on, test_on));
    assertThrowsIllegalArg(() -> ImmutableRatesProvider.combined(FX_MATRIX, test_pi, test_pi));
  }

}
