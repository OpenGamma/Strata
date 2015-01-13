/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.swap;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.swap.Swap;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Pricer for swaps.
 * <p>
 * This function provides the ability to price a {@link Swap}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public interface SwapPricerFn {

  /**
   * Calculates the present value of the swap.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * This is typically implemented as the discounted future value.
   * 
   * @param env  the pricing environment
   * @param swap  the swap to price
   * @return the present value of the swap
   */
  public abstract MultiCurrencyAmount presentValue(PricingEnvironment env, Swap swap);

  /**
   * Calculates the future value of the swap.
   * <p>
   * The future value of the trade is the value on the valuation date without discounting.
   * 
   * @param env  the pricing environment
   * @param swap  the swap to price
   * @return the future value of the swap
   */
  public abstract MultiCurrencyAmount futureValue(PricingEnvironment env, Swap swap);

}
