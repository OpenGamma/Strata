/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.dsf;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.dsf.ResolvedDsf;
import com.opengamma.strata.product.swap.ResolvedSwap;

/**
 * Pricer for for Deliverable Swap Futures (DSFs).
 * <p>
 * This function provides the ability to price a {@link ResolvedDsf}.
 * 
 * <h4>Price</h4>
 * The price of a DSF is based on the present value (NPV) of the underlying swap on the delivery date.
 * For example, a price of 100.182 represents a present value of $100,182.00, if the notional is $100,000.
 * This price can also be viewed as a percentage present value - {@code (100 + percentPv)}, or 0.182% in this example.
 * <p>
 * Strata uses <i>decimal prices</i> for DSFs in the trade model, pricers and market data.
 * The decimal price is based on the decimal multiplier equivalent to the implied percentage.
 * Thus the market price of 100.182 is represented in Strata by 1.00182.
 */
public final class DiscountingDsfProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingDsfProductPricer DEFAULT =
      new DiscountingDsfProductPricer(DiscountingSwapProductPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedSwap}.
   */
  private final DiscountingSwapProductPricer swapPricer;

  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link ResolvedSwap}.
   */
  public DiscountingDsfProductPricer(DiscountingSwapProductPricer swapPricer) {
    this.swapPricer = ArgChecker.notNull(swapPricer, "swapPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer used to price the underlying swap.
   * 
   * @return the pricer
   */
  DiscountingSwapProductPricer getSwapPricer() {
    return swapPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the number related to deliverable swap futures product on which the daily margin is computed.
   * <p>
   * For two consecutive settlement prices C1 and C2, the daily margin is computed as 
   *    {@code (marginIndex(future, C2) - marginIndex(future, C1))}.
   * 
   * @param future  the future
   * @param price  the price of the product, in decimal form
   * @return the index
   */
  double marginIndex(ResolvedDsf future, double price) {
    return price * future.getNotional();
  }

  /**
   * Calculates the margin index sensitivity of the deliverable swap futures product.
   * <p>
   * The margin index sensitivity is the sensitivity of the margin index to the underlying curves.
   * For two consecutive settlement prices C1 and C2, the daily margin is computed as 
   *    {@code (marginIndex(future, C2) - marginIndex(future, C1))}.
   * 
   * @param future  the future
   * @param priceSensitivity  the price sensitivity of the product
   * @return the index sensitivity
   */
  PointSensitivities marginIndexSensitivity(
      ResolvedDsf future,
      PointSensitivities priceSensitivity) {

    return priceSensitivity.multipliedBy(future.getNotional());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the deliverable swap futures product.
   * <p>
   * The price of the product is the price on the valuation date.
   * 
   * @param future  the future
   * @param ratesProvider  the rates provider
   * @return the price of the product, in decimal form
   */
  public double price(ResolvedDsf future, RatesProvider ratesProvider) {
    ResolvedSwap swap = future.getUnderlyingSwap();
    Currency currency = future.getCurrency();
    CurrencyAmount pvSwap = swapPricer.presentValue(swap, currency, ratesProvider);
    double df = ratesProvider.discountFactor(currency, future.getDeliveryDate());
    return 1d + pvSwap.getAmount() / df;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price sensitivity of the deliverable swap futures product.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * 
   * @param future  the future
   * @param ratesProvider  the rates provider
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivity(ResolvedDsf future, RatesProvider ratesProvider) {
    ResolvedSwap swap = future.getUnderlyingSwap();
    Currency currency = future.getCurrency();
    double pvSwap = swapPricer.presentValue(swap, currency, ratesProvider).getAmount();
    double dfInv = 1d / ratesProvider.discountFactor(currency, future.getDeliveryDate());
    PointSensitivityBuilder sensiSwapPv = swapPricer.presentValueSensitivity(swap, ratesProvider).multipliedBy(dfInv);
    PointSensitivityBuilder sensiDf = ratesProvider.discountFactors(currency)
        .zeroRatePointSensitivity(future.getDeliveryDate()).multipliedBy(-pvSwap * dfInv * dfInv);
    return sensiSwapPv.combinedWith(sensiDf).build();
  }

}
