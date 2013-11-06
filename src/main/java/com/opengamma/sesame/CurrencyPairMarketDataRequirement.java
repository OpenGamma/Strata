/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.financial.currency.CurrencyPair;

/**
 * A market data requirement for a currency pair.
 */
public class CurrencyPairMarketDataRequirement implements MarketDataRequirement {

  /**
   * The currency pair to get market data for.
   */
  private final CurrencyPair _currencyPair;

  /* package */ CurrencyPairMarketDataRequirement(CurrencyPair currencyPair) {
    _currencyPair = currencyPair;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    return _currencyPair.equals(((CurrencyPairMarketDataRequirement) o)._currencyPair);
  }

  @Override
  public int hashCode() {
    return _currencyPair.hashCode();
  }
}
