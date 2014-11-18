/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import java.time.LocalDate;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.swap.SwapTrade;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.TradePricerFn;
import com.opengamma.platform.pricer.swap.SwapPricerFn;

/**
 * Pricer implementation for swap trades.
 * <p>
 * The value of a swap trade is calculated by examining the embedded swap.
 */
public class DefaultSwapTradePricerFn
    implements TradePricerFn<SwapTrade> {

  /**
   * Default implementation.
   */
  public static final DefaultSwapTradePricerFn DEFAULT = new DefaultSwapTradePricerFn(
      DefaultSwapPricerFn.DEFAULT);

  /**
   * Pricer function.
   */
  private final SwapPricerFn swapPricerFn;

  /**
   * Creates an instance.
   * 
   * @param swapPricerFn  the swap pricer
   */
  public DefaultSwapTradePricerFn(
      SwapPricerFn swapPricerFn) {
    this.swapPricerFn = ArgChecker.notNull(swapPricerFn, "swapPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      SwapTrade trade) {
    return swapPricerFn.presentValue(env, valuationDate, trade.getSwap());
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount futureValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      SwapTrade trade) {
    return swapPricerFn.futureValue(env, valuationDate, trade.getSwap());
  }

}
