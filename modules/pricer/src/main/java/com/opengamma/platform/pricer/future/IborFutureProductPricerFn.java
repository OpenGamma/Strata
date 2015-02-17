/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.future;

import com.opengamma.platform.finance.future.IborFutureProduct;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Pricer for Ibor future products.
 * <p>
 * This function provides the ability to price a {@link IborFutureProduct}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of product
 */
public interface IborFutureProductPricerFn<T extends IborFutureProduct> {

  /**
   * Calculates the present value of the Ibor future product.
   * <p>
   * The price of the product is the price on the valuation date.
   * 
   * @param env  the pricing environment
   * @param iborFutureProduct  the product to price
   * @return the price of the product
   */
  public abstract double price(PricingEnvironment env, T iborFutureProduct);

}
