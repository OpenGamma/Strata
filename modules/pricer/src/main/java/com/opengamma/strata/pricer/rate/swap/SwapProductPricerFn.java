/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.rate.swap.SwapProduct;
import com.opengamma.strata.pricer.PricingEnvironment;

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
   * Calculates the present value of the swap product in a single currency.
   * <p>
   * The present value of the product is the value on the valuation date.
   * This is typically implemented as the discounted future value.
   * The result is converted to the specified currency.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @param currency  the currency to convert to
   * @return the present value of the product in the specified currency
   */
  public default CurrencyAmount presentValue(PricingEnvironment env, T product, Currency currency) {
    return env.fxConvert(presentValue(env, product), currency);
  }

  /**
   * Calculates the present value of the swap product.
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
   * Calculates the future value of the swap product.
   * <p>
   * The future value of the product is the value on the valuation date without discounting.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the future value of the product
   */
  public abstract MultiCurrencyAmount futureValue(PricingEnvironment env, T product);

}
