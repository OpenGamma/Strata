/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.index.ResolvedOvernightFuture;
import com.opengamma.strata.product.rate.RateComputation;

/**
 * Pricer for for Overnight rate future products.
 * <p>
 * This function provides the ability to price a {@link ResolvedOvernightFuture}.
 * 
 * <h4>Price</h4>
 * The price of an Overnight rate future is based on the interest rate of the underlying index.
 * It is defined as {@code (100 - percentRate)}.
 * <p>
 * Strata uses <i>decimal prices</i> for Overnight rate futures in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
 */
public class DiscountingOvernightFutureProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingOvernightFutureProductPricer DEFAULT =
      new DiscountingOvernightFutureProductPricer(RateComputationFn.standard());

  /**
   * Rate computation.
   */
  private final RateComputationFn<RateComputation> rateComputationFn;

  /**
   * Creates an instance.
   * 
   * @param rateComputationFn  the rate computation function
   */
  public DiscountingOvernightFutureProductPricer(RateComputationFn<RateComputation> rateComputationFn) {
    this.rateComputationFn = ArgChecker.notNull(rateComputationFn, "rateComputationFn");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the number related to Overnight rate futures product on which the daily margin is computed.
   * <p>
   * For two consecutive settlement prices C1 and C2, the daily margin is computed as 
   *    {@code (marginIndex(future, C2) - marginIndex(future, C1))}.
   * 
   * @param future  the future
   * @param price  the price of the product, in decimal form
   * @return the index
   */
  double marginIndex(ResolvedOvernightFuture future, double price) {
    return price * future.getNotional() * future.getAccrualFactor();
  }

  /**
   * Calculates the margin index sensitivity of the Overnight rate future product.
   * <p>
   * The margin index sensitivity is the sensitivity of the margin index to the underlying curves.
   * For two consecutive settlement prices C1 and C2, the daily margin is computed as 
   *    {@code (marginIndex(future, C2) - marginIndex(future, C1))}.
   * 
   * @param future  the future
   * @param priceSensitivity  the price sensitivity of the product
   * @return the index sensitivity
   */
  PointSensitivities marginIndexSensitivity(ResolvedOvernightFuture future, PointSensitivities priceSensitivity) {
    return priceSensitivity.multipliedBy(future.getNotional() * future.getAccrualFactor());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the Overnight rate future product.
   * <p>
   * The price of the product is the price on the valuation date.
   * 
   * @param future  the future
   * @param ratesProvider  the rates provider
   * @return the price of the product, in decimal form
   */
  public double price(ResolvedOvernightFuture future, RatesProvider ratesProvider) {
    double forwardRate = rateComputationFn.rate(
        future.getOvernightRate(),
        future.getOvernightRate().getStartDate(),
        future.getOvernightRate().getEndDate(),
        ratesProvider);
    return 1d - forwardRate;
  }

  /**
   * Calculates the price sensitivity of the Overnight rate future product.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * 
   * @param future  the future
   * @param ratesProvider  the rates provider
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivity(ResolvedOvernightFuture future, RatesProvider ratesProvider) {

    PointSensitivityBuilder forwardRateSensitivity = rateComputationFn.rateSensitivity(
        future.getOvernightRate(),
        future.getOvernightRate().getStartDate(),
        future.getOvernightRate().getEndDate(),
        ratesProvider);
    // The sensitivity should be to no currency or currency XXX. To avoid useless conversion, the dimension-less 
    // price sensitivity is reported in the future currency.
    return forwardRateSensitivity.build().multipliedBy(-1d);
  }

}
