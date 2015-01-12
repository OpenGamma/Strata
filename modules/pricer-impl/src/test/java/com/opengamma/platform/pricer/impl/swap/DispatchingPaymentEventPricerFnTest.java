/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.platform.finance.swap.NotionalExchange;
import com.opengamma.platform.finance.swap.PaymentEvent;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.PaymentEventPricerFn;

/**
 * Test.
 */
@Test
public class DispatchingPaymentEventPricerFnTest {

  private final PricingEnvironment mockEnv = mock(PricingEnvironment.class);

  public void test_presentValue_NotionalExchange() {
    double expected = 0.0123d;
    PaymentEventPricerFn<NotionalExchange> mockNotionalExchangeFn = mock(PaymentEventPricerFn.class);
    when(mockNotionalExchangeFn.presentValue(mockEnv, SwapDummyData.NOTIONAL_EXCHANGE))
        .thenReturn(expected);
    DispatchingPaymentEventPricerFn test = new DispatchingPaymentEventPricerFn(mockNotionalExchangeFn);
    assertEquals(test.presentValue(mockEnv, SwapDummyData.NOTIONAL_EXCHANGE), expected, 0d);
  }

  public void test_presentValue_unknownType() {
    PaymentEvent mockPaymentEvent = mock(PaymentEvent.class);
    DispatchingPaymentEventPricerFn test = DispatchingPaymentEventPricerFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.presentValue(mockEnv, mockPaymentEvent));
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_NotionalExchange() {
    double expected = 0.0123d;
    PaymentEventPricerFn<NotionalExchange> mockNotionalExchangeFn = mock(PaymentEventPricerFn.class);
    when(mockNotionalExchangeFn.futureValue(mockEnv, SwapDummyData.NOTIONAL_EXCHANGE))
        .thenReturn(expected);
    DispatchingPaymentEventPricerFn test = new DispatchingPaymentEventPricerFn(mockNotionalExchangeFn);
    assertEquals(test.futureValue(mockEnv, SwapDummyData.NOTIONAL_EXCHANGE), expected, 0d);
  }

  public void test_futureValue_unknownType() {
    PaymentEvent mockPaymentEvent = mock(PaymentEvent.class);
    DispatchingPaymentEventPricerFn test = DispatchingPaymentEventPricerFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.futureValue(mockEnv, mockPaymentEvent));
  }

}
