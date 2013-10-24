/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.google.common.base.Optional;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.util.money.Currency;

public interface CurrencyPairsFunction {

  /**
   * Finds the currency pair instance for the two currencies assuming it is one
   * of the defined pairs configured for the system.
   * <p>
   * The returned pair will be either 'currency1-currency2' or 'currency2-currency1'.
   *
   *
   * @param currency1  the first currency, not null
   * @param currency2 the second currency, not null
   * @return the market convention currency pair for the two currencies, null if not found
   */
  FunctionResult<CurrencyPair> getCurrencyPair(Currency currency1, Currency currency2);
}
