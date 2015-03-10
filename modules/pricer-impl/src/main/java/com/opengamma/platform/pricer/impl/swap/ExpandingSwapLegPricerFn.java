/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import com.opengamma.collect.ArgChecker;
import com.opengamma.collect.tuple.Pair;
import com.opengamma.platform.finance.swap.ExpandedSwapLeg;
import com.opengamma.platform.finance.swap.SwapLeg;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.sensitivity.multicurve.MulticurveSensitivity3LD;
import com.opengamma.platform.pricer.swap.SwapLegPricerFn;

/**
 * Pricer implementation for swap legs.
 * <p>
 * The swap leg is priced by by expanding the swap legs.
 */
public class ExpandingSwapLegPricerFn
    implements SwapLegPricerFn<SwapLeg> {

  /**
   * Default implementation.
   */
  public static final ExpandingSwapLegPricerFn DEFAULT = new ExpandingSwapLegPricerFn(
      DefaultExpandedSwapLegPricerFn.DEFAULT);

  /**
   * Pricer for {@link ExpandedSwapLeg}.
   */
  private final SwapLegPricerFn<ExpandedSwapLeg> expandedSwapLegPricerFn;

  /**
   * Creates an instance.
   * 
   * @param expandedSwapLegPricerFn  the pricer for {@link ExpandedSwapLeg}
   */
  public ExpandingSwapLegPricerFn(
      SwapLegPricerFn<ExpandedSwapLeg> expandedSwapLegPricerFn) {
    this.expandedSwapLegPricerFn = ArgChecker.notNull(expandedSwapLegPricerFn, "expandedSwapLegPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(PricingEnvironment env, SwapLeg swapLeg) {
    return expandedSwapLegPricerFn.presentValue(env, swapLeg.expand());
  }

  @Override
  public double futureValue(PricingEnvironment env, SwapLeg swapLeg) {
    return expandedSwapLegPricerFn.futureValue(env, swapLeg.expand());
  }

  @Override
  public Pair<Double, MulticurveSensitivity3LD> presentValueCurveSensitivity3LD(PricingEnvironment env,
      SwapLeg swapLeg) {
    return expandedSwapLegPricerFn.presentValueCurveSensitivity3LD(env, swapLeg.expand());
  }

}
