/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.finance.fx.ExpandedFxSingle;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates the future FX rate of an {@code FxSingleTrade} for each of a set of scenarios.
 */
public class FxSingleForwardFxRateFunction
    extends AbstractFxSingleFunction<FxRate> {

  @Override
  protected FxRate execute(ExpandedFxSingle product, RatesProvider provider) {
    return pricer().forwardFxRate(product, provider);
  }

}
