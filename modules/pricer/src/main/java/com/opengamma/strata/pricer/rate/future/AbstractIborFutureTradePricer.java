/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import java.time.LocalDate;
import java.util.Optional;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.finance.rate.future.IborFutureTrade;

/**
 * Base pricer for Ibor futures.
 * <p>
 * This function provides common code used when pricing an {@link IborFutureTrade}.
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
   * @param trade  the trade to price
   * @param currentPrice  the price on the valuation date
   * @param referencePrice  the price with respect to which the margining should be done. 
   * @return the present value
   */
  public CurrencyAmount presentValue(IborFutureTrade trade, double currentPrice, double referencePrice) {
    IborFuture future = trade.getSecurity().getProduct();
    double priceIndex = getProductPricer().marginIndex(future, currentPrice);
    double referenceIndex = getProductPricer().marginIndex(future, referencePrice);
    double pv = (priceIndex - referenceIndex) * trade.getQuantity();
    return CurrencyAmount.of(future.getCurrency(), pv);
  }
  
  /**
   * Calculates the reference price for a futures trade. The reference price is the trade price before any margining 
   * has taken place and the price used for the last margining otherwise.
   * 
   * @param trade  the trade to price
   * @param valuationDate  the date for which the reference price should be calculated
   * @param lastMarginPrice  the last price used in the margining 
   * @return the reference price
   */
  public double referencePrice(IborFutureTrade trade, LocalDate valuationDate, double lastMarginPrice) {
    Optional<LocalDate> tradeDate = trade.getTradeInfo().getTradeDate();
    ArgChecker.isTrue(tradeDate.isPresent(), "trade date should be populated");
    double referencePrice;
    if (tradeDate.get().equals(valuationDate)) {
      referencePrice = trade.getInitialPrice();
    } else {
      referencePrice = lastMarginPrice;
    }
    return referencePrice;
  }

}
