/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.rate.fra;

import com.opengamma.platform.finance.rate.fra.FraProduct;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.basics.currency.CurrencyAmount;

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
  public abstract CurrencyAmount presentValue(PricingEnvironment env, T product);

  /**
   * Calculates the present value sensitivity of the FRA product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the present value curve sensitivity of the product
   */
  public abstract PointSensitivities presentValueSensitivity(PricingEnvironment env, T product);

  /**
   * Calculates the future value of the FRA product.
   * <p>
   * The future value of the product is the value on the valuation date without discounting.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the future value of the product
   */
  public abstract CurrencyAmount futureValue(PricingEnvironment env, T product);

  /**
   * Calculates the future value sensitivity of the FRA product.
   * <p>
   * The future value sensitivity of the product is the sensitivity of the future value to
   * the underlying curves.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the future value curve sensitivity of the product
   */
  public abstract PointSensitivities futureValueSensitivity(PricingEnvironment env, T product);

}
