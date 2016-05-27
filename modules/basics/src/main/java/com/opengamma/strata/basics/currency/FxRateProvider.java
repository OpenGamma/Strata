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
   * Converts an amount in a currency to an amount in a different currency using this rate.
   * <p>
   * The currencies must both be included in the currency pair of this rate.
   *
   * @param amount  an amount in {@code fromCurrency}
   * @param fromCurrency  the currency of the amount
   * @param toCurrency  the currency into which the amount should be converted
   * @return the amount converted into {@code toCurrency}
   * @throws IllegalArgumentException if either of the currencies aren't included in the currency pair of this rate
   */
  public default double convert(double amount, Currency fromCurrency, Currency toCurrency) {
    return amount * fxRate(fromCurrency, toCurrency);
  }

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
