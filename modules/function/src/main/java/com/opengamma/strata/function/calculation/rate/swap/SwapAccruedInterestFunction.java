/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.swap;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates the accrued interest for a {@code SwapTrade} for each of a set of scenarios.
 */
public class SwapAccruedInterestFunction
    extends AbstractSwapFunction<MultiCurrencyAmount> {

  @Override
  protected MultiCurrencyAmount execute(ExpandedSwap product, RatesProvider provider) {
    return pricer().accruedInterest(product, provider);
  }

}
