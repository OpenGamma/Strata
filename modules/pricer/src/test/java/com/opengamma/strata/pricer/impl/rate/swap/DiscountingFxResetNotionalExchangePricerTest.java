/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.FxIndices.WM_GBP_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE_REC_USD;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.finance.rate.swap.FxResetNotionalExchange;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.FxIndexRates;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;

/**
 * Test.
 */
@Test
public class DiscountingFxResetNotionalExchangePricerTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2014, 6, 30);
  private static final LocalDateDoubleTimeSeries EMPTY_TIME_SERIES = LocalDateDoubleTimeSeries.empty();
  private static final FxMatrix FX_MATRIX = FxMatrix.of(GBP, USD, 1.6d);

  private static final CurveInterpolator INTERPOLATOR = Interpolator1DFactory.DOUBLE_QUADRATIC_INSTANCE;
  private static final Curve DISCOUNT_CURVE_GBP;
  private static final Curve DISCOUNT_CURVE_USD;
  static {
    double[] time_gbp = new double[] {0.0, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0};
    double[] rate_gbp = new double[] {0.0160, 0.0135, 0.0160, 0.0185, 0.0185, 0.0195, 0.0200, 0.0210};
    DISCOUNT_CURVE_GBP = InterpolatedNodalCurve.of("GBP-Discount", time_gbp, rate_gbp, INTERPOLATOR);
    double[] time_usd = new double[] {0.0, 0.5, 1.0, 2.0, 5.0, 10.0};
    double[] rate_usd = new double[] {0.0100, 0.0110, 0.0115, 0.0130, 0.0135, 0.0135};
    DISCOUNT_CURVE_USD = InterpolatedNodalCurve.of("USD-Discount", time_usd, rate_usd, INTERPOLATOR);
  }

  private static final double EPS_FD = 1.0e-7;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CALCULATOR =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    double discountFactor = 0.98d;
    FxResetNotionalExchange ne = FX_RESET_NOTIONAL_EXCHANGE_REC_USD;

    DiscountFactors mockDf = mock(DiscountFactors.class);
    when(mockDf.discountFactor(ne.getPaymentDate())).thenReturn(discountFactor);

    FxIndexRates mockFxRates = mock(FxIndexRates.class);
    when(mockFxRates.rate(ne.getReferenceCurrency(), ne.getFixingDate())).thenReturn(1.6d);

    SimpleRatesProvider prov = new SimpleRatesProvider(VAL_DATE);
    prov.setDiscountFactors(mockDf);
    prov.setFxIndexRates(mockFxRates);

    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    assertEquals(test.presentValue(ne, prov), ne.getNotional() * 1.6d * discountFactor, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxMatrix(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD))
        .timeSeries(ImmutableMap.of(WM_GBP_USD, EMPTY_TIME_SERIES))
        .dayCount(ACT_ACT_ISDA)
        .build();
    FxResetNotionalExchange[] expanded =
        new FxResetNotionalExchange[] {FX_RESET_NOTIONAL_EXCHANGE_REC_USD, FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP};
    for (int i = 0; i < 2; ++i) {
      FxResetNotionalExchange fxReset = expanded[i];
      DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();

      PointSensitivityBuilder pointSensitivityComputed = test.presentValueSensitivity(expanded[i], prov);
      CurveParameterSensitivities parameterSensitivityComputed = prov.parameterSensitivity(
          pointSensitivityComputed.build());
      CurveParameterSensitivities parameterSensitivityExpected = FD_CALCULATOR.sensitivity(
          prov, (p) -> CurrencyAmount.of(fxReset.getCurrency(), test.presentValue(fxReset, (p))));
      assertTrue(parameterSensitivityComputed.equalWithTolerance(
          parameterSensitivityExpected, Math.abs(expanded[i].getNotional()) * EPS_FD * 10.0));
    }
  }

  //-------------------------------------------------------------------------
  public void test_futureValue() {
    FxResetNotionalExchange ne = FX_RESET_NOTIONAL_EXCHANGE_REC_USD;

    FxIndexRates mockFxRates = mock(FxIndexRates.class);
    when(mockFxRates.rate(ne.getReferenceCurrency(), ne.getFixingDate())).thenReturn(1.6d);

    SimpleRatesProvider prov = new SimpleRatesProvider(VAL_DATE);
    prov.setFxIndexRates(mockFxRates);

    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    assertEquals(test.futureValue(ne, prov), ne.getNotional() * 1.6d, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_futureValueSensitivity() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxMatrix(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD))
        .timeSeries(ImmutableMap.of(WM_GBP_USD, EMPTY_TIME_SERIES))
        .dayCount(ACT_ACT_ISDA)
        .build();
    FxResetNotionalExchange[] expanded =
        new FxResetNotionalExchange[] {FX_RESET_NOTIONAL_EXCHANGE_REC_USD, FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP};
    for (int i = 0; i < 2; ++i) {
      FxResetNotionalExchange fxReset = expanded[i];
      DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();

      PointSensitivityBuilder pointSensitivityComputed = test.futureValueSensitivity(expanded[i], prov);
      CurveParameterSensitivities parameterSensitivityComputed = prov.parameterSensitivity(
          pointSensitivityComputed.build());
      CurveParameterSensitivities parameterSensitivityExpected = FD_CALCULATOR.sensitivity(
          prov, (p) -> CurrencyAmount.of(fxReset.getCurrency(), test.futureValue(fxReset, (p))));
      assertTrue(parameterSensitivityComputed.equalWithTolerance(
          parameterSensitivityExpected, Math.abs(expanded[i].getNotional()) * EPS_FD * 10.0));
    }
  }

}
