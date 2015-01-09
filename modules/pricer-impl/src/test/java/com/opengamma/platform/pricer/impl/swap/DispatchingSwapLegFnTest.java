/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.platform.finance.swap.ExpandedSwapLeg;
import com.opengamma.platform.finance.swap.RateCalculationSwapLeg;
import com.opengamma.platform.finance.swap.SwapLeg;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.SwapLegPricerFn;

/**
 * Test.
 */
@Test
public class DispatchingSwapLegFnTest {

  private final PricingEnvironment mockEnv = mock(PricingEnvironment.class);

  public void test_presentValue_ExpandedSwapLeg() {
    SwapLegPricerFn<ExpandedSwapLeg> mockExpandedFn = mock(SwapLegPricerFn.class);
    SwapLegPricerFn<RateCalculationSwapLeg> mockRateCalcFn = mock(SwapLegPricerFn.class);
    double expected = 0.0123d;
    when(mockExpandedFn.presentValue(mockEnv, SwapDummyData.IBOR_EXPANDED_SWAP_LEG))
        .thenReturn(expected);
    DispatchingSwapLegPricerFn test = new DispatchingSwapLegPricerFn(mockExpandedFn, mockRateCalcFn);
    assertEquals(test.presentValue(mockEnv, SwapDummyData.IBOR_EXPANDED_SWAP_LEG), expected);
  }

  public void test_presentValue_RateCalculationSwapLeg() {
    SwapLegPricerFn<ExpandedSwapLeg> mockExpandedFn = mock(SwapLegPricerFn.class);
    SwapLegPricerFn<RateCalculationSwapLeg> mockRateCalcFn = mock(SwapLegPricerFn.class);
    double expected = 0.0123d;
    when(mockRateCalcFn.presentValue(mockEnv, SwapDummyData.IBOR_RATECALC_SWAP_LEG))
        .thenReturn(expected);
    DispatchingSwapLegPricerFn test = new DispatchingSwapLegPricerFn(mockExpandedFn, mockRateCalcFn);
    assertEquals(test.presentValue(mockEnv, SwapDummyData.IBOR_RATECALC_SWAP_LEG), expected);
  }

  public void test_presentValue_unknownType() {
    SwapLeg mockSwapLeg = mock(SwapLeg.class);
    DispatchingSwapLegPricerFn test = DispatchingSwapLegPricerFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.presentValue(mockEnv, mockSwapLeg));
  }

  //-------------------------------------------------------------------------
  public void test_futureValue_ExpandedSwapLeg() {
    SwapLegPricerFn<ExpandedSwapLeg> mockExpandedFn = mock(SwapLegPricerFn.class);
    SwapLegPricerFn<RateCalculationSwapLeg> mockRateCalcFn = mock(SwapLegPricerFn.class);
    double expected = 0.0123d;
    when(mockExpandedFn.futureValue(mockEnv, SwapDummyData.IBOR_EXPANDED_SWAP_LEG))
        .thenReturn(expected);
    DispatchingSwapLegPricerFn test = new DispatchingSwapLegPricerFn(mockExpandedFn, mockRateCalcFn);
    assertEquals(test.futureValue(mockEnv, SwapDummyData.IBOR_EXPANDED_SWAP_LEG), expected);
  }

  public void test_futureValue_RateCalculationSwapLeg() {
    SwapLegPricerFn<ExpandedSwapLeg> mockExpandedFn = mock(SwapLegPricerFn.class);
    SwapLegPricerFn<RateCalculationSwapLeg> mockRateCalcFn = mock(SwapLegPricerFn.class);
    double expected = 0.0123d;
    when(mockRateCalcFn.futureValue(mockEnv, SwapDummyData.IBOR_RATECALC_SWAP_LEG))
        .thenReturn(expected);
    DispatchingSwapLegPricerFn test = new DispatchingSwapLegPricerFn(mockExpandedFn, mockRateCalcFn);
    assertEquals(test.futureValue(mockEnv, SwapDummyData.IBOR_RATECALC_SWAP_LEG), expected);
  }

  public void test_futureValue_unknownType() {
    SwapLeg mockSwapLeg = mock(SwapLeg.class);
    DispatchingSwapLegPricerFn test = DispatchingSwapLegPricerFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.futureValue(mockEnv, mockSwapLeg));
  }

}
