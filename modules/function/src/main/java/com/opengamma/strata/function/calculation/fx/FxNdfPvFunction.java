/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ExpandedFxNdf;

/**
 * Calculates the present value of an {@code FxNdfTrade} for each of a set of scenarios.
 */
public class FxNdfPvFunction
    extends AbstractFxNdfFunction<CurrencyAmount> {

  @Override
  protected CurrencyAmount execute(ExpandedFxNdf product, RatesProvider provider) {
    return pricer().presentValue(product, provider);
  }

}
