/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.impl.MockPricingEnvironment;

/**
 * Test.
 */
@Test
public class DefaultSwapLegPricerFnTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment(date(2014, 1, 22));
  private static final PricingEnvironment MOCK_ENV_FUTURE = new MockPricingEnvironment(date(2040, 1, 22));

  public void test_presentValue_withCurrency() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    when(mockPeriod.presentValue(MOCK_ENV, SwapDummyData.IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    when(mockEvent.presentValue(MOCK_ENV, SwapDummyData.NOTIONAL_EXCHANGE))
        .thenReturn(1000d);
    DefaultSwapLegPricerFn test = new DefaultSwapLegPricerFn(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(USD, 2000d * 1.6d);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.IBOR_EXPANDED_SWAP_LEG, USD), expected);
  }

  public void test_presentValue_withCurrency_past() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    DefaultSwapLegPricerFn test = new DefaultSwapLegPricerFn(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(USD, 0d);
    assertEquals(test.presentValue(MOCK_ENV_FUTURE, SwapDummyData.IBOR_EXPANDED_SWAP_LEG, USD), expected);
  }

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    when(mockPeriod.presentValue(MOCK_ENV, SwapDummyData.IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    when(mockEvent.presentValue(MOCK_ENV, SwapDummyData.NOTIONAL_EXCHANGE))
        .thenReturn(1000d);
    DefaultSwapLegPricerFn test = new DefaultSwapLegPricerFn(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 2000d);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.IBOR_EXPANDED_SWAP_LEG), expected);
  }

  public void test_presentValue_past() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    DefaultSwapLegPricerFn test = new DefaultSwapLegPricerFn(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 0d);
    assertEquals(test.presentValue(MOCK_ENV_FUTURE, SwapDummyData.IBOR_EXPANDED_SWAP_LEG), expected);
  }

  //-------------------------------------------------------------------------
  public void test_futureValue() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    when(mockPeriod.futureValue(MOCK_ENV, SwapDummyData.IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    when(mockEvent.futureValue(MOCK_ENV, SwapDummyData.NOTIONAL_EXCHANGE))
        .thenReturn(1000d);
    DefaultSwapLegPricerFn test = new DefaultSwapLegPricerFn(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 2000d);
    assertEquals(test.futureValue(MOCK_ENV, SwapDummyData.IBOR_EXPANDED_SWAP_LEG), expected);
  }

  public void test_futureValue_past() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    DefaultSwapLegPricerFn test = new DefaultSwapLegPricerFn(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 0d);
    assertEquals(test.futureValue(MOCK_ENV_FUTURE, SwapDummyData.IBOR_EXPANDED_SWAP_LEG), expected);
  }

}
