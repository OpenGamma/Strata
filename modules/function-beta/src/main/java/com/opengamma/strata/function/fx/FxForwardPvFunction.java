/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.fx;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.fx.FxTransaction;
import com.opengamma.strata.function.MarketDataRatesProvider;

/**
 * Calculates the present value of an {@code FxForward} for each of a set of scenarios.
 */
public class FxForwardPvFunction
    extends AbstractFxForwardFunction<MultiCurrencyAmount> {

  @Override
  protected MultiCurrencyAmount execute(FxTransaction product, MarketDataRatesProvider provider) {
    return pricer().presentValue(product, provider);
  }

}
