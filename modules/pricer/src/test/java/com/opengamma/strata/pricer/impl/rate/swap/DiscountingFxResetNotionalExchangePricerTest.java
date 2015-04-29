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
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.finance.rate.swap.FxResetNotionalExchange;
import com.opengamma.strata.pricer.ImmutableRatesProvider;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;

/**
 * Test.
 */
@Test
public class DiscountingFxResetNotionalExchangePricerTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2014, 6, 30);
  private static final LocalDateDoubleTimeSeries EMPTY_TIME_SERIES = LocalDateDoubleTimeSeries.empty();
  private static final FxMatrix FX_MATRIX = FxMatrix.of(GBP, USD, 1.6d);

  private static final YieldCurve DISCOUNT_CURVE_GBP;
  private static final YieldCurve DISCOUNT_CURVE_USD;
  static {
    CombinedInterpolatorExtrapolator interp = CombinedInterpolatorExtrapolatorFactory.getInterpolator(
        Interpolator1DFactory.DOUBLE_QUADRATIC, Interpolator1DFactory.FLAT_EXTRAPOLATOR,
        Interpolator1DFactory.FLAT_EXTRAPOLATOR);
    double[] time_gbp = new double[] {0.0, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0 };
    double[] rate_gbp = new double[] {0.0160, 0.0135, 0.0160, 0.0185, 0.0185, 0.0195, 0.0200, 0.0210 };
    InterpolatedDoublesCurve curve_gbp = InterpolatedDoublesCurve.from(time_gbp, rate_gbp, interp);
    DISCOUNT_CURVE_GBP = new YieldCurve("GBP-Discount", curve_gbp);
    double[] time_usd = new double[] {0.0, 0.5, 1.0, 2.0, 5.0, 10.0 };
    double[] rate_usd = new double[] {0.0100, 0.0110, 0.0115, 0.0130, 0.0135, 0.0135 };
    InterpolatedDoublesCurve curve_usd = InterpolatedDoublesCurve.from(time_usd, rate_usd, interp);
    DISCOUNT_CURVE_USD = new YieldCurve("USD-Discount", curve_usd);
  }

  private static final double EPS_FD = 1.0e-7;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CALCULATOR =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  public void test_presentValue() {
    double discountFactor = 0.98d;
    FxResetNotionalExchange ne = FX_RESET_NOTIONAL_EXCHANGE_REC_USD;
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.fxIndexRate(ne.getIndex(), ne.getReferenceCurrency(), ne.getFixingDate()))
        .thenReturn(1.6d);
    when(mockProv.discountFactor(ne.getCurrency(), ne.getPaymentDate()))
        .thenReturn(discountFactor);
    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    assertEquals(
        test.presentValue(ne, mockProv),
        ne.getNotional() * 1.6d * discountFactor, 0d);
  }

  public void test_presentValueSensitivity() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxMatrix(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD))
        .timeSeries(ImmutableMap.of(WM_GBP_USD, EMPTY_TIME_SERIES))
        .dayCount(ACT_ACT_ISDA)
        .build();
    FxResetNotionalExchange[] expanded = 
        new FxResetNotionalExchange[] {FX_RESET_NOTIONAL_EXCHANGE_REC_USD, FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP };
    for (int i=0;i<2;++i) {
      FxResetNotionalExchange fxReset = expanded[i];
      DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();

      PointSensitivityBuilder pointSensitivityComputed = test.presentValueSensitivity(expanded[i], prov);
      CurveParameterSensitivity parameterSensitivityComputed = prov.parameterSensitivity(
          pointSensitivityComputed.build());
      CurveParameterSensitivity parameterSensitivityExpected = FD_CALCULATOR.sensitivity(
          prov, (p) -> CurrencyAmount.of(fxReset.getCurrency(), test.presentValue(fxReset, (p))));
      assertTrue(parameterSensitivityComputed.equalWithTolerance(
          parameterSensitivityExpected, Math.abs(expanded[i].getNotional()) * EPS_FD * 10.0));
    }
  }

  public void test_futureValue() {
    FxResetNotionalExchange ne = FX_RESET_NOTIONAL_EXCHANGE_REC_USD;
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.fxIndexRate(ne.getIndex(), ne.getReferenceCurrency(), ne.getFixingDate()))
        .thenReturn(1.6d);
    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    assertEquals(
        test.futureValue(ne, mockProv),
        ne.getNotional() * 1.6d, 0d);
  }

  public void test_futureValueSensitivity() {
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxMatrix(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD))
        .timeSeries(ImmutableMap.of(WM_GBP_USD, EMPTY_TIME_SERIES))
        .dayCount(ACT_ACT_ISDA)
        .build();
    FxResetNotionalExchange[] expanded =
        new FxResetNotionalExchange[] {FX_RESET_NOTIONAL_EXCHANGE_REC_USD, FX_RESET_NOTIONAL_EXCHANGE_PAY_GBP };
    for (int i = 0; i < 2; ++i) {
      FxResetNotionalExchange fxReset = expanded[i];
      DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();

      PointSensitivityBuilder pointSensitivityComputed = test.futureValueSensitivity(expanded[i], prov);
      CurveParameterSensitivity parameterSensitivityComputed = prov.parameterSensitivity(
          pointSensitivityComputed.build());
      CurveParameterSensitivity parameterSensitivityExpected = FD_CALCULATOR.sensitivity(
          prov, (p) -> CurrencyAmount.of(fxReset.getCurrency(), test.futureValue(fxReset, (p))));
      assertTrue(parameterSensitivityComputed.equalWithTolerance(
          parameterSensitivityExpected, Math.abs(expanded[i].getNotional()) * EPS_FD * 10.0));
    }
  }

}
