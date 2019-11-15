/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.fx.ResolvedFxSwap;

/**
 * Test {@link DiscountingFxSwapProductPricer}.
 */
public class DiscountingFxSwapProductPricerTest {

  private static final RatesProvider PROVIDER = RatesProviderFxDataSets.createProvider();
  private static final Currency KRW = Currency.KRW;
  private static final Currency USD = Currency.USD;
  private static final LocalDate PAYMENT_DATE_NEAR = RatesProviderFxDataSets.VAL_DATE_2014_01_22.plusWeeks(1);
  private static final LocalDate PAYMENT_DATE_FAR = PAYMENT_DATE_NEAR.plusMonths(1);
  private static final LocalDate PAYMENT_DATE_PAST = PAYMENT_DATE_NEAR.minusMonths(1);
  private static final LocalDate PAYMENT_DATE_LONG_PAST = PAYMENT_DATE_NEAR.minusMonths(2);
  private static final double NOMINAL_USD = 100_000_000;
  private static final double FX_RATE = 1109.5;
  private static final double FX_FWD_POINTS = 4.45;
  private static final ResolvedFxSwap SWAP_PRODUCT = ResolvedFxSwap.ofForwardPoints(
      CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, PAYMENT_DATE_NEAR, PAYMENT_DATE_FAR);
  private static final DiscountingFxSwapProductPricer PRICER = DiscountingFxSwapProductPricer.DEFAULT;
  private static final double TOL = 1.0e-12;
  private static final double EPS_FD = 1E-7;
  private static final double TOLERANCE_SPREAD_DELTA = 1.0e-4;
  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue_beforeStart() {
    MultiCurrencyAmount computed = PRICER.presentValue(SWAP_PRODUCT, PROVIDER);
    double expectedUsd = NOMINAL_USD *
        (PROVIDER.discountFactor(USD, PAYMENT_DATE_NEAR) - PROVIDER.discountFactor(USD, PAYMENT_DATE_FAR));
    double expectedKrw = NOMINAL_USD *
        (-FX_RATE * PROVIDER.discountFactor(KRW, PAYMENT_DATE_NEAR)
        + (FX_RATE + FX_FWD_POINTS) * PROVIDER.discountFactor(KRW, PAYMENT_DATE_FAR));
    assertThat(computed.getAmount(USD).getAmount()).isCloseTo(expectedUsd, offset(NOMINAL_USD * TOL));
    assertThat(computed.getAmount(KRW).getAmount()).isCloseTo(expectedKrw, offset(NOMINAL_USD * FX_RATE * TOL));

    // currency exposure
    MultiCurrencyAmount exposure = PRICER.currencyExposure(SWAP_PRODUCT, PROVIDER);
    assertThat(exposure).isEqualTo(computed);
  }

  @Test
  public void test_presentValue_started() {
    ResolvedFxSwap product = ResolvedFxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, PAYMENT_DATE_PAST, PAYMENT_DATE_NEAR);
    MultiCurrencyAmount computed = PRICER.presentValue(product, PROVIDER);
    double expectedUsd = -NOMINAL_USD * PROVIDER.discountFactor(USD, PAYMENT_DATE_NEAR);
    double expectedKrw = NOMINAL_USD * (FX_RATE + FX_FWD_POINTS) * PROVIDER.discountFactor(KRW, PAYMENT_DATE_NEAR);
    assertThat(computed.getAmount(USD).getAmount()).isCloseTo(expectedUsd, offset(NOMINAL_USD * TOL));
    assertThat(computed.getAmount(KRW).getAmount()).isCloseTo(expectedKrw, offset(NOMINAL_USD * FX_RATE * TOL));

    // currency exposure
    MultiCurrencyAmount exposure = PRICER.currencyExposure(product, PROVIDER);
    assertThat(exposure).isEqualTo(computed);
  }

  @Test
  public void test_presentValue_ended() {
    ResolvedFxSwap product = ResolvedFxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, PAYMENT_DATE_LONG_PAST, PAYMENT_DATE_PAST);
    MultiCurrencyAmount computed = PRICER.presentValue(product, PROVIDER);
    assertThat(computed).isEqualTo(MultiCurrencyAmount.empty());

    // currency exposure
    MultiCurrencyAmount exposure = PRICER.currencyExposure(product, PROVIDER);
    assertThat(exposure).isEqualTo(computed);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parSpread_beforeStart() {
    double parSpread = PRICER.parSpread(SWAP_PRODUCT, PROVIDER);
    ResolvedFxSwap product = ResolvedFxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS + parSpread, PAYMENT_DATE_NEAR, PAYMENT_DATE_FAR);
    MultiCurrencyAmount pv = PRICER.presentValue(product, PROVIDER);
    assertThat(pv.convertedTo(USD, PROVIDER).getAmount()).isCloseTo(0d, offset(NOMINAL_USD * TOL));
  }

  @Test
  public void test_parSpread_started() {
    ResolvedFxSwap product = ResolvedFxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, PAYMENT_DATE_PAST, PAYMENT_DATE_NEAR);
    double parSpread = PRICER.parSpread(product, PROVIDER);
    ResolvedFxSwap productPar = ResolvedFxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS + parSpread, PAYMENT_DATE_PAST, PAYMENT_DATE_NEAR);
    MultiCurrencyAmount pv = PRICER.presentValue(productPar, PROVIDER);
    assertThat(pv.convertedTo(USD, PROVIDER).getAmount()).isCloseTo(0d, offset(NOMINAL_USD * TOL));
  }

  @Test
  public void test_parSpread_ended() {
    ResolvedFxSwap product = ResolvedFxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, PAYMENT_DATE_LONG_PAST, PAYMENT_DATE_PAST);
    double parSpread = PRICER.parSpread(product, PROVIDER);
    assertThat(parSpread).isCloseTo(0d, offset(TOL));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity_beforeStart() {
    PointSensitivities point = PRICER.presentValueSensitivity(SWAP_PRODUCT, PROVIDER);
    CurrencyParameterSensitivities computed = PROVIDER.parameterSensitivity(point);
    CurrencyParameterSensitivities expectedUsd = CAL_FD.sensitivity(
        PROVIDER, (p) -> PRICER.presentValue(SWAP_PRODUCT, (p)).getAmount(USD));
    CurrencyParameterSensitivities expectedKrw = CAL_FD.sensitivity(
        PROVIDER, (p) -> PRICER.presentValue(SWAP_PRODUCT, (p)).getAmount(KRW));
    assertThat(computed.equalWithTolerance(expectedUsd.combinedWith(expectedKrw), NOMINAL_USD * FX_RATE * EPS_FD)).isTrue();
  }

  @Test
  public void test_presentValueSensitivity_started() {
    ResolvedFxSwap product = ResolvedFxSwap.ofForwardPoints(CurrencyAmount.of(
        USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, PAYMENT_DATE_PAST, PAYMENT_DATE_NEAR);
    PointSensitivities point = PRICER.presentValueSensitivity(product, PROVIDER);
    CurrencyParameterSensitivities computed = PROVIDER.parameterSensitivity(point);
    CurrencyParameterSensitivities expectedUsd = CAL_FD.sensitivity(
        PROVIDER, (p) -> PRICER.presentValue(product, (p)).getAmount(USD));
    CurrencyParameterSensitivities expectedKrw = CAL_FD.sensitivity(
        PROVIDER, (p) -> PRICER.presentValue(product, (p)).getAmount(KRW));
    assertThat(computed.equalWithTolerance(expectedUsd.combinedWith(expectedKrw), NOMINAL_USD * FX_RATE * EPS_FD)).isTrue();
  }

  @Test
  public void test_presentValueSensitivity_ended() {
    ResolvedFxSwap product = ResolvedFxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, PAYMENT_DATE_LONG_PAST, PAYMENT_DATE_PAST);
    PointSensitivities computed = PRICER.presentValueSensitivity(product, PROVIDER);
    assertThat(computed).isEqualTo(PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parSpreadSensitivity_beforeStart() {
    PointSensitivities pts = PRICER.parSpreadSensitivity(SWAP_PRODUCT, PROVIDER);
    CurrencyParameterSensitivities computed = PROVIDER.parameterSensitivity(pts);
    CurrencyParameterSensitivities expected = CAL_FD.sensitivity(
        PROVIDER, (p) -> CurrencyAmount.of(KRW, PRICER.parSpread(SWAP_PRODUCT, p)));
    assertThat(computed.equalWithTolerance(expected, TOLERANCE_SPREAD_DELTA)).isTrue();
  }

  @Test
  public void test_parSpreadSensitivity_started() {
    ResolvedFxSwap product = ResolvedFxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, PAYMENT_DATE_PAST, PAYMENT_DATE_NEAR);
    PointSensitivities pts = PRICER.parSpreadSensitivity(product, PROVIDER);
    CurrencyParameterSensitivities computed = PROVIDER.parameterSensitivity(pts);
    CurrencyParameterSensitivities expected = CAL_FD.sensitivity(
        PROVIDER, (p) -> CurrencyAmount.of(KRW, PRICER.parSpread(product, p)));
    assertThat(computed.equalWithTolerance(expected, TOLERANCE_SPREAD_DELTA)).isTrue();
  }

  @Test
  public void test_parSpreadSensitivity_ended() {
    ResolvedFxSwap product = ResolvedFxSwap.ofForwardPoints(
        CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, PAYMENT_DATE_LONG_PAST, PAYMENT_DATE_PAST);
    PointSensitivities pts = PRICER.parSpreadSensitivity(product, PROVIDER);
    CurrencyParameterSensitivities computed = PROVIDER.parameterSensitivity(pts);
    assertThat(computed.equalWithTolerance(CurrencyParameterSensitivities.empty(), TOLERANCE_SPREAD_DELTA)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_currentCash_zero() {
    MultiCurrencyAmount computed = PRICER.currentCash(SWAP_PRODUCT, PROVIDER.getValuationDate());
    assertThat(computed).isEqualTo(MultiCurrencyAmount.empty());
  }

  @Test
  public void test_currentCash_firstPayment() {
    MultiCurrencyAmount computed = PRICER.currentCash(SWAP_PRODUCT, PAYMENT_DATE_NEAR);
    assertThat(computed).isEqualTo(MultiCurrencyAmount.of(
        SWAP_PRODUCT.getNearLeg().getBaseCurrencyPayment().getValue(),
        SWAP_PRODUCT.getNearLeg().getCounterCurrencyPayment().getValue()));
  }

  @Test
  public void test_currentCash_secondPayment() {
    MultiCurrencyAmount computed = PRICER.currentCash(SWAP_PRODUCT, PAYMENT_DATE_FAR);
    assertThat(computed).isEqualTo(MultiCurrencyAmount.of(
        SWAP_PRODUCT.getFarLeg().getBaseCurrencyPayment().getValue(),
        SWAP_PRODUCT.getFarLeg().getCounterCurrencyPayment().getValue()));
  }

}
