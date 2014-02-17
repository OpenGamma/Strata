/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.UnorderedCurrencyPair;
import com.opengamma.util.result.Result;

/**
 * Function capable of providing an ordered currency pair.
 */
public interface CurrencyPairsFn {

  /**
   * Finds the currency pair instance for the two currencies assuming it is one
   * of the defined pairs configured for the system.
   * <p>
   * The returned pair will be either 'currency1-currency2' or 'currency2-currency1'.
   *
   * @param currency1  the first currency, not null
   * @param currency2  the second currency, not null
   * @return the market convention currency pair for the two currencies, a failure result if not found
   */
  Result<CurrencyPair> getCurrencyPair(Currency currency1, Currency currency2);

  /**
   * Finds the currency pair instance for the supplied pair assuming it is one
   * of the defined pairs configured for the system.
   * <p>
   * Note that if a matching pair is available, then it will be in a specific order
   * as opposed to the pair passed.
   *
   * @param pair  the currencies to be checked, not null
   * @return the market convention currency pair for the two currencies, a failure result if not found
   */
  Result<CurrencyPair> getCurrencyPair(UnorderedCurrencyPair pair);

}
