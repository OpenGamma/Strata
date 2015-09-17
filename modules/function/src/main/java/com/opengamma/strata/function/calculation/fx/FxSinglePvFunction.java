/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fx;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.finance.fx.ExpandedFxSingle;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates the present value of an {@code FxSingleTrade} for each of a set of scenarios.
 */
public class FxSinglePvFunction
    extends AbstractFxSingleFunction<MultiCurrencyAmount> {

  @Override
  protected MultiCurrencyAmount execute(ExpandedFxSingle product, RatesProvider provider) {
    return pricer().presentValue(product, provider);
  }

}
