/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.index.ResolvedIborFuture;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;

/**
 * Base pricer for Ibor futures.
 * <p>
 * This function provides common code used when pricing a {@link ResolvedIborFutureTrade}.
 */
public abstract class AbstractIborFutureTradePricer {

  /**
   * Creates an instance.
   */
  protected AbstractIborFutureTradePricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer used to price the product underlying the trade.
   * 
   * @return the pricer
   */
  protected abstract AbstractIborFutureProductPricer getProductPricer();

  /**
   * Calculates the present value of the Ibor future trade from the current price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param trade  the trade
   * @param currentPrice  the price on the valuation date
   * @param referencePrice  the price with respect to which the margining should be done. 
   * @return the present value
   */
  public CurrencyAmount presentValue(ResolvedIborFutureTrade trade, double currentPrice, double referencePrice) {
    ResolvedIborFuture future = trade.getProduct();
    double priceIndex = getProductPricer().marginIndex(future, currentPrice);
    double referenceIndex = getProductPricer().marginIndex(future, referencePrice);
    double pv = (priceIndex - referenceIndex) * trade.getQuantity();
    return CurrencyAmount.of(future.getCurrency(), pv);
  }

  /**
   * Calculates the reference price for a futures trade.
   * <p>
   * The reference price is the trade price before any margining has taken place,
   * and the price used for the last margining otherwise.
   * 
   * @param trade  the trade
   * @param valuationDate  the date for which the reference price should be calculated
   * @param lastMarginPrice  the last price used in the margining 
   * @return the reference price
   */
  public double referencePrice(ResolvedIborFutureTrade trade, LocalDate valuationDate, double lastMarginPrice) {
    ArgChecker.notNull(valuationDate, "valuation date");
    return (trade.getTradeDate().equals(valuationDate) ? trade.getPrice() : lastMarginPrice);
  }

}
