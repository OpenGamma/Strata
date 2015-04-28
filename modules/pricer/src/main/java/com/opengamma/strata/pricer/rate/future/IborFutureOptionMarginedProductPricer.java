/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import com.opengamma.strata.finance.common.FutureOptionPremiumStyle;
import com.opengamma.strata.finance.rate.future.IborFutureOption;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;

/**
 * Pricer for Ibor future option products with daily margin.
 * <p>
 * This function provides the ability to price an {@link IborFutureOption}.
 * The option must be based on {@linkplain FutureOptionPremiumStyle#DAILY_MARGIN daily margin}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public abstract class IborFutureOptionMarginedProductPricer {

  /**
   * Creates an instance.
   */
  protected IborFutureOptionMarginedProductPricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the Ibor future option product.
   * <p>
   * The price of the option is the price on the valuation date.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param futureProvider  the provider of future/option pricing data
   * @return the price of the product, in decimal form
   */
  abstract double price(
      IborFutureOption option,
      RatesProvider ratesProvider,
      IborFutureProvider futureProvider);

  //-------------------------------------------------------------------------
  /**
   * Calculates the price sensitivity of the Ibor future option product.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param futureProvider  the provider of future/option pricing data
   * @return the price curve sensitivity of the product
   */
  abstract PointSensitivities priceSensitivity(
      IborFutureOption option,
      RatesProvider ratesProvider,
      IborFutureProvider futureProvider);

  //-------------------------------------------------------------------------
  /**
   * Calculates the number related to Ibor futures product on which the daily margin is computed.
   * <p>
   * For two consecutive closing prices C1 and C2, the daily margin is computed as 
   *    {@code marginIndex(future, C2) - marginIndex(future, C1)}.
   *    
   * @param option  the option product to price
   * @param price  the price of the product, in decimal form
   * @return the index
   */
  public double marginIndex(IborFutureOption option, double price) {
    double notional = option.getUnderlying().getProduct().getNotional();
    double accrualFactor = option.getUnderlying().getProduct().getAccrualFactor();
    return price * notional * accrualFactor;
  }

  /**
   * Calculates the margin index sensitivity of the Ibor future product.
   * <p>
   * For two consecutive closing prices C1 and C2, the daily margin is computed as 
   *    {@code marginIndex(future, C2) - marginIndex(future, C1)}.
   * The margin index sensitivity if the sensitivity of the margin index to the underlying curves.
   * 
   * @param option  the option product to price
   * @param priceSensitivity  the price sensitivity of the product
   * @return the index sensitivity
   */
  public PointSensitivities marginIndexSensitivity(IborFutureOption option, PointSensitivities priceSensitivity) {
    double notional = option.getUnderlying().getProduct().getNotional();
    double accrualFactor = option.getUnderlying().getProduct().getAccrualFactor();
    return priceSensitivity.multipliedBy(notional * accrualFactor);
  }

}
