/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.deposit;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.deposit.ExpandedTermDeposit;

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
