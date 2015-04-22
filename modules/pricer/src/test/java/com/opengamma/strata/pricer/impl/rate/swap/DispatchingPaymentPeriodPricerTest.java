/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.finance.rate.swap.RatePaymentPeriod;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.impl.MockPricingEnvironment;
import com.opengamma.strata.pricer.rate.swap.PaymentPeriodPricer;
import com.opengamma.strata.pricer.rate.swap.SwapDummyData;

/**
 * Test.
 */
@Test
public class DispatchingPaymentPeriodPricerTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment();

  public void test_presentValue_RatePaymentPeriod() {
    double expected = 0.0123d;
    PaymentPeriodPricer<RatePaymentPeriod> mockNotionalExchangeFn = mock(PaymentPeriodPricer.class);
    when(mockNotionalExchangeFn.presentValue(MOCK_ENV, SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC))
        .thenReturn(expected);
    DispatchingPaymentPeriodPricer test = new DispatchingPaymentPeriodPricer(mockNotionalExchangeFn);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC), expected, 0d);
  }

  public void test_presentValue_unknownType() {
    PaymentPeriod mockPaymentPeriod = mock(PaymentPeriod.class);
    DispatchingPaymentPeriodPricer test = DispatchingPaymentPeriodPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.presentValue(MOCK_ENV, mockPaymentPeriod));
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_RatePaymentPeriod() {
    double expected = 0.0123d;
    PaymentPeriodPricer<RatePaymentPeriod> mockNotionalExchangeFn = mock(PaymentPeriodPricer.class);
    when(mockNotionalExchangeFn.futureValue(MOCK_ENV, SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC))
        .thenReturn(expected);
    DispatchingPaymentPeriodPricer test = new DispatchingPaymentPeriodPricer(mockNotionalExchangeFn);
    assertEquals(test.futureValue(MOCK_ENV, SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_REC), expected, 0d);
  }

  public void test_futureValue_unknownType() {
    PaymentPeriod mockPaymentPeriod = mock(PaymentPeriod.class);
    DispatchingPaymentPeriodPricer test = DispatchingPaymentPeriodPricer.DEFAULT;
    assertThrowsIllegalArg(() -> test.futureValue(MOCK_ENV, mockPaymentPeriod));
  }

}
