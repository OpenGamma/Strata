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
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.pricer.ImmutableRatesProvider;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.sensitivity.NameCurrencySensitivityKey;
import com.opengamma.strata.pricer.sensitivity.SensitivityKey;

/**
 * Tests {@link RatesFiniteDifferenceSensitivityCalculator}.
 */
public class RatesFiniteDifferenceSensitivityCalculatorTest {

  private static final RatesFiniteDifferenceSensitivityCalculator FD_CALCULATOR =
      RatesFiniteDifferenceSensitivityCalculator.DEFAULT;

  private static final double TOLERANCE_DELTA = 1.0E-8;

  @Test
  public void sensitivity_single_curve() {
    CurveParameterSensitivity sensiComputed = FD_CALCULATOR.sensitivity(RatesProviderDataSets.USD_SINGLE, this::fn);
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
    CurveParameterSensitivity sensiComputed = FD_CALCULATOR.sensitivity(RatesProviderDataSets.USD_MULTI, this::fn);
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
    ImmutableMap<Currency, YieldAndDiscountCurve> mapCurrency = provider.getDiscountCurves();
    for (Entry<Currency, YieldAndDiscountCurve> entry : mapCurrency.entrySet()) {
      InterpolatedDoublesCurve curveInt = checkInterpolated(entry.getValue());
      result += sumProduct(curveInt);
    }
    // Index
    ImmutableMap<Index, YieldAndDiscountCurve> mapIndex = provider.getIndexCurves();
    for (Entry<Index, YieldAndDiscountCurve> entry : mapIndex.entrySet()) {
      InterpolatedDoublesCurve curveInt = checkInterpolated(entry.getValue());
      result += sumProduct(curveInt);
    }
    return CurrencyAmount.of(USD, result);
  }

  // compute the sum of the product of times and rates
  private double sumProduct(InterpolatedDoublesCurve curveInt) {
    double result = 0.0;
    double[] x = curveInt.getXDataAsPrimitive();
    double[] y = curveInt.getYDataAsPrimitive();
    int nbNodePoint = curveInt.getXDataAsPrimitive().length;
    for (int i = 0; i < nbNodePoint; i++) {
      result += x[i] * y[i];
    }
    return result;
  }

  // check that the curve is yield curve and the underlying is an InterpolatedDoublesCurve and returns the last
  private InterpolatedDoublesCurve checkInterpolated(YieldAndDiscountCurve curve) {
    ArgChecker.isTrue(curve instanceof YieldCurve, "Curve should be a YieldCurve");
    YieldCurve curveYield = (YieldCurve) curve;
    ArgChecker.isTrue(curveYield.getCurve() instanceof InterpolatedDoublesCurve,
        "Yield curve should be based on InterpolatedDoublesCurve");
    return (InterpolatedDoublesCurve) curveYield.getCurve();
  }

}
