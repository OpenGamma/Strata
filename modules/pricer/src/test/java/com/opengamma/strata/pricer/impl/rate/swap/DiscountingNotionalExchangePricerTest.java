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
import com.opengamma.strata.pricer.RatesProvider;
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
    RatesProvider mockProv = mock(RatesProvider.class);
    when(mockProv.discountFactor(NOTIONAL_EXCHANGE_REC_GBP.getCurrency(), NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate()))
        .thenReturn(discountFactor);
    DiscountingNotionalExchangePricer test = new DiscountingNotionalExchangePricer();
    assertEquals(
        test.presentValue(NOTIONAL_EXCHANGE_REC_GBP, mockProv),
        NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount().getAmount() * discountFactor, 0d);
  }

  public void test_futureValue() {
    RatesProvider mockProv = mock(RatesProvider.class);
    DiscountingNotionalExchangePricer test = new DiscountingNotionalExchangePricer();
    assertEquals(
        test.futureValue(NOTIONAL_EXCHANGE_REC_GBP, mockProv),
        NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount().getAmount(), 0d);
  }

  /**
  * Test present value sensitivity.
  */
  public void test_presentValueSensitivity() {
    double discountFactor = 0.98d;
    double paymentTime = 0.75;
    RatesProvider mockProv = mock(RatesProvider.class);
    LocalDate paymentDate = NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate();
    when(mockProv.relativeTime(paymentDate))
        .thenReturn(paymentTime);
    when(mockProv.discountFactor(NOTIONAL_EXCHANGE_REC_GBP.getCurrency(), paymentDate))
        .thenReturn(discountFactor);
    PointSensitivityBuilder builder = ZeroRateSensitivity.of(NOTIONAL_EXCHANGE_REC_GBP.getCurrency(),
        paymentDate, -discountFactor * paymentTime); // this is implemented in provider
    when(mockProv.discountFactorZeroRateSensitivity(NOTIONAL_EXCHANGE_REC_GBP.getCurrency(), paymentDate))
        .thenReturn(builder);
    DiscountingNotionalExchangePricer pricer = DiscountingNotionalExchangePricer.DEFAULT;
    PointSensitivities senseComputed = pricer.presentValueSensitivity(NOTIONAL_EXCHANGE_REC_GBP, mockProv).build();

    double eps = 1.0e-7;
    PointSensitivities senseExpected = PointSensitivities.of(dscSensitivityFD(mockProv,
        NOTIONAL_EXCHANGE_REC_GBP, eps));
    CurveSensitivityTestUtil.assertMulticurveSensitivity(senseComputed, senseExpected, NOTIONAL_EXCHANGE_REC_GBP
        .getPaymentAmount().getAmount() * eps);
  }

  /**
  * Test future value sensitivity.
  */
  public void test_futureValueSensitivity() {
    RatesProvider mockProv = mock(RatesProvider.class);
    DiscountingNotionalExchangePricer pricer = DiscountingNotionalExchangePricer.DEFAULT;
    PointSensitivities senseComputed = pricer.futureValueSensitivity(NOTIONAL_EXCHANGE_REC_GBP, mockProv).build();

    double eps = 1.0e-12;
    PointSensitivities senseExpected = PointSensitivities.NONE;
    CurveSensitivityTestUtil.assertMulticurveSensitivity(senseComputed, senseExpected, NOTIONAL_EXCHANGE_REC_GBP
        .getPaymentAmount().getAmount() * eps);
  }

  private List<ZeroRateSensitivity> dscSensitivityFD(RatesProvider provider, NotionalExchange event, double eps) {
    Currency currency = event.getCurrency();
    LocalDate paymentDate = event.getPaymentDate();
    double discountFactor = provider.discountFactor(currency, paymentDate);
    double paymentTime = provider.relativeTime(paymentDate);
    RatesProvider provUp = mock(RatesProvider.class);
    RatesProvider provDw = mock(RatesProvider.class);
    when(provUp.discountFactor(currency, paymentDate)).thenReturn(discountFactor * Math.exp(-eps * paymentTime));
    when(provDw.discountFactor(currency, paymentDate)).thenReturn(discountFactor * Math.exp(eps * paymentTime));
    DiscountingNotionalExchangePricer pricer = DiscountingNotionalExchangePricer.DEFAULT;
    double pvUp = pricer.presentValue(event, provUp);
    double pvDw = pricer.presentValue(event, provDw);
    double res = 0.5 * (pvUp - pvDw) / eps;
    List<ZeroRateSensitivity> zeroRateSensi = new ArrayList<>();
    zeroRateSensi.add(ZeroRateSensitivity.of(currency, paymentDate, res));
    return zeroRateSensi;
  }
}
