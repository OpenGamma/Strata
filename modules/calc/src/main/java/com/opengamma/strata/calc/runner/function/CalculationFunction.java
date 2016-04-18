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
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.ReportingCurrency;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.Result;

/**
 * Primary interface for all calculation functions that calculate measures.
 * <p>
 * Implementations of this interface provide the ability to calculate one or more measures
 * for a target (trade) using one or more sets of market data (scenarios).
 * The methods of the function allow the {@link CalculationRunner} to correctly invoke the function:
 * <ul>
 * <li>{@link #targetType()}
 *  - the target type that the function applies to
 * <li>{@link #supportedMeasures()}
 *  - the set of measures that can be calculated
 * <li>{@link #naturalCurrency(CalculationTarget, ReferenceData)}
 *  - the "natural" currency of the target
 * <li>{@link #requirements(CalculationTarget, Set, ReferenceData)}
 *  - the market data requirements for performing the calculation
 * <li>{@link #calculate(CalculationTarget, Set, CalculationMarketData, ReferenceData)}
 *  - perform the calculation
 * </ul>
 * <p>
 * If any of the calculated values contain any currency amounts and implement {@link CurrencyConvertible}
 * the calculation engine will automatically convert the amounts into the reporting currency.
 *
 * @param <T>  the type of target handled by this function
 */
public interface CalculationFunction<T extends CalculationTarget> {

  /**
   * Gets the target type that this function applies to.
   * <p>
   * The target type will typically be a concrete class.
   *
   * @return the target type
   */
  public abstract Class<T> targetType();

  /**
   * Returns the set of measures that the function can calculate.
   *
   * @return the read-only set of measures that the function can calculate
   */
  public abstract Set<Measure> supportedMeasures();

  /**
   * Returns the "natural" currency for the specified target.
   * <p>
   * This is the currency to which currency amounts are converted if the "natural"
   * reporting currency is requested using {@link ReportingCurrency#NATURAL}.
   * Most targets have a "natural" currency, for example the currency of a FRA or
   * the base currency of an FX forward.
   * <p>
   * It is required that all functions that return a currency-convertible measure
   * must choose a "natural" currency for each trade. The choice must be consistent
   * not random, given the same trade the same currency must be returned.
   * This might involve picking, the first leg or base currency from a currency pair.
   * An exception must only be thrown if the function handles no currency-convertible measures.
   *
   * @param target  the target of the calculation
   * @param refData  the reference data to be used in the calculation
   * @return the "natural" currency of the target
   * @throws IllegalStateException if the function calculates no currency-convertible measures
   */
  public abstract Currency naturalCurrency(T target, ReferenceData refData);

  /**
   * Determines the market data required by this function to perform its calculations.
   * <p>
   * Any market data needed by the {@code calculate} method should be specified.
   *
   * @param target  the target of the calculation
   * @param measures  the set of measures to be calculated
   * @param refData  the reference data to be used in the calculation
   * @return the requirements specifying the market data the function needs to perform calculations
   */
  public abstract FunctionRequirements requirements(
      T target,
      Set<Measure> measures,
      ReferenceData refData);

  /**
   * Calculates values of multiple measures for the target using multiple sets of market data.
   * <p>
   * The set of measures must only contain measures that the function supports,
   * as returned by {@link #supportedMeasures()}. The market data must provide at least the
   * set of data requested by {@link #requirements(CalculationTarget, Set, ReferenceData)}.
   * <p>
   * The result of this method will often be an instance of {@link ScenarioResult}, which
   * handles the common case where there is one calculated value for each scenario.
   * However, it is also possible for the function to calculate an aggregated result, such
   * as the maximum or minimum value across all scenarios, in which case the result would
   * not implement {@code ScenarioResult}.
   *
   * @param target  the target of the calculation
   * @param measures  the set of measures to calculate
   * @param marketData  the multi-scenario market data to be used in the calculation
   * @param refData  the reference data to be used in the calculation
   * @return the read-only map of calculated values, keyed by their measure
   */
  public abstract Map<Measure, Result<?>> calculate(
      T target,
      Set<Measure> measures,
      CalculationMarketData marketData,
      ReferenceData refData);

}
