/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.fx.ExpandedFxNonDeliverableForward;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates the currency exposure of an {@code FxNonDeliverableForwardTrade} for each of a set of scenarios.
 */
public class FxNdfCurrencyExposureFunction
    extends AbstractFxNdfFunction<MultiCurrencyAmount> {

  @Override
  protected MultiCurrencyAmount execute(ExpandedFxNonDeliverableForward product, RatesProvider provider) {
    return pricer().currencyExposure(product, provider);
  }

}
