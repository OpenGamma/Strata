/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.future.DeliverableSwapFuture;
import com.opengamma.strata.finance.rate.future.DeliverableSwapFutureTrade;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Pricer implementation for deliverable swap futures.
 * <p>
 * This function provides the ability to price a {@link DeliverableSwapFutureTrade}.
 */
public class DiscountingDeliverableSwapFutureTradePricer
    extends AbstractDeliverableSwapFutureTradePricer {

  /**
  * Default implementation.
  */
  public static final DiscountingDeliverableSwapFutureTradePricer DEFAULT =
      new DiscountingDeliverableSwapFutureTradePricer(DiscountingDeliverableSwapFutureProductPricer.DEFAULT);

  /**
  * Underlying pricer.
  */
  private final DiscountingDeliverableSwapFutureProductPricer productPricer;

  /**
  * Creates an instance.
  * 
  * @param productPricer  the pricer for {@link DeliverableSwapFuture}
  */
  public DiscountingDeliverableSwapFutureTradePricer(
      DiscountingDeliverableSwapFutureProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  @Override
  protected DiscountingDeliverableSwapFutureProductPricer getProductPricer() {
    return productPricer;
  }

  //-------------------------------------------------------------------------
  /**
  * Calculates the price of the underlying deliverable swap futures product.
  * <p>
  * The price of the trade is the price on the valuation date.
  * 
  * @param trade  the trade to price
  * @param provider  the rates provider
  * @return the price of the trade, in decimal form
  */
  public double price(DeliverableSwapFutureTrade trade, RatesProvider provider) {
    return productPricer.price(trade.getSecurity().getProduct(), provider);
  }

  /**
  * Calculates the present value of the deliverable swap futures trade.
  * <p>
  * The present value of the product is the value on the valuation date.
  * 
  * @param trade  the trade to price
  * @param provider  the rates provider
  * @param referencePrice  the price with respect to which the margining should be done. The reference price is
  *   the trade price before any margining has taken place and the price used for the last margining otherwise.
  * @return the present value
  */
  public CurrencyAmount presentValue(DeliverableSwapFutureTrade trade, RatesProvider provider, double referencePrice) {
    double price = price(trade, provider);
    return presentValue(trade, price, referencePrice);
  }

  /**
  * Calculates the present value sensitivity of the deliverable swap futures trade.
  * <p>
  * The present value sensitivity of the trade is the sensitivity of the present value to
  * the underlying curves.
  * 
  * @param trade  the trade to price
  * @param provider  the rates provider
  * @return the present value curve sensitivity of the trade
  */
  public PointSensitivities presentValueSensitivity(DeliverableSwapFutureTrade trade, RatesProvider provider) {
    DeliverableSwapFuture product = trade.getProduct();
    PointSensitivities priceSensi = productPricer.priceSensitivity(product, provider);
    PointSensitivities marginIndexSensi = productPricer.marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }

  /**
  * Calculates the currency exposure of the deliverable swap futures trade.
  * <p>
  * Since the deliverable swap futures is based on a single currency, the trade is exposed to only this currency.  
  * 
  * @param trade  the trade to price
  * @param provider  the rates provider
  * @param referencePrice  the price with respect to which the margining should be done. The reference price is
  *   the trade price before any margining has taken place and the price used for the last margining otherwise.
  * @return the currency exposure of the trade
  */
  public MultiCurrencyAmount currencyExposure(
      DeliverableSwapFutureTrade trade,
      RatesProvider provider,
      double referencePrice) {

    return MultiCurrencyAmount.of(presentValue(trade, provider, referencePrice));
  }

}
