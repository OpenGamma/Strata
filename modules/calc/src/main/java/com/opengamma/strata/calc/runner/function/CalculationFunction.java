/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function;

import java.util.Optional;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;

/**
 * Supertype of all functions that calculate values of measures for a target.
 * <p>
 * All functions must be able to specify the data they require to perform their calculations.
 * <p>
 * More specific function types can calculate values for one measure or multiple measures, and
 * may rely on the engine to convert values into the reporting currency or handle it themselves.
 *
 * @param <T>  the type of target handled by this function
 */
public interface CalculationFunction<T extends CalculationTarget> {

  /**
   * Returns requirements specifying the market data the function needs to perform its calculations.
   *
   * @param target  a target
   * @return requirements specifying the market data the function needs to perform its calculations for the target
   */
  public abstract FunctionRequirements requirements(T target);

  /**
   * Returns the default reporting currency for the result of performing the calculation for the target
   * if there is a sensible default.
   * <p>
   * This is the currency to which currency amounts are converted if the reporting rules don't specify
   * a reporting currency. This is normally the 'natural' currency for the trade, for example
   * the currency of a FRA or the base currency of an FX forward.
   * <p>
   * The default implementation returns an empty optional.
   *
   * @param target  the target of the calculation
   * @return the default reporting currency for the target if there is a sensible default
   */
  public default Optional<Currency> defaultReportingCurrency(T target) {
    return Optional.empty();
  }
}
