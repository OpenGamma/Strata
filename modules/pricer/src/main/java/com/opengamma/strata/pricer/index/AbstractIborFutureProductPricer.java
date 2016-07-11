/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.product.index.ResolvedIborFuture;

/**
 * Base pricer for Ibor futures.
 * <p>
 * This function provides common code used when pricing a {@link ResolvedIborFuture}.
 * 
 * <h4>Price</h4>
 * The price of an Ibor future is based on the interest rate of the underlying index.
 * It is defined as {@code (100 - percentRate)}.
 * <p>
 * Strata uses <i>decimal prices</i> for Ibor futures in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
 */
public abstract class AbstractIborFutureProductPricer {

  /**
   * Creates an instance.
   */
  protected AbstractIborFutureProductPricer() {
  }

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
  double marginIndex(ResolvedIborFuture future, double price) {
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
  PointSensitivities marginIndexSensitivity(ResolvedIborFuture future, PointSensitivities priceSensitivity) {
    return priceSensitivity.multipliedBy(future.getNotional() * future.getAccrualFactor());
  }

}
