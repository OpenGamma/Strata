/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import com.opengamma.strata.finance.rate.future.DeliverableSwapFuture;
import com.opengamma.strata.market.sensitivity.PointSensitivities;

/**
 * Base pricer for deliverable swap futures.
 * <p>
 * This function provides common code used when pricing an {@link DeliverableSwapFuture}.
 */
public abstract class AbstractDeliverableSwapFutureProductPricer {

  /**
   * Creates an instance.
   */
  protected AbstractDeliverableSwapFutureProductPricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the number related to deliverable swap futures product on which the daily margin is computed.
   * <p>
   * For two consecutive closing prices C1 and C2, the daily margin is computed as 
   *    {@code (marginIndex(future, C2) - marginIndex(future, C1))}.
   * 
   * @param futures  the futures
   * @param price  the price of the product, in decimal form
   * @return the index
   */
  protected double marginIndex(DeliverableSwapFuture futures, double price) {
    return price * futures.getNotional();
  }

  /**
   * Calculates the margin index sensitivity of the deliverable swap futures product.
   * <p>
   * The margin index sensitivity is the sensitivity of the margin index to the underlying curves.
   * For two consecutive closing prices C1 and C2, the daily margin is computed as 
   *    {@code (marginIndex(future, C2) - marginIndex(future, C1))}.
   * 
   * @param futures  the futures
   * @param priceSensitivity  the price sensitivity of the product
   * @return the index sensitivity
   */
  protected PointSensitivities marginIndexSensitivity(DeliverableSwapFuture futures, PointSensitivities priceSensitivity) {
    return priceSensitivity.multipliedBy(futures.getNotional());
  }

}
