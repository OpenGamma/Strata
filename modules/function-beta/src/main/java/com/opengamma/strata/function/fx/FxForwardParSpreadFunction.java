/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.fx;

import com.opengamma.strata.finance.fx.FxTransaction;
import com.opengamma.strata.pricer.RatesProvider;

/**
 * Calculates the par spread of an {@code FxForwardTrade} for each of a set of scenarios.
 */
public class FxForwardParSpreadFunction
    extends AbstractFxForwardFunction<Double> {

  @Override
  protected Double execute(FxTransaction product, RatesProvider provider) {
    return pricer().parSpread(product, provider);
  }

}
