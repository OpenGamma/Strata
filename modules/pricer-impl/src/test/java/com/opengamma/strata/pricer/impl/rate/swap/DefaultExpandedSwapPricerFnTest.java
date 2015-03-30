/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.impl.MockPricingEnvironment;
import com.opengamma.strata.pricer.rate.swap.SwapLegPricerFn;

/**
 * Test.
 */
@Test
public class DefaultExpandedSwapPricerFnTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment();

  public void test_presentValue_singleCurrency() {
    SwapLegPricerFn<ExpandedSwapLeg> mockSwapLegFn = mock(SwapLegPricerFn.class);
    when(mockSwapLegFn.presentValue(MOCK_ENV, SwapDummyData.IBOR_EXPANDED_SWAP_LEG))
        .thenReturn(1000d);
    when(mockSwapLegFn.presentValue(MOCK_ENV, SwapDummyData.FIXED_EXPANDED_SWAP_LEG))
        .thenReturn(-500d);
    DefaultExpandedSwapPricerFn test = new DefaultExpandedSwapPricerFn(mockSwapLegFn);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.SWAP.expand()), MultiCurrencyAmount.of(GBP, 500d));
  }

  public void test_presentValue_crossCurrency() {
    SwapLegPricerFn<ExpandedSwapLeg> mockSwapLegFn = mock(SwapLegPricerFn.class);
    when(mockSwapLegFn.presentValue(MOCK_ENV, SwapDummyData.IBOR_EXPANDED_SWAP_LEG))
        .thenReturn(1000d);
    when(mockSwapLegFn.presentValue(MOCK_ENV, SwapDummyData.FIXED_EXPANDED_SWAP_LEG_USD))
        .thenReturn(-500d);
    DefaultExpandedSwapPricerFn test = new DefaultExpandedSwapPricerFn(mockSwapLegFn);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 1000d), CurrencyAmount.of(USD, -500d));
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.SWAP_CROSS_CURRENCY.expand()), expected);
  }

  public void test_presentValue_withCurrency_crossCurrency() {
    SwapLegPricerFn<ExpandedSwapLeg> mockSwapLegFn = mock(SwapLegPricerFn.class);
    when(mockSwapLegFn.presentValue(MOCK_ENV, SwapDummyData.IBOR_EXPANDED_SWAP_LEG))
        .thenReturn(1000d);
    when(mockSwapLegFn.presentValue(MOCK_ENV, SwapDummyData.FIXED_EXPANDED_SWAP_LEG_USD))
        .thenReturn(-500d);
    DefaultExpandedSwapPricerFn test = new DefaultExpandedSwapPricerFn(mockSwapLegFn);
    CurrencyAmount expected = CurrencyAmount.of(USD, 1000d * MockPricingEnvironment.RATE - 500d);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.SWAP_CROSS_CURRENCY.expand(), USD), expected);
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_singleCurrency() {
    SwapLegPricerFn<ExpandedSwapLeg> mockSwapLegFn = mock(SwapLegPricerFn.class);
    when(mockSwapLegFn.futureValue(MOCK_ENV, SwapDummyData.IBOR_EXPANDED_SWAP_LEG))
        .thenReturn(1000d);
    when(mockSwapLegFn.futureValue(MOCK_ENV, SwapDummyData.FIXED_EXPANDED_SWAP_LEG))
        .thenReturn(-500d);
    DefaultExpandedSwapPricerFn test = new DefaultExpandedSwapPricerFn(mockSwapLegFn);
    assertEquals(test.futureValue(MOCK_ENV, SwapDummyData.SWAP.expand()), MultiCurrencyAmount.of(GBP, 500d));
  }

  public void test_futureValue_crossCurrency() {
    SwapLegPricerFn<ExpandedSwapLeg> mockSwapLegFn = mock(SwapLegPricerFn.class);
    when(mockSwapLegFn.futureValue(MOCK_ENV, SwapDummyData.IBOR_EXPANDED_SWAP_LEG))
        .thenReturn(1000d);
    when(mockSwapLegFn.futureValue(MOCK_ENV, SwapDummyData.FIXED_EXPANDED_SWAP_LEG_USD))
        .thenReturn(-500d);
    DefaultExpandedSwapPricerFn test = new DefaultExpandedSwapPricerFn(mockSwapLegFn);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 1000d), CurrencyAmount.of(USD, -500d));
    assertEquals(test.futureValue(MOCK_ENV, SwapDummyData.SWAP_CROSS_CURRENCY.expand()), expected);
  }

}
