/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.finance.rate.swap.SwapProduct;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.impl.MockPricingEnvironment;
import com.opengamma.strata.pricer.rate.swap.SwapProductPricerFn;

/**
 * Test.
 */
@Test
public class DefaultSwapTradePricerFnTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment();

  public void test_presentValue_withCurrency() {
    CurrencyAmount expected = CurrencyAmount.of(USD, 1000d);
    SwapProductPricerFn<SwapProduct> mockSwapProductFn = mock(SwapProductPricerFn.class);
    when(mockSwapProductFn.presentValue(MOCK_ENV, SwapDummyData.SWAP_TRADE.getProduct(), USD))
        .thenReturn(expected);
    DefaultSwapTradePricerFn test = new DefaultSwapTradePricerFn(mockSwapProductFn);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.SWAP_TRADE, USD), expected);
  }

  public void test_presentValue() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(GBP, 1000d);
    SwapProductPricerFn<SwapProduct> mockSwapProductFn = mock(SwapProductPricerFn.class);
    when(mockSwapProductFn.presentValue(MOCK_ENV, SwapDummyData.SWAP_TRADE.getProduct()))
        .thenReturn(expected);
    DefaultSwapTradePricerFn test = new DefaultSwapTradePricerFn(mockSwapProductFn);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.SWAP_TRADE), expected);
  }

  public void test_futureValue() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(GBP, 1000d);
    SwapProductPricerFn<SwapProduct> mockSwapProductFn = mock(SwapProductPricerFn.class);
    when(mockSwapProductFn.futureValue(MOCK_ENV, SwapDummyData.SWAP_TRADE.getProduct()))
        .thenReturn(expected);
    DefaultSwapTradePricerFn test = new DefaultSwapTradePricerFn(mockSwapProductFn);
    assertEquals(test.futureValue(MOCK_ENV, SwapDummyData.SWAP_TRADE), expected);
  }

}
