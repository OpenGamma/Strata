/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP;
import static com.opengamma.strata.pricer.swap.SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE_REC_USD;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.FxIndexRates;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.swap.FxResetNotionalExchange;

/**
 * Test.
 */
@Test
public class DiscountingFxResetNotionalExchangePricerTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2014, 6, 30);
  private static final double DISCOUNT_FACTOR = 0.98d;
  private static final double FX_RATE = 1.6d;
  private static final double TOLERANCE = 1.0e-10;
  private static final FxMatrix FX_MATRIX = FxMatrix.of(GBP, USD, 1.6d);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.DOUBLE_QUADRATIC;
  private static final Curve DISCOUNT_CURVE_GBP;
  private static final Curve DISCOUNT_CURVE_USD;
  static {
    DoubleArray time_gbp = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0);
    DoubleArray rate_gbp = DoubleArray.of(0.0160, 0.0135, 0.0160, 0.0185, 0.0185, 0.0195, 0.0200, 0.0210);
    DISCOUNT_CURVE_GBP = InterpolatedNodalCurve.of(
        Curves.zeroRates("GBP-Discount", ACT_ACT_ISDA), time_gbp, rate_gbp, INTERPOLATOR);
    DoubleArray time_usd = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
    DoubleArray rate_usd = DoubleArray.of(0.0100, 0.0110, 0.0115, 0.0130, 0.0135, 0.0135);
    DISCOUNT_CURVE_USD = InterpolatedNodalCurve.of(
        Curves.zeroRates("USD-Discount", ACT_ACT_ISDA), time_usd, rate_usd, INTERPOLATOR);
  }

  private static final double EPS_FD = 1.0e-7;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CALCULATOR =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    SimpleRatesProvider prov = createProvider(FX_RESET_NOTIONAL_EXCHANGE_REC_USD);

    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    double calculated = test.presentValue(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov);
    assertEquals(calculated, FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getNotional() * FX_RATE * DISCOUNT_FACTOR, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD))
        .build();
    FxResetNotionalExchange[] expanded =
        new FxResetNotionalExchange[] {FX_RESET_NOTIONAL_EXCHANGE_REC_USD, FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP};
    for (int i = 0; i < 2; ++i) {
      FxResetNotionalExchange fxReset = expanded[i];
      DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();

      PointSensitivityBuilder pointSensitivityComputed = test.presentValueSensitivity(expanded[i], prov);
      CurveCurrencyParameterSensitivities parameterSensitivityComputed = prov.curveParameterSensitivity(
          pointSensitivityComputed.build());
      CurveCurrencyParameterSensitivities parameterSensitivityExpected = FD_CALCULATOR.sensitivity(
          prov, (p) -> CurrencyAmount.of(fxReset.getCurrency(), test.presentValue(fxReset, (p))));
      assertTrue(parameterSensitivityComputed.equalWithTolerance(
          parameterSensitivityExpected, Math.abs(expanded[i].getNotional()) * EPS_FD * 10.0));
    }
  }

  //-------------------------------------------------------------------------
  public void test_forecastValue() {
    SimpleRatesProvider prov = createProvider(FX_RESET_NOTIONAL_EXCHANGE_REC_USD);

    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    double calculated = test.forecastValue(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov);
    assertEquals(calculated, FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getNotional() * FX_RATE, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_forecastValueSensitivity() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD))
        .build();
    FxResetNotionalExchange[] expanded =
        new FxResetNotionalExchange[] {FX_RESET_NOTIONAL_EXCHANGE_REC_USD, FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP};
    for (int i = 0; i < 2; ++i) {
      FxResetNotionalExchange fxReset = expanded[i];
      DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();

      PointSensitivityBuilder pointSensitivityComputed = test.forecastValueSensitivity(expanded[i], prov);
      CurveCurrencyParameterSensitivities parameterSensitivityComputed = prov.curveParameterSensitivity(
          pointSensitivityComputed.build());
      CurveCurrencyParameterSensitivities parameterSensitivityExpected = FD_CALCULATOR.sensitivity(
          prov, (p) -> CurrencyAmount.of(fxReset.getCurrency(), test.forecastValue(fxReset, (p))));
      assertTrue(parameterSensitivityComputed.equalWithTolerance(
          parameterSensitivityExpected, Math.abs(expanded[i].getNotional()) * EPS_FD * 10.0));
    }
  }

  //-------------------------------------------------------------------------
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
    assertEquals(explain.get(ExplainKey.ENTRY_TYPE).get(), "FxResetNotionalExchange");
    assertEquals(explain.get(ExplainKey.PAYMENT_DATE).get(), FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getPaymentDate());
    assertEquals(explain.get(ExplainKey.PAYMENT_CURRENCY).get(), paymentCurrency);
    assertEquals(explain.get(ExplainKey.TRADE_NOTIONAL).get().getCurrency(), notionalCurrency);
    assertEquals(explain.get(ExplainKey.TRADE_NOTIONAL).get().getAmount(), notional, TOLERANCE);
    assertEquals(explain.get(ExplainKey.DISCOUNT_FACTOR).get(), DISCOUNT_FACTOR, TOLERANCE);
    assertEquals(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency(), paymentCurrency);
    assertEquals(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount(), convertedNotional, TOLERANCE);
    assertEquals(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency(), paymentCurrency);
    assertEquals(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount(), convertedNotional * DISCOUNT_FACTOR, TOLERANCE);
  }

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
    assertEquals(explain.get(ExplainKey.ENTRY_TYPE).get(), "FxResetNotionalExchange");
    assertEquals(explain.get(ExplainKey.PAYMENT_DATE).get(), FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getPaymentDate());
    assertEquals(explain.get(ExplainKey.PAYMENT_CURRENCY).get(), paymentCurrency);
    assertEquals(explain.get(ExplainKey.TRADE_NOTIONAL).get().getCurrency(), notionalCurrency);
    assertEquals(explain.get(ExplainKey.TRADE_NOTIONAL).get().getAmount(), notional, TOLERANCE);
    assertEquals(explain.get(ExplainKey.FORECAST_VALUE).get().getCurrency(), paymentCurrency);
    assertEquals(explain.get(ExplainKey.FORECAST_VALUE).get().getAmount(), 0d, TOLERANCE);
    assertEquals(explain.get(ExplainKey.PRESENT_VALUE).get().getCurrency(), paymentCurrency);
    assertEquals(explain.get(ExplainKey.PRESENT_VALUE).get().getAmount(), 0d * DISCOUNT_FACTOR, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    double eps = 1.0e-14;
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD))
        .build();
    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    double notional = FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getNotional();
    // USD
    MultiCurrencyAmount computedUSD = test.currencyExposure(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov);
    PointSensitivities pointUSD = test.presentValueSensitivity(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov).build();
    MultiCurrencyAmount expectedUSD = prov.currencyExposure(pointUSD.convertedTo(USD, prov)).plus(CurrencyAmount.of(
        FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getCurrency(), test.presentValue(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov)));
    assertFalse(computedUSD.contains(GBP)); // 0 GBP
    assertEquals(computedUSD.getAmount(USD).getAmount(), expectedUSD.getAmount(USD).getAmount(), eps * notional);
    // GBP
    MultiCurrencyAmount computedGBP = test.currencyExposure(FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP, prov);
    PointSensitivities pointGBP = test.presentValueSensitivity(FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP, prov).build();
    MultiCurrencyAmount expectedGBP = prov.currencyExposure(pointGBP.convertedTo(GBP, prov)).plus(CurrencyAmount.of(
        FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP.getCurrency(), test.presentValue(FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP, prov)));
    assertFalse(computedGBP.contains(USD)); // 0 USD
    assertEquals(computedGBP.getAmount(GBP).getAmount(), expectedGBP.getAmount(GBP).getAmount(), eps * notional);
  }

  public void test_currentCash_zero() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxRateProvider(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD))
        .build();
    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    double cc = test.currentCash(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov);
    assertEquals(cc, 0d);
  }

  public void test_currentCash_onPayment() {
    double eps = 1.0e-14;
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getPaymentDate())
        .fxRateProvider(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD))
        .build();
    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    double notional = FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getNotional();
    double rate = prov.fxIndexRates(FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getIndex()).rate(
        FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getReferenceCurrency(), FX_RESET_NOTIONAL_EXCHANGE_REC_USD.getFixingDate());
    double ccUSD = test.currentCash(FX_RESET_NOTIONAL_EXCHANGE_REC_USD, prov);
    assertEquals(ccUSD, notional * rate, eps);
    double ccGBP = test.currentCash(FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP, prov);
    assertEquals(ccGBP, -notional / rate, eps);
  }

  //-------------------------------------------------------------------------
  // creates a simple provider
  private SimpleRatesProvider createProvider(FxResetNotionalExchange ne) {
    LocalDate paymentDate = ne.getPaymentDate();
    double paymentTime = ACT_360.relativeYearFraction(VAL_DATE, ne.getPaymentDate());
    Currency currency = ne.getCurrency();

    DiscountFactors mockDf = mock(DiscountFactors.class);
    when(mockDf.discountFactor(paymentDate)).thenReturn(DISCOUNT_FACTOR);
    ZeroRateSensitivity sens = ZeroRateSensitivity.of(currency, paymentDate, -DISCOUNT_FACTOR * paymentTime);
    when(mockDf.zeroRatePointSensitivity(paymentDate)).thenReturn(sens);
    FxIndexRates mockFxRates = mock(FxIndexRates.class);
    when(mockFxRates.rate(ne.getReferenceCurrency(), ne.getFixingDate())).thenReturn(FX_RATE);
    SimpleRatesProvider prov = new SimpleRatesProvider(VAL_DATE);
    prov.setDiscountFactors(mockDf);
    prov.setFxIndexRates(mockFxRates);
    prov.setDayCount(ACT_360);
    return prov;
  }

}
