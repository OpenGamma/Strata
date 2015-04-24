/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.fra;

import com.opengamma.strata.finance.rate.fra.ExpandedFra;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.rate.fra.DiscountingFraProductPricerBeta;

/**
 * Calculates the par spread of a {@code FraTrade} for each of a set of scenarios.
 */
public class FraParSpreadFunction
    extends AbstractFraFunction<Double> {

  // Fra pricer
  private static final DiscountingFraProductPricerBeta PRICER = DiscountingFraProductPricerBeta.DEFAULT;

  @Override
  protected Double execute(ExpandedFra product, RatesProvider provider) {
    return PRICER.parSpread(product, provider);
  }

}
