/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.fx;

import com.opengamma.strata.finance.fx.ExpandedFx;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates the future FX rate of an {@code FxForwardTrade} for each of a set of scenarios.
 */
public class FxForwardFutureFxRateFunction
    extends AbstractFxForwardFunction<Double> {

  @Override
  protected Double execute(ExpandedFx product, RatesProvider provider) {
    return pricer().forwardFxRate(product, provider);
  }

}
