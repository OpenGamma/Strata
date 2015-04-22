/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.finance.rate.future.IborFutureTrade;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Pricer implementation for Ibor future trades.
 * <p>
 * This function provides the ability to price a {@link FraTrade}.
 */
public class DiscountingIborFutureTradePricer
    extends AbstractIborFutureTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingIborFutureTradePricer DEFAULT =
      new DiscountingIborFutureTradePricer(DiscountingIborFutureProductPricer.DEFAULT);

  /**
   * Underlying pricer.
   */
  private final DiscountingIborFutureProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link IborFuture}
   */
  public DiscountingIborFutureTradePricer(
      DiscountingIborFutureProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  @Override
  protected DiscountingIborFutureProductPricer getProductPricer() {
    return productPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the Ibor future trade.
   * <p>
   * The price of the trade is the price on the valuation date.
   * 
   * @param provider  the rates provider
   * @param trade  the trade to price
   * @return the price of the trade, in decimal form
   */
  public double price(RatesProvider provider, IborFutureTrade trade) {
    return productPricer.price(provider, trade.getSecurity().getProduct());
  }

  /**
   * Calculates the present value of the Ibor future trade.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param provider  the rates provider
   * @param trade  the trade to price
   * @param referencePrice  the price with respect to which the margining should be done. The reference price is
   *   the trade date before any margining has taken place and the price used for the last margining otherwise.
   * @return the present value
   */
  public CurrencyAmount presentValue(RatesProvider provider, IborFutureTrade trade, double referencePrice) {
    double price = price(provider, trade);
    return presentValue(trade, price, referencePrice);
  }

  /**
   * Calculates the present value sensitivity of the Ibor future trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param provider  the rates provider
   * @param trade  the trade to price
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivity(RatesProvider provider, IborFutureTrade trade) {
    IborFuture product = trade.getSecurity().getProduct();
    PointSensitivities priceSensi = productPricer.priceSensitivity(provider, product);
    PointSensitivities marginIndexSensi = productPricer.marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par spread of the Ibor future trade.
   * <p>
   * The par spread is defined in the following way. When the reference price (or market quote)
   * is increased by the par spread, the present value of the trade is zero.
   * 
   * @param provider  the rates provider
   * @param trade  the trade to price
   * @param referencePrice  the price with respect to which the margining should be done. The reference price is
   *   the trade date before any margining has taken place and the price used for the last margining otherwise.
   * @return the par spread.
   */
  public double parSpread(RatesProvider provider, IborFutureTrade trade, double referencePrice) {
    return price(provider, trade) - referencePrice;
  }

  /**
   * Calculates the par spread sensitivity of the Ibor future trade.
   * <p>
   * The par spread sensitivity of the trade is the sensitivity of the par spread to
   * the underlying curves.
   * 
   * @param provider  the rates provider
   * @param trade  the trade to price
   * @return the par spread curve sensitivity of the trade
   */
  public PointSensitivities parSpreadSensitivity(RatesProvider provider, IborFutureTrade trade) {
    return productPricer.priceSensitivity(provider, trade.getSecurity().getProduct());
  }

}
