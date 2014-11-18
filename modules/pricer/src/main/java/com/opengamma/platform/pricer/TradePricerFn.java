/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer;

import java.time.LocalDate;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.Trade;

/**
 * Pricer for trades.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of trade
 */
public interface TradePricerFn<T extends Trade> {

  /**
   * Calculates the present value of the trade.
   * 
   * @param env  the pricing environment
   * @param valuationDate  the valuation date
   * @param trade  the trade to price
   * @return the present value of the swap
   */
  public abstract MultiCurrencyAmount presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      T trade);

  /**
   * Calculates the future value of the trade.
   * 
   * @param env  the pricing environment
   * @param valuationDate  the valuation date
   * @param trade  the trade to price
   * @return the future value of the swap
   */
  public abstract MultiCurrencyAmount futureValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      T trade);

}
