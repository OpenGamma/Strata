/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;

/**
 * Provides the ability for objects to be automatically currency converted.
 * <p>
 * The interface is intended to operate with multi-scenario objects.
 * As such, the supplied market data is scenario aware, and the FX rate used to convert
 * each value may be different.
 * 
 * @param <R>  the type of the currency converted result
 */
public interface CurrencyConvertible<R> {

  /**
   * Returns a copy of this object with any currency amounts converted into the reporting currency.
   * <p>
   * Each currency amount in the object will be converted to the specified currency.
   * Any object that is not a currency amount will be left unchanged.
   * <p>
   * The supplied market data is scenario aware. If this object is also scenario aware, then the
   * FX rate used to convert each value may be different.
   * In that case, the market data must have the same number of scenarios as this instance.
   *
   * @param reportingCurrency  the currency into which all currency amounts should be converted
   * @param marketData  the multi-scenario market data containing any FX rates needed to perform the conversion
   * @return a copy of this result with any currency amounts converted into the reporting currency
   * @throws RuntimeException if no FX rate could be found
   */
  public abstract R convertedTo(Currency reportingCurrency, CalculationMarketData marketData);

}
