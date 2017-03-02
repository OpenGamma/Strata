/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.FxIndices.EUR_GBP_ECB;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;

/**
 * Test {@link ForwardFxIndexRates}.
 */
@Test
public class ForwardFxIndexRatesTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_BEFORE = date(2015, 6, 3);
  private static final LocalDate DATE_AFTER = date(2015, 7, 30);

  private static final FxIndexObservation OBS_VAL = FxIndexObservation.of(GBP_USD_WM, DATE_VAL, REF_DATA);
  private static final FxIndexObservation OBS_BEFORE = FxIndexObservation.of(GBP_USD_WM, DATE_BEFORE, REF_DATA);
  private static final FxIndexObservation OBS_AFTER = FxIndexObservation.of(GBP_USD_WM, DATE_AFTER, REF_DATA);
  private static final FxIndexObservation OBS_EUR_VAL = FxIndexObservation.of(EUR_GBP_ECB, DATE_VAL, REF_DATA);

  private static final CurrencyPair PAIR_GBP_USD = CurrencyPair.of(GBP, USD);
  private static final CurrencyPair PAIR_USD_GBP = CurrencyPair.of(USD, GBP);
  private static final CurrencyPair PAIR_EUR_GBP = CurrencyPair.of(EUR, GBP);
  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final FxRate FX_RATE_EUR_GBP = FxRate.of(EUR, GBP, 0.7d);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveMetadata METADATA1 = Curves.zeroRates("TestCurve", ACT_365F);
  private static final CurveMetadata METADATA2 = Curves.zeroRates("TestCurveUSD", ACT_365F);
  private static final InterpolatedNodalCurve CURVE1 =
      InterpolatedNodalCurve.of(METADATA1, DoubleArray.of(0, 10), DoubleArray.of(0.01, 0.02), INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE2 =
      InterpolatedNodalCurve.of(METADATA2, DoubleArray.of(0, 10), DoubleArray.of(0.015, 0.025), INTERPOLATOR);
  private static final ZeroRateDiscountFactors DFCURVE_GBP = ZeroRateDiscountFactors.of(GBP, DATE_VAL, CURVE1);
  private static final ZeroRateDiscountFactors DFCURVE_USD = ZeroRateDiscountFactors.of(USD, DATE_VAL, CURVE2);
  private static final ZeroRateDiscountFactors DFCURVE_EUR = ZeroRateDiscountFactors.of(EUR, DATE_VAL, CURVE2);

  private static final double RATE_BEFORE = 0.013d;
  private static final double RATE_VAL = 0.014d;
  private static final LocalDateDoubleTimeSeries SERIES = LocalDateDoubleTimeSeries.builder()
      .put(DATE_BEFORE, RATE_BEFORE)
      .put(DATE_VAL, RATE_VAL)
      .build();
  private static final LocalDateDoubleTimeSeries SERIES_MINIMAL = LocalDateDoubleTimeSeries.of(DATE_VAL, RATE_VAL);
  private static final LocalDateDoubleTimeSeries SERIES_EMPTY = LocalDateDoubleTimeSeries.empty();

  private static final FxForwardRates FWD_RATES = DiscountFxForwardRates.of(PAIR_GBP_USD, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
  private static final FxForwardRates FWD_RATES_USD_GBP =
      DiscountFxForwardRates.of(PAIR_USD_GBP, FX_RATE.inverse(), DFCURVE_USD, DFCURVE_GBP);
  private static final FxForwardRates FWD_RATES_EUR_GBP =
      DiscountFxForwardRates.of(PAIR_EUR_GBP, FX_RATE_EUR_GBP, DFCURVE_EUR, DFCURVE_GBP);

  //-------------------------------------------------------------------------
  public void test_of_withoutFixings() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES);
    assertEquals(test.getIndex(), GBP_USD_WM);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getFixings(), SERIES_EMPTY);
    assertEquals(test.getFxForwardRates(), FWD_RATES);
    assertEquals(test.findData(CURVE1.getName()), Optional.of(CURVE1));
    assertEquals(test.findData(CURVE2.getName()), Optional.of(CURVE2));
    assertEquals(test.findData(CurveName.of("Rubbish")), Optional.empty());
    assertEquals(test.getParameterCount(), FWD_RATES.getParameterCount());
    assertEquals(test.getParameter(0), FWD_RATES.getParameter(0));
    assertEquals(test.getParameterMetadata(0), FWD_RATES.getParameterMetadata(0));
    assertEquals(test.withParameter(0, 1d).getFxForwardRates(), FWD_RATES.withParameter(0, 1d));
    assertEquals(
        test.withPerturbation((i, v, m) -> v + 1d).getFxForwardRates(),
        FWD_RATES.withPerturbation((i, v, m) -> v + 1d));
  }

  public void test_of_withFixings() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    assertEquals(test.getIndex(), GBP_USD_WM);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getFixings(), SERIES);
    assertEquals(test.getFxForwardRates(), FWD_RATES);
  }

  public void test_of_nonMatchingCurrency() {
    assertThrowsIllegalArg(() -> ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES_USD_GBP, SERIES));
    assertThrowsIllegalArg(() -> ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES_EUR_GBP, SERIES));
  }

  //-------------------------------------------------------------------------
  public void test_rate_beforeValuation_fixing() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    assertEquals(test.rate(OBS_BEFORE, GBP), RATE_BEFORE);
    assertEquals(test.rate(OBS_BEFORE, USD), 1d / RATE_BEFORE);
  }

  public void test_rate_beforeValuation_noFixing_emptySeries() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES_EMPTY);
    assertThrowsIllegalArg(() -> test.rate(OBS_BEFORE, GBP));
  }

  public void test_rate_beforeValuation_noFixing_notEmptySeries() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES_MINIMAL);
    assertThrowsIllegalArg(() -> test.rate(OBS_BEFORE, GBP));
  }

  public void test_rate_onValuation_fixing() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    assertEquals(test.rate(OBS_VAL, GBP), RATE_VAL);
    assertEquals(test.rate(OBS_VAL, USD), 1d / RATE_VAL);
  }

  public void test_rate_onValuation_noFixing() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES_EMPTY);
    LocalDate maturityDate = GBP_USD_WM.calculateMaturityFromFixing(DATE_VAL, REF_DATA);
    double dfCcyBaseAtMaturity = DFCURVE_GBP.discountFactor(maturityDate);
    double dfCcyCounterAtMaturity = DFCURVE_USD.discountFactor(maturityDate);
    double expected = FX_RATE.fxRate(GBP, USD) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
    assertEquals(test.rate(OBS_VAL, GBP), expected, 1e-8);
    assertEquals(test.rate(OBS_VAL, USD), 1d / expected, 1e-8);
  }

  public void test_rate_afterValuation() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    LocalDate maturityDate = GBP_USD_WM.calculateMaturityFromFixing(DATE_AFTER, REF_DATA);
    double dfCcyBaseAtMaturity = DFCURVE_GBP.discountFactor(maturityDate);
    double dfCcyCounterAtMaturity = DFCURVE_USD.discountFactor(maturityDate);
    double expected = FX_RATE.fxRate(GBP, USD) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
    assertEquals(test.rate(OBS_AFTER, GBP), expected, 1e-8);
    assertEquals(test.rate(OBS_AFTER, USD), 1d / expected, 1e-8);
  }

  public void test_rate_nonMatchingCurrency() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    assertThrowsIllegalArg(() -> test.rate(OBS_EUR_VAL, EUR));
  }

  //-------------------------------------------------------------------------
  public void test_ratePointSensitivity_fixing() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    assertEquals(test.ratePointSensitivity(OBS_BEFORE, GBP), PointSensitivityBuilder.none());
    assertEquals(test.ratePointSensitivity(OBS_VAL, GBP), PointSensitivityBuilder.none());
  }

  public void test_ratePointSensitivity_onValuation_noFixing() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES_EMPTY);
    assertEquals(test.ratePointSensitivity(OBS_VAL, GBP), FxIndexSensitivity.of(OBS_VAL, GBP, 1d));
    assertEquals(test.ratePointSensitivity(OBS_VAL, USD), FxIndexSensitivity.of(OBS_VAL, USD, 1d));
  }

  public void test_ratePointSensitivity_afterValuation() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    assertEquals(test.ratePointSensitivity(OBS_AFTER, GBP), FxIndexSensitivity.of(OBS_AFTER, GBP, 1d));
    assertEquals(test.ratePointSensitivity(OBS_AFTER, USD), FxIndexSensitivity.of(OBS_AFTER, USD, 1d));
  }

  public void test_ratePointSensitivity_nonMatchingCurrency() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    assertThrowsIllegalArg(() -> test.ratePointSensitivity(OBS_EUR_VAL, EUR));
  }

  //-------------------------------------------------------------------------
  //proper end-to-end tests are elsewhere
  public void test_parameterSensitivity() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    FxIndexSensitivity point = FxIndexSensitivity.of(OBS_VAL, GBP, 1d);
    assertEquals(test.parameterSensitivity(point).size(), 2);
    FxIndexSensitivity point2 = FxIndexSensitivity.of(OBS_VAL, USD, 1d);
    assertEquals(test.parameterSensitivity(point2).size(), 2);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    coverImmutableBean(test);
    ForwardFxIndexRates test2 = ForwardFxIndexRates.of(EUR_GBP_ECB, FWD_RATES_EUR_GBP, SERIES_MINIMAL);
    coverBeanEquals(test, test2);
  }

}
