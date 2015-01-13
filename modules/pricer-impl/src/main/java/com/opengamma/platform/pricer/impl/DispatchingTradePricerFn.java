/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.Trade;
import com.opengamma.platform.finance.swap.SwapTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.TradePricerFn;
import com.opengamma.platform.pricer.impl.swap.DefaultSwapTradePricerFn;

/**
 * Pricer implementation for trades using multiple dispatch.
 * <p>
 * Dispatches the request to the correct implementation.
 */
public class DispatchingTradePricerFn
    implements TradePricerFn<Trade> {

  /**
   * Default implementation.
   */
  public static final DispatchingTradePricerFn DEFAULT = new DispatchingTradePricerFn(
      DefaultSwapTradePricerFn.DEFAULT);

  /**
   * Pricer for {@link SwapTrade}.
   */
  private final TradePricerFn<SwapTrade> swapTradePricerFn;

  /**
   * Creates an instance.
   * 
   * @param swapTradePricerFn  the rate provider for {@link SwapTrade}
   */
  public DispatchingTradePricerFn(
      TradePricerFn<SwapTrade> swapTradePricerFn) {
    this.swapTradePricerFn = ArgChecker.notNull(swapTradePricerFn, "swapTradePricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount presentValue(PricingEnvironment env, Trade trade) {
    // dispatch by runtime type
    if (trade instanceof SwapTrade) {
      return swapTradePricerFn.presentValue(env, (SwapTrade) trade);
    } else {
      throw new IllegalArgumentException("Unknown Trade type: " + trade.getClass().getSimpleName());
    }
  }

}
