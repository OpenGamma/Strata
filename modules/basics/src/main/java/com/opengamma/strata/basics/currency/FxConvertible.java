/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

/**
 * Defines a standard mechanism for converting an object representing one or more
 * monetary amounts to a single currency.
 * <p>
 * The single method allows a monetary object to be converted to a similar object
 * expressed in terms of the specified currency.
 * The conversion is permitted to return a different type.
 * <p>
 * For example, the {@link MultiCurrencyAmount} class implements this interface
 * and returns a {@link CurrencyAmount} instance.
 * <p>
 * Implementations do not have to be immutable, but calls to the method must be thread-safe.
 * 
 * @param <R>  the result type expressed in a single currency
 */
public interface FxConvertible<R> {

  /**
   * Converts this instance to an equivalent amount in the specified currency.
   * <p>
   * The result, which may be of a different type, will be expressed in terms of the given currency.
   * Any FX conversion that is required will use rates from the provider.
   * 
   * @param resultCurrency  the currency of the result
   * @param rateProvider  the provider of FX rates
   * @return the converted instance, which should be expressed in the specified currency
   * @throws RuntimeException if no FX rate could be found
   */
  public abstract R convertedTo(Currency resultCurrency, FxRateProvider rateProvider);

}
