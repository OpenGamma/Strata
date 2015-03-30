/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.impl.MockPricingEnvironment;
import com.opengamma.strata.pricer.rate.swap.SwapLegPricerFn;

/**
 * Test.
 */
@Test
public class ExpandingSwapLegPricerFnTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment();

  public void test_presentValue() {
    double expected = 1000d;
    SwapLegPricerFn<ExpandedSwapLeg> mockSwapLegFn = mock(SwapLegPricerFn.class);
    when(mockSwapLegFn.presentValue(MOCK_ENV, SwapDummyData.IBOR_RATECALC_SWAP_LEG.expand()))
        .thenReturn(expected);
    ExpandingSwapLegPricerFn test = new ExpandingSwapLegPricerFn(mockSwapLegFn);
    assertEquals(test.presentValue(MOCK_ENV, SwapDummyData.IBOR_RATECALC_SWAP_LEG), expected);
  }

  public void test_futureValue() {
    double expected = 1000d;
    SwapLegPricerFn<ExpandedSwapLeg> mockSwapLegFn = mock(SwapLegPricerFn.class);
    when(mockSwapLegFn.futureValue(MOCK_ENV, SwapDummyData.IBOR_RATECALC_SWAP_LEG.expand()))
        .thenReturn(expected);
    ExpandingSwapLegPricerFn test = new ExpandingSwapLegPricerFn(mockSwapLegFn);
    assertEquals(test.futureValue(MOCK_ENV, SwapDummyData.IBOR_RATECALC_SWAP_LEG), expected);
  }

}
