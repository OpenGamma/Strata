/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.deposit;

import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.deposit.ExpandedTermDeposit;

/**
 * Calculates the par spread of a {@code TermDepositTrade} for each of a set of scenarios.
 */
public class TermDepositParSpreadFunction
    extends AbstractTermDepositFunction<Double> {

  @Override
  protected Double execute(ExpandedTermDeposit product, RatesProvider provider) {
    return pricer().parSpread(product, provider);
  }

}
