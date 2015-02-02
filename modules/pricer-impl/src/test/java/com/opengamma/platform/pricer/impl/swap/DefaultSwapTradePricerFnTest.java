/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.platform.finance.swap.Swap;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.SwapProductPricerFn;

/**
 * Test.
 */
@Test
public class DefaultSwapTradePricerFnTest {

  private final PricingEnvironment mockEnv = mock(PricingEnvironment.class);

  public void test_presentValue() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(GBP, 500d);
    SwapProductPricerFn<Swap> mockSwapFn = mock(SwapProductPricerFn.class);
    when(mockSwapFn.presentValue(mockEnv, SwapDummyData.SWAP))
        .thenReturn(expected);
    DefaultSwapTradePricerFn test = new DefaultSwapTradePricerFn(mockSwapFn);
    assertEquals(test.presentValue(mockEnv, SwapDummyData.SWAP_TRADE), expected);
  }

}
