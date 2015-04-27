/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import com.opengamma.strata.finance.rate.future.IborFuture;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

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
   * @param env  the pricing environment
   * @param future  the future to price
   * @return the price of the product, in decimal form
   */
  public double price(PricingEnvironment env, IborFuture future) {
    double forward = env.iborIndexRate(future.getIndex(), future.getFixingDate());
    return 1.0 - forward;
  }

  /**
   * Calculates the price sensitivity of the Ibor future product.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * 
   * @param env  the pricing environment
   * @param future  the future to price
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivity(PricingEnvironment env, IborFuture future) {
    IborRateSensitivity sensi = IborRateSensitivity.of(future.getIndex(), future.getFixingDate(), -1.0d);
    // The sensitivity should be to no currency or currency XXX. To avoid useless conversion, the dimension-less 
    // price sensitivity is reported in the future currency.
    return PointSensitivities.of(sensi);
  }

}
