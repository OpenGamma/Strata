/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.product.bond.ResolvedBondFuture;
import com.opengamma.strata.product.bond.ResolvedBondFutureTrade;

/**
 * Base pricer for bond futures.
 * <p>
 * This function provides common code used when pricing an {@link ResolvedBondFuture}.
 */
public abstract class AbstractBondFutureTradePricer {

  /**
   * Creates an instance.
   */
  protected AbstractBondFutureTradePricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer used to price the product underlying the trade.
   * 
   * @return the pricer
   */
  protected abstract AbstractBondFutureProductPricer getProductPricer();

  /**
   * Calculates the present value of the bond future trade from the current price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param trade  the trade
   * @param currentPrice  the price on the valuation date
   * @param referencePrice  the price with respect to which the margining should be done. The reference price is
   *   the trade date before any margining has taken place and the price used for the last margining otherwise.
   * @return the present value
   */
  public CurrencyAmount presentValue(ResolvedBondFutureTrade trade, double currentPrice, double referencePrice) {
    ResolvedBondFuture future = trade.getProduct();
    double priceIndex = getProductPricer().marginIndex(future, currentPrice);
    double referenceIndex = getProductPricer().marginIndex(future, referencePrice);
    double pv = (priceIndex - referenceIndex) * trade.getQuantity();
    return CurrencyAmount.of(future.getCurrency(), pv);
  }

  /**
   * Calculates the currency exposure of the bond future trade from the current price.
   * 
   * @param trade  the trade
   * @param currentPrice  the price on the valuation date
   * @param referencePrice  the price with respect to which the margining should be done. The reference price is
   *   the trade date before any margining has taken place and the price used for the last margining otherwise.
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(ResolvedBondFutureTrade trade, double currentPrice, double referencePrice) {
    return MultiCurrencyAmount.of(presentValue(trade, currentPrice, referencePrice));
  }

}
