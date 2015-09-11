/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculation.function;

import java.util.Map;
import java.util.Set;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;

/**
 * A function that calculates currency values for a target using multiple sets of market data.
 * <p>
 * The value must be converted into each of the reporting currencies before it is returned.
 * <p>
 * A function should implement this interface if it calculates contains any currency amounts and
 * it needs to perform the conversion into the reporting currencies itself.
 * <p>
 * It is recommended that functions which calculate currency amounts implement {@link CalculationSingleFunction} and
 * return an implementation of {@link CurrencyConvertible}. This allows the calculation engine to convert
 * currency values into the reporting currencies. This interface is should only be implemented by
 * functions where the automatic currency conversion is insufficient.
 * <p>
 * If the calculated value contains no currency amounts it is recommended to implement {@link CalculationSingleFunction}.
 *
 * @param <T>  the type of target handled by this function
 * @param <R>  the type of value calculated by this function
 */
public interface CurrencyAwareCalculationSingleFunction<T extends CalculationTarget, R>
    extends CalculationFunction<T> {

  /**
   * Calculates a value for the target using multiple sets of market data and converts it into each of the
   * reporting currencies.
   *
   * @param target  the target of the calculation
   * @param marketData  the market data used in the calculation
   * @param reportingCurrencies  the currencies in which the result should be reported
   * @return the result of the calculation, converted into each of the reporting currencies
   */
  public abstract Map<Currency, Result<R>> execute(
      T target,
      CalculationMarketData marketData,
      Set<Currency> reportingCurrencies);
}
