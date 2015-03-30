/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate.swap;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.rate.swap.ExpandedSwap;
import com.opengamma.platform.finance.rate.swap.SwapProduct;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.rate.swap.SwapProductPricerFn;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Pricer implementation for swap products.
 * <p>
 * The swap product is priced by by expanding it.
 */
public class ExpandingSwapProductPricerFn
    implements SwapProductPricerFn<SwapProduct> {

  /**
   * Default implementation.
   */
  public static final ExpandingSwapProductPricerFn DEFAULT = new ExpandingSwapProductPricerFn(
      DefaultExpandedSwapPricerFn.DEFAULT);

  /**
   * Pricer for {@link ExpandedSwap}.
   */
  private final SwapProductPricerFn<ExpandedSwap> expandedSwapPricerFn;

  /**
   * Creates an instance.
   * 
   * @param expandedSwapPricerFn  the pricer for {@link ExpandedSwap}
   */
  public ExpandingSwapProductPricerFn(
      SwapProductPricerFn<ExpandedSwap> expandedSwapPricerFn) {
    this.expandedSwapPricerFn = ArgChecker.notNull(expandedSwapPricerFn, "expandedSwapPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyAmount presentValue(PricingEnvironment env, SwapProduct product, Currency currency) {
    return expandedSwapPricerFn.presentValue(env, product.expand(), currency);
  }

  @Override
  public MultiCurrencyAmount presentValue(PricingEnvironment env, SwapProduct product) {
    return expandedSwapPricerFn.presentValue(env, product.expand());
  }

  @Override
  public MultiCurrencyAmount futureValue(PricingEnvironment env, SwapProduct product) {
    return expandedSwapPricerFn.futureValue(env, product.expand());
  }

}
