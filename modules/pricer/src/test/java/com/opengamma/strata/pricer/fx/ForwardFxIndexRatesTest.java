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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

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
  @Test
  public void test_of_withoutFixings() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES);
    assertThat(test.getIndex()).isEqualTo(GBP_USD_WM);
    assertThat(test.getValuationDate()).isEqualTo(DATE_VAL);
    assertThat(test.getFixings()).isEqualTo(SERIES_EMPTY);
    assertThat(test.getFxForwardRates()).isEqualTo(FWD_RATES);
    assertThat(test.findData(CURVE1.getName())).isEqualTo(Optional.of(CURVE1));
    assertThat(test.findData(CURVE2.getName())).isEqualTo(Optional.of(CURVE2));
    assertThat(test.findData(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.getParameterCount()).isEqualTo(FWD_RATES.getParameterCount());
    assertThat(test.getParameter(0)).isEqualTo(FWD_RATES.getParameter(0));
    assertThat(test.getParameterMetadata(0)).isEqualTo(FWD_RATES.getParameterMetadata(0));
    assertThat(test.withParameter(0, 1d).getFxForwardRates()).isEqualTo(FWD_RATES.withParameter(0, 1d));
    assertThat(test.withPerturbation((i, v, m) -> v + 1d).getFxForwardRates()).isEqualTo(FWD_RATES.withPerturbation((i, v, m) -> v + 1d));
  }

  @Test
  public void test_of_withFixings() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    assertThat(test.getIndex()).isEqualTo(GBP_USD_WM);
    assertThat(test.getValuationDate()).isEqualTo(DATE_VAL);
    assertThat(test.getFixings()).isEqualTo(SERIES);
    assertThat(test.getFxForwardRates()).isEqualTo(FWD_RATES);
  }

  @Test
  public void test_of_nonMatchingCurrency() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES_USD_GBP, SERIES));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES_EUR_GBP, SERIES));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_rate_beforeValuation_fixing() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    assertThat(test.rate(OBS_BEFORE, GBP)).isEqualTo(RATE_BEFORE);
    assertThat(test.rate(OBS_BEFORE, USD)).isEqualTo(1d / RATE_BEFORE);
  }

  @Test
  public void test_rate_beforeValuation_noFixing_emptySeries() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES_EMPTY);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.rate(OBS_BEFORE, GBP));
  }

  @Test
  public void test_rate_beforeValuation_noFixing_notEmptySeries() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES_MINIMAL);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.rate(OBS_BEFORE, GBP));
  }

  @Test
  public void test_rate_onValuation_fixing() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    assertThat(test.rate(OBS_VAL, GBP)).isEqualTo(RATE_VAL);
    assertThat(test.rate(OBS_VAL, USD)).isEqualTo(1d / RATE_VAL);
  }

  @Test
  public void test_rate_onValuation_noFixing() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES_EMPTY);
    LocalDate maturityDate = GBP_USD_WM.calculateMaturityFromFixing(DATE_VAL, REF_DATA);
    double dfCcyBaseAtMaturity = DFCURVE_GBP.discountFactor(maturityDate);
    double dfCcyCounterAtMaturity = DFCURVE_USD.discountFactor(maturityDate);
    double expected = FX_RATE.fxRate(GBP, USD) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
    assertThat(test.rate(OBS_VAL, GBP)).isCloseTo(expected, offset(1e-8));
    assertThat(test.rate(OBS_VAL, USD)).isCloseTo(1d / expected, offset(1e-8));
  }

  @Test
  public void test_rate_afterValuation() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    LocalDate maturityDate = GBP_USD_WM.calculateMaturityFromFixing(DATE_AFTER, REF_DATA);
    double dfCcyBaseAtMaturity = DFCURVE_GBP.discountFactor(maturityDate);
    double dfCcyCounterAtMaturity = DFCURVE_USD.discountFactor(maturityDate);
    double expected = FX_RATE.fxRate(GBP, USD) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
    assertThat(test.rate(OBS_AFTER, GBP)).isCloseTo(expected, offset(1e-8));
    assertThat(test.rate(OBS_AFTER, USD)).isCloseTo(1d / expected, offset(1e-8));
  }

  @Test
  public void test_rate_nonMatchingCurrency() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.rate(OBS_EUR_VAL, EUR));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ratePointSensitivity_fixing() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    assertThat(test.ratePointSensitivity(OBS_BEFORE, GBP)).isEqualTo(PointSensitivityBuilder.none());
    assertThat(test.ratePointSensitivity(OBS_VAL, GBP)).isEqualTo(PointSensitivityBuilder.none());
  }

  @Test
  public void test_ratePointSensitivity_onValuation_noFixing() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES_EMPTY);
    assertThat(test.ratePointSensitivity(OBS_VAL, GBP)).isEqualTo(FxIndexSensitivity.of(OBS_VAL, GBP, 1d));
    assertThat(test.ratePointSensitivity(OBS_VAL, USD)).isEqualTo(FxIndexSensitivity.of(OBS_VAL, USD, 1d));
  }

  @Test
  public void test_ratePointSensitivity_afterValuation() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    assertThat(test.ratePointSensitivity(OBS_AFTER, GBP)).isEqualTo(FxIndexSensitivity.of(OBS_AFTER, GBP, 1d));
    assertThat(test.ratePointSensitivity(OBS_AFTER, USD)).isEqualTo(FxIndexSensitivity.of(OBS_AFTER, USD, 1d));
  }

  @Test
  public void test_ratePointSensitivity_nonMatchingCurrency() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.ratePointSensitivity(OBS_EUR_VAL, EUR));
  }

  //-------------------------------------------------------------------------
  //proper end-to-end tests are elsewhere
  @Test
  public void test_parameterSensitivity() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    FxIndexSensitivity point = FxIndexSensitivity.of(OBS_VAL, GBP, 1d);
    assertThat(test.parameterSensitivity(point).size()).isEqualTo(2);
    FxIndexSensitivity point2 = FxIndexSensitivity.of(OBS_VAL, USD, 1d);
    assertThat(test.parameterSensitivity(point2).size()).isEqualTo(2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ForwardFxIndexRates test = ForwardFxIndexRates.of(GBP_USD_WM, FWD_RATES, SERIES);
    coverImmutableBean(test);
    ForwardFxIndexRates test2 = ForwardFxIndexRates.of(EUR_GBP_ECB, FWD_RATES_EUR_GBP, SERIES_MINIMAL);
    coverBeanEquals(test, test2);
  }

}
