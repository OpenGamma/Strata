/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

/**
 * Provides multiple values of an item of market data, one for each scenario.
 * <p>
 * In the simplest case a scenario market data value might be a list of values, one for each scenario.
 * This is handled by {@link ScenarioValuesList}.
 * <p>
 * There are two obvious reasons for creating implementations of this interface with special handling for
 * certain types of market data:
 * <ul>
 *   <li>Reducing memory usage</li>
 *   <li>Improving performance</li>
 * </ul>
 * For example, if the system stores multiple copies of a curve as a list it must store the x values with
 * each copy of the curve. This data is mostly redundant as the x values are the same in every scenario.
 * A custom data type for storing scenario data for a curve can store one set of x values shared between
 * all scenarios, reducing memory footprint.
 * <p>
 * When dealing with primitive data it is likely be more efficient to store the scenario values in a primitive
 * array instead of using a list. This removes the need for boxing and reduces memory footprint.
 * Also, if a function calculates values for all scenarios at the same time, it is likely to be more efficient
 * if the market data is stored in arrays as the values will be stored in a contiguous block of memory.
 * <p>
 * A scenario market data value is associated with the type of the single values of market data used in the
 * calculations and can return an instance of the single value for a specific scenario.
 * For example, a scenario value containing multiple curves
 *
 * @param <T>  the type of the single item of market data
 */
public interface ScenarioMarketDataValue<T> {

  /**
   * Returns a market data value for a scenario.
   *
   * @param scenarioIndex  the index of the scenario
   * @return the value of the market data for the specified scenario
   */
  public abstract T getValue(int scenarioIndex);

  /**
   * The number of scenarios for which this object contains data.
   *
   * @return the number of scenarios for which this object contains data
   */
  public abstract int getScenarioCount();
}
