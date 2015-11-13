/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ExpandedFxNdf;

/**
 * Calculates the future FX rate of an {@code FxNdfTrade} for each of a set of scenarios.
 */
public class FxNdfForwardFxRateFunction
    extends AbstractFxNdfFunction<FxRate> {

  @Override
  protected FxRate execute(ExpandedFxNdf product, RatesProvider provider) {
    return pricer().forwardFxRate(product, provider);
  }

}
