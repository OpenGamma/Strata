/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * An array of values, one for each scenario.
 * <p>
 * Implementations of this interface provide scenario-specific values.
 * <p>
 * In the simplest case, this might be a list of values, one for each scenario.
 * This is handled by this factory methods on this interface.
 * <p>
 * There are two obvious reasons for creating implementations of this interface with
 * special handling for certain types of value:
 * <ul>
 *   <li>Reducing memory usage</li>
 *   <li>Improving performance</li>
 * </ul>
 * For example, if the system stores multiple copies of a curve as a list it must store the x-values with
 * each copy of the curve. This data is mostly redundant as the x-values are the same in every scenario.
 * A custom data type for storing scenario data for a curve can store one set of x-values shared between
 * all scenarios, reducing memory footprint.
 * <p>
 * When dealing with primitive data it is likely be more efficient to store the scenario values in a primitive
 * array instead of using a list. This removes the need for boxing and reduces memory footprint.
 * Also, if a function calculates values for all scenarios at the same time, it is likely to be more efficient
 * if the data is stored in arrays as the values will be stored in a contiguous block of memory.
 * <p>
 * The generic type parameter is the type of the single value associated with each scenario.
 * For example, in the case of optimized curve storage, the single value is a curve.
 *
 * @param <T>  the type of each individual value
 */
public interface ScenarioArray<T> {

  /**
   * Obtains an instance from the specified array of values.
   *
   * @param <T>  the type of the value
   * @param values  the values, one value for each scenario
   * @return an instance with the specified values
   */
  @SafeVarargs
  public static <T> ScenarioArray<T> of(T... values) {
    return DefaultScenarioArray.of(values);
  }

  /**
   * Obtains an instance from the specified list of values.
   *
   * @param <T>  the type of the value
   * @param values  the values, one value for each scenario
   * @return an instance with the specified values
   */
  public static <T> ScenarioArray<T> of(List<T> values) {
    return DefaultScenarioArray.of(values);
  }

  /**
   * Obtains an instance using a function to create the entries.
   * <p>
   * The function is passed the scenario index and returns the value for that index.
   * 
   * @param <T>  the type of the value
   * @param scenarioCount  the number of scenarios
   * @param valueFunction  the function used to obtain each value
   * @return an instance initialized using the function
   * @throws IllegalArgumentException is size is zero or less
   */
  public static <T> ScenarioArray<T> of(int scenarioCount, IntFunction<T> valueFunction) {
    return DefaultScenarioArray.of(scenarioCount, valueFunction);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a single value where the value applies to all scenarios.
   *
   * @param <T>  the type of the value
   * @param scenarioCount  the nnumber of scenarios
   * @param value  the single value, used for all scenarios
   * @return an instance with the specified values
   */
  public static <T> ScenarioArray<T> ofSingleValue(int scenarioCount, T value) {
    return SingleScenarioArray.of(scenarioCount, value);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the number of scenarios.
   *
   * @return the number of scenarios
   */
  public abstract int getScenarioCount();

  /**
   * Gets the value at the specified scenario index.
   *
   * @param scenarioIndex  the zero-based index of the scenario
   * @return the value at the specified index
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public abstract T get(int scenarioIndex);

  /**
   * Returns a stream of the values.
   * <p>
   * The stream will return the value for each scenario.
   *
   * @return a stream of the values
   */
  public default Stream<T> stream() {
    return IntStream.range(0, getScenarioCount()).mapToObj(i -> get(i));
  }

}
