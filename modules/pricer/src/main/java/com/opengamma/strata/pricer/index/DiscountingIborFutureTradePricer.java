/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.index.ResolvedIborFuture;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;

/**
 * Pricer implementation for Ibor future trades.
 * <p>
 * This function provides the ability to price a {@link ResolvedIborFutureTrade}.
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
   * @param productPricer  the pricer for {@link ResolvedIborFuture}
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
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the price of the trade, in decimal form
   */
  public double price(ResolvedIborFutureTrade trade, RatesProvider provider) {
    return productPricer.price(trade.getProduct(), provider);
  }

  /**
   * Calculates the present value of the Ibor future trade.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @param lastMarginPrice  the last price used in margining. If the valuation is done on the trade date, the trade 
   * price will be used as a reference price; if not, the last margin price will be used.
   * @return the present value
   */
  public CurrencyAmount presentValue(ResolvedIborFutureTrade trade, RatesProvider provider, double lastMarginPrice) {
    double referencePrice = referencePrice(trade, provider.getValuationDate(), lastMarginPrice);
    double price = price(trade, provider);
    return presentValue(trade, price, referencePrice);
  }

  /**
   * Calculates the present value sensitivity of the Ibor future trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivity(ResolvedIborFutureTrade trade, RatesProvider provider) {
    ResolvedIborFuture product = trade.getProduct();
    PointSensitivities priceSensi = productPricer.priceSensitivity(product, provider);
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
   * @param trade  the trade
   * @param provider  the rates provider
   * @param lastMarginPrice  the last price used in margining. If the valuation is done on the trade date, the trade 
   * price will be used as a reference price; if not, the last margin price will be used.
   * @return the par spread.
   */
  public double parSpread(ResolvedIborFutureTrade trade, RatesProvider provider, double lastMarginPrice) {
    double referencePrice = referencePrice(trade, provider.getValuationDate(), lastMarginPrice);
    return price(trade, provider) - referencePrice;
  }

  /**
   * Calculates the par spread sensitivity of the Ibor future trade.
   * <p>
   * The par spread sensitivity of the trade is the sensitivity of the par spread to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par spread curve sensitivity of the trade
   */
  public PointSensitivities parSpreadSensitivity(ResolvedIborFutureTrade trade, RatesProvider provider) {
    return productPricer.priceSensitivity(trade.getProduct(), provider);
  }

}
