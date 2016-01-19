/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.Result;

/**
 * A function that calculates multiple measures for a target using multiple sets of market data.
 * <p>
 * If any of the calculated values contain any currency amounts and implement {@link CurrencyConvertible}
 * the calculation engine will automatically convert the amounts into the reporting currency.
 * <p>
 * If any of the calculated values contain currency amounts and the automatic currency conversion is
 * insufficient the function should implement {@link CurrencyAwareCalculationMultiFunction}.
 *
 * @param <T>  the type of target handled by this function
 */
public interface CalculationMultiFunction<T extends CalculationTarget>
    extends CalculationFunction<T> {

  /**
   * Returns the set of measures that the function can calculate.
   *
   * @return the read-only set of measures that the function can calculate
   */
  public abstract Set<Measure> supportedMeasures();

  /**
   * Returns the default reporting currency for the result of performing the calculation for the target.
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
  @Override
  public default Optional<Currency> defaultReportingCurrency(T target) {
    return Optional.empty();
  }

  /**
   * Determines the market data requirements that function has to perform its calculations.
   * <p>
   * Any market data needed by the {@code calculate} method should be specified.
   *
   * @param target  the target of the calculation
   * @param measures  the set of measures to be calculated
   * @return the requirements specifying the market data the function needs to perform calculations
   */
  public abstract FunctionRequirements requirements(T target, Set<Measure> measures);

  /**
   * Calculates values of multiple measures for the target using multiple sets of market data.
   * <p>
   * The set of measures must only contain measures that the function supports,
   * as returned by {@link #supportedMeasures()}.
   *
   * @param target  the target of the calculation
   * @param measures  the set of measures to calculate
   * @param marketData  the market data to be used in the calculation
   * @return the read-only map of calculated values, keyed by their measure
   */
  public abstract Map<Measure, Result<ScenarioResult<?>>> calculate(
      T target,
      Set<Measure> measures,
      CalculationMarketData marketData);

}
