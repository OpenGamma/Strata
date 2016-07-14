/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.fx.ResolvedFxSingle;

/**
 * Test {@link DiscountingFxSingleProductPricer}.
 */
@Test
public class DiscountingFxSingleProductPricerTest {

  private static final RatesProvider PROVIDER = RatesProviderFxDataSets.createProvider();
  private static final Currency KRW = Currency.KRW;
  private static final Currency USD = Currency.USD;
  private static final LocalDate PAYMENT_DATE = RatesProviderFxDataSets.VAL_DATE_2014_01_22.plusWeeks(8);
  private static final LocalDate PAYMENT_DATE_PAST = RatesProviderFxDataSets.VAL_DATE_2014_01_22.minusDays(1);
  private static final double NOMINAL_USD = 100_000_000;
  private static final double FX_RATE = 1123.45;
  private static final ResolvedFxSingle FWD = ResolvedFxSingle.of(
      CurrencyAmount.of(USD, NOMINAL_USD), FxRate.of(USD, KRW, FX_RATE), PAYMENT_DATE);
  private static final DiscountingFxSingleProductPricer PRICER = DiscountingFxSingleProductPricer.DEFAULT;
  private static final double TOL = 1.0e-12;
  private static final double EPS_FD = 1E-7;
  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  public void test_presentValue() {
    MultiCurrencyAmount computed = PRICER.presentValue(FWD, PROVIDER);
    double expected1 = NOMINAL_USD * PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double expected2 = -NOMINAL_USD * FX_RATE * PROVIDER.discountFactor(KRW, PAYMENT_DATE);
    assertEquals(computed.getAmount(USD).getAmount(), expected1, NOMINAL_USD * TOL);
    assertEquals(computed.getAmount(KRW).getAmount(), expected2, NOMINAL_USD * TOL);
  }

  public void test_presentValue_ended() {
    ResolvedFxSingle fwd =
        ResolvedFxSingle.of(CurrencyAmount.of(USD, NOMINAL_USD), FxRate.of(USD, KRW, FX_RATE), PAYMENT_DATE_PAST);
    MultiCurrencyAmount computed = PRICER.presentValue(fwd, PROVIDER);
    assertEquals(computed, MultiCurrencyAmount.empty());
  }

  public void test_parSpread() {
    double spread = PRICER.parSpread(FWD, PROVIDER);
    ResolvedFxSingle fwdSp =
        ResolvedFxSingle.of(CurrencyAmount.of(USD, NOMINAL_USD), FxRate.of(USD, KRW, FX_RATE + spread), PAYMENT_DATE);
    MultiCurrencyAmount pv = PRICER.presentValue(fwdSp, PROVIDER);
    assertEquals(pv.convertedTo(USD, PROVIDER).getAmount(), 0d, NOMINAL_USD * TOL);
  }

  public void test_parSpread_ended() {
    ResolvedFxSingle fwd =
        ResolvedFxSingle.of(CurrencyAmount.of(USD, NOMINAL_USD), FxRate.of(USD, KRW, FX_RATE), PAYMENT_DATE_PAST);
    double spread = PRICER.parSpread(fwd, PROVIDER);
    assertEquals(spread, 0d, TOL);
  }

  public void test_forwardFxRate() {
    // forward rate is computed by discounting for any RatesProvider input.
    FxRate computed = PRICER.forwardFxRate(FWD, PROVIDER);
    double df1 = PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double df2 = PROVIDER.discountFactor(KRW, PAYMENT_DATE);
    double spot = PROVIDER.fxRate(USD, KRW);
    FxRate expected = FxRate.of(USD, KRW, spot * df1 / df2);
    assertEquals(computed, expected);
  }

  public void test_forwardFxRatePointSensitivity() {
    PointSensitivityBuilder computed = PRICER.forwardFxRatePointSensitivity(FWD, PROVIDER);
    FxForwardSensitivity expected = FxForwardSensitivity.of(CurrencyPair.of(USD, KRW), USD, FWD.getPaymentDate(), 1d);
    assertEquals(computed, expected);
  }

  public void test_forwardFxRateSpotSensitivity() {
    double computed = PRICER.forwardFxRateSpotSensitivity(FWD, PROVIDER);
    double df1 = PROVIDER.discountFactor(USD, PAYMENT_DATE);
    double df2 = PROVIDER.discountFactor(KRW, PAYMENT_DATE);
    assertEquals(computed, df1 / df2);
  }

  public void test_presentValueSensitivity() {
    PointSensitivities point = PRICER.presentValueSensitivity(FWD, PROVIDER);
    CurrencyParameterSensitivities computed = PROVIDER.parameterSensitivity(point);
    CurrencyParameterSensitivities expectedUsd =
        CAL_FD.sensitivity(PROVIDER, (p) -> PRICER.presentValue(FWD, (p)).getAmount(USD));
    CurrencyParameterSensitivities expectedKrw =
        CAL_FD.sensitivity(PROVIDER, (p) -> PRICER.presentValue(FWD, (p)).getAmount(KRW));
    assertTrue(computed.equalWithTolerance(expectedUsd.combinedWith(expectedKrw), NOMINAL_USD * FX_RATE * EPS_FD));
  }

  public void test_presentValueSensitivity_ended() {
    ResolvedFxSingle fwd =
        ResolvedFxSingle.of(CurrencyAmount.of(USD, NOMINAL_USD), FxRate.of(USD, KRW, FX_RATE), PAYMENT_DATE_PAST);
    PointSensitivities computed = PRICER.presentValueSensitivity(fwd, PROVIDER);
    assertEquals(computed, PointSensitivities.empty());
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    MultiCurrencyAmount computed = PRICER.currencyExposure(FWD, PROVIDER);
    MultiCurrencyAmount expected = PRICER.presentValue(FWD, PROVIDER);
    assertEquals(computed, expected);
  }

  public void test_currentCash_zero() {
    MultiCurrencyAmount computed = PRICER.currentCash(FWD, PROVIDER.getValuationDate());
    assertEquals(computed, MultiCurrencyAmount.empty());
  }

  public void test_currentCash_onPayment() {
    MultiCurrencyAmount computed = PRICER.currentCash(FWD, PAYMENT_DATE);
    assertEquals(computed, MultiCurrencyAmount.of(
        FWD.getBaseCurrencyPayment().getValue(),
        FWD.getCounterCurrencyPayment().getValue()));
  }
}
