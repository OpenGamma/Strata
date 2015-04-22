/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.finance.rate.swap.NotionalExchange;
import com.opengamma.strata.pricer.CurveSensitivityTestUtil;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.sensitivity.ZeroRateSensitivity;

/**
 * Test.
 */
@Test
public class DiscountingNotionalExchangePricerTest {

  public void test_presentValue() {
    double discountFactor = 0.98d;
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.discountFactor(NOTIONAL_EXCHANGE_REC_GBP.getCurrency(), NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate()))
        .thenReturn(discountFactor);
    DiscountingNotionalExchangePricer test = new DiscountingNotionalExchangePricer();
    assertEquals(
        test.presentValue(mockEnv, NOTIONAL_EXCHANGE_REC_GBP),
        NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount().getAmount() * discountFactor, 0d);
  }

  public void test_futureValue() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    DiscountingNotionalExchangePricer test = new DiscountingNotionalExchangePricer();
    assertEquals(
        test.futureValue(mockEnv, NOTIONAL_EXCHANGE_REC_GBP),
        NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount().getAmount(), 0d);
  }

  /**
  * Test present value sensitivity.
  */
  public void test_presentValueSensitivity() {
    double discountFactor = 0.98d;
    double paymentTime = 0.75;
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.relativeTime(NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate())).thenReturn(paymentTime);
    when(env.discountFactor(NOTIONAL_EXCHANGE_REC_GBP.getCurrency(), NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate())).thenReturn(
        discountFactor);
    PointSensitivityBuilder builder = ZeroRateSensitivity.of(NOTIONAL_EXCHANGE_REC_GBP.getCurrency(),
        NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate(), -discountFactor * paymentTime); // this is implemented in environment
    when(env.discountFactorZeroRateSensitivity(NOTIONAL_EXCHANGE_REC_GBP.getCurrency(), NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate()))
        .thenReturn(builder);
    DiscountingNotionalExchangePricer pricer = DiscountingNotionalExchangePricer.DEFAULT;
    PointSensitivities senseComputed = pricer.presentValueSensitivity(env, NOTIONAL_EXCHANGE_REC_GBP).build();

    double eps = 1.0e-7;
    PointSensitivities senseExpected = PointSensitivities.of(dscSensitivityFD(env,
        NOTIONAL_EXCHANGE_REC_GBP, eps));
    CurveSensitivityTestUtil.assertMulticurveSensitivity(senseComputed, senseExpected, NOTIONAL_EXCHANGE_REC_GBP
        .getPaymentAmount().getAmount() * eps);
  }

  /**
  * Test future value sensitivity.
  */
  public void test_futureValueSensitivity() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    DiscountingNotionalExchangePricer pricer = DiscountingNotionalExchangePricer.DEFAULT;
    PointSensitivities senseComputed = pricer.futureValueSensitivity(env, NOTIONAL_EXCHANGE_REC_GBP).build();

    double eps = 1.0e-12;
    PointSensitivities senseExpected = PointSensitivities.NONE;
    CurveSensitivityTestUtil.assertMulticurveSensitivity(senseComputed, senseExpected, NOTIONAL_EXCHANGE_REC_GBP
        .getPaymentAmount().getAmount() * eps);
  }

  private List<ZeroRateSensitivity> dscSensitivityFD(PricingEnvironment env, NotionalExchange event, double eps) {
    Currency currency = event.getCurrency();
    LocalDate paymentDate = event.getPaymentDate();
    double discountFactor = env.discountFactor(currency, paymentDate);
    double paymentTime = env.relativeTime(paymentDate);
    PricingEnvironment envUp = mock(PricingEnvironment.class);
    PricingEnvironment envDw = mock(PricingEnvironment.class);
    when(envUp.discountFactor(currency, paymentDate)).thenReturn(discountFactor * Math.exp(-eps * paymentTime));
    when(envDw.discountFactor(currency, paymentDate)).thenReturn(discountFactor * Math.exp(eps * paymentTime));
    DiscountingNotionalExchangePricer pricer = DiscountingNotionalExchangePricer.DEFAULT;
    double pvUp = pricer.presentValue(envUp, event);
    double pvDw = pricer.presentValue(envDw, event);
    double res = 0.5 * (pvUp - pvDw) / eps;
    List<ZeroRateSensitivity> zeroRateSensi = new ArrayList<>();
    zeroRateSensi.add(ZeroRateSensitivity.of(currency, paymentDate, res));
    return zeroRateSensi;
  }
}
