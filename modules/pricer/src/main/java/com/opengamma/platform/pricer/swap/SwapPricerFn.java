/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.swap;

import java.time.LocalDate;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.swap.Swap;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Pricer for swaps.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public interface SwapPricerFn {

  /**
   * Calculates the present value of the swap.
   * 
   * @param env  the pricing environment
   * @param valuationDate  the valuation date
   * @param swap  the swap to price
   * @return the present value of the swap
   */
  public abstract MultiCurrencyAmount presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      Swap swap);

}
