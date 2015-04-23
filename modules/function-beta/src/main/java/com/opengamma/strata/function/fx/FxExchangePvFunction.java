/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.fx;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.fx.FxExchange;
import com.opengamma.strata.function.MarketDataRatesProvider;

/**
 * Calculates the present value of an {@code FxExchange} for each of a set of scenarios.
 */
public class FxExchangePvFunction
    extends AbstractFxExchangeFunction<MultiCurrencyAmount> {

  @Override
  protected MultiCurrencyAmount execute(FxExchange product, MarketDataRatesProvider provider) {
    return pricer().presentValue(product, provider);
  }

}
