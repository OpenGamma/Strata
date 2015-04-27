/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations.function;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;

/**
 * Interface for objects containing currency amounts that can be automatically converted to a different currency
 * by the calculation engine for reporting purposes.
 */
public interface CurrencyConvertible<T> {

  /**
   * Returns a copy of the object with any currency amounts converted into the reporting currency.
   *
   * @param reportingCurrency  the currency into which all currency amounts should be converted
   * @param marketData  market data for multiple scenarios containing any FX rates needed to perform the conversion
   * @return a copy of the object with any currency amounts converted into the reporting currency
   */
  public abstract T convertedTo(Currency reportingCurrency, CalculationMarketData marketData);
}
