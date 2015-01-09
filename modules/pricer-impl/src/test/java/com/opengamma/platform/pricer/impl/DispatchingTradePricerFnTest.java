/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl;

import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.Trade;
import com.opengamma.platform.finance.swap.SwapTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.TradePricerFn;
import com.opengamma.platform.pricer.impl.swap.SwapDummyData;

/**
 * Test.
 */
@Test
public class DispatchingTradePricerFnTest {

  private final PricingEnvironment mockEnv = mock(PricingEnvironment.class);

  public void test_presentValue_SwapTrade() {
    TradePricerFn<SwapTrade> mockSwapFn = mock(TradePricerFn.class);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(Currency.GBP, 0.0123d);
    when(mockSwapFn.presentValue(mockEnv, SwapDummyData.SWAP_TRADE))
        .thenReturn(expected);
    DispatchingTradePricerFn test = new DispatchingTradePricerFn(mockSwapFn);
    assertEquals(test.presentValue(mockEnv, SwapDummyData.SWAP_TRADE), expected);
  }

  public void test_presentValue_unknownType() {
    Trade mockTrade = mock(Trade.class);
    DispatchingTradePricerFn test = DispatchingTradePricerFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.presentValue(mockEnv, mockTrade));
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_SwapTrade() {
    TradePricerFn<SwapTrade> mockSwapFn = mock(TradePricerFn.class);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(Currency.GBP, 0.0123d);
    when(mockSwapFn.futureValue(mockEnv, SwapDummyData.SWAP_TRADE))
        .thenReturn(expected);
    DispatchingTradePricerFn test = new DispatchingTradePricerFn(mockSwapFn);
    assertEquals(test.futureValue(mockEnv, SwapDummyData.SWAP_TRADE), expected);
  }

  public void test_futureValue_unknownType() {
    Trade mockTrade = mock(Trade.class);
    DispatchingTradePricerFn test = DispatchingTradePricerFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.futureValue(mockEnv, mockTrade));
  }

}
