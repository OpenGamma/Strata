/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxConvertible;

/**
 * Provides the ability for objects to be automatically currency converted.
 * <p>
 * The interface is intended to operate with multi-scenario objects.
 * As such, the supplied market data is scenario aware, and the FX rate used to convert
 * each value may be different.
 * <p>
 * For example, the object implementing this interface might hold 100 instances of
 * {@link CurrencyAmount}, one for each scenario. When invoked, the {@link ScenarioFxRateProvider}
 * will be used to convert each of the 100 amounts, with each conversion potentially
 * having a different FX rate.
 * <p>
 * This is the multi-scenario version of {@link FxConvertible}.
 * 
 * @param <R>  the type of the currency converted result
 */
public interface ScenarioFxConvertible<R> {

  /**
   * Converts this instance to an equivalent amount in the specified currency.
   * <p>
   * The result, which may be of a different type, will be expressed in terms of the given currency.
   * Any FX conversion that is required will use rates from the provider.
   * <p>
   * Any object that is not a currency amount will be left unchanged.
   * The number of scenarios of this instance must match the number of scenarios of the specified provider.
   * 
   * @param resultCurrency  the currency of the result
   * @param rateProvider  the multi-scenario provider of FX rates
   * @return the converted instance, which should be expressed in the specified currency
   * @throws RuntimeException if no FX rate could be found
   */
  public abstract R convertedTo(Currency resultCurrency, ScenarioFxRateProvider rateProvider);

}
