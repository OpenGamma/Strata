/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.ignoreThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.finance.rate.swap.KnownAmountPaymentPeriod;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.RatePaymentPeriod;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.pricer.impl.MockRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.swap.PaymentPeriodPricer;
import com.opengamma.strata.pricer.rate.swap.SwapDummyData;

/**
 * Test.
 */
@Test
public class DispatchingPaymentPeriodPricerTest {

  private static final RatesProvider MOCK_PROV = new MockRatesProvider();
  private static final PaymentPeriodPricer<RatePaymentPeriod> MOCK_RATE = mock(PaymentPeriodPricer.class);
  private static final PaymentPeriodPricer<KnownAmountPaymentPeriod> MOCK_KNOWN = mock(PaymentPeriodPricer.class);

  public void test_presentValue_RatePaymentPeriod() {
    double expected = 0.0123d;
    PaymentPeriodPricer<RatePaymentPeriod> mockNotionalExchangeFn = mock(PaymentPeriodPricer.class);
    when(mockNotionalExchangeFn.presentValue(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(expected);
    DispatchingPaymentPeriodPricer test = new DispatchingPaymentPeriodPricer(mockNotionalExchangeFn, MOCK_KNOWN);
    assertEquals(test.presentValue(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV), expected, 0d);
  }

  public void test_presentValue_unknownType() {
    PaymentPeriod mockPaymentPeriod = mock(PaymentPeriod.class);
    DispatchingPaymentPeriodPricer test = DispatchingPaymentPeriodPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.presentValue(mockPaymentPeriod, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_RatePaymentPeriod() {
    double expected = 0.0123d;
    PaymentPeriodPricer<RatePaymentPeriod> mockNotionalExchangeFn = mock(PaymentPeriodPricer.class);
    when(mockNotionalExchangeFn.futureValue(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV))
        .thenReturn(expected);
    DispatchingPaymentPeriodPricer test = new DispatchingPaymentPeriodPricer(mockNotionalExchangeFn, MOCK_KNOWN);
    assertEquals(test.futureValue(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV), expected, 0d);
  }

  public void test_futureValue_unknownType() {
    PaymentPeriod mockPaymentPeriod = mock(PaymentPeriod.class);
    DispatchingPaymentPeriodPricer test = DispatchingPaymentPeriodPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.futureValue(mockPaymentPeriod, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity_unknownType() {
    PaymentPeriod mockPaymentPeriod = mock(PaymentPeriod.class);
    DispatchingPaymentPeriodPricer test = DispatchingPaymentPeriodPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.presentValueSensitivity(mockPaymentPeriod, MOCK_PROV));
  }

  //-------------------------------------------------------------------------
  public void test_futureValueSensitivity_unknownType() {
    PaymentPeriod mockPaymentPeriod = mock(PaymentPeriod.class);
    DispatchingPaymentPeriodPricer test = DispatchingPaymentPeriodPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.futureValueSensitivity(mockPaymentPeriod, MOCK_PROV));
  }

  //------------------------------------------------------------------------- 
  public void coverage() {
    DispatchingPaymentPeriodPricer test = new DispatchingPaymentPeriodPricer(
        MOCK_RATE,
        MOCK_KNOWN);

    PaymentPeriod kapp = KnownAmountPaymentPeriod.builder()
        .payment(Payment.of(CurrencyAmount.of(GBP, 1000), date(2015, 8, 21)))
        .startDate(date(2015, 5, 19))
        .endDate(date(2015, 8, 19))
        .build();
    PaymentPeriod mockPaymentPeriod = mock(PaymentPeriod.class);

    ignoreThrows(() -> test.presentValue(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV));
    ignoreThrows(() -> test.presentValue(kapp, MOCK_PROV));
    ignoreThrows(() -> test.presentValue(mockPaymentPeriod, MOCK_PROV));

    ignoreThrows(() -> test.futureValue(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV));
    ignoreThrows(() -> test.futureValue(kapp, MOCK_PROV));
    ignoreThrows(() -> test.futureValue(mockPaymentPeriod, MOCK_PROV));

    ignoreThrows(() -> test.presentValueSensitivity(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV));
    ignoreThrows(() -> test.presentValueSensitivity(kapp, MOCK_PROV));
    ignoreThrows(() -> test.presentValueSensitivity(mockPaymentPeriod, MOCK_PROV));

    ignoreThrows(() -> test.futureValueSensitivity(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV));
    ignoreThrows(() -> test.futureValueSensitivity(kapp, MOCK_PROV));
    ignoreThrows(() -> test.futureValueSensitivity(mockPaymentPeriod, MOCK_PROV));

    ignoreThrows(() -> test.accruedInterest(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV));
    ignoreThrows(() -> test.accruedInterest(kapp, MOCK_PROV));
    ignoreThrows(() -> test.accruedInterest(mockPaymentPeriod, MOCK_PROV));

    ExplainMapBuilder explain = ExplainMap.builder();
    ignoreThrows(() -> test.explainPresentValue(SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC_GBP, MOCK_PROV, explain));
    ignoreThrows(() -> test.explainPresentValue(kapp, MOCK_PROV, explain));
    ignoreThrows(() -> test.explainPresentValue(mockPaymentPeriod, MOCK_PROV, explain));
  }

}
