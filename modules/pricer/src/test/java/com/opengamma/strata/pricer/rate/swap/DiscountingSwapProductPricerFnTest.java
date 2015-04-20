/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.FIXED_RATE_PAYMENT_PERIOD_USD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.IBOR_RATE_PAYMENT_PERIOD;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.SWAP;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.SWAP_CROSS_CURRENCY;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.SWAP_TRADE;
import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.SWAP_TRADE_CROSS_CURRENCY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.finance.rate.swap.PaymentEvent;
import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.impl.MockPricingEnvironment;

/**
 * Test.
 */
@Test
public class DiscountingSwapProductPricerFnTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment(date(2014, 1, 22));

  public void test_presentValue_singleCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(MOCK_ENV, FIXED_RATE_PAYMENT_PERIOD))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    when(mockEvent.presentValue(MOCK_ENV, NOTIONAL_EXCHANGE))
        .thenReturn(35d);
    DiscountingSwapProductPricer test = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    ExpandedSwap expanded = SWAP.expand();
    assertEquals(test.presentValue(MOCK_ENV, expanded), MultiCurrencyAmount.of(GBP, 570d));

    // test via SwapTrade
    DiscountingSwapTradePricer testTrade = new DiscountingSwapTradePricer(test);
    assertEquals(testTrade.presentValue(MOCK_ENV, SWAP_TRADE), test.presentValue(MOCK_ENV, expanded));
  }

  public void test_presentValue_crossCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(MOCK_ENV, FIXED_RATE_PAYMENT_PERIOD_USD))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapProductPricer test = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 1000d), CurrencyAmount.of(USD, -500d));
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    assertEquals(test.presentValue(MOCK_ENV, expanded), expected);

    // test via SwapTrade
    DiscountingSwapTradePricer testTrade = new DiscountingSwapTradePricer(test);
    assertEquals(testTrade.presentValue(MOCK_ENV, SWAP_TRADE_CROSS_CURRENCY), test.presentValue(MOCK_ENV, expanded));
  }

  public void test_presentValue_withCurrency_crossCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.presentValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    when(mockPeriod.presentValue(MOCK_ENV, FIXED_RATE_PAYMENT_PERIOD_USD))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapProductPricer test = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    CurrencyAmount expected = CurrencyAmount.of(USD, 1000d * MockPricingEnvironment.RATE - 500d);
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    assertEquals(test.presentValue(MOCK_ENV, expanded, USD), expected);

    // test via SwapTrade
    DiscountingSwapTradePricer testTrade = new DiscountingSwapTradePricer(test);
    assertEquals(testTrade.presentValue(MOCK_ENV, SWAP_TRADE_CROSS_CURRENCY), test.presentValue(MOCK_ENV, expanded));
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_singleCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.futureValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    when(mockPeriod.futureValue(MOCK_ENV, FIXED_RATE_PAYMENT_PERIOD))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapProductPricer test = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    ExpandedSwap expanded = SWAP.expand();
    assertEquals(test.futureValue(MOCK_ENV, expanded), MultiCurrencyAmount.of(GBP, 500d));

    // test via SwapTrade
    DiscountingSwapTradePricer testTrade = new DiscountingSwapTradePricer(test);
    assertEquals(testTrade.futureValue(MOCK_ENV, SWAP_TRADE), test.futureValue(MOCK_ENV, expanded));
  }

  public void test_futureValue_crossCurrency() {
    PaymentPeriodPricer<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricer.class);
    when(mockPeriod.futureValue(MOCK_ENV, IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    when(mockPeriod.futureValue(MOCK_ENV, FIXED_RATE_PAYMENT_PERIOD_USD))
        .thenReturn(-500d);
    PaymentEventPricer<PaymentEvent> mockEvent = mock(PaymentEventPricer.class);
    DiscountingSwapProductPricer test = new DiscountingSwapProductPricer(mockPeriod, mockEvent);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 1000d), CurrencyAmount.of(USD, -500d));
    ExpandedSwap expanded = SWAP_CROSS_CURRENCY.expand();
    assertEquals(test.futureValue(MOCK_ENV, expanded), expected);

    // test via SwapTrade
    DiscountingSwapTradePricer testTrade = new DiscountingSwapTradePricer(test);
    assertEquals(testTrade.futureValue(MOCK_ENV, SWAP_TRADE_CROSS_CURRENCY), test.futureValue(MOCK_ENV, expanded));
  }

}
