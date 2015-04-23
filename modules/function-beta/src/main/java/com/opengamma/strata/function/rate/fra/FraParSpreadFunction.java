/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.fra;

import com.opengamma.strata.finance.rate.fra.ExpandedFra;
import com.opengamma.strata.function.MarketDataRatesProvider;
import com.opengamma.strata.pricer.rate.fra.DiscountingFraProductPricerBeta;

/**
 * Calculates the par spread of a Forward Rate Agreement for each of a set of scenarios.
 */
public class FraParSpreadFunction
    extends AbstractFraFunction<Double> {

  // Fra pricer
  private static final DiscountingFraProductPricerBeta PRICER = DiscountingFraProductPricerBeta.DEFAULT;

  @Override
  protected Double execute(ExpandedFra product, MarketDataRatesProvider provider) {
    return PRICER.parSpread(product, provider);
  }

}
