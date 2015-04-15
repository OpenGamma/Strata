/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.swap.SwapProduct;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.rate.swap.SwapProductPricerFn;
import com.opengamma.strata.pricer.rate.swap.SwapTradePricerFn;

/**
 * Pricer implementation for swap trades.
 * <p>
 * The swap trade is priced by pricing the product.
 */
public class DefaultSwapTradePricerFn
    implements SwapTradePricerFn {

  /**
   * Default implementation.
   */
  public static final DefaultSwapTradePricerFn DEFAULT = new DefaultSwapTradePricerFn(
      DefaultSwapProductPricerFn.DEFAULT);

  /**
   * Pricer for {@link SwapProduct}.
   */
  private final SwapProductPricerFn<SwapProduct> swapProductPricerFn;

  /**
   * Creates an instance.
   * 
   * @param swapProductPricerFn  the pricer for {@link SwapProduct}
   */
  public DefaultSwapTradePricerFn(
      SwapProductPricerFn<SwapProduct> swapProductPricerFn) {
    this.swapProductPricerFn = ArgChecker.notNull(swapProductPricerFn, "swapProductPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public CurrencyAmount presentValue(PricingEnvironment env, SwapTrade trade, Currency currency) {
    return swapProductPricerFn.presentValue(env, trade.getProduct(), currency);
  }

  @Override
  public MultiCurrencyAmount presentValue(PricingEnvironment env, SwapTrade trade) {
    return swapProductPricerFn.presentValue(env, trade.getProduct());
  }

  @Override
  public MultiCurrencyAmount futureValue(PricingEnvironment env, SwapTrade trade) {
    return swapProductPricerFn.futureValue(env, trade.getProduct());
  }

}
