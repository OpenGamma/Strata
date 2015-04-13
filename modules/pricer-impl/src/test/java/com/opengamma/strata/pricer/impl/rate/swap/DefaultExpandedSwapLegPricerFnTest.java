/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.impl.MockPricingEnvironment;
import com.opengamma.strata.pricer.rate.swap.PaymentEventPricerFn;
import com.opengamma.strata.pricer.rate.swap.PaymentPeriodPricerFn;

/**
 * Test.
 */
@Test
public class DefaultExpandedSwapLegPricerFnTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment(date(2014, 1, 22));
  private static final PricingEnvironment MOCK_ENV_FUTURE = new MockPricingEnvironment(date(2040, 1, 22));

  public void test_presentValue() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    when(mockPeriod.presentValue(MOCK_ENV, SwapDummyData.IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    when(mockEvent.presentValue(MOCK_ENV, SwapDummyData.NOTIONAL_EXCHANGE))
        .thenReturn(1000d);
    DefaultExpandedSwapLegPricerFn test = new DefaultExpandedSwapLegPricerFn(mockPeriod, mockEvent);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.IBOR_EXPANDED_SWAP_LEG), 2000d, 0d);
  }

  public void test_presentValue_past() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    DefaultExpandedSwapLegPricerFn test = new DefaultExpandedSwapLegPricerFn(mockPeriod, mockEvent);
    assertEquals(test.presentValue(MOCK_ENV_FUTURE, SwapDummyData.IBOR_EXPANDED_SWAP_LEG), 0d, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_futureValue() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    when(mockPeriod.futureValue(MOCK_ENV, SwapDummyData.IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    when(mockEvent.futureValue(MOCK_ENV, SwapDummyData.NOTIONAL_EXCHANGE))
        .thenReturn(1000d);
    DefaultExpandedSwapLegPricerFn test = new DefaultExpandedSwapLegPricerFn(mockPeriod, mockEvent);
    assertEquals(test.futureValue(MOCK_ENV, SwapDummyData.IBOR_EXPANDED_SWAP_LEG), 2000d, 0d);
  }

  public void test_futureValue_past() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    DefaultExpandedSwapLegPricerFn test = new DefaultExpandedSwapLegPricerFn(mockPeriod, mockEvent);
    assertEquals(test.futureValue(MOCK_ENV_FUTURE, SwapDummyData.IBOR_EXPANDED_SWAP_LEG), 0d, 0d);
  }

}
