/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ExpandedFxSingle;

/**
 * Calculates the par spread of an {@code FxSingleTrade} for each of a set of scenarios.
 */
public class FxSingleParSpreadFunction
    extends AbstractFxSingleFunction<Double> {

  @Override
  protected Double execute(ExpandedFxSingle product, RatesProvider provider) {
    return pricer().parSpread(product, provider);
  }

}
