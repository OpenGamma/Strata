/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.tuple.Pair;
import com.opengamma.platform.finance.swap.ExpandedSwapLeg;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.CurveSensitivityTestUtil;
import com.opengamma.platform.pricer.sensitivity.multicurve.ForwardRateSensitivityLD;
import com.opengamma.platform.pricer.sensitivity.multicurve.MulticurveSensitivity3LD;
import com.opengamma.platform.pricer.sensitivity.multicurve.ZeroRateSensitivityLD;
import com.opengamma.platform.pricer.swap.SwapLegPricerFn;

/**
 * Test.
 */
@Test
public class DefaultExpandedSwapPricerFnTest {

  private final PricingEnvironment mockEnv = mock(PricingEnvironment.class);
  private static final double TOL = 1.0e-12;

  public void test_presentValue_singleCurrency() {
    SwapLegPricerFn<ExpandedSwapLeg> mockSwapLegFn = mock(SwapLegPricerFn.class);
    when(mockSwapLegFn.presentValue(mockEnv, SwapDummyData.IBOR_EXPANDED_SWAP_LEG))
        .thenReturn(1000d);
    when(mockSwapLegFn.presentValue(mockEnv, SwapDummyData.FIXED_EXPANDED_SWAP_LEG))
        .thenReturn(-500d);
    DefaultExpandedSwapPricerFn test = new DefaultExpandedSwapPricerFn(mockSwapLegFn);
    assertEquals(test.presentValue(mockEnv, SwapDummyData.SWAP.expand()), MultiCurrencyAmount.of(GBP, 500d));
  }

  public void test_presentValue_crossCurrency() {
    SwapLegPricerFn<ExpandedSwapLeg> mockSwapLegFn = mock(SwapLegPricerFn.class);
    when(mockSwapLegFn.presentValue(mockEnv, SwapDummyData.IBOR_EXPANDED_SWAP_LEG))
        .thenReturn(1000d);
    when(mockSwapLegFn.presentValue(mockEnv, SwapDummyData.FIXED_EXPANDED_SWAP_LEG_USD))
        .thenReturn(-500d);
    DefaultExpandedSwapPricerFn test = new DefaultExpandedSwapPricerFn(mockSwapLegFn);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 1000d), CurrencyAmount.of(USD, -500d));
    assertEquals(test.presentValue(mockEnv, SwapDummyData.SWAP_CROSS_CURRENCY.expand()), expected);
  }

  public void test_futureValue_singleCurrency() {
    SwapLegPricerFn<ExpandedSwapLeg> mockSwapLegFn = mock(SwapLegPricerFn.class);
    when(mockSwapLegFn.futureValue(mockEnv, SwapDummyData.IBOR_EXPANDED_SWAP_LEG))
        .thenReturn(1000d);
    when(mockSwapLegFn.futureValue(mockEnv, SwapDummyData.FIXED_EXPANDED_SWAP_LEG))
        .thenReturn(-500d);
    DefaultExpandedSwapPricerFn test = new DefaultExpandedSwapPricerFn(mockSwapLegFn);
    assertEquals(test.futureValue(mockEnv, SwapDummyData.SWAP.expand()), MultiCurrencyAmount.of(GBP, 500d));
  }

  public void test_futureValue_crossCurrency() {
    SwapLegPricerFn<ExpandedSwapLeg> mockSwapLegFn = mock(SwapLegPricerFn.class);
    when(mockSwapLegFn.futureValue(mockEnv, SwapDummyData.IBOR_EXPANDED_SWAP_LEG))
        .thenReturn(1000d);
    when(mockSwapLegFn.futureValue(mockEnv, SwapDummyData.FIXED_EXPANDED_SWAP_LEG_USD))
        .thenReturn(-500d);
    DefaultExpandedSwapPricerFn test = new DefaultExpandedSwapPricerFn(mockSwapLegFn);
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 1000d), CurrencyAmount.of(USD, -500d));
    assertEquals(test.futureValue(mockEnv, SwapDummyData.SWAP_CROSS_CURRENCY.expand()), expected);
  }

  /**
   * Test present value sensitivity. 
   */
  public void test_presentValue_sensitivity() {
    /* ibor leg */
    double pvIbor = 1000d;
    List<ForwardRateSensitivityLD> forwardRateSensi = new ArrayList<>();
    ForwardRateSensitivityLD fwdSense = new ForwardRateSensitivityLD(GBP_LIBOR_3M,
        SwapDummyData.IBOR_RATE_OBSERVATION.getFixingDate(), 140.0, GBP);
    forwardRateSensi.add(fwdSense);
    MulticurveSensitivity3LD sensiIbor = MulticurveSensitivity3LD.ofForwardRate(forwardRateSensi);
    List<ZeroRateSensitivityLD> dscRateSensi = new ArrayList<>();
    ZeroRateSensitivityLD dscSense = new ZeroRateSensitivityLD(GBP,
        SwapDummyData.IBOR_RATE_PAYMENT_PERIOD.getPaymentDate(), -162.0, GBP);
    dscRateSensi.add(dscSense);
    MulticurveSensitivity3LD sensiDsc = MulticurveSensitivity3LD.ofZeroRate(dscRateSensi);
    sensiIbor.add(sensiDsc);
    /* fixed leg */
    double pvFixed = -500d;
    List<ZeroRateSensitivityLD> dscRateSensiFix = new ArrayList<>();
    ZeroRateSensitivityLD dscSenseFix = new ZeroRateSensitivityLD(GBP,
        SwapDummyData.IBOR_RATE_PAYMENT_PERIOD.getPaymentDate(), 152.0, GBP);
    dscRateSensiFix.add(dscSenseFix);
    MulticurveSensitivity3LD sensiFix = MulticurveSensitivity3LD.ofZeroRate(dscRateSensiFix);

    SwapLegPricerFn<ExpandedSwapLeg> mockSwapLegFn = mock(SwapLegPricerFn.class);
    PricingEnvironment mockEnv = mock(PricingEnvironment.class);
    when(mockSwapLegFn.presentValueCurveSensitivity3LD(mockEnv, SwapDummyData.IBOR_EXPANDED_SWAP_LEG))
        .thenReturn(Pair.of(pvIbor, sensiIbor));
    when(mockSwapLegFn.presentValueCurveSensitivity3LD(mockEnv, SwapDummyData.FIXED_EXPANDED_SWAP_LEG))
        .thenReturn(Pair.of(pvFixed, sensiFix));

    DefaultExpandedSwapPricerFn test = new DefaultExpandedSwapPricerFn(mockSwapLegFn);
    Pair<MultiCurrencyAmount, MulticurveSensitivity3LD> res = test.presentValueCurveSensitivity3LD(mockEnv,
        SwapDummyData.SWAP.expand());

    sensiIbor.add(sensiFix);
    assertEquals(res.getFirst().getAmount(GBP).getAmount(), pvIbor + pvFixed, TOL);
    CurveSensitivityTestUtil.assertMulticurveSensitivity3LD(res.getSecond(), sensiIbor, TOL);
  }
}
