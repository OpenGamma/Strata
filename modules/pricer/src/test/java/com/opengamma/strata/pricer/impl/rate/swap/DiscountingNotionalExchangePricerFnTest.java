/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static com.opengamma.strata.pricer.rate.swap.SwapDummyData.NOTIONAL_EXCHANGE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.pricer.PricingEnvironment;

/**
 * Test.
 */
@Test
public class DiscountingNotionalExchangePricerFnTest {

  public void test_presentValue() {
    double discountFactor = 0.98d;
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockEnv.discountFactor(NOTIONAL_EXCHANGE.getCurrency(), NOTIONAL_EXCHANGE.getPaymentDate()))
        .thenReturn(discountFactor);
    DiscountingNotionalExchangePricer test = new DiscountingNotionalExchangePricer();
    assertEquals(
        test.presentValue(mockEnv, NOTIONAL_EXCHANGE),
        NOTIONAL_EXCHANGE.getPaymentAmount().getAmount() * discountFactor, 0d);
  }

  public void test_futureValue() {
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    DiscountingNotionalExchangePricer test = new DiscountingNotionalExchangePricer();
    assertEquals(
        test.futureValue(mockEnv, NOTIONAL_EXCHANGE),
        NOTIONAL_EXCHANGE.getPaymentAmount().getAmount(), 0d);
  }

}
