/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.bond.BondFutureOption;
import com.opengamma.strata.product.common.FutureOptionPremiumStyle;

/**
 * Pricer for bond future option products with daily margin.
 * <p>
 * This function provides the ability to price an {@link BondFutureOption}.
 * The option must be based on {@linkplain FutureOptionPremiumStyle#DAILY_MARGIN daily margin}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public abstract class BondFutureOptionMarginedProductPricer {

  /**
   * Creates an instance.
   */
  protected BondFutureOptionMarginedProductPricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the bond future option product.
   * <p>
   * The price of the option is the price on the valuation date.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param futureProvider  the provider of future/option pricing data
   * @return the price of the product, in decimal form
   */
  abstract double price(
      BondFutureOption option,
      LegalEntityDiscountingProvider ratesProvider,
      BondFutureProvider futureProvider);

  //-------------------------------------------------------------------------
  /**
   * Calculates the price sensitivity of the bond future option product.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * 
   * @param option  the option product to price
   * @param ratesProvider  the rates provider
   * @param futureProvider  the provider of future/option pricing data
   * @return the price curve sensitivity of the product
   */
  abstract PointSensitivities priceSensitivity(
      BondFutureOption option,
      LegalEntityDiscountingProvider ratesProvider,
      BondFutureProvider futureProvider);

  //-------------------------------------------------------------------------
  /**
   * Calculates the number related to bond futures product on which the daily margin is computed.
   * <p>
   * For two consecutive closing prices C1 and C2, the daily margin is computed as 
   *    {@code marginIndex(future, C2) - marginIndex(future, C1)}.
   *    
   * @param option  the option product to price
   * @param price  the price of the product, in decimal form
   * @return the index
   */
  public double marginIndex(BondFutureOption option, double price) {
    double notional = option.getUnderlying().getNotional();
    return price * notional;
  }

  /**
   * Calculates the margin index sensitivity of the bond future product.
   * <p>
   * For two consecutive closing prices C1 and C2, the daily margin is computed as 
   *    {@code marginIndex(future, C2) - marginIndex(future, C1)}.
   * The margin index sensitivity if the sensitivity of the margin index to the underlying curves.
   * 
   * @param option  the option product to price
   * @param priceSensitivity  the price sensitivity of the product
   * @return the index sensitivity
   */
  public PointSensitivities marginIndexSensitivity(BondFutureOption option, PointSensitivities priceSensitivity) {
    double notional = option.getUnderlying().getNotional();
    return priceSensitivity.multipliedBy(notional);
  }

}
