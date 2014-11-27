/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.swap;

import java.time.LocalDate;

import com.opengamma.platform.finance.swap.SwapLeg;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Pricer for swap legs.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of period
 */
public interface SwapLegPricerFn<T extends SwapLeg> {

  /**
   * Calculates the present value of the swap.
   * 
   * @param env  the pricing environment
   * @param valuationDate  the valuation date
   * @param swapLeg  the swap leg to price
   * @return the present value of the swap
   */
  public abstract double presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      T swapLeg);

  /**
   * Calculates the future value of the swap.
   * 
   * @param env  the pricing environment
   * @param valuationDate  the valuation date
   * @param swapLeg  the swap leg to price
   * @return the future value of the swap
   */
  public abstract double futureValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      T swapLeg);

}
