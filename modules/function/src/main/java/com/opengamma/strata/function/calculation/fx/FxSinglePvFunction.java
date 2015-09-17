/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.fx.ExpandedFx;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates the present value of an {@code FxTrade} for each of a set of scenarios.
 */
public class FxSinglePvFunction
    extends AbstractFxSingleFunction<MultiCurrencyAmount> {

  @Override
  protected MultiCurrencyAmount execute(ExpandedFx product, RatesProvider provider) {
    return pricer().presentValue(product, provider);
  }

}
