/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function;

import java.util.Map;
import java.util.Set;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.collect.result.Result;

/**
 * A function that calculates multiple values for a target using multiple sets of market data.
 * <p>
 * The values must be converted into each of the reporting currencies before they are returned.
 * <p>
 * A function should implement this interface if at least one of the values it calculates contains a currency amount
 * and it needs to perform the conversion into the reporting currencies itself.
 * <p>
 * It is recommended that functions which calculate currency amounts implement {@link CalculationMultiFunction} and
 * return an implementations of {@link CurrencyConvertible}. This allows the calculation engine to convert
 * currency values into the reporting currencies. This interface is should only be implemented by
 * functions where the automatic currency conversion is insufficient.
 * <p>
 * If none of the calculated values contain currency amounts it is recommended to implement {@link CalculationMultiFunction}.
 *
 * @param <T>  the type of target handled by this function
 */
public interface CurrencyAwareCalculationMultiFunction<T extends CalculationTarget>
    extends CalculationFunction<T> {

  /**
   * Calculates values of multiple measures for the target using multiple sets of market data and converts
   * them into each of the reporting currencies.
   *
   * @param target  the target of the calculation
   * @param marketData  the market data used in the calculation
   * @param measures  the measures the function should calculate with the currencies in which their
   *   values should be reported
   * @return the result of the calculation
   */
  public abstract Map<Measure, Map<Currency, Result<?>>> execute(
      T target,
      CalculationMarketData marketData,
      Map<Measure, Set<Currency>> measures);
}
