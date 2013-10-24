/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Set;

import com.opengamma.financial.currency.CurrencyPair;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;

public class CurrencyPairs implements CurrencyPairsFunction {

  private final Set<CurrencyPair> _currencyPairs;

  private final ResultGenerator _resultGenerator;

  public CurrencyPairs(ResultGenerator resultGenerator,
                       Set<CurrencyPair> currencyPairs) {
    _currencyPairs = currencyPairs;
    _resultGenerator = resultGenerator;
  }

  @Override
  public FunctionResult<CurrencyPair> getCurrencyPair(Currency currency1, Currency currency2) {

    ArgumentChecker.notNull(currency1, "currency1");
    ArgumentChecker.notNull(currency2, "currency2");

    CurrencyPair requestedPair = CurrencyPair.of(currency1, currency2);
    CurrencyPair matchingPair = findMatchingPair(requestedPair);

    return matchingPair != null ?
        _resultGenerator.generateSuccessResult(matchingPair) :
        _resultGenerator.<CurrencyPair>generateFailureResult(
            ResultStatus.MISSING_DATA, "No currency pair matching {} was found", requestedPair);
  }

  private CurrencyPair findMatchingPair(CurrencyPair pair) {
    if (_currencyPairs.contains(pair)) {
      return pair;
    } else {
      CurrencyPair inverse = pair.inverse();
      return _currencyPairs.contains(inverse) ? inverse : null;
    }
  }
}
