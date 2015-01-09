/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.swap.ExpandedSwapLeg;
import com.opengamma.platform.finance.swap.RateCalculationSwapLeg;
import com.opengamma.platform.finance.swap.SwapLeg;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.SwapLegPricerFn;

/**
 * Pricer implementation for swap legs using multiple dispatch.
 * <p>
 * Dispatches the request to the correct implementation.
 */
public class DispatchingSwapLegPricerFn
    implements SwapLegPricerFn<SwapLeg> {

  /**
   * Default implementation.
   */
  public static final DispatchingSwapLegPricerFn DEFAULT = new DispatchingSwapLegPricerFn(
      DefaultExpandedSwapLegPricerFn.DEFAULT,
      DefaultRateCalculationSwapLegPricerFn.DEFAULT);

  /**
   * Pricer for {@link ExpandedSwapLeg}.
   */
  private final SwapLegPricerFn<ExpandedSwapLeg> expandedSwapLegPricerFn;
  /**
   * Pricer for {@link RateCalculationSwapLeg}.
   */
  private final SwapLegPricerFn<RateCalculationSwapLeg> rateCalculationSwapLegPricerFn;

  /**
   * Creates an instance.
   * 
   * @param expandedSwapLegPricerFn  the pricer for {@link ExpandedSwapLeg}
   * @param rateCalculationSwapLegPricerFn  the pricer for {@link RateCalculationSwapLeg}
   */
  public DispatchingSwapLegPricerFn(
      SwapLegPricerFn<ExpandedSwapLeg> expandedSwapLegPricerFn,
      SwapLegPricerFn<RateCalculationSwapLeg> rateCalculationSwapLegPricerFn) {
    this.expandedSwapLegPricerFn = ArgChecker.notNull(expandedSwapLegPricerFn, "expandedSwapLegPricerFn");
    this.rateCalculationSwapLegPricerFn = ArgChecker.notNull(rateCalculationSwapLegPricerFn, "rateCalculationSwapLegPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(PricingEnvironment env, SwapLeg swapLeg) {
    // dispatch by runtime type
    if (swapLeg instanceof ExpandedSwapLeg) {
      return expandedSwapLegPricerFn.presentValue(env, (ExpandedSwapLeg) swapLeg);
    } else if (swapLeg instanceof RateCalculationSwapLeg) {
      return rateCalculationSwapLegPricerFn.presentValue(env, (RateCalculationSwapLeg) swapLeg);
    } else {
      throw new IllegalArgumentException("Unknown SwapLeg type: " + swapLeg.getClass().getSimpleName());
    }
  }

  @Override
  public double futureValue(PricingEnvironment env, SwapLeg swapLeg) {
    // dispatch by runtime type
    if (swapLeg instanceof ExpandedSwapLeg) {
      return expandedSwapLegPricerFn.futureValue(env, (ExpandedSwapLeg) swapLeg);
    } else if (swapLeg instanceof RateCalculationSwapLeg) {
      return rateCalculationSwapLegPricerFn.futureValue(env, (RateCalculationSwapLeg) swapLeg);
    } else {
      throw new IllegalArgumentException("Unknown SwapLeg type: " + swapLeg.getClass().getSimpleName());
    }
  }

}
