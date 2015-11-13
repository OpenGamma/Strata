/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.fra;

import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.fra.ExpandedFra;

/**
 * Calculates the par spread of a {@code FraTrade} for each of a set of scenarios.
 */
public class FraParSpreadFunction
    extends AbstractFraFunction<Double> {

  @Override
  protected Double execute(ExpandedFra product, RatesProvider provider) {
    return pricer().parSpread(product, provider);
  }

}
