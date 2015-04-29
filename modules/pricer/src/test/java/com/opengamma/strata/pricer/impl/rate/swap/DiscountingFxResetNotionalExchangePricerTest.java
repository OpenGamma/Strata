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
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

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
    double[] rate_gbp = new double[] {0.0150, 0.0125, 0.0150, 0.0175, 0.0175, 0.0190, 0.0200, 0.0210 };
    InterpolatedDoublesCurve curve_gbp = InterpolatedDoublesCurve.from(time_gbp, rate_gbp, interp);
    DISCOUNT_CURVE_GBP = new YieldCurve("GBP-Discount", curve_gbp);
    double[] time_usd = new double[] {0.0, 0.5, 1.0, 2.0, 5.0, 10.0 };
    double[] rate_usd = new double[] {0.0100, 0.0120, 0.0120, 0.0140, 0.0140, 0.0140 };
    InterpolatedDoublesCurve curve_usd = InterpolatedDoublesCurve.from(time_usd, rate_usd, interp);
    DISCOUNT_CURVE_USD = new YieldCurve("USD-Discount", curve_usd);
  }

  private static final double EPS_FD = 1.0e-8;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CALCULATOR =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  public void test_presentValue() {
    double discountFactor = 0.98d;
    FxResetNotionalExchange ne = FX_RESET_NOTIONAL_EXCHANGE;
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
    FxResetNotionalExchange ne = FX_RESET_NOTIONAL_EXCHANGE;
    ImmutableRatesProvider prov = ImmutableRatesProvider.builder()
        .valuationDate(VAL_DATE)
        .fxMatrix(FX_MATRIX)
        .discountCurves(ImmutableMap.of(GBP, DISCOUNT_CURVE_GBP, USD, DISCOUNT_CURVE_USD))
        .timeSeries(ImmutableMap.of(WM_GBP_USD, EMPTY_TIME_SERIES))
        .dayCount(ACT_ACT_ISDA)
        .build();
    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    PointSensitivityBuilder pointSensitivityComputed = test.presentValueSensitivity(ne, prov);
    CurveParameterSensitivity parameterSensitivityComputed = prov.parameterSensitivity(
        pointSensitivityComputed.build());

    CurveParameterSensitivity parameterSensitivityExpected = FD_CALCULATOR.sensitivity(prov,
        (p) -> CurrencyAmount.of(FX_RESET_NOTIONAL_EXCHANGE.getCurrency(), test.presentValue(ne, (p))));

    //    assertTrue(parameterSensitivityComputed.equalWithTolerance(parameterSensitivityExpected,
    //        FX_RESET_NOTIONAL_EXCHANGE.getNotional() * EPS_FD));
  }

  public void test_futureValue() {
    FxResetNotionalExchange ne = FX_RESET_NOTIONAL_EXCHANGE;
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.fxIndexRate(ne.getIndex(), ne.getReferenceCurrency(), ne.getFixingDate()))
        .thenReturn(1.6d);
    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    assertEquals(
        test.futureValue(ne, mockProv),
        ne.getNotional() * 1.6d, 0d);
  }

  public void test_futureValueSensitivity() {
    FxResetNotionalExchange ne = FX_RESET_NOTIONAL_EXCHANGE;
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.fxIndexRate(ne.getIndex(), ne.getReferenceCurrency(), ne.getFixingDate()))
        .thenReturn(1.6d);
    DiscountingFxResetNotionalExchangePricer test = new DiscountingFxResetNotionalExchangePricer();
    PointSensitivityBuilder pointSensitivityComputed = test.futureValueSensitivity(ne, mockProv);
    CurveParameterSensitivity parameterSensitivityComputed = mockProv.parameterSensitivity(
        pointSensitivityComputed.build());

  }

}
