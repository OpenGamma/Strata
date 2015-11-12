/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function.result;

import java.util.stream.Stream;

import com.opengamma.strata.calc.runner.function.CalculationMultiFunction;
import com.opengamma.strata.calc.runner.function.CalculationSingleFunction;

/**
 * A container for multiple results produced by performing a single calculation across multiple scenarios.
 * <p>
 * This class is intended to be used as the return value from the {@code execute} method of
 * implementations of {@link CalculationSingleFunction} and {@link CalculationMultiFunction}.
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
   * This is required to be the same as the number of scenarios in the market data provided to the function
   *
   * @return the number of results
   */
  public abstract int size();

  /**
   * Returns the result at the specified index.
   *
   * @param index  the index of the result that should be returned
   * @return the result at the specified index
   */
  public abstract T get(int index);

  /**
   * Returns a stream of the results.
   *
   * @return a stream of the results
   */
  public abstract Stream<T> stream();
}
