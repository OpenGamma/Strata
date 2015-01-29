/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.swap.ExpandedSwap;
import com.opengamma.platform.finance.swap.SwapProduct;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.SwapProductPricerFn;

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
  public MultiCurrencyAmount presentValue(PricingEnvironment env, SwapProduct swapProduct) {
    return expandedSwapPricerFn.presentValue(env, swapProduct.expand());
  }

  @Override
  public MultiCurrencyAmount futureValue(PricingEnvironment env, SwapProduct swapProduct) {
    return expandedSwapPricerFn.futureValue(env, swapProduct.expand());
  }

}
