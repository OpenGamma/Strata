/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.pricer.PricingEnvironment;

/**
 * Pricer for swap legs.
 * <p>
 * This function provides the ability to price a {@link SwapLeg}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of leg
 */
public interface SwapLegPricerFn<T extends SwapLeg> {

  /**
   * Calculates the present value of the swap leg in a single currency.
   * <p>
   * The present value of the leg is the value on the valuation date.
   * This is typically implemented as the discounted future value.
   * The result is converted to the specified currency.
   * 
   * @param env  the pricing environment
   * @param leg  the leg to price
   * @param currency  the currency to convert to
   * @return the present value of the swap leg in the specified currency
   */
  public default CurrencyAmount presentValue(PricingEnvironment env, T leg, Currency currency) {
    return env.fxConvert(presentValue(env, leg), currency);
  }

  /**
   * Calculates the present value of the swap leg.
   * <p>
   * The amount is expressed in the currency of the leg.
   * This returns the value of the leg with discounting.
   * 
   * @param env  the pricing environment
   * @param leg  the swap leg to price
   * @return the present value of the swap leg
   */
  public abstract CurrencyAmount presentValue(PricingEnvironment env, T leg);

  /**
   * Calculates the future value of the swap leg.
   * <p>
   * The amount is expressed in the currency of the leg.
   * This returns the value of the leg without discounting.
   * 
   * @param env  the pricing environment
   * @param leg  the swap leg to price
   * @return the future value of the swap leg
   */
  public abstract CurrencyAmount futureValue(PricingEnvironment env, T leg);

}
