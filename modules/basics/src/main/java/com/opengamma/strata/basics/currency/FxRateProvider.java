/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import java.util.function.Supplier;

import com.opengamma.strata.collect.Messages;

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
   * Returns an {@code FxRateProvider} that delays fetching its underlying provider
   * until actually necessary.
   * <p>
   * This is typically useful where you <em>may</em> need a {@code MarketDataFxRateProvider}
   * but want to delay loading market data to construct the provider until you are sure you actually do need it.
   *
   * @param target  the supplier of the underlying provider
   * @return the provider
   */
  public static FxRateProvider lazy(Supplier<FxRateProvider> target) {
    return new LazyFxRateProvider(target);
  }

  /**
   * Returns a provider that always throws an exception.
   * <p>
   * The provider will always throw an exception, even if the two currencies are the same.
   * 
   * @return the provider
   */
  public static FxRateProvider noConversion() {
    return (Currency baseCurrency, Currency counterCurrency) -> {
      throw new IllegalArgumentException(Messages.format(
          "FX rate conversion is not supported, requested for {}/{}", baseCurrency, counterCurrency));
    };
  }

  /**
   * Returns a provider that provides minimal behavior.
   * <p>
   * This provider returns a rate or 1 when the two currencies are the same.
   * When the two currencies differ an exception will be thrown.
   * 
   * @return the provider
   */
  public static FxRateProvider minimal() {
    return (Currency baseCurrency, Currency counterCurrency) -> {
      if (baseCurrency.equals(counterCurrency)) {
        return 1;
      }
      throw new IllegalArgumentException(Messages.format(
          "FX rate conversion is not supported for {}/{}", baseCurrency, counterCurrency));
    };
  }

  //-------------------------------------------------------------------------
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
