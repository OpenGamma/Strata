/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.future.DeliverableSwapFuture;
import com.opengamma.strata.finance.rate.future.DeliverableSwapFutureTrade;

/**
 * Base pricer for deliverable swap futures.
 * <p>
 * This function provides common code used when pricing an {@link DeliverableSwapFutureTrade}.
 */
public abstract class AbstractDeliverableSwapFutureTradePricer {

  /**
   * Creates an instance.
   */
  protected AbstractDeliverableSwapFutureTradePricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer used to price the product underlying the trade.
   * 
   * @return the pricer
   */
  protected abstract AbstractDeliverableSwapFutureProductPricer getProductPricer();

  /**
   * Calculates the present value of the deliverable swap futures trade from the current price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param trade  the trade to price
   * @param currentPrice  the price on the valuation date
   * @param referencePrice  the price with respect to which the margining should be done. The reference price is
   *   the trade price before any margining has taken place and the price used for the last margining otherwise.
   * @return the present value
   */
  public CurrencyAmount presentValue(DeliverableSwapFutureTrade trade, double currentPrice, double referencePrice) {
    DeliverableSwapFuture future = trade.getProduct();
    double priceIndex = getProductPricer().marginIndex(future, currentPrice);
    double referenceIndex = getProductPricer().marginIndex(future, referencePrice);
    double pv = (priceIndex - referenceIndex) * trade.getQuantity();
    return CurrencyAmount.of(future.getCurrency(), pv);
  }

}
