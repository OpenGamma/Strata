/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.index.IborFutureOption;
import com.opengamma.strata.product.index.ResolvedIborFutureOption;
import com.opengamma.strata.product.option.FutureOptionPremiumStyle;

/**
 * Pricer for Ibor future option products with daily margin.
 * <p>
 * This function provides the ability to price an {@link IborFutureOption}.
 * The option must be based on {@linkplain FutureOptionPremiumStyle#DAILY_MARGIN daily margin}.
 * 
 * <h4>Price</h4>
 * The price of an Ibor future option is based on the price of the underlying future, the volatility
 * and the time to expiry. The price of the at-the-money option tends to zero as expiry approaches.
 * <p>
 * Strata uses <i>decimal prices</i> for Ibor future options in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, an option price of 0.2 is related to a futures price of 99.32 that implies an
 * interest rate of 0.68%. Strata represents the price of the future as 0.9932 and thus
 * represents the price of the option as 0.002.
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
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the option volatilities
   * @return the price of the product, in decimal form
   */
  abstract double price(
      ResolvedIborFutureOption option,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities);

  //-------------------------------------------------------------------------
  /**
   * Calculates the price sensitivity of the Ibor future option product.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the option volatilities
   * @return the price curve sensitivity of the product
   */
  abstract PointSensitivities priceSensitivity(
      ResolvedIborFutureOption option,
      RatesProvider ratesProvider,
      IborFutureOptionVolatilities volatilities);

  //-------------------------------------------------------------------------
  /**
   * Calculates the number related to Ibor futures product on which the daily margin is computed.
   * <p>
   * For two consecutive closing prices C1 and C2, the daily margin is computed as 
   *    {@code marginIndex(future, C2) - marginIndex(future, C1)}.
   *    
   * @param option  the option product
   * @param price  the price of the product, in decimal form
   * @return the index
   */
  public double marginIndex(ResolvedIborFutureOption option, double price) {
    double notional = option.getUnderlyingFuture().getNotional();
    double accrualFactor = option.getUnderlyingFuture().getAccrualFactor();
    return price * notional * accrualFactor;
  }

  /**
   * Calculates the margin index sensitivity of the Ibor future product.
   * <p>
   * For two consecutive closing prices C1 and C2, the daily margin is computed as 
   *    {@code marginIndex(future, C2) - marginIndex(future, C1)}.
   * The margin index sensitivity if the sensitivity of the margin index to the underlying curves.
   * 
   * @param option  the option product
   * @param priceSensitivity  the price sensitivity of the product
   * @return the index sensitivity
   */
  public PointSensitivities marginIndexSensitivity(
      ResolvedIborFutureOption option,
      PointSensitivities priceSensitivity) {

    double notional = option.getUnderlyingFuture().getNotional();
    double accrualFactor = option.getUnderlyingFuture().getAccrualFactor();
    return priceSensitivity.multipliedBy(notional * accrualFactor);
  }

}
