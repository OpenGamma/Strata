/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.testng.Assert.assertEquals;

import java.util.Map.Entry;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivities;
import com.opengamma.strata.market.sensitivity.NameCurrencySensitivityKey;
import com.opengamma.strata.market.sensitivity.SensitivityKey;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Tests {@link RatesFiniteDifferenceSensitivityCalculator}.
 */
public class RatesFiniteDifferenceSensitivityCalculatorTest {

  private static final RatesFiniteDifferenceSensitivityCalculator FD_CALCULATOR =
      RatesFiniteDifferenceSensitivityCalculator.DEFAULT;

  private static final double TOLERANCE_DELTA = 1.0E-8;

  @Test
  public void sensitivity_single_curve() {
    CurveParameterSensitivities sensiComputed = FD_CALCULATOR.sensitivity(RatesProviderDataSets.USD_SINGLE, this::fn);
    double[] times = RatesProviderDataSets.TIMES_1;
    ImmutableMap<SensitivityKey, double[]> sensi = sensiComputed.getSensitivities();
    assertEquals(sensi.size(), 1);
    double[] s = sensi.get(NameCurrencySensitivityKey.of(RatesProviderDataSets.USD_SINGLE_NAME, USD));
    assertEquals(s.length, times.length);
    for (int i = 0; i < times.length; i++) {
      assertEquals(s[i], times[i] * 4.0d, TOLERANCE_DELTA);
    }
  }

  @Test
  public void sensitivity_multi_curve() {
    CurveParameterSensitivities sensiComputed = FD_CALCULATOR.sensitivity(RatesProviderDataSets.MULTI_USD, this::fn);
    double[] times1 = RatesProviderDataSets.TIMES_1;
    double[] times2 = RatesProviderDataSets.TIMES_2;
    double[] times3 = RatesProviderDataSets.TIMES_3;
    ImmutableMap<SensitivityKey, double[]> sensi = sensiComputed.getSensitivities();
    assertEquals(sensi.size(), 3);
    double[] s1 = sensi.get(NameCurrencySensitivityKey.of(RatesProviderDataSets.USD_DSC_NAME, USD));
    assertEquals(s1.length, times1.length);
    for (int i = 0; i < times1.length; i++) {
      assertEquals(times1[i] * 2.0d, s1[i], TOLERANCE_DELTA);
    }
    double[] s2 = sensi.get(NameCurrencySensitivityKey.of(RatesProviderDataSets.USD_L3_NAME, USD));
    assertEquals(s2.length, times2.length);
    for (int i = 0; i < times2.length; i++) {
      assertEquals(times2[i], s2[i], TOLERANCE_DELTA);
    }
    double[] s3 = sensi.get(NameCurrencySensitivityKey.of(RatesProviderDataSets.USD_L6_NAME, USD));
    assertEquals(s3.length, times3.length);
    for (int i = 0; i < times3.length; i++) {
      assertEquals(times3[i], s3[i], TOLERANCE_DELTA);
    }
  }

  // private function for testing. Returns the sum of rates multiplied by time
  private CurrencyAmount fn(ImmutableRatesProvider provider) {
    double result = 0.0;
    // Currency
    ImmutableMap<Currency, Curve> mapCurrency = provider.getDiscountCurves();
    for (Entry<Currency, Curve> entry : mapCurrency.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
      result += sumProduct(curveInt);
    }
    // Index
    ImmutableMap<Index, Curve> mapIndex = provider.getIndexCurves();
    for (Entry<Index, Curve> entry : mapIndex.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
      result += sumProduct(curveInt);
    }
    return CurrencyAmount.of(USD, result);
  }

  // compute the sum of the product of times and rates
  private double sumProduct(InterpolatedNodalCurve curveInt) {
    double result = 0.0;
    double[] x = curveInt.getXValues();
    double[] y = curveInt.getYValues();
    int nbNodePoint = x.length;
    for (int i = 0; i < nbNodePoint; i++) {
      result += x[i] * y[i];
    }
    return result;
  }

  // check that the curve is yield curve and the underlying is an InterpolatedDoublesCurve and returns the last
  private InterpolatedNodalCurve checkInterpolated(Curve curve) {
    ArgChecker.isTrue(curve instanceof InterpolatedNodalCurve, "Curve should be a InterpolatedNodalCurve");
    return (InterpolatedNodalCurve) curve;
  }

}
