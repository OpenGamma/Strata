/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.future;

import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.platform.finance.future.IborFutureOptionProduct;
import com.opengamma.platform.finance.future.IborFutureOptionSecurityTrade;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Pricer for Ibor future option products.
 * <p>
 * This function provides the ability to price a {@link IborFutureOptionProduct}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of product
 */
public interface IborFutureOptionProductPricerFn<T extends IborFutureOptionProduct> {

  //TODO decide implementation of model parameters including volatility surface
  //TODO curve sensitivity
  //  public abstract MulticurveSensitivity priceCurveSensitivity(PricingEnvironment env, T iborFutureOptionProduct);

  /**
   * Calculates the price of the Ibor future option product.
   * <p>
   * The price of the product is the price on the valuation date.
   * 
   * @param env  the pricing environment
   * @param iborFutureOption  the product to price
   * @param surface  the volatility surface
   * @return the price of the product
   */
  public abstract double price(PricingEnvironment env, T iborFutureOption, Object surface);

  /**
   * Calculates the present value of the Ibor future option trade.
   * 
   * @param env  the pricing environment
   * @param iborFutureOption  the product to price
   * @param trade  the trade 
   * @param lastClosingPrice  the last closing price
   * @param surface  the volatility surface
   * @return the present value
   */
  public abstract CurrencyAmount presentValue(
      PricingEnvironment env,
      T iborFutureOption,
      IborFutureOptionSecurityTrade trade,
      double lastClosingPrice,
      Object surface);

}
