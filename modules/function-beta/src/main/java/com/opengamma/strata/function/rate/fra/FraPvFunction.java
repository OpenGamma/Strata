/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.fra;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.fra.ExpandedFra;
import com.opengamma.strata.pricer.RatesProvider;

/**
 * Calculates the present value of a {@code FraTrade} for each of a set of scenarios.
 */
public class FraPvFunction
    extends AbstractFraFunction<CurrencyAmount> {

  @Override
  protected CurrencyAmount execute(ExpandedFra product, RatesProvider provider) {
    return pricer().presentValue(product, provider);
  }

}
