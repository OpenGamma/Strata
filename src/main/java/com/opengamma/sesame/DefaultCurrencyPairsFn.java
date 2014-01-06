/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static com.opengamma.util.result.FailureStatus.MISSING_DATA;
import static com.opengamma.util.result.ResultGenerator.failure;
import static com.opengamma.util.result.ResultGenerator.success;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.UnorderedCurrencyPair;
import com.opengamma.util.result.Result;

public class DefaultCurrencyPairsFn implements CurrencyPairsFn {

  private final Map<UnorderedCurrencyPair, CurrencyPair> _currencyPairs;

  public DefaultCurrencyPairsFn(Set<CurrencyPair> currencyPairs) {
    ArgumentChecker.notNull(currencyPairs, "currencyPairs");
    ImmutableMap.Builder<UnorderedCurrencyPair, CurrencyPair> builder = ImmutableMap.builder();
    for (CurrencyPair pair : currencyPairs) {
      builder.put(UnorderedCurrencyPair.of(pair.getBase(), pair.getCounter()), pair);
    }
    _currencyPairs = builder.build();
  }

  @Override
  public Result<CurrencyPair> getCurrencyPair(Currency currency1, Currency currency2) {

    return getCurrencyPair(UnorderedCurrencyPair.of(currency1, currency2));
  }

  @Override
  public Result<CurrencyPair> getCurrencyPair(UnorderedCurrencyPair pair) {

    ArgumentChecker.notNull(pair, "pair");

    if (_currencyPairs.containsKey(pair)) {
      return success(_currencyPairs.get(pair));
    } else {
      return failure(MISSING_DATA, "No currency pair matching {} was found", pair);
    }
  }
}
