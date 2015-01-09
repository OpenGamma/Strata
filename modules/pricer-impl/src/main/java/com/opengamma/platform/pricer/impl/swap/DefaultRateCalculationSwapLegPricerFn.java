/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.swap.ExpandedSwapLeg;
import com.opengamma.platform.finance.swap.RateCalculationSwapLeg;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.SwapLegPricerFn;

/**
 * Pricer implementation for rate calculation swap legs.
 * <p>
 * The swap leg is priced by by expanding the swap legs.
 */
public class DefaultRateCalculationSwapLegPricerFn
    implements SwapLegPricerFn<RateCalculationSwapLeg> {

  /**
   * Default implementation.
   */
  public static final DefaultRateCalculationSwapLegPricerFn DEFAULT = new DefaultRateCalculationSwapLegPricerFn(
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
  public DefaultRateCalculationSwapLegPricerFn(
      SwapLegPricerFn<ExpandedSwapLeg> expandedSwapLegPricerFn) {
    this.expandedSwapLegPricerFn = ArgChecker.notNull(expandedSwapLegPricerFn, "expandedSwapLegPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(PricingEnvironment env, RateCalculationSwapLeg swapLeg) {
    return expandedSwapLegPricerFn.presentValue(env, swapLeg.expand());
  }

  @Override
  public double futureValue(PricingEnvironment env, RateCalculationSwapLeg swapLeg) {
    return expandedSwapLegPricerFn.futureValue(env, swapLeg.expand());
  }

}
