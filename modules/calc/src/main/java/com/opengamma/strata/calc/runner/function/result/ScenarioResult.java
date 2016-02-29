/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function.result;

import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A container for multiple results produced by performing a single calculation across multiple scenarios.
 * <p>
 * A {@link CalculationFunction} produces a map of measure to result, where the value of
 * the result is typically an instance of {@code ScenarioResult}. This interface represents
 * the common case where there is one calculated value for each scenario. In this case, the
 * size must match the number of scenarios in the market data provided to the function.
 *
 * @param <T> the type of the individual results
 */
public interface ScenarioResult<T> {

  /**
   * Obtains an instance from the specified array of values.
   *
   * @param <T>  the type of the result
   * @param values  the values, one value for each scenario
   * @return an instance with the specified values
   */
  @SafeVarargs
  public static <T> ScenarioResult<T> of(T... values) {
    return of(ImmutableList.copyOf(values));
  }

  /**
   * Obtains an instance from the specified list of values.
   *
   * @param <T>  the type of the result
   * @param values  the values, one value for each scenario
   * @return an instance with the specified values
   */
  public static <T> ScenarioResult<T> of(List<T> values) {
    return DefaultScenarioResult.of(values);
  }

  /**
   * Obtains an instance using a function to create the entries.
   * <p>
   * The function is passed the scenario index and returns the value for that index.
   * 
   * @param <T>  the result type
   * @param size  the number of elements
   * @param valueFunction  the function used to obtain each value
   * @return an instance initialized using the function
   * @throws IllegalArgumentException is size is zero or less
   */
  public static <T> ScenarioResult<T> of(int size, IntFunction<T> valueFunction) {
    ArgChecker.notNegativeOrZero(size, "size");
    ImmutableList.Builder<T> builder = ImmutableList.builder();
    for (int i = 0; i < size; i++) {
      builder.add(valueFunction.apply(i));
    }
    return ScenarioResult.of(builder.build());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the number of values in the result.
   * <p>
   * This is required to be the same as the number of scenarios in the market data provided to the function.
   *
   * @return the number of results
   */
  public abstract int size();

  /**
   * Returns the value at the specified index.
   * <p>
   * The index must be valid, between zero (inclusive) and {@code size()} (exclusive).
   *
   * @param index  the index of the result that should be returned
   * @return the result at the specified index
   */
  public abstract T get(int index);

  /**
   * Returns a stream of the values.
   * <p>
   * The stream will return one value for each scenario.
   *
   * @return a stream of the values
   */
  public abstract Stream<T> stream();

}
