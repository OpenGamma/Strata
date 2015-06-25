/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.sensitivity.FxForwardSensitivity;

/**
 * Test {@link DiscountFxForwardRates}.
 */
@Test
public class DiscountFxForwardRatesTest {

  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_REF = date(2015, 7, 30);
  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(GBP, USD);

  private static final CurveInterpolator INTERPOLATOR = Interpolator1DFactory.LINEAR_INSTANCE;
  private static final CurveName NAME1 = CurveName.of("TestCurve");
  private static final CurveName NAME2 = CurveName.of("TestCurveUSD");
  private static final InterpolatedNodalCurve CURVE1 =
      InterpolatedNodalCurve.of(NAME1, ACT_365F, new double[] {0, 10 }, new double[] {0.01, 0.02 }, INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE2 =
      InterpolatedNodalCurve.of(NAME2, ACT_365F, new double[] {0, 10 }, new double[] {0.015, 0.025 }, INTERPOLATOR);
  private static final ZeroRateDiscountFactors DFCURVE_GBP = ZeroRateDiscountFactors.of(GBP, DATE_VAL, CURVE1);
  private static final ZeroRateDiscountFactors DFCURVE_GBP2 = ZeroRateDiscountFactors.of(GBP, DATE_VAL, CURVE2);
  private static final ZeroRateDiscountFactors DFCURVE_USD = ZeroRateDiscountFactors.of(USD, DATE_VAL, CURVE2);
  private static final ZeroRateDiscountFactors DFCURVE_USD2 = ZeroRateDiscountFactors.of(USD, DATE_VAL, CURVE1);

  //-------------------------------------------------------------------------
  public void test_of() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getBaseCurrencyDiscountFactors(), DFCURVE_GBP);
    assertEquals(test.getCounterCurrencyDiscountFactors(), DFCURVE_USD);
    assertEquals(test.getFxRateProvider(), FX_RATE);
  }

  public void test_of_nonMatchingCurrency() {
    assertThrowsIllegalArg(() -> DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_GBP));
    assertThrowsIllegalArg(() -> DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_USD, DFCURVE_USD));
  }

  public void test_of_nonMatchingValuationDates() {
    DiscountFactors curve2 = ZeroRateDiscountFactors.of(USD, DATE_REF, CURVE2);
    assertThrowsIllegalArg(() -> DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, curve2));
  }

  //-------------------------------------------------------------------------
  public void test_withDiscountFactors() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    test = test.withDiscountFactors(DFCURVE_GBP2, DFCURVE_USD2);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getBaseCurrencyDiscountFactors(), DFCURVE_GBP2);
    assertEquals(test.getCounterCurrencyDiscountFactors(), DFCURVE_USD2);
    assertEquals(test.getFxRateProvider(), FX_RATE);
  }

  //-------------------------------------------------------------------------
  public void test_rate() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR,  FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    double dfCcyBaseAtMaturity = DFCURVE_GBP.discountFactor(DATE_REF);
    double dfCcyCounterAtMaturity = DFCURVE_USD.discountFactor(DATE_REF);
    double expected = FX_RATE.fxRate(GBP, USD) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
    assertEquals(test.rate(GBP, DATE_REF), expected, 1e-12);
    assertEquals(test.rate(USD, DATE_REF), 1d / expected, 1e-12);
  }

  public void test_rate_nonMatchingCurrency() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertThrowsIllegalArg(() -> test.rate(EUR, DATE_VAL));
  }

  //-------------------------------------------------------------------------
  public void test_ratePointSensitivity() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertEquals(test.ratePointSensitivity(GBP, DATE_REF),
        FxForwardSensitivity.of(CURRENCY_PAIR, GBP, DATE_REF, 1d));
    assertEquals(test.ratePointSensitivity(USD, DATE_REF),
        FxForwardSensitivity.of(CURRENCY_PAIR, USD, DATE_REF, 1d));
  }

  public void test_ratePointSensitivity_nonMatchingCurrency() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertThrowsIllegalArg(() -> test.ratePointSensitivity(EUR, DATE_VAL));
  }

  //-------------------------------------------------------------------------
  public void test_rateSpotSensitivity() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    double dfCcyBaseAtMaturity = DFCURVE_GBP.discountFactor(DATE_REF);
    double dfCcyCounterAtMaturity = DFCURVE_USD.discountFactor(DATE_REF);
    double expected = dfCcyBaseAtMaturity / dfCcyCounterAtMaturity;
    assertEquals(test.rateSpotSensitivity(GBP, DATE_REF), expected, 1e-12);
    assertEquals(test.rateSpotSensitivity(USD, DATE_REF), 1d / expected, 1e-12);
  }

  public void test_rateSpotSensitivity_nonMatchingCurrency() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertThrowsIllegalArg(() -> test.rateSpotSensitivity(EUR, DATE_VAL));
  }

  //-------------------------------------------------------------------------
  //proper end-to-end tests are elsewhere
  public void test_curveParameterSensitivity() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    FxForwardSensitivity point = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, DATE_VAL, 1d);
    assertEquals(test.curveParameterSensitivity(point).size(), 2);
    FxForwardSensitivity point2 = FxForwardSensitivity.of(CURRENCY_PAIR, USD, DATE_VAL, 1d);
    assertEquals(test.curveParameterSensitivity(point2).size(), 2);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DiscountFxForwardRates test1 = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    coverImmutableBean(test1);
    DiscountFxForwardRates test2 =
        DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE.inverse(), DFCURVE_GBP2, DFCURVE_USD2);
    coverBeanEquals(test1, test2);
    DiscountFxForwardRates test3 = DiscountFxForwardRates.of(CurrencyPair.of(USD, EUR), FxRate.of(EUR, USD, 1.2d),
        DFCURVE_USD, ZeroRateDiscountFactors.of(EUR, DATE_VAL, CURVE2));
    coverBeanEquals(test1, test3);
  }

}
