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
 * 
 * <h4>Price</h4>
 * The price of an Ibor future is based on the interest rate of the underlying index.
 * It is defined as {@code (100 - percentRate)}.
 * <p>
 * Strata uses <i>decimal prices</i> for Ibor futures in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
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
   * <p>
   * The calculation is performed against a reference price. The reference price should
   * be the settlement price except on the trade date, when it is the trade price.
   * 
   * @param trade  the trade
   * @param currentPrice  the price on the valuation date
   * @param referencePrice  the price with respect to which the margining should be done
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
   * @param settlementPrice  the last settlement price used for margining
   * @return the reference price
   */
  public double referencePrice(ResolvedIborFutureTrade trade, LocalDate valuationDate, double settlementPrice) {
    ArgChecker.notNull(valuationDate, "valuation date");
    return (trade.getTradeDate().equals(valuationDate) ? trade.getPrice() : settlementPrice);
  }

}
