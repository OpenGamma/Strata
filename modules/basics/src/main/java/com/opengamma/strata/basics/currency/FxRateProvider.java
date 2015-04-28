/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

/**
 * A provider of FX rates.
 * <p>
 * This provides the ability to obtain an FX rate. The interface does not mandate when the
 * rate applies, however it typically represents the current rate.
 * <p>
 * One possible implementation is {@link FxMatrix}.
 * <p>
 * Implementations do not have to be immutable, but calls to the methods must be thread-safe.
 */
public interface FxRateProvider {

  /**
   * Gets the FX rate for the specified currency pair.
   * <p>
   * The rate returned is the rate from the base currency to the counter currency
   * as defined by this formula: {@code (1 * baseCurrency = fxRate * counterCurrency)}.
   * 
   * @param baseCurrency  the base currency, to convert from
   * @param counterCurrency  the counter currency, to convert to
   * @return the FX rate for the currency pair
   * @throws RuntimeException if no FX rate could be found
   */
  public abstract double fxRate(Currency baseCurrency, Currency counterCurrency);

  /**
   * Gets the FX rate for the specified currency pair.
   * <p>
   * The rate returned is the rate from the base currency to the counter currency
   * as defined by this formula: {@code (1 * baseCurrency = fxRate * counterCurrency)}.
   * 
   * @param currencyPair  the ordered currency pair defining the rate required
   * @return the FX rate for the currency pair
   * @throws RuntimeException if no FX rate could be found
   */
  public default double fxRate(CurrencyPair currencyPair) {
    return fxRate(currencyPair.getBase(), currencyPair.getCounter());
  }

}
