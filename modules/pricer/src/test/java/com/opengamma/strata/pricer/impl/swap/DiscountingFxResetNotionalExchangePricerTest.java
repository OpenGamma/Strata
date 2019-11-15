/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE_REC_USD;
import static com.opengamma.strata.pricer.swap.SwapDummyData.NOTIONAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.fx.FxIndexRates;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.swap.FxResetNotionalExchange;

/**
 * Test.
 */
public class DiscountingFxResetNotionalExchangePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 6, 30);
  private static final double DISCOUNT_FACTOR = 0.98d;
  private static final double FX_RATE = 1.6d;
  private static final double TOLERANCE = 1.0e-10;
  private static final FxMatrix FX_MATRIX = FxMatrix.of(GBP, USD, FX_RATE);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.DOUBLE_QUADRATIC;
  private static final Curve DISCOUNT_CURVE_GBP;
  private static final Curve DISCOUNT_CURVE_USD;
  static {
    DoubleArray timeGbp = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0);
    DoubleArray rateGbp = DoubleArray.of(0.0160, 0.0135, 0.0160, 0.0185, 0.0185, 0.0195, 0.0200, 0.0210);
    DISCOUNT_CURVE_GBP = InterpolatedNodalCurve.of(
        Curves.zeroRates("GBP-Discount", ACT_ACT_ISDA), timeGbp, rateGbp, INTERPOLATOR);
    DoubleArray timeUsd = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
    DoubleArray rateUsd = DoubleArray.of(0.0100, 0.0110, 0.0115, 0.0130, 0.0135, 0.0135);
    DISCOUNT_CURVE_USD = InterpolatedNodalCurve.of(
        Curves.zeroRates("USD-Discount", ACT_ACT_ISDA), timeUsd, rateUsd, INTERPOLATOR);
  }

  private static final double EPS_FD = 1.0e-7;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CALCULATOR =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    SimpleRatesProvider prov = createProvider(FX_RESET_NOTIONAL_EXCHANGE_REC_USD);

    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    double calculated = test.presentValue(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov);
    assertThat(calculated).isCloseTo(FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getNotional() * FX_RATE * DISCOUNT_FACTOR, offset(0d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .build();
    FxResetNotionalExchange[] expanded =
        new FxResetNotionalExchange[] {FX_RESET_NOTIONAL_EXCHANGE_REC_USD, FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP};
    for (int i = 0; i < 2; ++i) {
      FxResetNotionalExchange fxReset = expanded[i];
      DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();

      PointSensitivityBuilder pointSensitivityComputed = test.presentValueSensitivity(expanded[i], prov);
      CurrencyParameterSensitivities parameterSensitivityComputed = prov.parameterSensitivity(
          pointSensitivityComputed.build());
      CurrencyParameterSensitivities parameterSensitivityExpected = FD_CALCULATOR.sensitivity(
          prov, (p) -> CurrencyAmount.of(fxReset.getCurrency(), test.presentValue(fxReset, (p))));
      assertThat(parameterSensitivityComputed.equalWithTolerance(
          parameterSensitivityExpected, Math.abs(expanded[i].getNotional()) * EPS_FD * 10.0)).isTrue();
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forecastValue() {
    SimpleRatesProvider prov = createProvider(FX_RESET_NOTIONAL_EXCHANGE_REC_USD);

    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    double calculated = test.forecastValue(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov);
    assertThat(calculated).isCloseTo(FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getNotional() * FX_RATE, offset(0d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forecastValueSensitivity() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .build();
    FxResetNotionalExchange[] expanded =
        new FxResetNotionalExchange[] {FX_RESET_NOTIONAL_EXCHANGE_REC_USD, FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP};
    for (int i = 0; i < 2; ++i) {
      FxResetNotionalExchange fxReset = expanded[i];
      DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();

      PointSensitivityBuilder pointSensitivityComputed = test.forecastValueSensitivity(expanded[i], prov);
      CurrencyParameterSensitivities parameterSensitivityComputed = prov.parameterSensitivity(
          pointSensitivityComputed.build());
      CurrencyParameterSensitivities parameterSensitivityExpected = FD_CALCULATOR.sensitivity(
          prov, (p) -> CurrencyAmount.of(fxReset.getCurrency(), test.forecastValue(fxReset, (p))));
      assertThat(parameterSensitivityComputed.equalWithTolerance(
          parameterSensitivityExpected, Math.abs(expanded[i].getNotional()) * EPS_FD * 10.0)).isTrue();
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_explainPresentValue() {
    SimpleRatesProvider prov = createProvider(FX_RESET_NOTIONAL_EXCHANGE_REC_USD);

    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    ExplainMapBuilder builder = ExplainMap.builder();
    test.explainPresentValue(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov, builder);
    ExplainMap explain = builder.build();

    Currency paymentCurrency = FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getCurrency();
    Currency notionalCurrency = FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getReferenceCurrency();
    double notional = FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getNotional();
    double convertedNotional = notional * FX_RATE;
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("FxResetNotionalExchange");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getPaymentDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(paymentCurrency);
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getCurrency()).isEqualTo(notionalCurrency);
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getAmount()).isCloseTo(notional, offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.DISCOUNT_FACTOR).get()).isCloseTo(DISCOUNT_FACTOR, offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency()).isEqualTo(paymentCurrency);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(convertedNotional, offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency()).isEqualTo(paymentCurrency);
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(convertedNotional * DISCOUNT_FACTOR, offset(TOLERANCE));
  }

  @Test
  public void test_explainPresentValue_paymentDateInPast() {
    SimpleRatesProvider prov = createProvider(FX_RESET_NOTIONAL_EXCHANGE_REC_USD);
    prov.setValuationDate(VAL_DATE.plusYears(1));

    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    ExplainMapBuilder builder = ExplainMap.builder();
    test.explainPresentValue(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov, builder);
    ExplainMap explain = builder.build();

    Currency paymentCurrency = FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getCurrency();
    Currency notionalCurrency = FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getReferenceCurrency();
    double notional = FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getNotional();
    assertThat(explain.get(ExplainKey.ENTRY_TYPE).get()).isEqualTo("FxResetNotionalExchange");
    assertThat(explain.get(ExplainKey.PAYMENT_DATE).get()).isEqualTo(FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getPaymentDate());
    assertThat(explain.get(ExplainKey.PAYMENT_CURRENCY).get()).isEqualTo(paymentCurrency);
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getCurrency()).isEqualTo(notionalCurrency);
    assertThat(explain.get(ExplainKey.TRADE_NOTIONAL).get().getAmount()).isCloseTo(notional, offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency()).isEqualTo(paymentCurrency);
    assertThat(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount()).isCloseTo(0d, offset(TOLERANCE));
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency()).isEqualTo(paymentCurrency);
    assertThat(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount()).isCloseTo(0d * DISCOUNT_FACTOR, offset(TOLERANCE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currencyExposure() {
    double eps = 1.0e-14;
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .build();
    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    // USD
    MultiCurrencyAmount computedUSD = test.currencyExposure(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov);
    PointSensitivities pointUSD = test.presentValueSensitivity(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov).build();
    MultiCurrencyAmount expectedUSD = prov.currencyExposure(pointUSD.convertedTo(USD, prov)).plus(CurrencyAmount.of(
        FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getCurrency(), test.presentValue(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov)));
    assertThat(computedUSD.contains(GBP)).isFalse(); // 0 GBP
    assertThat(computedUSD.getAmount(USD).getAmount()).isCloseTo(expectedUSD.getAmount(USD).getAmount(), offset(eps * NOTIONAL));
    // GBP
    MultiCurrencyAmount computedGBP = test.currencyExposure(FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP, prov);
    PointSensitivities pointGBP = test.presentValueSensitivity(FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP, prov).build();
    MultiCurrencyAmount expectedGBP = prov.currencyExposure(pointGBP.convertedTo(GBP, prov)).plus(CurrencyAmount.of(
        FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP.getCurrency(), test.presentValue(FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP, prov)));
    assertThat(computedGBP.contains(USD)).isFalse(); // 0 USD
    assertThat(computedGBP.getAmount(GBP).getAmount()).isCloseTo(expectedGBP.getAmount(GBP).getAmount(), offset(eps * NOTIONAL));
    // FD approximation
    FxMatrix fxMatrixUp = FxMatrix.of(GBP, USD, FX_RATE + EPS_FD);
    ImmutableRatesProvider provUp = ImmutableRatesProvider.builder(VAL_DATE)
        .fxRateProvider(fxMatrixUp)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .build();
    double expectedFdUSD = -(test.presentValue(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, provUp) -
        test.presentValue(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov)) * FX_RATE * FX_RATE / EPS_FD;
    assertThat(computedUSD.getAmount(USD).getAmount()).isCloseTo(expectedFdUSD, offset(EPS_FD * NOTIONAL));
    double expectedFdGBP = (test.presentValue(FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP, provUp) -
        test.presentValue(FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP, prov)) / EPS_FD;
    assertThat(computedGBP.getAmount(GBP).getAmount()).isCloseTo(expectedFdGBP, offset(EPS_FD * NOTIONAL));
  }

  @Test
  public void test_currencyExposureBetweenFixingAndPayment() {
    double eps = 1.0e-14;
    LocalDate valuationDate = date(2014, 6, 30);
    LocalDate paymentDate = date(2014, 7, 1);
    LocalDate fixingDate = date(2014, 6, 27);
    FxResetNotionalExchange resetNotionalUSD = FxResetNotionalExchange.of(
        CurrencyAmount.of(USD, NOTIONAL), paymentDate, FxIndexObservation.of(GBP_USD_WM, fixingDate, REF_DATA));
    FxResetNotionalExchange resetNotionalGBP = FxResetNotionalExchange.of(
        CurrencyAmount.of(GBP, -NOTIONAL), paymentDate, FxIndexObservation.of(GBP_USD_WM, fixingDate, REF_DATA));
    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.of(LocalDate.of(2014, 6, 27), 1.65);
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(valuationDate)
        .fxRateProvider(FX_MATRIX)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .timeSeries(FxIndices.GBP_USD_WM, ts)
        .build();
    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    // USD
    MultiCurrencyAmount computedUSD = test.currencyExposure(resetNotionalUSD, prov);
    PointSensitivities pointUSD = test.presentValueSensitivity(resetNotionalUSD, prov).build();
    MultiCurrencyAmount expectedUSD = prov.currencyExposure(pointUSD.convertedTo(USD, prov)).plus(CurrencyAmount.of(
        resetNotionalUSD.getCurrency(), test.presentValue(resetNotionalUSD, prov)));
    assertThat(computedUSD.contains(USD)).isFalse(); // 0 USD
    assertThat(computedUSD.getAmount(GBP).getAmount()).isCloseTo(expectedUSD.getAmount(GBP).getAmount(), offset(eps * NOTIONAL));
    // GBP
    MultiCurrencyAmount computedGBP = test.currencyExposure(resetNotionalGBP, prov);
    PointSensitivities pointGBP = test.presentValueSensitivity(resetNotionalGBP, prov).build();
    MultiCurrencyAmount expectedGBP = prov.currencyExposure(pointGBP.convertedTo(GBP, prov)).plus(CurrencyAmount.of(
        resetNotionalGBP.getCurrency(), test.presentValue(resetNotionalGBP, prov)));
    assertThat(computedGBP.contains(GBP)).isFalse(); // 0 GBP
    assertThat(computedGBP.getAmount(USD).getAmount()).isCloseTo(expectedGBP.getAmount(USD).getAmount(), offset(eps * NOTIONAL));
    // FD approximation
    FxMatrix fxMatrixUp = FxMatrix.of(GBP, USD, FX_RATE + EPS_FD);
    ImmutableRatesProvider provUp = ImmutableRatesProvider.builder(valuationDate)
        .fxRateProvider(fxMatrixUp)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .timeSeries(FxIndices.GBP_USD_WM, ts)
        .build();
    double expectedFdUSD = -(test.presentValue(resetNotionalUSD, provUp) -
        test.presentValue(resetNotionalUSD, prov)) * FX_RATE * FX_RATE / EPS_FD;
    assertThat(!computedUSD.contains(USD) && DoubleMath.fuzzyEquals(expectedFdUSD, 0d, eps)).isTrue();
    double expectedFdGBP = (test.presentValue(resetNotionalGBP, provUp) -
        test.presentValue(resetNotionalGBP, prov)) / EPS_FD;
    assertThat(!computedGBP.contains(GBP) && DoubleMath.fuzzyEquals(expectedFdGBP, 0d, eps)).isTrue();
  }

  @Test
  public void test_currencyExposureOnFixing_noTimeSeries() {
    double eps = 1.0e-14;
    LocalDate valuationDate = date(2014, 6, 27);
    LocalDate paymentDate = date(2014, 7, 1);
    LocalDate fixingDate = date(2014, 6, 27);
    FxResetNotionalExchange resetNotionalUSD = FxResetNotionalExchange.of(
        CurrencyAmount.of(USD, NOTIONAL), paymentDate, FxIndexObservation.of(GBP_USD_WM, fixingDate, REF_DATA));
    FxResetNotionalExchange resetNotionalGBP = FxResetNotionalExchange.of(
        CurrencyAmount.of(GBP, -NOTIONAL), paymentDate, FxIndexObservation.of(GBP_USD_WM, fixingDate, REF_DATA));
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(valuationDate)
        .fxRateProvider(FX_MATRIX)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .build();
    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    // USD
    MultiCurrencyAmount computedUSD = test.currencyExposure(resetNotionalUSD, prov);
    PointSensitivities pointUSD = test.presentValueSensitivity(resetNotionalUSD, prov).build();
    MultiCurrencyAmount expectedUSD = prov.currencyExposure(pointUSD.convertedTo(USD, prov)).plus(CurrencyAmount.of(
        resetNotionalUSD.getCurrency(), test.presentValue(resetNotionalUSD, prov)));
    assertThat(computedUSD.contains(GBP)).isFalse(); // 0 GBP
    assertThat(computedUSD.getAmount(USD).getAmount()).isCloseTo(expectedUSD.getAmount(USD).getAmount(), offset(eps * NOTIONAL));
    // GBP
    MultiCurrencyAmount computedGBP = test.currencyExposure(resetNotionalGBP, prov);
    PointSensitivities pointGBP = test.presentValueSensitivity(resetNotionalGBP, prov).build();
    MultiCurrencyAmount expectedGBP = prov.currencyExposure(pointGBP.convertedTo(GBP, prov)).plus(CurrencyAmount.of(
        resetNotionalGBP.getCurrency(), test.presentValue(resetNotionalGBP, prov)));
    assertThat(computedGBP.contains(USD)).isFalse(); // 0 USD
    assertThat(computedGBP.getAmount(GBP).getAmount()).isCloseTo(expectedGBP.getAmount(GBP).getAmount(), offset(eps * NOTIONAL));
    // FD approximation
    FxMatrix fxMatrixUp = FxMatrix.of(GBP, USD, FX_RATE + EPS_FD);
    ImmutableRatesProvider provUp = ImmutableRatesProvider.builder(valuationDate)
        .fxRateProvider(fxMatrixUp)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .build();
    double expectedFdUSD = -(test.presentValue(resetNotionalUSD, provUp) -
        test.presentValue(resetNotionalUSD, prov)) * FX_RATE * FX_RATE / EPS_FD;
    assertThat(computedUSD.getAmount(USD).getAmount()).isCloseTo(expectedFdUSD, offset(EPS_FD * NOTIONAL));
    double expectedFdGBP = (test.presentValue(resetNotionalGBP, provUp) -
        test.presentValue(resetNotionalGBP, prov)) / EPS_FD;
    assertThat(computedGBP.getAmount(GBP).getAmount()).isCloseTo(expectedFdGBP, offset(EPS_FD * NOTIONAL));
  }

  @Test
  public void test_currentCash_zero() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .build();
    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    double cc = test.currentCash(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov);
    assertThat(cc).isEqualTo(0d);
  }

  @Test
  public void test_currentCash_onPayment() {
    double eps = 1.0e-14;
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder(FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getPaymentDate())
        .fxRateProvider(FX_MATRIX)
        .discountCurve(GBP, DISCOUNT_CURVE_GBP)
        .discountCurve(USD, DISCOUNT_CURVE_USD)
        .build();
    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    double rate = prov.fxIndexRates(FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getObservation().getIndex()).rate(
        FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getObservation(), FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getReferenceCurrency());
    double ccUSD = test.currentCash(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov);
    assertThat(ccUSD).isCloseTo(NOTIONAL * rate, offset(eps));
    double ccGBP = test.currentCash(FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP, prov);
    assertThat(ccGBP).isCloseTo(-NOTIONAL / rate, offset(eps));
  }

  //-------------------------------------------------------------------------
  // creates a simple provider
  private SimpleRatesProvider createProvider(FxResetNotionalExchange ne) {
    LocalDate paymentDate = ne.getPaymentDate();
    double paymentTime = ACT_360.relativeYearFraction(VAL_DATE, paymentDate);
    Currency currency = ne.getCurrency();

    DiscountFactors mockDf = mock(DiscountFactors.class);
    when(mockDf.discountFactor(paymentDate)).thenReturn(DISCOUNT_FACTOR);
    ZeroRateSensitivity sens = ZeroRateSensitivity.of(currency, paymentTime, -DISCOUNT_FACTOR * paymentTime);
    when(mockDf.zeroRatePointSensitivity(paymentDate)).thenReturn(sens);
    FxIndexRates mockFxRates = mock(FxIndexRates.class);
    when(mockFxRates.rate(ne.getObservation(), ne.getReferenceCurrency())).thenReturn(FX_RATE);
    SimpleRatesProvider prov = new SimpleRatesProvider(VAL_DATE);
    prov.setDiscountFactors(mockDf);
    prov.setFxIndexRates(mockFxRates);
    prov.setDayCount(ACT_360);
    return prov;
  }

}
