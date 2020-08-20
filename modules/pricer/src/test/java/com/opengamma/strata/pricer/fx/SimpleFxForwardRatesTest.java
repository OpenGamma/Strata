/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.impl.swap.DiscountingRatePaymentPeriodPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.swap.FxReset;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;

/**
 * Test {@link SimpleFxForwardRates}.
 */
public class SimpleFxForwardRatesTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_REF = date(2015, 7, 30);
  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.25d);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(GBP, USD);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveMetadata METADATA1 = Curves.forwardRates("TestCurve", ACT_365F);
  private static final CurveMetadata METADATA2 = Curves.forwardRates("TestCurve2", ACT_365F);
  private static final InterpolatedNodalCurve CURVE1 =
      InterpolatedNodalCurve.of(METADATA1, DoubleArray.of(0, 10), DoubleArray.of(1.2, 1.3), INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE2 =
      InterpolatedNodalCurve.of(METADATA2, DoubleArray.of(0, 10), DoubleArray.of(1.2, 1.4), INTERPOLATOR);
  private static final RatesProvider PROVIDER = ImmutableRatesProvider.builder(DATE_VAL)
      .indexCurve(GBP_USD_WM, CURVE1)
      .fxRateProvider(FX_RATE).build();
  private static final DiscountingRatePaymentPeriodPricer PERIOD_PRICER = DiscountingRatePaymentPeriodPricer.DEFAULT;
  private static final double TOLERANCE = 1e-6;

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    SimpleFxForwardRates test = SimpleFxForwardRates.of(GBP_USD_WM, DATE_VAL, CURVE1);
    assertThat(test.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(test.getValuationDate()).isEqualTo(DATE_VAL);
    assertThat(test.getIndex()).isEqualTo(GBP_USD_WM);
    assertThat(test.getCurve()).isEqualTo(CURVE1);
    assertThat(test.getFixings()).isEqualTo(LocalDateDoubleTimeSeries.empty());
    assertThat(test.findData(CURVE1.getName())).hasValue(CURVE1);
    assertThat(test.findData(CURVE2.getName())).isEmpty();

    assertThat(test.getParameterCount()).isEqualTo(CURVE1.getParameterCount());
    assertThat(test.getParameter(0)).isEqualTo(CURVE1.getParameter(0));
    assertThat(test.getParameterMetadata(0)).isEqualTo(CURVE1.getParameterMetadata(0));
    assertThat(test.withParameter(0, 1d).getCurve()).isEqualTo(CURVE1.withParameter(0, 1d));
    assertThat(test.withPerturbation((i, v, m) -> v + 1d).getCurve())
        .isEqualTo(CURVE1.withPerturbation((i, v, m) -> v + 1d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_rate() {
    SimpleFxForwardRates test = SimpleFxForwardRates.of(GBP_USD_WM, DATE_VAL, CURVE1);
    double expected = CURVE1.yValue(ACT_365F.relativeYearFraction(DATE_VAL, DATE_REF));
    assertThat(test.rate(GBP, DATE_REF)).isCloseTo(expected, offset(1e-12));
    assertThat(test.rate(USD, DATE_REF)).isCloseTo(1d / expected, offset(1e-12));
  }

  @Test
  public void test_rate_nonMatchingCurrency() {
    SimpleFxForwardRates test = SimpleFxForwardRates.of(GBP_USD_WM, DATE_VAL, CURVE1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.rate(EUR, DATE_VAL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ratePointSensitivity() {
    SimpleFxForwardRates test = SimpleFxForwardRates.of(GBP_USD_WM, DATE_VAL, CURVE1);
    assertThat(test.ratePointSensitivity(GBP, DATE_REF)).isEqualTo(FxForwardSensitivity.of(CURRENCY_PAIR, GBP, DATE_REF, 1d));
    assertThat(test.ratePointSensitivity(USD, DATE_REF)).isEqualTo(FxForwardSensitivity.of(CURRENCY_PAIR, USD, DATE_REF, 1d));
  }

  @Test
  public void test_ratePointSensitivity_nonMatchingCurrency() {
    SimpleFxForwardRates test = SimpleFxForwardRates.of(GBP_USD_WM, DATE_VAL, CURVE1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.ratePointSensitivity(EUR, DATE_VAL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_rateFxSpotSensitivity() {
    SimpleFxForwardRates test = SimpleFxForwardRates.of(GBP_USD_WM, DATE_VAL, CURVE1);
    assertThat(test.rateFxSpotSensitivity(GBP, DATE_REF)).isZero();
    assertThat(test.rateFxSpotSensitivity(USD, DATE_REF)).isZero();
  }

  @Test
  public void test_rateFxSpotSensitivity_nonMatchingCurrency() {
    SimpleFxForwardRates test = SimpleFxForwardRates.of(GBP_USD_WM, DATE_VAL, CURVE1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.rateFxSpotSensitivity(EUR, DATE_VAL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parameterSensitivity() {
    SimpleFxForwardRates test = SimpleFxForwardRates.of(GBP_USD_WM, DATE_VAL, CURVE1);
    FxForwardSensitivity point = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, DATE_VAL, 1d);
    assertThat(test.parameterSensitivity(point).size()).isEqualTo(1);
    FxForwardSensitivity point2 = FxForwardSensitivity.of(CURRENCY_PAIR, USD, DATE_VAL, 1d);
    assertThat(test.parameterSensitivity(point2).size()).isEqualTo(1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void currency_exposure_GBP() {
    LocalDate startDate = LocalDate.of(2016, 8, 2);
    LocalDate fixingDate = LocalDate.of(2016, 11, 2);
    LocalDate endDate = LocalDate.of(2016, 11, 4);
    double yearFraction = 0.25;
    double rate = 0.10;
    RateAccrualPeriod accrual = RateAccrualPeriod.builder().startDate(startDate)
        .endDate(endDate).yearFraction(yearFraction).rateComputation(FixedRateComputation.of(rate)).build();
    double notional = 1000000;
    RatePaymentPeriod fixedFx = RatePaymentPeriod.builder()
        .accrualPeriods(accrual)
        .fxReset(FxReset.of(FxIndexObservation.of(FxIndices.GBP_USD_WM, fixingDate, REF_DATA), GBP))
        .notional(notional)
        .paymentDate(endDate)
        .dayCount(DayCounts.ONE_ONE)
        .currency(USD).build(); // 1_000_000 GBP paid in USD at maturity
    PointSensitivityBuilder pts = PERIOD_PRICER.presentValueSensitivity(fixedFx, PROVIDER);
    MultiCurrencyAmount ceComputed = PERIOD_PRICER.currencyExposure(fixedFx, PROVIDER);
    double dfGbp = PROVIDER.discountFactor(GBP, endDate);
    double ceGbpExpected = notional * yearFraction * rate * dfGbp;
    assertThat(ceComputed.getAmount(GBP).getAmount()).isCloseTo(ceGbpExpected, offset(1.0E-6));
    MultiCurrencyAmount ceWithoutPvComputed = PROVIDER.currencyExposure(pts.build().convertedTo(GBP, PROVIDER));
    CurrencyAmount pvComputed = CurrencyAmount.of(USD, PERIOD_PRICER.presentValue(fixedFx, PROVIDER));
    MultiCurrencyAmount ceComputed2 = ceWithoutPvComputed.plus(pvComputed);
    assertThat(ceComputed2.getAmount(GBP).getAmount()).isCloseTo(ceGbpExpected, offset(TOLERANCE));
    assertThat(ceComputed2.getAmount(USD).getAmount()).isCloseTo(0.0, offset(TOLERANCE));
  }

  @Test
  public void currency_exposure_USD() {
    LocalDate startDate = LocalDate.of(2016, 8, 2);
    LocalDate fixingDate = LocalDate.of(2016, 11, 2);
    LocalDate endDate = LocalDate.of(2016, 11, 4);
    double yearFraction = 0.25;
    double rate = 0.10;
    RateAccrualPeriod accrual = RateAccrualPeriod.builder().startDate(startDate)
        .endDate(endDate).yearFraction(yearFraction).rateComputation(FixedRateComputation.of(rate)).build();
    double notional = 1000000;
    RatePaymentPeriod fixedFx = RatePaymentPeriod.builder()
        .accrualPeriods(accrual)
        .fxReset(FxReset.of(FxIndexObservation.of(FxIndices.GBP_USD_WM, fixingDate, REF_DATA), USD))
        .notional(notional)
        .paymentDate(endDate)
        .dayCount(DayCounts.ONE_ONE)
        .currency(GBP).build(); // 1_000_000 USD paid in GBP at maturity
    PointSensitivityBuilder pts = PERIOD_PRICER.presentValueSensitivity(fixedFx, PROVIDER);
    MultiCurrencyAmount ceComputed = PERIOD_PRICER.currencyExposure(fixedFx, PROVIDER);
    double dfUsd = PROVIDER.discountFactor(USD, endDate);
    double ceUsdExpected = notional * yearFraction * rate * dfUsd;
    assertThat(ceComputed.getAmount(USD).getAmount()).isCloseTo(ceUsdExpected, offset(1.0E-6));
    MultiCurrencyAmount ceWithoutPvComputed = PROVIDER.currencyExposure(pts.build().convertedTo(USD, PROVIDER));
    CurrencyAmount pvComputed = CurrencyAmount.of(GBP, PERIOD_PRICER.presentValue(fixedFx, PROVIDER));
    MultiCurrencyAmount ceComputed2 = ceWithoutPvComputed.plus(pvComputed);
    assertThat(ceComputed2.getAmount(USD).getAmount()).isCloseTo(ceUsdExpected, offset(TOLERANCE));
    assertThat(ceComputed2.getAmount(GBP).getAmount()).isCloseTo(0.0, offset(TOLERANCE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SimpleFxForwardRates test1 = SimpleFxForwardRates.of(GBP_USD_WM, DATE_VAL, CURVE1);
    coverImmutableBean(test1);
    SimpleFxForwardRates test2 = SimpleFxForwardRates.of(FxIndices.EUR_GBP_ECB, DATE_VAL.plusDays(1), CURVE2);
    coverBeanEquals(test1, test2);
  }

}
