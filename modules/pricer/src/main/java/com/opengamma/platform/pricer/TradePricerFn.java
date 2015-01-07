/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.Trade;

/**
 * Pricer for trades.
 * <p>
 * This function provides the ability to price a {@link Trade}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of trade
 */
public interface TradePricerFn<T extends Trade> {

  /**
   * Calculates the present value of the trade.
   * <p>
   * The present value is the current value of the trade.
   * Present value expresses the notion that a trade that pays USD 1000 in 6 months
   * time is worth less than a similar trade that pays USD 1000 tomorrow.
   * <p>
   * Present value is typically calculated by applying a discount rate to each future cash flow.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @return the present value of the trade
   */
  public abstract MultiCurrencyAmount presentValue(PricingEnvironment env, T trade);

  /**
   * Calculates the future value of the trade.
   * <p>
   * The future value is the sum of all future cash flows.
   * This is expressed without present value discounting.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @return the future value of the trade
   */
  public abstract MultiCurrencyAmount futureValue(PricingEnvironment env, T trade);

}
