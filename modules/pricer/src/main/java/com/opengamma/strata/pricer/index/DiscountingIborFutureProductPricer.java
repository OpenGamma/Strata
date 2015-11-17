/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.value.IborIndexRates;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.index.IborFuture;

/**
 * Pricer for for Ibor future products.
 * <p>
 * This function provides the ability to price a {@link IborFuture}.
 */
public class DiscountingIborFutureProductPricer
    extends AbstractIborFutureProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingIborFutureProductPricer DEFAULT = new DiscountingIborFutureProductPricer();

  /**
   * Creates an instance.
   */
  public DiscountingIborFutureProductPricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the Ibor future product.
   * <p>
   * The price of the product is the price on the valuation date.
   * 
   * @param future  the future to price
   * @param provider  the rates provider
   * @return the price of the product, in decimal form
   */
  public double price(IborFuture future, RatesProvider provider) {
    IborIndexRates rates = provider.iborIndexRates(future.getIndex());
    double forward = rates.rate(future.getFixingDate());
    return 1.0 - forward;
  }

  /**
   * Calculates the price sensitivity of the Ibor future product.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * 
   * @param future  the future to price
   * @param provider  the rates provider
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivity(IborFuture future, RatesProvider provider) {
    IborRateSensitivity sensi = IborRateSensitivity.of(future.getIndex(), future.getFixingDate(), -1.0d);
    // The sensitivity should be to no currency or currency XXX. To avoid useless conversion, the dimension-less 
    // price sensitivity is reported in the future currency.
    return PointSensitivities.of(sensi);
  }

}
