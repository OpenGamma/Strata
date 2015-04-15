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
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.impl.MockPricingEnvironment;

/**
 * Test.
 */
@Test
public class DefaultSwapProductPricerFnTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment(date(2014, 1, 22));

  public void test_presentValue_singleCurrency() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    when(mockPeriod.presentValue(MOCK_ENV, SwapDummyData.IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(MOCK_ENV, SwapDummyData.FIXED_RATE_PAYMENT_PERIOD))
        .thenReturn(-500d);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    when(mockEvent.presentValue(MOCK_ENV, SwapDummyData.NOTIONAL_EXCHANGE))
        .thenReturn(35d);
    DefaultSwapProductPricerFn test = new DefaultSwapProductPricerFn(mockPeriod, mockEvent);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.SWAP.expand()), MultiCurrencyAmount.of(GBP, 570d));
  }

  public void test_presentValue_crossCurrency() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    when(mockPeriod.presentValue(MOCK_ENV, SwapDummyData.IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(MOCK_ENV, SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_USD))
        .thenReturn(-500d);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    DefaultSwapProductPricerFn test = new DefaultSwapProductPricerFn(mockPeriod, mockEvent);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 1000d), CurrencyAmount.of(USD, -500d));
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.SWAP_CROSS_CURRENCY.expand()), expected);
  }

  public void test_presentValue_withCurrency_crossCurrency() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    when(mockPeriod.presentValue(MOCK_ENV, SwapDummyData.IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(MOCK_ENV, SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_USD))
        .thenReturn(-500d);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    DefaultSwapProductPricerFn test = new DefaultSwapProductPricerFn(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(USD, 1000d * MockPricingEnvironment.RATE - 500d);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.SWAP_CROSS_CURRENCY.expand(), USD), expected);
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_singleCurrency() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    when(mockPeriod.futureValue(MOCK_ENV, SwapDummyData.IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    when(mockPeriod.futureValue(MOCK_ENV, SwapDummyData.FIXED_RATE_PAYMENT_PERIOD))
        .thenReturn(-500d);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    DefaultSwapProductPricerFn test = new DefaultSwapProductPricerFn(mockPeriod, mockEvent);
    assertEquals(test.futureValue(MOCK_ENV, SwapDummyData.SWAP.expand()), MultiCurrencyAmount.of(GBP, 500d));
  }

  public void test_futureValue_crossCurrency() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    when(mockPeriod.futureValue(MOCK_ENV, SwapDummyData.IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    when(mockPeriod.futureValue(MOCK_ENV, SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_USD))
        .thenReturn(-500d);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    DefaultSwapProductPricerFn test = new DefaultSwapProductPricerFn(mockPeriod, mockEvent);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 1000d), CurrencyAmount.of(USD, -500d));
    assertEquals(test.futureValue(MOCK_ENV, SwapDummyData.SWAP_CROSS_CURRENCY.expand()), expected);
  }

}
