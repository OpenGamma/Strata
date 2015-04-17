/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.finance.rate.future.IborFutureTrade;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Pricer implementation for Ibor future trades.
 */
public class DefaultIborFutureTradePricer
    extends BaseIborFuturePricer {

  /**
   * Default implementation.
   */
  public static final DefaultIborFutureTradePricer DEFAULT =
      new DefaultIborFutureTradePricer(DefaultIborFutureProductPricer.DEFAULT);

  /**
   * Underlying pricer.
   */
  private final DefaultIborFutureProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link IborFuture}
   */
  public DefaultIborFutureTradePricer(
      DefaultIborFutureProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the Ibor future trade.
   * <p>
   * The price of the trade is the price on the valuation date.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @return the price of the trade, in decimal form
   */
  public double price(PricingEnvironment env, IborFutureTrade trade) {
    return productPricer.price(env, trade.getSecurity().getProduct());
  }

  /**
   * Calculates the present value of the Ibor future trade.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @param referencePrice  the price with respect to which the margining should be done. The reference price is
   *   the trade date before any margining has taken place and the price used for the last margining otherwise.
   * @return the present value
   */
  public CurrencyAmount presentValue(PricingEnvironment env, IborFutureTrade trade, double referencePrice) {
    double price = price(env, trade);
    return presentValue(price, trade, referencePrice);
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
  public PointSensitivities presentValueSensitivity(PricingEnvironment env, IborFutureTrade trade) {
    IborFuture product = trade.getSecurity().getProduct();
    PointSensitivities priceSensi = productPricer.priceSensitivity(env, product);
    PointSensitivities marginIndexSensi = marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par spread of the Ibor future trade.
   * <p>
   * The par spread is defined in the following way. When the reference price (or market quote)
   * is increased by the par spread, the present value of the trade is zero.
   * 
   * @param env  the pricing environment
   * @param trade  the trade to price
   * @param referencePrice  the price with respect to which the margining should be done. The reference price is
   *   the trade date before any margining has taken place and the price used for the last margining otherwise.
   * @return the par spread.
   */
  public double parSpread(PricingEnvironment env, IborFutureTrade trade, double referencePrice) {
    return price(env, trade) - referencePrice;
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
  public PointSensitivities parSpreadSensitivity(PricingEnvironment env, IborFutureTrade trade) {
    return productPricer.priceSensitivity(env, trade.getSecurity().getProduct());
  }

}
