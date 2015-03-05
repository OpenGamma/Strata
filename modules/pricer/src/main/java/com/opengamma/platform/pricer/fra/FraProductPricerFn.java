/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.fra;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.fra.FraProduct;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Pricer for FRA products.
 * <p>
 * This function provides the ability to price a {@link FraProduct}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of product
 */
public interface FraProductPricerFn<T extends FraProduct> {

  /**
   * Calculates the present value of the FRA product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * This is typically implemented as the discounted future value.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the present value of the product
   */
  public abstract MultiCurrencyAmount presentValue(PricingEnvironment env, T product);

  /**
   * Calculates the future value of the FRA product.
   * <p>
   * The future value of the product is the value on the valuation date without discounting.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the future value of the product
   */
  public abstract MultiCurrencyAmount futureValue(PricingEnvironment env, T product);

}
