/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.rate.swap;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.rate.swap.Swap;
import com.opengamma.platform.finance.rate.swap.SwapTrade;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Pricer for Swap trades.
 * <p>
 * This function provides the ability to price a {@link Swap}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public interface SwapTradePricerFn {

  /**
   * Calculates the present value of the Swap trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * This is typically implemented as the discounted future value.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @return the present value of the trade
   */
  public abstract MultiCurrencyAmount presentValue(PricingEnvironment env, SwapTrade trade);

  /**
   * Calculates the future value of the Swap trade.
   * <p>
   * The future value of the trade is the value on the valuation date without discounting.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @return the future value of the trade
   */
  public abstract MultiCurrencyAmount futureValue(PricingEnvironment env, SwapTrade trade);

}
