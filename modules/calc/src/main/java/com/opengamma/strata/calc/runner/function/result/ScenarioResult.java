/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function.result;

import java.util.stream.Stream;

import com.opengamma.strata.calc.runner.function.CalculationFunction;

/**
 * A container for multiple results produced by performing a single calculation across multiple scenarios.
 * <p>
 * A {@link CalculationFunction} produces a map of measure to result.
 * Each result is an instance of this type, providing access to the scenarios that were calculated.
 * <p>
 * The number of results is required to be the same as the number of scenarios in the market data
 * provided to the function.
 *
 * @param <T> the type of the individual results
 */
public interface ScenarioResult<T> {

  /**
   * Returns the number of results.
   * <p>
   * This is required to be the same as the number of scenarios in the market data provided to the function.
   *
   * @return the number of results
   */
  public abstract int size();

  /**
   * Returns the result at the specified index.
   * <p>
   * The index must be valid, between zero (inclusive) and {@code size()} (exclusive).
   *
   * @param index  the index of the result that should be returned
   * @return the result at the specified index
   */
  public abstract T get(int index);

  /**
   * Returns a stream of the results.
   * <p>
   * The stream will return one entry for each scenario.
   *
   * @return a stream of the results
   */
  public abstract Stream<T> stream();

}
