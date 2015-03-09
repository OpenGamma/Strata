/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import static com.opengamma.basics.currency.Currency.GBP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.swap.ExpandedSwap;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.MockPricingEnvironment;
import com.opengamma.platform.pricer.swap.SwapProductPricerFn;

/**
 * Test.
 */
@Test
public class ExpandingSwapTradePricerFnTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment();

  public void test_presentValue() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(GBP, 1000d);
    SwapProductPricerFn<ExpandedSwap> mockSwapProductFn = mock(SwapProductPricerFn.class);
    when(mockSwapProductFn.presentValue(MOCK_ENV, SwapDummyData.SWAP_TRADE.getProduct().expand()))
        .thenReturn(expected);
    ExpandingSwapTradePricerFn test = new ExpandingSwapTradePricerFn(mockSwapProductFn);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.SWAP_TRADE), expected);
  }

  public void test_futureValue() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(GBP, 1000d);
    SwapProductPricerFn<ExpandedSwap> mockSwapProductFn = mock(SwapProductPricerFn.class);
    when(mockSwapProductFn.futureValue(MOCK_ENV, SwapDummyData.SWAP_TRADE.getProduct().expand()))
        .thenReturn(expected);
    ExpandingSwapTradePricerFn test = new ExpandingSwapTradePricerFn(mockSwapProductFn);
    assertEquals(test.futureValue(MOCK_ENV, SwapDummyData.SWAP_TRADE), expected);
  }

}
