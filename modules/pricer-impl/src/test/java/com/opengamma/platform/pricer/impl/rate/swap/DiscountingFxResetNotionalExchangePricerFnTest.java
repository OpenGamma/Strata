/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate.swap;

import static com.opengamma.platform.pricer.impl.rate.swap.SwapDummyData.FX_RESET_NOTIONAL_EXCHANGE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.strata.finance.rate.swap.FxResetNotionalExchange;

/**
 * Test.
 */
@Test
public class DiscountingFxResetNotionalExchangePricerFnTest {

  public void test_presentValue() {
    double discountFactor = 0.98d;
    FxResetNotionalExchange ne = FX_RESET_NOTIONAL_EXCHANGE;
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.fxIndexRate(ne.getIndex(), ne.getReferenceCurrency(), ne.getFixingDate()))
        .thenReturn(1.6d);
    when(mockEnv.discountFactor(ne.getCurrency(), ne.getPaymentDate()))
        .thenReturn(discountFactor);
    DiscountingFxResetNotionalExchangePricerFn test = new DiscountingFxResetNotionalExchangePricerFn();
    assertEquals(
        test.presentValue(mockEnv, ne),
        ne.getNotional() * 1.6d * discountFactor, 0d);
  }

  public void test_futureValue() {
    FxResetNotionalExchange ne = FX_RESET_NOTIONAL_EXCHANGE;
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.fxIndexRate(ne.getIndex(), ne.getReferenceCurrency(), ne.getFixingDate()))
        .thenReturn(1.6d);
    DiscountingFxResetNotionalExchangePricerFn test = new DiscountingFxResetNotionalExchangePricerFn();
    assertEquals(
        test.futureValue(mockEnv, ne),
        ne.getNotional() * 1.6d, 0d);
  }

}
