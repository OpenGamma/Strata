/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.deposit;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.deposit.ExpandedTermDeposit;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Calculates the present value of a {@code TermDepositTrade} for each of a set of scenarios.
 */
public class TermDepositPvFunction
    extends AbstractTermDepositFunction<CurrencyAmount> {

  @Override
  protected CurrencyAmount execute(ExpandedTermDeposit product, RatesProvider provider) {
    return pricer().presentValue(product, provider);
  }

}
