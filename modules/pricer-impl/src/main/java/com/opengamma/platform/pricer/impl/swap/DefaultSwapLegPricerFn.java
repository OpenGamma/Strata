/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import java.time.LocalDate;

import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.swap.ExpandedSwapLeg;
import com.opengamma.platform.finance.swap.SwapLeg;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.SwapLegPricerFn;

/**
 * Pricer for swap legs.
 */
public class DefaultSwapLegPricerFn
    implements SwapLegPricerFn<SwapLeg> {

  /**
   * Default implementation.
   */
  public static final DefaultSwapLegPricerFn DEFAULT = new DefaultSwapLegPricerFn(
      DefaultExpandedSwapLegPricerFn.DEFAULT);

  //-------------------------------------------------------------------------
  /**
   * Handle {@link ExpandedSwapLeg}.
   */
  private SwapLegPricerFn<ExpandedSwapLeg> expandedSwapLegPricerFn;

  /**
   * Creates an instance.
   * 
   * @param expandedSwapLegPricerFn  the pricer for {@link ExpandedSwapLeg}
   */
  public DefaultSwapLegPricerFn(
      SwapLegPricerFn<ExpandedSwapLeg> expandedSwapLegPricerFn) {
    super();
    this.expandedSwapLegPricerFn = ArgChecker.notNull(expandedSwapLegPricerFn, "expandedSwapLegPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      SwapLeg swapLeg) {
    return expandedSwapLegPricerFn.presentValue(env, valuationDate, swapLeg.toExpanded());
  }

  @Override
  public double futureValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      SwapLeg swapLeg) {
    return expandedSwapLegPricerFn.futureValue(env, valuationDate, swapLeg.toExpanded());
  }

}
