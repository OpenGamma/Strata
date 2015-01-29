/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.swap;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.swap.SwapProduct;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Pricer for swap products.
 * <p>
 * This function provides the ability to price a {@link SwapProduct}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of product
 */
public interface SwapProductPricerFn<T extends SwapProduct> {

  /**
   * Calculates the present value of the swap product.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * This is typically implemented as the discounted future value.
   * 
   * @param env  the pricing environment
   * @param swapProduct  the swap product to price
   * @return the present value of the swap product
   */
  public abstract MultiCurrencyAmount presentValue(PricingEnvironment env, T swapProduct);

  /**
   * Calculates the future value of the swap product.
   * <p>
   * The future value of the trade is the value on the valuation date without discounting.
   * 
   * @param env  the pricing environment
   * @param swapProduct  the swap product to price
   * @return the future value of the swap product
   */
  public abstract MultiCurrencyAmount futureValue(PricingEnvironment env, T swapProduct);

}
