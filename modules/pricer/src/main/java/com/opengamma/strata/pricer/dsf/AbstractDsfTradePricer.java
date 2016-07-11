/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.dsf;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.dsf.ResolvedDsf;
import com.opengamma.strata.product.dsf.ResolvedDsfTrade;

/**
 * Base pricer for Deliverable Swap Futures (DSFs).
 * <p>
 * This function provides common code used when pricing an {@link ResolvedDsfTrade}.
 */
public abstract class AbstractDsfTradePricer {

  /**
   * Creates an instance.
   */
  protected AbstractDsfTradePricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer used to price the product underlying the trade.
   * 
   * @return the pricer
   */
  protected abstract AbstractDsfProductPricer getProductPricer();

  /**
   * Calculates the present value of the deliverable swap futures trade from the current price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * <p>
   * The calculation is performed against a reference price. The reference price should
   * be the settlement price except on the trade date, when it is the trade price.
   * 
   * @param trade  the trade
   * @param currentPrice  the price on the valuation date
   * @param referencePrice  the price with respect to which the margining should be done
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedDsfTrade trade,
      double currentPrice,
      double referencePrice) {

    ResolvedDsf future = trade.getProduct();
    double priceIndex = getProductPricer().marginIndex(future, currentPrice);
    double referenceIndex = getProductPricer().marginIndex(future, referencePrice);
    double pv = (priceIndex - referenceIndex) * trade.getQuantity();
    return CurrencyAmount.of(future.getCurrency(), pv);
  }

}
