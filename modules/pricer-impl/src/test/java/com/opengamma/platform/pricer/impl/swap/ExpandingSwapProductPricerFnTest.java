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
import com.opengamma.platform.pricer.swap.SwapProductPricerFn;

/**
 * Test.
 */
@Test
public class ExpandingSwapProductPricerFnTest {

  private final PricingEnvironment mockEnv = mock(PricingEnvironment.class);

  public void test_presentValue() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(GBP, 1000d);
    SwapProductPricerFn<ExpandedSwap> mockSwapLegFn = mock(SwapProductPricerFn.class);
    when(mockSwapLegFn.presentValue(mockEnv, SwapDummyData.SWAP.expand()))
        .thenReturn(expected);
    ExpandingSwapProductPricerFn test = new ExpandingSwapProductPricerFn(mockSwapLegFn);
    assertEquals(test.presentValue(mockEnv, SwapDummyData.SWAP), expected);
  }

  public void test_futureValue() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(GBP, 1000d);
    SwapProductPricerFn<ExpandedSwap> mockSwapLegFn = mock(SwapProductPricerFn.class);
    when(mockSwapLegFn.futureValue(mockEnv, SwapDummyData.SWAP.expand()))
        .thenReturn(expected);
    ExpandingSwapProductPricerFn test = new ExpandingSwapProductPricerFn(mockSwapLegFn);
    assertEquals(test.futureValue(mockEnv, SwapDummyData.SWAP), expected);
  }

}
