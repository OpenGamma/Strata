/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_EXPANDED_SWAP_LEG;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_PAYMENT_PERIOD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE;
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
public class DiscountingSwapLegPricerFnTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment(date(2014, 1, 22));
  private static final PricingEnvironment MOCK_ENV_FUTURE = new MockPricingEnvironment(date(2040, 1, 22));

  public void test_presentValue_withCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.presentValue(MOCK_ENV, NOTIONAL_EXCHANGE))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(USD, 2000d * 1.6d);
    assertEquals(test.presentValue(MOCK_ENV, IBOR_EXPANDED_SWAP_LEG, USD), expected);
  }

  public void test_presentValue_withCurrency_past() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(USD, 0d);
    assertEquals(test.presentValue(MOCK_ENV_FUTURE, IBOR_EXPANDED_SWAP_LEG, USD), expected);
  }

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.presentValue(MOCK_ENV, NOTIONAL_EXCHANGE))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 2000d);
    assertEquals(test.presentValue(MOCK_ENV, IBOR_EXPANDED_SWAP_LEG), expected);
  }

  public void test_presentValue_past() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 0d);
    assertEquals(test.presentValue(MOCK_ENV_FUTURE, IBOR_EXPANDED_SWAP_LEG), expected);
  }

  //-------------------------------------------------------------------------
  public void test_futureValue() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.futureValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.futureValue(MOCK_ENV, NOTIONAL_EXCHANGE))
        .thenReturn(1000d);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 2000d);
    assertEquals(test.futureValue(MOCK_ENV, IBOR_EXPANDED_SWAP_LEG), expected);
  }

  public void test_futureValue_past() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapLegPricer test = new DiscountingSwapLegPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(GBP, 0d);
    assertEquals(test.futureValue(MOCK_ENV_FUTURE, IBOR_EXPANDED_SWAP_LEG), expected);
  }

}
