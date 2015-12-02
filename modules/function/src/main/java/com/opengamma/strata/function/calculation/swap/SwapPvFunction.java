/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.swap.ExpandedSwap;

/**
 * Calculates the present value of a {@code SwapTrade} for each of a set of scenarios.
 */
public class SwapPvFunction extends MultiCurrencyAmountSwapFunction {

  @Override
  protected MultiCurrencyAmount execute(ExpandedSwap product, RatesProvider provider) {
    return pricer().presentValue(product, provider);
  }

}
