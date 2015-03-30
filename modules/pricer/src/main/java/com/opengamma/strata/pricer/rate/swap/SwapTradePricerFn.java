/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.pricer.PricingEnvironment;

/**
 * Pricer for swap trades.
 * <p>
 * This function provides the ability to price a {@link Swap}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public interface SwapTradePricerFn {

  /**
   * Calculates the present value of the swap trade in a single currency.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * This is typically implemented as the discounted future value.
   * The result is converted to the specified currency.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @param currency  the currency to convert to
   * @return the present value of the trade in the specified currency
   */
  public default CurrencyAmount presentValue(PricingEnvironment env, SwapTrade trade, Currency currency) {
    return env.fxConvert(presentValue(env, trade), currency);
  }

  /**
   * Calculates the present value of the swap trade.
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
   * Calculates the future value of the swap trade.
   * <p>
   * The future value of the trade is the value on the valuation date without discounting.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @return the future value of the trade
   */
  public abstract MultiCurrencyAmount futureValue(PricingEnvironment env, SwapTrade trade);

}
