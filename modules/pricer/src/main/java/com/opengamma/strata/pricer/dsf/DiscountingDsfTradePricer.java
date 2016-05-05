/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.dsf;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.dsf.Dsf;
import com.opengamma.strata.product.dsf.ResolvedDsf;
import com.opengamma.strata.product.dsf.ResolvedDsfTrade;

/**
 * Pricer implementation for Deliverable Swap Futures (DSFs).
 * <p>
 * This function provides the ability to price a {@link ResolvedDsfTrade}.
 */
public class DiscountingDsfTradePricer
    extends AbstractDsfTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingDsfTradePricer DEFAULT =
      new DiscountingDsfTradePricer(DiscountingDsfProductPricer.DEFAULT);

  /**
   * Underlying pricer.
   */
  private final DiscountingDsfProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link Dsf}
   */
  public DiscountingDsfTradePricer(
      DiscountingDsfProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  @Override
  protected DiscountingDsfProductPricer getProductPricer() {
    return productPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the underlying deliverable swap futures product.
   * <p>
   * The price of the trade is the price on the valuation date.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the price of the trade, in decimal form
   */
  public double price(ResolvedDsfTrade trade, RatesProvider provider) {
    return productPricer.price(trade.getProduct(), provider);
  }

  /**
   * Calculates the present value of the deliverable swap futures trade.
   * <p>
   * The present value of the product is the value on the valuation date.
   * <p>
   * The calculation is performed against a reference price. The reference price should
   * be the settlement price except on the trade date, when it is the trade price.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @param referencePrice  the price with respect to which the margining should be done
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedDsfTrade trade,
      RatesProvider provider,
      double referencePrice) {

    double price = price(trade, provider);
    return presentValue(trade, price, referencePrice);
  }

  /**
   * Calculates the present value sensitivity of the deliverable swap futures trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivity(ResolvedDsfTrade trade, RatesProvider provider) {
    ResolvedDsf product = trade.getProduct();
    PointSensitivities priceSensi = productPricer.priceSensitivity(product, provider);
    PointSensitivities marginIndexSensi = productPricer.marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }

  /**
   * Calculates the currency exposure of the deliverable swap futures trade.
   * <p>
   * Since the deliverable swap futures is based on a single currency, the trade is exposed to only this currency.  
   * <p>
   * The calculation is performed against a reference price. The reference price should
   * be the settlement price except on the trade date, when it is the trade price.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @param referencePrice  the price with respect to which the margining should be done
   * @return the currency exposure of the trade
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedDsfTrade trade,
      RatesProvider provider,
      double referencePrice) {

    return MultiCurrencyAmount.of(presentValue(trade, provider, referencePrice));
  }

}
