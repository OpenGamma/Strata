/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swap;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToDoubleBiFunction;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapLegType;
import com.opengamma.strata.finance.rate.swap.SwapProduct;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Pricer for for rate swap products.
 * <p>
 * This function provides the ability to price a {@link SwapProduct}.
 * The product is priced by pricing each leg.
 */
public class DiscountingSwapProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingSwapProductPricer DEFAULT = new DiscountingSwapProductPricer(
      DiscountingSwapLegPricer.DEFAULT);

  /**
   * Pricer for {@link SwapLeg}.
   */
  private final DiscountingSwapLegPricer legPricer;

  /**
   * Creates an instance.
   * 
   * @param legPricer  the pricer for {@link SwapLeg}
   */
  public DiscountingSwapProductPricer(
      DiscountingSwapLegPricer legPricer) {
    this.legPricer = ArgChecker.notNull(legPricer, "legPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swap product, converted to the specified currency.
   * <p>
   * The present value of the product is the value on the valuation date.
   * This is the discounted future value.
   * The result is converted to the specified currency.
   * 
   * @param product  the product to price
   * @param currency  the currency to convert to
   * @param provider  the rates provider
   * @return the present value of the swap product in the specified currency
   */
  public CurrencyAmount presentValue(SwapProduct product, Currency currency, RatesProvider provider) {
    double totalPv = 0;
    for (ExpandedSwapLeg leg : product.expand().getLegs()) {
      double pv = legPricer.presentValueInternal(leg, provider);
      totalPv += (pv * provider.fxRate(leg.getCurrency(), currency));
    }
    return CurrencyAmount.of(currency, totalPv);
  }

  /**
   * Calculates the present value of the swap product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * This is the discounted future value.
   * The result is expressed using the payment currency of each leg.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value of the swap product
   */
  public MultiCurrencyAmount presentValue(SwapProduct product, RatesProvider provider) {
    return swapValue(provider, product.expand(), legPricer::presentValueInternal);
  }

  /**
   * Calculates the future value of the swap product.
   * <p>
   * The future value of the product is the value on the valuation date without present value discounting.
   * The result is expressed using the payment currency of each leg.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the future value of the swap product
   */
  public MultiCurrencyAmount futureValue(SwapProduct product, RatesProvider provider) {
    return swapValue(provider, product.expand(), legPricer::futureValueInternal);
  }

  //-------------------------------------------------------------------------
  // calculate present or future value for the swap
  private static MultiCurrencyAmount swapValue(
      RatesProvider provider,
      ExpandedSwap swap,
      ToDoubleBiFunction<SwapLeg, RatesProvider> legFn) {

    if (swap.isCrossCurrency()) {
      return swap.getLegs().stream()
          .map(leg -> CurrencyAmount.of(leg.getCurrency(), legFn.applyAsDouble(leg, provider)))
          .collect(MultiCurrencyAmount.collector());
    } else {
      Currency currency = swap.getLegs().iterator().next().getCurrency();
      double total = 0d;
      for (ExpandedSwapLeg leg : swap.getLegs()) {
        total += legFn.applyAsDouble(leg, provider);
      }
      return MultiCurrencyAmount.of(currency, total);
    }
  }

  /**
   * Computes the par rate for swaps with a fixed leg. 
   * <p>
   * The par rate is the common rate on all payments of the fixed leg for which the total swap present value is 0.
   * <p>
   * At least one leg should be a fixed leg. The par rate will be computed with respect to the first fixed leg.
   * All the payments in that leg should be fixed payments with a unique accrual period (no compounding) and no FX reset.
   * 
   * @param product  the swap product for which the par rate should be computed
   * @param provider  the rates provider
   * @return the par rate
   */
  public double parRate(SwapProduct product, RatesProvider provider) {
    // find fixed leg
    ExpandedSwap swap = product.expand();
    List<ExpandedSwapLeg> fixedLegs = swap.getLegs(SwapLegType.FIXED);
    if (fixedLegs.isEmpty()) {
      throw new IllegalArgumentException("Swap must contain a fixed leg");
    }
    ExpandedSwapLeg fixedLeg = fixedLegs.get(0);
    Currency ccyFixedLeg = fixedLeg.getCurrency();
    // other payments (not fixed leg coupons) converted in fixed leg currency
    double otherLegsConvertedPv = 0.0;
    for (ExpandedSwapLeg leg : swap.getLegs()) {
      if (leg != fixedLeg) {
        double pvLocal = legPricer.presentValueInternal(leg, provider);
        otherLegsConvertedPv += (pvLocal * provider.fxRate(leg.getCurrency(), ccyFixedLeg));
      }
    }
    double fixedLegEventsPv = legPricer.presentValueEventsInternal(fixedLeg, provider);
    // PVBP
    double pvbpFixedLeg = legPricer.pvbp(fixedLeg, provider);
    // Par rate
    return -(otherLegsConvertedPv + fixedLegEventsPv) / pvbpFixedLeg;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swap product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder presentValueSensitivity(SwapProduct product, RatesProvider provider) {
    return swapValueSensitivity(
        product.expand(),
        provider,
        legPricer::presentValueSensitivity);
  }

  /**
   * Calculates the future value sensitivity of the swap product.
   * <p>
   * The future value sensitivity of the product is the sensitivity of the future value to
   * the underlying curves.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the future value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder futureValueSensitivity(SwapProduct product, RatesProvider provider) {
    return swapValueSensitivity(
        product.expand(),
        provider,
        legPricer::futureValueSensitivity);
  }

  // calculate present or future value sensitivity for the swap
  private static PointSensitivityBuilder swapValueSensitivity(
      ExpandedSwap swap,
      RatesProvider provider,
      BiFunction<SwapLeg, RatesProvider, PointSensitivityBuilder> legFn) {

    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (ExpandedSwapLeg leg : swap.getLegs()) {
      builder = builder.combinedWith(legFn.apply(leg, provider));
    }
    return builder;
  }

}
