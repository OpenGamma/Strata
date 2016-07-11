/**
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
 * <p>
 * The price of a DSF is based on the present value (NPV) of the underlying swap on the delivery date.
 * For example, a price of 100.1822 represents a present value of $100,182.20, if the notional is $100,000.
 */
public final class DiscountingDsfProductPricer extends AbstractDsfProductPricer {

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
   * Calculates the price of the deliverable swap futures product.
   * <p>
   * The price of the product is the price on the valuation date.
   * 
   * @param future  the future
   * @param ratesProvider  the rates provider
   * @return the price of the product
   */
  public double price(ResolvedDsf future, RatesProvider ratesProvider) {
    ResolvedSwap swap = future.getUnderlyingSwap();
    Currency currency = future.getCurrency();
    CurrencyAmount pvSwap = swapPricer.presentValue(swap, currency, ratesProvider);
    double df = ratesProvider.discountFactor(currency, future.getDeliveryDate());
    return (1d + pvSwap.getAmount() / df) * 100d;
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
    return sensiSwapPv.combinedWith(sensiDf).build().multipliedBy(100d);
  }

}
