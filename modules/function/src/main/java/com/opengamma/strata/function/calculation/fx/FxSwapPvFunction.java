/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ExpandedFxSwap;

/**
 * Calculates the present value of an {@code FxSwapTrade} for each of a set of scenarios.
 */
public class FxSwapPvFunction extends MultiCurrencyAmountFxSwapFunction {

  @Override
  protected MultiCurrencyAmount execute(ExpandedFxSwap product, RatesProvider provider) {
    return pricer().presentValue(product, provider);
  }

}
