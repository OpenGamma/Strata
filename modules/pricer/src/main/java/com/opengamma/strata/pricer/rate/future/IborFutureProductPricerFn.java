/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Pricer for Ibor future trades.
 * <p>
 * This function provides the ability to price an {@link IborFuture}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public interface IborFutureProductPricerFn {

  /**
   * Calculates the price of the Ibor future product.
   * <p>
   * The price of the product is the price on the valuation date.
   * 
   * @param env  the pricing environment
   * @param future  the future to price
   * @return the price of the product, in decimal form
   */
  public abstract double price(PricingEnvironment env, IborFuture future);

  /**
   * Calculates the price sensitivity of the Ibor future product.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * 
   * @param env  the pricing environment
   * @param future  the future to price
   * @return the price curve sensitivity of the product
   */
  public abstract PointSensitivities priceSensitivity(PricingEnvironment env, IborFuture future);

  //-------------------------------------------------------------------------
  /**
   * Calculates the number related to Ibor futures product on which the daily margin is computed.
   * <p>
   * For two consecutive closing prices C1 and C2, the daily margin is computed as 
   *    {@code (marginIndex(future, C2) - marginIndex(future, C1))}.
   * 
   * @param future  the future
   * @param price  the price of the product, in decimal form
   * @return the index
   */
  public default double marginIndex(IborFuture future, double price) {
    return price * future.getNotional() * future.getAccrualFactor();
  }

  /**
   * Calculates the margin index sensitivity of the Ibor future product.
   * <p>
   * The margin index sensitivity is the sensitivity of the margin index to the underlying curves.
   * For two consecutive closing prices C1 and C2, the daily margin is computed as 
   *    {@code (marginIndex(future, C2) - marginIndex(future, C1))}.
   * 
   * @param future  the future
   * @param priceSensitivity  the price sensitivity of the product
   * @return the index sensitivity
   */
  public default PointSensitivities marginIndexSensitivity(IborFuture future, PointSensitivities priceSensitivity) {
    return priceSensitivity.multipliedBy(future.getNotional() * future.getAccrualFactor());
  }

}
