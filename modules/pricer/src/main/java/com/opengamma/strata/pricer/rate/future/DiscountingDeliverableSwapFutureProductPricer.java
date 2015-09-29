/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.future.DeliverableSwapFuture;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapProductPricer;

/**
 * Pricer for for deliverable swap futures.
 * <p>
 * This function provides the ability to price a {@link DeliverableSwapFuture}.
 */
public final class DiscountingDeliverableSwapFutureProductPricer extends AbstractDeliverableSwapFutureProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingDeliverableSwapFutureProductPricer DEFAULT =
      new DiscountingDeliverableSwapFutureProductPricer(DiscountingSwapProductPricer.DEFAULT);

  /**
   * Pricer for {@link Swap}.
   */
  private final DiscountingSwapProductPricer swapPricer;

  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link Swap}.
   */
  public DiscountingDeliverableSwapFutureProductPricer(DiscountingSwapProductPricer swapPricer) {
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
   * Calculates the price of the deliverable swap futures product.
   * <p>
   * The price of the product is the price on the valuation date.
   * 
   * @param futures  the futures
   * @param ratesProvider  the rates provider
   * @return the price of the product, in decimal form
   */
  public double price(DeliverableSwapFuture futures, RatesProvider ratesProvider) {
    Swap swap = futures.getUnderlyingSwap();
    Currency currency = futures.getCurrency();
    CurrencyAmount pvSwap = swapPricer.presentValue(swap, currency, ratesProvider);
    double df = ratesProvider.discountFactor(currency, futures.getDeliveryDate());
    return 1d + pvSwap.getAmount() / df;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price sensitivity of the deliverable swap futures product.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * 
   * @param futures  the futures
   * @param ratesProvider  the rates provider
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivity(DeliverableSwapFuture futures, RatesProvider ratesProvider) {
    Swap swap = futures.getUnderlyingSwap();
    Currency currency = futures.getCurrency();
    double pvSwap = swapPricer.presentValue(swap, currency, ratesProvider).getAmount();
    double dfInv = 1d / ratesProvider.discountFactor(currency, futures.getDeliveryDate());
    PointSensitivityBuilder sensiSwapPv = swapPricer.presentValueSensitivity(swap, ratesProvider).multipliedBy(dfInv);
    PointSensitivityBuilder sensiDf = ratesProvider.discountFactors(currency)
        .zeroRatePointSensitivity(futures.getDeliveryDate()).multipliedBy(-pvSwap * dfInv * dfInv);
    return sensiSwapPv.combinedWith(sensiDf).build();
  }

}
