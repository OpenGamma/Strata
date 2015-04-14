/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.finance.rate.future.IborFutureTrade;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Pricer for Ibor future trades.
 * <p>
 * This function provides the ability to price an {@link IborFutureTrade}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public interface IborFutureTradePricerFn {
  
  /**
   * Returns the {@link IborFutureProductPricerFn} used for the computation related to the future underlying the trade.
   * @return  the future product pricer
   */
  public abstract IborFutureProductPricerFn getFutureProductPricerFn();

  /**
   * Calculates the price of the Ibor future trade.
   * <p>
   * The price of the trade is the price on the valuation date.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @return the price of the trade, in decimal form
   */
  public default double price(PricingEnvironment env, IborFutureTrade trade) {
    return getFutureProductPricerFn().price(env, trade.getSecurity().getProduct());
  }

  /**
   * Calculates the present value of the Ibor future trade from the current price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param currentPrice  the price on the valuation date
   * @param trade  the trade to price
   * @param referencePrice  the price with respect to which the margining should be done. The reference price is
   * the trade date before any margining has taken place and the price used for the last margining otherwise.
   * @return the present value
   */
  public default CurrencyAmount presentValue(double currentPrice, IborFutureTrade trade, double referencePrice){
    IborFuture future = trade.getSecurity().getProduct();
    double priceIndex = getFutureProductPricerFn().marginIndex(future, currentPrice);
    double referenceIndex = getFutureProductPricerFn().marginIndex(future, referencePrice);
    double pv = (priceIndex - referenceIndex) * trade.getQuantity();
    return CurrencyAmount.of(future.getCurrency(), pv);
  }

  /**
   * Calculates the present value of the Ibor future trade.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @param referencePrice  the price with respect to which the margining should be done. The reference price is
   * the trade date before any margining has taken place and the price used for the last margining otherwise.
   * @return the present value
   */
  public default CurrencyAmount presentValue(PricingEnvironment env, IborFutureTrade trade, double referencePrice) {
    double price = price(env, trade);
    return presentValue(price, trade, referencePrice);
  }
  
  /**
   * Calculates the par spread of the ibor future trade.
   * <p>
   * The par spread is defined in the following way. When the reference price (or market quote) is increased by the
   * par spread, the present value of the trade is 0.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @param referencePrice  the price with respect to which the margining should be done. The reference price is
   * the trade date before any margining has taken place and the price used for the last margining otherwise.
   * @return the par spread.
   */
  public default double parSpread(PricingEnvironment env, IborFutureTrade trade, double referencePrice) {
    return price(env, trade) - referencePrice;
  }

  /**
   * Calculates the present value sensitivity of the Ibor future trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @return the present value curve sensitivity of the trade
   */
  public default PointSensitivities presentValueSensitivity(PricingEnvironment env, IborFutureTrade trade) {
    IborFuture product = trade.getSecurity().getProduct();
    PointSensitivities priceSensi = getFutureProductPricerFn().priceSensitivity(env, product);
    PointSensitivities marginIndexSensi = getFutureProductPricerFn().marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }
  
  /**
   * Calculates the par spread sensitivity of the Ibor future trade.
   * <p>
   * The par spread sensitivity of the trade is the sensitivity of the par spread to
   * the underlying curves.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @return the par spread curve sensitivity of the trade
   */
  public default PointSensitivities parSpreadSensitivity(PricingEnvironment env, IborFutureTrade trade) {
    return getFutureProductPricerFn().priceSensitivity(env, trade.getSecurity().getProduct());
  }

}
