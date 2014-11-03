/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.swap;

import java.time.LocalDate;

import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.platform.finance.swap.SwapTrade;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Pricer for swaps.
 */
public interface SwapPricerFn {

  /**
   * Calculates the present value of the swap.
   * 
   * @param env  the pricing environment
   * @param valuationDate  the valuation date
   * @param trade  the trade to price
   * @return the present value of the swap
   */
  public CurrencyAmount presentValue(PricingEnvironment env, LocalDate valuationDate, SwapTrade trade);

}
