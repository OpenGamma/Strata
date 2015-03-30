/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.rate.fra;

import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.fra.FraTrade;

/**
 * Pricer for FRA trades.
 * <p>
 * This function provides the ability to price a {@link FraTrade}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public interface FraTradePricerFn {

  /**
   * Calculates the present value of the FRA trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * This is typically implemented as the discounted future value.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @return the present value of the trade
   */
  public abstract CurrencyAmount presentValue(PricingEnvironment env, FraTrade trade);

  /**
   * Calculates the future value of the FRA trade.
   * <p>
   * The future value of the trade is the value on the valuation date without discounting.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @return the future value of the trade
   */
  public abstract CurrencyAmount futureValue(PricingEnvironment env, FraTrade trade);

}
