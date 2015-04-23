/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import com.opengamma.strata.finance.common.FutureOptionPremiumStyle;
import com.opengamma.strata.finance.rate.future.IborFutureOption;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Pricer for Ibor future option products with {@link FutureOptionPremiumStyle} DAILY_MARGIN.
 * <p>
 * This function provides the ability to price an {@link IborFutureOption}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public abstract class IborFutureOptionMarginedProductPricer {

  /**
   * Calculates the price of the Ibor future option product.
   * <p>
   * The price of the option is the price on the valuation date.
   * @param option  the option product to price
   * @param env  the pricing environment
   * @param parameters  the model parameters
   * 
   * @return the price of the product, in decimal form
   */
  public abstract double price(IborFutureOption option, PricingEnvironment env, IborFutureParameters parameters);
  
  /**
   * Calculates the price sensitivity of the Ibor future option product.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * @param option  the option product to price
   * @param env  the pricing environment
   * @param parameters  the model parameters
   * 
   * @return the price curve sensitivity of the product
   */
  public abstract PointSensitivities priceSensitivity(IborFutureOption option, PricingEnvironment env, 
      IborFutureParameters parameters);

  /**
   * Calculates the number related to Ibor futures product on which the daily margin is computed.
   * <p>
   * For two consecutive closing prices C1 and C2, the daily margin is computed as 
   *    marginIndex(future, C2) - marginIndex(future, C1).
   * @param option  the option product to price
   * @param price  the price of the product, in decimal form
   * @return the index
   */
  public double marginIndex(IborFutureOption option, double price) {
    return price * option.getUnderlying().getProduct().getNotional() * 
        option.getUnderlying().getProduct().getAccrualFactor();
  }

  /**
   * Calculates the margin index sensitivity of the Ibor future product.
   * <p>
   * For two consecutive closing prices C1 and C2, the daily margin is computed as 
   *    marginIndex(future, C2) - marginIndex(future, C1).
   * The margin index sensitivity if the sensitivity of the margin index to the underlying curves.
   * @param option  the option product to price
   * @param priceSensitivity  the price sensitivity of the product
   * @return the index sensitivity
   */
  public PointSensitivities marginIndexSensitivity(IborFutureOption option, PointSensitivities priceSensitivity) {
    return priceSensitivity.multipliedBy(option.getUnderlying().getProduct().getNotional() * 
        option.getUnderlying().getProduct().getAccrualFactor());
  }

}
