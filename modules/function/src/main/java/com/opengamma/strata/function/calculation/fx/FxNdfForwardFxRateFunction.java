/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.finance.fx.ExpandedFxNonDeliverableForward;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates the future FX rate of an {@code FxNonDeliverableForwardTrade} for each of a set of scenarios.
 */
public class FxNdfForwardFxRateFunction
    extends AbstractFxNdfFunction<FxRate> {

  @Override
  protected FxRate execute(ExpandedFxNonDeliverableForward product, RatesProvider provider) {
    return pricer().forwardFxRate(product, provider);
  }

}
