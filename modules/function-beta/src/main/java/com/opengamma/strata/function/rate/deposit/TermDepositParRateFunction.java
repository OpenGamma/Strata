/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.deposit;

import com.opengamma.strata.finance.rate.deposit.ExpandedTermDeposit;
import com.opengamma.strata.pricer.RatesProvider;

/**
 * Calculates the par rate of a {@code TermDepositTrade} for each of a set of scenarios.
 */
public class TermDepositParRateFunction
    extends AbstractTermDepositFunction<Double> {

  @Override
  protected Double execute(ExpandedTermDeposit product, RatesProvider provider) {
    return pricer().parRate(product, provider);
  }

}
