/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.ReportingCurrency;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioFxConvertible;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

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
 * <li>{@link #requirements(CalculationTarget, Set, CalculationParameters, ReferenceData)}
 *  - the market data requirements for performing the calculation
 * <li>{@link #calculate(CalculationTarget, Set, CalculationParameters, ScenarioMarketData, ReferenceData)}
 *  - perform the calculation
 * </ul>
 * <p>
 * If any of the calculated values contain any currency amounts and implement {@link ScenarioFxConvertible}
 * the calculation runner will automatically convert the amounts into the reporting currency.
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
   * Returns an identifier that should uniquely identify the specified target.
   * <p>
   * This identifier is used in error messages to identify the target.
   * This should normally be overridden to provide a suitable identifier.
   * For example, if the target is a trade, there will typically be a trade identifier available.
   * <p>
   * This method must not throw an exception.
   *
   * @param target  the target of the calculation
   * @return the identifier of the target, empty if no suitable identifier available
   */
  public default Optional<String> identifier(T target) {
    return Optional.empty();
  }

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
   * <p>
   * The set of measures may include measures that are not supported by this function.
   *
   * @param target  the target of the calculation
   * @param measures  the set of measures to be calculated
   * @param parameters  the parameters that affect how the calculation is performed
   * @param refData  the reference data to be used in the calculation
   * @return the requirements specifying the market data the function needs to perform calculations
   */
  public abstract FunctionRequirements requirements(
      T target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData);

  /**
   * Calculates values of multiple measures for the target using multiple sets of market data.
   * <p>
   * The set of measures must only contain measures that the function supports,
   * as returned by {@link #supportedMeasures()}. The market data must provide at least the
   * set of data requested by {@link #requirements(CalculationTarget, Set, CalculationParameters, ReferenceData)}.
   * <p>
   * The result of this method will often be an instance of {@link ScenarioArray}, which
   * handles the common case where there is one calculated value for each scenario.
   * However, it is also possible for the function to calculate an aggregated result, such
   * as the maximum or minimum value across all scenarios, in which case the result would
   * not implement {@code ScenarioArray}.
   *
   * @param target  the target of the calculation
   * @param measures  the set of measures to calculate
   * @param parameters  the parameters that affect how the calculation is performed
   * @param marketData  the multi-scenario market data to be used in the calculation
   * @param refData  the reference data to be used in the calculation
   * @return the read-only map of calculated values, keyed by their measure
   */
  public abstract Map<Measure, Result<?>> calculate(
      T target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData marketData,
      ReferenceData refData);

}
