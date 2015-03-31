/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine;

import java.util.stream.Stream;

/**
 * Holds the multiple results created when a calculation is run
 * with a set of scenarios.
 * <p>
 * The number of results returned will depend on how the scenarios are defined.
 * 
 * @param <T>  the type of each of the calculation results.
 */
public interface ScenarioResults<T> {

  /**
   * Returns the number of results.
   *
   * @return the number of results
   */
  public abstract int size();

  /**
   * Get the result held at a particular index.
   * <p>
   * The specified index must be greater than or equal to zero and less than the value
   * returned by {@link #size()}.
   *
   * @param index  index of the result to retrieve
   * @return the result
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public abstract T getItem(int index);

  /**
   * Returns a stream containing all the scenario results.
   *
   * @return a stream containing all the scenario results
   */
  public abstract Stream<T> stream();

}
