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
import com.opengamma.collect.id.StandardId;
import com.opengamma.platform.finance.QuantityTrade;
import com.opengamma.platform.finance.OtcTrade;
import com.opengamma.platform.finance.SecurityLink;
import com.opengamma.platform.finance.Trade;
import com.opengamma.platform.finance.equity.Equity;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.TradePricerFn;
import com.opengamma.platform.pricer.impl.swap.SwapDummyData;

/**
 * Test.
 */
@Test
public class DispatchingTradePricerFnTest {

  private static final PricingEnvironment MOCK_ENV = mock(PricingEnvironment.class);

  public void test_presentValue_OtcTrade() {
    TradePricerFn<OtcTrade<?>> mockOtcFn = mock(TradePricerFn.class);
    TradePricerFn<QuantityTrade<?>> mockQuantityFn = mock(TradePricerFn.class);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(Currency.GBP, 0.0123d);
    when(mockOtcFn.presentValue(MOCK_ENV, SwapDummyData.SWAP_TRADE))
        .thenReturn(expected);
    DispatchingTradePricerFn test = new DispatchingTradePricerFn(mockOtcFn, mockQuantityFn);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.SWAP_TRADE), expected);
  }

  public void test_presentValue_QuantityTrade() {
    QuantityTrade<Equity> listed =
        QuantityTrade.builder(SecurityLink.resolvable(StandardId.of("OG-Ticker", "1"), Equity.class))
            .standardId(StandardId.of("OG-Trade", "1"))
            .build();
    TradePricerFn<OtcTrade<?>> mockOtcFn = mock(TradePricerFn.class);
    TradePricerFn<QuantityTrade<?>> mockQuantityFn = mock(TradePricerFn.class);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(Currency.GBP, 0.0123d);
    when(mockQuantityFn.presentValue(MOCK_ENV, listed))
        .thenReturn(expected);
    DispatchingTradePricerFn test = new DispatchingTradePricerFn(mockOtcFn, mockQuantityFn);
    assertEquals(test.presentValue(MOCK_ENV, listed), expected);
  }

  public void test_presentValue_unknownType() {
    Trade mockTrade = mock(Trade.class);
    DispatchingTradePricerFn test = DispatchingTradePricerFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.presentValue(MOCK_ENV, mockTrade));
  }

}
