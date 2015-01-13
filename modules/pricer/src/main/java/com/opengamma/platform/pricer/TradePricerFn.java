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
   * The present value of the trade is the value on the valuation date.
   * <p>
   * Present value expresses the time value of money and the uncertainty about the amount of cash flows.
   * In classical quantitative finance, present value is computed as the expected value of the discounted cash flows.
   * <p>
   * For simple instruments, present value is typically calculated by applying
   * a discount rate to each future cash flow.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @return the present value of the trade
   */
  public abstract MultiCurrencyAmount presentValue(PricingEnvironment env, T trade);

}
