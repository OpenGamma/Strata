/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import static com.opengamma.platform.pricer.impl.swap.SwapDummyData.NOTIONAL_EXCHANGE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.basics.currency.Currency;
import com.opengamma.collect.tuple.Pair;
import com.opengamma.platform.finance.swap.NotionalExchange;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.CurveSensitivityTestUtil;
import com.opengamma.platform.pricer.sensitivity.multicurve.MulticurveSensitivity3LD;
import com.opengamma.platform.pricer.sensitivity.multicurve.ZeroRateSensitivityLD;

/**
 * Test.
 */
@Test
public class DiscountingNotionalExchangePricerFnTest {
  private static final double TOL = 1.0e-12;

  public void test_presentValue() {
    double discountFactor = 0.98d;
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.discountFactor(NOTIONAL_EXCHANGE.getCurrency(), NOTIONAL_EXCHANGE.getPaymentDate()))
        .thenReturn(discountFactor);
    DiscountingNotionalExchangePricerFn test = new DiscountingNotionalExchangePricerFn();
    assertEquals(
        test.presentValue(mockEnv, NOTIONAL_EXCHANGE),
        NOTIONAL_EXCHANGE.getPaymentAmount().getAmount() * discountFactor, 0d);
  }

  public void test_futureValue() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    DiscountingNotionalExchangePricerFn test = new DiscountingNotionalExchangePricerFn();
    assertEquals(
        test.futureValue(mockEnv, NOTIONAL_EXCHANGE),
        NOTIONAL_EXCHANGE.getPaymentAmount().getAmount(), 0d);
  }

  /**
   * Test present value sensitivity. 
   */
  public void test_presentValueSensitivity() {
    double discountFactor = 0.98d;
    double paymentTime = 0.75;
    PricingEnvironment env = mock(PricingEnvironment.class);
    when(env.relativeTime(NOTIONAL_EXCHANGE.getPaymentDate())).thenReturn(paymentTime);
    when(env.discountFactor(NOTIONAL_EXCHANGE.getCurrency(), NOTIONAL_EXCHANGE.getPaymentDate()))
        .thenReturn(discountFactor);
    DiscountingNotionalExchangePricerFn pricer = DiscountingNotionalExchangePricerFn.DEFAULT;
    Pair<Double, MulticurveSensitivity3LD> senseAndPvComputed = pricer.presentValueCurveSensitivity3LD(env,
        NOTIONAL_EXCHANGE);

    double eps = 1.0e-7;
    double pvExpected = pricer.presentValue(env, NOTIONAL_EXCHANGE);
    MulticurveSensitivity3LD senseExpected = MulticurveSensitivity3LD.ofZeroRate(dscSensitivityFD(env,
        NOTIONAL_EXCHANGE, eps));
    assertEquals(senseAndPvComputed.getFirst(), pvExpected, NOTIONAL_EXCHANGE.getPaymentAmount().getAmount() * TOL);
    CurveSensitivityTestUtil.assertMulticurveSensitivity3LD(senseAndPvComputed.getSecond(), senseExpected,
        NOTIONAL_EXCHANGE.getPaymentAmount().getAmount() * eps);
  }

  private List<ZeroRateSensitivityLD> dscSensitivityFD(PricingEnvironment env, NotionalExchange event, double eps) {
    Currency currency = event.getCurrency();
    LocalDate paymentDate = event.getPaymentDate();
    double discountFactor = env.discountFactor(currency, paymentDate);
    double paymentTime = env.relativeTime(paymentDate);
    PricingEnvironment envUp = mock(PricingEnvironment.class);
    PricingEnvironment envDw = mock(PricingEnvironment.class);
    when(envUp.discountFactor(currency, paymentDate)).thenReturn(discountFactor * Math.exp(-eps * paymentTime));
    when(envDw.discountFactor(currency, paymentDate)).thenReturn(discountFactor * Math.exp(eps * paymentTime));
    DiscountingNotionalExchangePricerFn pricer = DiscountingNotionalExchangePricerFn.DEFAULT;
    double pvUp = pricer.presentValue(envUp, event);
    double pvDw = pricer.presentValue(envDw, event);
    double res = 0.5 * (pvUp - pvDw) / eps;
    List<ZeroRateSensitivityLD> zeroRateSensi = new ArrayList<>();
    zeroRateSensi.add(new ZeroRateSensitivityLD(currency, paymentDate, res, currency));
    return zeroRateSensi;
  }
}
