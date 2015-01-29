/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import java.util.function.ToDoubleBiFunction;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.swap.ExpandedSwap;
import com.opengamma.platform.finance.swap.ExpandedSwapLeg;
import com.opengamma.platform.finance.swap.SwapLeg;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.SwapLegPricerFn;
import com.opengamma.platform.pricer.swap.SwapProductPricerFn;

/**
 * Pricer implementation for swaps.
 * <p>
 * The swap is priced by examining the swap legs.
 */
public class DefaultExpandedSwapPricerFn
    implements SwapProductPricerFn<ExpandedSwap> {

  /**
   * Default implementation.
   */
  public static final DefaultExpandedSwapPricerFn DEFAULT = new DefaultExpandedSwapPricerFn(
      DefaultExpandedSwapLegPricerFn.DEFAULT);

  /**
   * Pricer for {@link SwapLeg}.
   */
  private final SwapLegPricerFn<ExpandedSwapLeg> swapLegPricerFn;

  /**
   * Creates an instance.
   * 
   * @param swapLegPricerFn  the pricer for {@link ExpandedSwapLeg}
   */
  public DefaultExpandedSwapPricerFn(
      SwapLegPricerFn<ExpandedSwapLeg> swapLegPricerFn) {
    this.swapLegPricerFn = ArgChecker.notNull(swapLegPricerFn, "swapLegPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount presentValue(PricingEnvironment env, ExpandedSwap swap) {
    return value(env, swap, swapLegPricerFn::presentValue);
  }

  @Override
  public MultiCurrencyAmount futureValue(PricingEnvironment env, ExpandedSwap swap) {
    return value(env, swap, swapLegPricerFn::futureValue);
  }

  // calculate present or future value
  private static MultiCurrencyAmount value(
      PricingEnvironment env,
      ExpandedSwap swap,
      ToDoubleBiFunction<PricingEnvironment, ExpandedSwapLeg> valueFn) {
    if (swap.isCrossCurrency()) {
      return swap.getLegs().stream()
          .map(leg -> CurrencyAmount.of(leg.getCurrency(), valueFn.applyAsDouble(env, leg)))
          .collect(MultiCurrencyAmount.collector());
    } else {
      Currency currency = swap.getLegs().iterator().next().getCurrency();
      double pv = swap.getLegs().stream()
          .mapToDouble(leg -> valueFn.applyAsDouble(env, leg))
          .sum();
      return MultiCurrencyAmount.of(currency, pv);
    }
  }

}
