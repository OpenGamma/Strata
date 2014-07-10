/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.FailureStatus.MISSING_DATA;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.sesame.component.CurrencyPairSet;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.UnorderedCurrencyPair;
import com.opengamma.util.result.Result;

/**
 * Function implementation returning ordered currency pairs.
 */
public class DefaultCurrencyPairsFn implements CurrencyPairsFn {

  /**
   * The map of unordered to ordered currency pairs/
   */
  private final Map<UnorderedCurrencyPair, CurrencyPair> _currencyPairs;

  /**
   * Constructor using a currency pair set. Note that naturally this
   * would accept a set of currency pairs. However, due to a serialization
   * bug in Fudge (http://jira.fudgemsg.org/browse/FRJ-128), this does not
   * work correctly over remote connections.
   *
   * @param currencyPairs the currency pairs to be used
   */
  public DefaultCurrencyPairsFn(CurrencyPairSet currencyPairs) {
    ArgumentChecker.notNull(currencyPairs, "currencyPairs");
    ImmutableMap.Builder<UnorderedCurrencyPair, CurrencyPair> builder = ImmutableMap.builder();
    for (CurrencyPair pair : currencyPairs.getCurrencyPairs()) {
      builder.put(UnorderedCurrencyPair.of(pair.getBase(), pair.getCounter()), pair);
    }
    _currencyPairs = builder.build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Result<CurrencyPair> getCurrencyPair(Currency currency1, Currency currency2) {

    return getCurrencyPair(UnorderedCurrencyPair.of(currency1, currency2));
  }

  @Override
  public Result<CurrencyPair> getCurrencyPair(UnorderedCurrencyPair pair) {

    ArgumentChecker.notNull(pair, "pair");

    if (_currencyPairs.containsKey(pair)) {
      return Result.success(_currencyPairs.get(pair));
    } else {
      return Result.failure(MISSING_DATA, "No currency pair matching {} was found", pair);
    }
  }

}
