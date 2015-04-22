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
import com.opengamma.strata.pricer.PricingEnvironment;
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
   * @param env  the pricing environment
   * @param product  the product to price
   * @param currency  the currency to convert to
   * @return the present value of the swap product in the specified currency
   */
  public CurrencyAmount presentValue(PricingEnvironment env, SwapProduct product, Currency currency) {
    double totalPv = 0;
    for (ExpandedSwapLeg leg : product.expand().getLegs()) {
      double pv = legPricer.presentValueInternal(env, leg);
      totalPv += (pv * env.fxRate(leg.getCurrency(), currency));
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
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the present value of the swap product
   */
  public MultiCurrencyAmount presentValue(PricingEnvironment env, SwapProduct product) {
    return swapValue(env, product.expand(), legPricer::presentValueInternal);
  }

  /**
   * Calculates the future value of the swap product.
   * <p>
   * The future value of the product is the value on the valuation date without present value discounting.
   * The result is expressed using the payment currency of each leg.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the future value of the swap product
   */
  public MultiCurrencyAmount futureValue(PricingEnvironment env, SwapProduct product) {
    return swapValue(env, product.expand(), legPricer::futureValueInternal);
  }

  //-------------------------------------------------------------------------
  // calculate present or future value for the swap
  private static MultiCurrencyAmount swapValue(
      PricingEnvironment env,
      ExpandedSwap swap,
      ToDoubleBiFunction<PricingEnvironment, SwapLeg> legFn) {

    if (swap.isCrossCurrency()) {
      return swap.getLegs().stream()
          .map(leg -> CurrencyAmount.of(leg.getCurrency(), legFn.applyAsDouble(env, leg)))
          .collect(MultiCurrencyAmount.collector());
    } else {
      Currency currency = swap.getLegs().iterator().next().getCurrency();
      double total = 0d;
      for (ExpandedSwapLeg leg : swap.getLegs()) {
        total += legFn.applyAsDouble(env, leg);
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
   * @param env  the pricing environment
   * @param product  the swap product for which the par rate should be computed
   * @return the par rate
   */
  public double parRate(PricingEnvironment env, SwapProduct product) {
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
        double pvLocal = legPricer.presentValueInternal(env, leg);
        otherLegsConvertedPv += (pvLocal * env.fxRate(leg.getCurrency(), ccyFixedLeg));
      }
    }
    double fixedLegEventsPv = legPricer.presentValueEventsInternal(env, fixedLeg);
    // PVBP
    double pvbpFixedLeg = legPricer.pvbp(env, fixedLeg);
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
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the present value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder presentValueSensitivity(PricingEnvironment env, SwapProduct product) {
    return swapValueSensitivity(
        env,
        product.expand(),
        legPricer::presentValueSensitivity);
  }

  /**
   * Calculates the future value sensitivity of the swap product.
   * <p>
   * The future value sensitivity of the product is the sensitivity of the future value to
   * the underlying curves.
   * 
   * @param env  the pricing environment
   * @param product  the product to price
   * @return the future value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder futureValueSensitivity(PricingEnvironment env, SwapProduct product) {
    return swapValueSensitivity(
        env,
        product.expand(),
        legPricer::futureValueSensitivity);
  }

  // calculate present or future value sensitivity for the swap
  private static PointSensitivityBuilder swapValueSensitivity(
      PricingEnvironment env,
      ExpandedSwap swap,
      BiFunction<PricingEnvironment, SwapLeg, PointSensitivityBuilder> legFn) {

    PointSensitivityBuilder builder = PointSensitivityBuilder.none();
    for (ExpandedSwapLeg leg : swap.getLegs()) {
      builder = builder.combinedWith(legFn.apply(env, leg));
    }
    return builder;
  }

}
