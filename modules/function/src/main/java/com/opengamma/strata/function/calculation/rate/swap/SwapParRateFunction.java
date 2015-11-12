/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.swap;

import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.swap.ExpandedSwap;

/**
 * Calculates the par rate of a {@code SwapTrade} for each of a set of scenarios.
 */
public class SwapParRateFunction
    extends AbstractSwapFunction<Double> {

  @Override
  protected Double execute(ExpandedSwap product, RatesProvider provider) {
    return pricer().parRate(product, provider);
  }

}
