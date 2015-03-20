/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate.swap;

import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.platform.finance.rate.swap.FxResetNotionalExchange;
import com.opengamma.platform.finance.rate.swap.NotionalExchange;
import com.opengamma.platform.finance.rate.swap.PaymentEvent;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.MockPricingEnvironment;
import com.opengamma.platform.pricer.rate.swap.PaymentEventPricerFn;

/**
 * Test.
 */
@Test
public class DispatchingPaymentEventPricerFnTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment();
  private static final PaymentEventPricerFn<NotionalExchange> MOCK_NOTIONAL_EXG = mock(PaymentEventPricerFn.class);
  private static final PaymentEventPricerFn<FxResetNotionalExchange> MOCK_FX_NOTIONAL_EXG =
      mock(PaymentEventPricerFn.class);

  //-------------------------------------------------------------------------
  public void test_presentValue_NotionalExchange() {
    double expected = 0.0123d;
    PaymentEventPricerFn<NotionalExchange> mockCalledFn = mock(PaymentEventPricerFn.class);
    when(mockCalledFn.presentValue(MOCK_ENV, SwapDummyData.NOTIONAL_EXCHANGE))
        .thenReturn(expected);
    DispatchingPaymentEventPricerFn test = new DispatchingPaymentEventPricerFn(
        mockCalledFn,
        MOCK_FX_NOTIONAL_EXG);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.NOTIONAL_EXCHANGE), expected, 0d);
  }

  public void test_presentValue_FxResetNotionalExchange() {
    double expected = 0.0123d;
    PaymentEventPricerFn<FxResetNotionalExchange> mockCalledFn = mock(PaymentEventPricerFn.class);
    when(mockCalledFn.presentValue(MOCK_ENV, SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE))
        .thenReturn(expected);
    DispatchingPaymentEventPricerFn test = new DispatchingPaymentEventPricerFn(
        MOCK_NOTIONAL_EXG,
        mockCalledFn);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE), expected, 0d);
  }

  public void test_presentValue_unknownType() {
    PaymentEvent mockPaymentEvent = mock(PaymentEvent.class);
    DispatchingPaymentEventPricerFn test = DispatchingPaymentEventPricerFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.presentValue(MOCK_ENV, mockPaymentEvent));
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_NotionalExchange() {
    double expected = 0.0123d;
    PaymentEventPricerFn<NotionalExchange> mockCalledFn = mock(PaymentEventPricerFn.class);
    when(mockCalledFn.futureValue(MOCK_ENV, SwapDummyData.NOTIONAL_EXCHANGE))
        .thenReturn(expected);
    DispatchingPaymentEventPricerFn test = new DispatchingPaymentEventPricerFn(
        mockCalledFn,
        MOCK_FX_NOTIONAL_EXG);
    assertEquals(test.futureValue(MOCK_ENV, SwapDummyData.NOTIONAL_EXCHANGE), expected, 0d);
  }

  public void test_futureValue_FxResetNotionalExchange() {
    double expected = 0.0123d;
    PaymentEventPricerFn<FxResetNotionalExchange> mockCalledFn = mock(PaymentEventPricerFn.class);
    when(mockCalledFn.futureValue(MOCK_ENV, SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE))
        .thenReturn(expected);
    DispatchingPaymentEventPricerFn test = new DispatchingPaymentEventPricerFn(
        MOCK_NOTIONAL_EXG,
        mockCalledFn);
    assertEquals(test.futureValue(MOCK_ENV, SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE), expected, 0d);
  }

  public void test_futureValue_unknownType() {
    PaymentEvent mockPaymentEvent = mock(PaymentEvent.class);
    DispatchingPaymentEventPricerFn test = DispatchingPaymentEventPricerFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.futureValue(MOCK_ENV, mockPaymentEvent));
  }

}
