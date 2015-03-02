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
import com.opengamma.platform.finance.MockSimpleProduct;
import com.opengamma.platform.finance.OtcTrade;
import com.opengamma.platform.finance.fra.FraProduct;
import com.opengamma.platform.finance.swap.SwapProduct;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.fra.FraProductPricerFn;
import com.opengamma.platform.pricer.impl.fra.FraDummyData;
import com.opengamma.platform.pricer.impl.swap.SwapDummyData;
import com.opengamma.platform.pricer.swap.SwapProductPricerFn;

/**
 * Test.
 */
@Test
public class DispatchingProductOtcTradePricerFnTest {

  private static final PricingEnvironment MOCK_ENV = mock(PricingEnvironment.class);
  private static final SwapProductPricerFn<SwapProduct> MOCK_SWAP = mock(SwapProductPricerFn.class);
  private static final FraProductPricerFn<FraProduct> MOCK_FRA = mock(FraProductPricerFn.class);

  public void test_presentValue_SwapProduct() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(Currency.GBP, 0.0123d);
    SwapProductPricerFn<SwapProduct> mockSwapFn = mock(SwapProductPricerFn.class);
    when(mockSwapFn.presentValue(MOCK_ENV, SwapDummyData.SWAP))
        .thenReturn(expected);
    DispatchingProductOtcTradePricerFn test = new DispatchingProductOtcTradePricerFn(mockSwapFn, MOCK_FRA);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.SWAP_TRADE), expected);
  }

  public void test_presentValue_FraTrade() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(Currency.GBP, 0.0123d);
    FraProductPricerFn<FraProduct> mockFraFn = mock(FraProductPricerFn.class);
    when(mockFraFn.presentValue(MOCK_ENV, FraDummyData.FRA))
        .thenReturn(expected);
    DispatchingProductOtcTradePricerFn test = new DispatchingProductOtcTradePricerFn(MOCK_SWAP, mockFraFn);
    assertEquals(test.presentValue(MOCK_ENV, FraDummyData.FRA_TRADE), expected);
  }

  public void test_presentValue_unknownType() {
    OtcTrade<MockSimpleProduct> mockTrade =
        OtcTrade.builder(MockSimpleProduct.MOCK1)
            .standardId(StandardId.of("OG-Trade", "1"))
            .build();
    DispatchingProductOtcTradePricerFn test = DispatchingProductOtcTradePricerFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.presentValue(MOCK_ENV, mockTrade));
  }

}
