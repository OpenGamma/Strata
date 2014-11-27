/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import java.time.LocalDate;

import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.swap.Swap;
import com.opengamma.platform.finance.swap.SwapLeg;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.SwapLegPricerFn;
import com.opengamma.platform.pricer.swap.SwapPricerFn;

/**
 * Pricer for swaps.
 */
public class DefaultSwapPricerFn implements SwapPricerFn {

  /**
   * Default implementation.
   */
  public static final DefaultSwapPricerFn DEFAULT = new DefaultSwapPricerFn(
      DefaultSwapLegPricerFn.DEFAULT);

  /**
   * Payment period pricer.
   */
  private final SwapLegPricerFn<SwapLeg> swapLegPricerFn;

  /**
   * Creates an instance.
   * 
   * @param swapLegPricerFn  the pricer for {@link SwapLeg}
   */
  public DefaultSwapPricerFn(
      SwapLegPricerFn<SwapLeg> swapLegPricerFn) {
    this.swapLegPricerFn = ArgChecker.notNull(swapLegPricerFn, "swapLegPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public MultiCurrencyAmount presentValue(PricingEnvironment env, LocalDate valuationDate, Swap swap) {
    return swap.getLegs().stream()
      .map(leg -> CurrencyAmount.of(leg.getCurrency(), swapLegPricerFn.presentValue(env, valuationDate, leg)))
      .reduce(MultiCurrencyAmount.of(), MultiCurrencyAmount::plus, MultiCurrencyAmount::plus);
  }

  @Override
  public MultiCurrencyAmount futureValue(PricingEnvironment env, LocalDate valuationDate, Swap swap) {
    return swap.getLegs().stream()
        .map(leg -> CurrencyAmount.of(leg.getCurrency(), swapLegPricerFn.futureValue(env, valuationDate, leg)))
        .reduce(MultiCurrencyAmount.of(), MultiCurrencyAmount::plus, MultiCurrencyAmount::plus);
  }

}
