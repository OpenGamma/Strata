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
import com.opengamma.strata.finance.fx.FxPayment;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;

/**
 * Test {@link DiscountingFxPaymentPricer}.
 */
@Test
public class DiscountingFxPaymentPricerTest {

  private static final RatesProvider PROVIDER = RatesProviderFxDataSets.createProvider();
  private static final Currency USD = Currency.USD;
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2012, 5, 4);
  private static final double NOMINAL_USD = 100_000_000;
  private static final double FX_RATE = 1123.45;
  private static final FxPayment PAY = FxPayment.of(CurrencyAmount.of(USD, NOMINAL_USD), PAYMENT_DATE);
  private static final DiscountingFxPaymentPricer PRICER = DiscountingFxPaymentPricer.DEFAULT;
  private static final double TOL = 1.0e-12;
  private static final double EPS_FD = 1E-7;
  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    CurrencyAmount computed = PRICER.presentValue(PAY, PROVIDER);
    double expected1 = NOMINAL_USD * PROVIDER.discountFactor(USD, PAYMENT_DATE);
    assertEquals(computed.getAmount(), expected1, NOMINAL_USD * TOL);
  }

  public void test_presentValue_ended() {
    FxPayment fwd = FxPayment.of(CurrencyAmount.of(USD, NOMINAL_USD), LocalDate.of(2011, 11, 2));
    CurrencyAmount computed = PRICER.presentValue(fwd, PROVIDER);
    assertEquals(computed, CurrencyAmount.zero(USD));
  }

  public void test_presentValueSensitivity() {
    PointSensitivities point = PRICER.presentValueSensitivity(PAY, PROVIDER).build();
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expectedUsd = CAL_FD.sensitivity(
        (ImmutableRatesProvider) PROVIDER, (p) -> PRICER.presentValue(PAY, (p)));
    assertTrue(computed.equalWithTolerance(expectedUsd, NOMINAL_USD * FX_RATE * EPS_FD));
  }

  public void test_presentValueSensitivity_ended() {
    FxPayment pay = FxPayment.of(CurrencyAmount.of(USD, NOMINAL_USD), LocalDate.of(2011, 11, 2));
    PointSensitivities computed = PRICER.presentValueSensitivity(pay, PROVIDER).build();
    assertEquals(computed, PointSensitivities.empty());
  }

}
