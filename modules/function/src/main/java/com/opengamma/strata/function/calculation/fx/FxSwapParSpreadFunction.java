/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.finance.fx.ExpandedFxSwap;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates the par spread of an {@code FxSwapTrade} for each of a set of scenarios.
 */
public class FxSwapParSpreadFunction
    extends AbstractFxSwapFunction<Double> {

  @Override
  protected Double execute(ExpandedFxSwap product, RatesProvider provider) {
    return pricer().parSpread(product, provider);
  }

}
