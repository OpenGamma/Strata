/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.fx;

import com.opengamma.strata.finance.fx.FxExchange;
import com.opengamma.strata.function.MarketDataRatesProvider;

/**
 * Calculates the par spread of an {@code FxExchange} for each of a set of scenarios.
 */
public class FxExchangeParSpreadFunction
    extends AbstractFxExchangeFunction<Double> {

  @Override
  protected Double execute(FxExchange product, MarketDataRatesProvider provider) {
    return pricer().parSpread(product, provider);
  }

}
