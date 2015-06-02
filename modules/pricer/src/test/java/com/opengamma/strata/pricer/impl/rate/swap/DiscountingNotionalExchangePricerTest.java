/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE_REC_GBP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.finance.rate.swap.NotionalExchange;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;

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
    LocalDate valDate = NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate().minusDays(90);
    double paymentTime = ACT_360.relativeYearFraction(valDate, NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate());
    DiscountFactors mockDf = mock(DiscountFactors.class);
    SimpleRatesProvider simpleProv = new SimpleRatesProvider(valDate, mockDf);
    simpleProv.setDayCount(ACT_360);

    LocalDate paymentDate = NOTIONAL_EXCHANGE_REC_GBP.getPaymentDate();
    when(mockDf.discountFactor(paymentDate)).thenReturn(discountFactor);
    PointSensitivityBuilder builder = ZeroRateSensitivity.of(NOTIONAL_EXCHANGE_REC_GBP.getCurrency(),
        paymentDate, -discountFactor * paymentTime); // this is implemented in provider
    when(mockDf.pointSensitivity(paymentDate)).thenReturn(builder);
    DiscountingNotionalExchangePricer pricer = DiscountingNotionalExchangePricer.DEFAULT;
    PointSensitivities senseComputed = pricer.presentValueSensitivity(NOTIONAL_EXCHANGE_REC_GBP, simpleProv).build();

    double eps = 1.0e-7;
    PointSensitivities senseExpected = PointSensitivities.of(dscSensitivityFD(simpleProv,
        NOTIONAL_EXCHANGE_REC_GBP, eps));
    assertTrue(senseComputed.equalWithTolerance(
        senseExpected, NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount().getAmount() * eps));
  }

  /**
  * Test future value sensitivity.
  */
  public void test_futureValueSensitivity() {
    RatesProvider mockProv = mock(RatesProvider.class);
    DiscountingNotionalExchangePricer pricer = DiscountingNotionalExchangePricer.DEFAULT;
    PointSensitivities senseComputed = pricer.futureValueSensitivity(NOTIONAL_EXCHANGE_REC_GBP, mockProv).build();

    double eps = 1.0e-12;
    PointSensitivities senseExpected = PointSensitivities.empty();
    assertTrue(senseComputed.equalWithTolerance(
        senseExpected, NOTIONAL_EXCHANGE_REC_GBP.getPaymentAmount().getAmount() * eps));
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
