/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.fx.FxSwap;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;

/**
 * Test {@link DiscountingFxSwapProductPricer}.
 */
@Test
public class DiscountingFxSwapProductPricerTest {

  private static final RatesProvider PROVIDER = RatesProviderFxDataSets.createProvider();
  private static final Currency KRW = Currency.KRW;
  private static final Currency USD = Currency.USD;
  private static final LocalDate PAYMENT_DATE_NEAR = date(2011, 11, 21);
  private static final LocalDate PAYMENT_DATE_FAR = date(2011, 12, 21);
  private static final double NOMINAL_USD = 100_000_000;
  private static final double FX_RATE = 1109.5;
  private static final double FX_FWD_POINTS = 4.45;
  private static final FxSwap SWAP_PRODUCT = FxSwap.ofForwardPoints(
      CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, PAYMENT_DATE_NEAR, PAYMENT_DATE_FAR);
  private static final DiscountingFxSwapProductPricer PRICER = DiscountingFxSwapProductPricer.DEFAULT;
  private static final double TOL = 1.0e-12;
  private static final double EPS_FD = 1E-7;
  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  //-------------------------------------------------------------------------
  public void test_presentValue_beforeStart() {
    MultiCurrencyAmount computed = PRICER.presentValue(SWAP_PRODUCT, PROVIDER);
    double expected_usd = NOMINAL_USD *
        (PROVIDER.discountFactor(USD, PAYMENT_DATE_NEAR) - PROVIDER.discountFactor(USD, PAYMENT_DATE_FAR));
    double expected_krw = NOMINAL_USD *
        (-FX_RATE * PROVIDER.discountFactor(KRW, PAYMENT_DATE_NEAR)
        + (FX_RATE + FX_FWD_POINTS) * PROVIDER.discountFactor(KRW, PAYMENT_DATE_FAR));
    assertEquals(computed.getAmount(USD).getAmount(), expected_usd, NOMINAL_USD * TOL);
    assertEquals(computed.getAmount(KRW).getAmount(), expected_krw, NOMINAL_USD * FX_RATE * TOL);

    // currency exposure
    MultiCurrencyAmount exposure = PRICER.currencyExposure(SWAP_PRODUCT, PROVIDER);
    assertEquals(exposure, computed);
  }

  public void test_presentValue_started() {
    FxSwap product = FxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, date(2011, 10, 21), PAYMENT_DATE_NEAR);
    MultiCurrencyAmount computed = PRICER.presentValue(product, PROVIDER);
    double expected_usd = -NOMINAL_USD * PROVIDER.discountFactor(USD, PAYMENT_DATE_NEAR);
    double expected_krw = NOMINAL_USD * (FX_RATE + FX_FWD_POINTS) * PROVIDER.discountFactor(KRW, PAYMENT_DATE_NEAR);
    assertEquals(computed.getAmount(USD).getAmount(), expected_usd, NOMINAL_USD * TOL);
    assertEquals(computed.getAmount(KRW).getAmount(), expected_krw, NOMINAL_USD * FX_RATE * TOL);

    // currency exposure
    MultiCurrencyAmount exposure = PRICER.currencyExposure(product, PROVIDER);
    assertEquals(exposure, computed);
  }

  public void test_presentValue_ended() {
    FxSwap product = FxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, date(2011, 9, 21), date(2011, 10, 21));
    MultiCurrencyAmount computed = PRICER.presentValue(product, PROVIDER);
    assertEquals(computed, MultiCurrencyAmount.empty());

    // currency exposure
    MultiCurrencyAmount exposure = PRICER.currencyExposure(product, PROVIDER);
    assertEquals(exposure, computed);
  }

  //-------------------------------------------------------------------------
  public void test_parSpread_beforeStart() {
    double parSpread = PRICER.parSpread(SWAP_PRODUCT, PROVIDER);
    FxSwap product = FxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS + parSpread, PAYMENT_DATE_NEAR, PAYMENT_DATE_FAR);
    MultiCurrencyAmount pv = PRICER.presentValue(product, PROVIDER);
    assertEquals(pv.convertedTo(USD, PROVIDER).getAmount(), 0d, NOMINAL_USD * TOL);
  }

  public void test_parSpread_started() {
    FxSwap product = FxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, date(2011, 10, 21), PAYMENT_DATE_NEAR);
    double parSpread = PRICER.parSpread(product, PROVIDER);
    FxSwap productPar = FxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS + parSpread, date(2011, 10, 21), PAYMENT_DATE_NEAR);
    MultiCurrencyAmount pv = PRICER.presentValue(productPar, PROVIDER);
    assertEquals(pv.convertedTo(USD, PROVIDER).getAmount(), 0d, NOMINAL_USD * TOL);
  }

  public void test_parSpread_ended() {
    FxSwap product = FxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, date(2011, 9, 21), date(2011, 10, 21));
    double parSpread = PRICER.parSpread(product, PROVIDER);
    assertEquals(parSpread, 0d, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity_beforeStart() {
    PointSensitivities point = PRICER.presentValueSensitivity(SWAP_PRODUCT, PROVIDER);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expectedUsd = CAL_FD.sensitivity(
        (ImmutableRatesProvider) PROVIDER, (p) -> PRICER.presentValue(SWAP_PRODUCT, (p)).getAmount(USD));
    CurveCurrencyParameterSensitivities expectedKrw = CAL_FD.sensitivity(
        (ImmutableRatesProvider) PROVIDER, (p) -> PRICER.presentValue(SWAP_PRODUCT, (p)).getAmount(KRW));
    assertTrue(computed.equalWithTolerance(expectedUsd.combinedWith(expectedKrw), NOMINAL_USD * FX_RATE * EPS_FD));
  }

  public void test_presentValueSensitivity_started() {
    FxSwap product = FxSwap.ofForwardPoints(CurrencyAmount.of(
        USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, date(2011, 10, 21), PAYMENT_DATE_NEAR);
    PointSensitivities point = PRICER.presentValueSensitivity(product, PROVIDER);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expectedUsd = CAL_FD.sensitivity(
        (ImmutableRatesProvider) PROVIDER, (p) -> PRICER.presentValue(product, (p)).getAmount(USD));
    CurveCurrencyParameterSensitivities expectedKrw = CAL_FD.sensitivity(
        (ImmutableRatesProvider) PROVIDER, (p) -> PRICER.presentValue(product, (p)).getAmount(KRW));
    assertTrue(computed.equalWithTolerance(expectedUsd.combinedWith(expectedKrw), NOMINAL_USD * FX_RATE * EPS_FD));
  }

  public void test_presentValueSensitivity_ended() {
    FxSwap product = FxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, date(2011, 9, 21), date(2011, 10, 21));
    PointSensitivities computed = PRICER.presentValueSensitivity(product, PROVIDER);
    assertEquals(computed, PointSensitivities.empty());
  }

}
