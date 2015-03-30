/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import com.opengamma.platform.finance.rate.swap.ExpandedSwap;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.MockPricingEnvironment;
import com.opengamma.platform.pricer.rate.swap.SwapProductPricerFn;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;

/**
 * Test.
 */
@Test
public class ExpandingSwapProductPricerFnTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment();

  public void test_presentValue_withCurrency() {
    CurrencyAmount expected = CurrencyAmount.of(USD, 1000d);
    SwapProductPricerFn<ExpandedSwap> mockSwapProductFn = mock(SwapProductPricerFn.class);
    when(mockSwapProductFn.presentValue(MOCK_ENV, SwapDummyData.SWAP_TRADE.getProduct().expand(), USD))
        .thenReturn(expected);
    ExpandingSwapProductPricerFn test = new ExpandingSwapProductPricerFn(mockSwapProductFn);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.SWAP, USD), expected);
  }

  public void test_presentValue() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(GBP, 1000d);
    SwapProductPricerFn<ExpandedSwap> mockSwapProductFn = mock(SwapProductPricerFn.class);
    when(mockSwapProductFn.presentValue(MOCK_ENV, SwapDummyData.SWAP.expand()))
        .thenReturn(expected);
    ExpandingSwapProductPricerFn test = new ExpandingSwapProductPricerFn(mockSwapProductFn);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.SWAP), expected);
  }

  public void test_futureValue() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(GBP, 1000d);
    SwapProductPricerFn<ExpandedSwap> mockSwapProductFn = mock(SwapProductPricerFn.class);
    when(mockSwapProductFn.futureValue(MOCK_ENV, SwapDummyData.SWAP.expand()))
        .thenReturn(expected);
    ExpandingSwapProductPricerFn test = new ExpandingSwapProductPricerFn(mockSwapProductFn);
    assertEquals(test.futureValue(MOCK_ENV, SwapDummyData.SWAP), expected);
  }

}
