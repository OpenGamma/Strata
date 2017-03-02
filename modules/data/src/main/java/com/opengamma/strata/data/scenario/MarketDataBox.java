/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.opengamma.strata.collect.function.ObjIntFunction;

/**
 * A box which can provide values for an item of market data used in scenarios.
 * <p>
 * A box can contain a single value for the data or it can contain multiple values, one for each scenario.
 * If it contains a single value then the same value is used in every scenario.
 * <p>
 * Wrapping the data in a box allows a simple interface for looking up market data that hides whether there
 * is one value or multiple values. Without the box every function that uses market data would have to
 * handle the two cases separately.
 * <p>
 * The box also takes care of transforming the market data when using it to build other market data values
 * (see the {@code apply} methods). This means that market data functions and perturbations don't need
 * different logic to deal with single and multiple values.
 * <p>
 * Using a box allows scenario data to be stored more efficiently in some cases. For example, curve data for
 * multiple scenarios can include one copy of the x-axis data which is used in all scenarios. If a separate
 * curve were stored for each scenario that data would be unnecessarily stored multiple times.
 * <p>
 * In some cases a function might need to access the data for all scenarios at the same time. For example, if
 * part of the calculation is the same for all scenarios it can be done once and reused instead of recalculated
 * for each scenario. In this case a {@link ScenarioMarketDataId} should be used to retrieve the scenario
 * value from the market data container.
 * 
 * @param <T>  the type of data held in the box
 */
public interface MarketDataBox<T> {

  /**
   * Obtains an instance containing a single market data value that is used in all scenarios.
   *
   * @param <T> the type of the market data value used in each scenario
   * @param singleValue  the market data value containing data for a single scenario
   * @return a box containing a single market data value that is used in all scenarios
   */
  public static <T> MarketDataBox<T> ofSingleValue(T singleValue) {
    return SingleMarketDataBox.of(singleValue);
  }

  /**
   * Obtains an instance containing a scenario market data value with data for multiple scenarios.
   * <p>
   * The market data is made up of multiple values, one for each scenario.
   * The {@link ScenarioArray} instance may provide optimized internal storage of these values.
   * <p>
   * A box may be created that contains a value for one scenario. Such a box is distinct from
   * a box created using {@link #ofSingleValue(Object)}, which is valid for any number of scenarios.
   *
   * @param <T> the type of the market data value used in each scenario
   * @param scenarioValue  the market data value containing data for multiple scenarios
   * @return a box containing a scenario market data value with data for multiple scenarios
   */
  public static <T> MarketDataBox<T> ofScenarioValue(ScenarioArray<T> scenarioValue) {
    return ScenarioMarketDataBox.of(scenarioValue);
  }

  /**
   * Obtains an instance containing a scenario market data value with data for multiple scenarios.
   * <p>
   * The market data is made up of multiple values, one for each scenario.
   * <p>
   * A box may be created that contains a value for one scenario. Such a box is distinct from
   * a box created using {@link #ofSingleValue(Object)}, which is valid for any number of scenarios.
   *
   * @param <T> the type of the market data value used in each scenario
   * @param scenarioValues  the market data values for each scenario
   * @return a box containing a scenario market data value with data for multiple scenarios
   */
  @SafeVarargs
  public static <T> MarketDataBox<T> ofScenarioValues(T... scenarioValues) {
    return ScenarioMarketDataBox.of(scenarioValues);
  }

  /**
   * Obtains an instance containing a scenario market data value with data for multiple scenarios.
   * <p>
   * The market data is made up of multiple values, one for each scenario.
   * <p>
   * A box may be created that contains a value for one scenario. Such a box is distinct from
   * a box created using {@link #ofSingleValue(Object)}, which is valid for any number of scenarios.
   *
   * @param <T> the type of the market data value used in each scenario
   * @param scenarioValues  the market data values for each scenario
   * @return a box containing a scenario market data value with data for multiple scenarios
   */
  public static <T> MarketDataBox<T> ofScenarioValues(List<T> scenarioValues) {
    return ScenarioMarketDataBox.of(scenarioValues);
  }

  /**
   * Obtains an instance containing no market data.
   *
   * @param <T> the type of the market data value used in each scenario
   * @return a box containing no market data
   */
  public static <T> MarketDataBox<T> empty() {
    return EmptyMarketDataBox.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the single market data value used for all scenarios if available.
   * <p>
   * If this box contains data for multiple scenarios an exception is thrown.
   * <p>
   * This method should only be called if {@link #isSingleValue()} returns {@code true}
   * or {@link #isScenarioValue()} return {@code false}.
   *
   * @return the single market data value used for all scenarios if available
   * @throws UnsupportedOperationException if this box contains data for multiple scenarios
   */
  public abstract T getSingleValue();

  /**
   * Gets the market data value containing data for multiple scenarios.
   * <p>
   * If this box contains data for a single scenario an exception is thrown.
   * <p>
   * This method should only be called if {@link #isSingleValue()} returns {@code false}
   * or {@link #isScenarioValue()} return {@code true}.
   *
   * @return the market data value containing data for multiple scenarios
   * @throws UnsupportedOperationException if this box contains data for a single scenario
   */
  public abstract ScenarioArray<T> getScenarioValue();

  /**
   * Gets the market data value associated with the specified scenario.
   *
   * @param scenarioIndex  the index of the scenario
   * @return the market data value associated with the scenario
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public abstract T getValue(int scenarioIndex);

  //-------------------------------------------------------------------------
  /**
   * Checks if this box contains a single market data value that is used for all scenarios.
   *
   * @return true if this box contains a single market data value that is used for all scenarios
   */
  public abstract boolean isSingleValue();

  /**
   * Checks if this box contains market data for multiple scenarios.
   *
   * @return true if this box contains market data for multiple scenarios
   */
  public default boolean isScenarioValue() {
    return !isSingleValue();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the number of scenarios for which this box contains data.
   * <p>
   * A "single value" box can be used with any number of scenarios.
   * To indicate this, the method will return -1.
   *
   * @return the number of scenarios for which this box contains data, -1 if the number is not specified
   */
  public abstract int getScenarioCount();

  /**
   * Gets the type of the market data value used in each scenario.
   *
   * @return the type of the market data value used in each scenario
   */
  public abstract Class<?> getMarketDataType();

  //-------------------------------------------------------------------------
  /**
   * Applies a function to the contents of the box and returns another box.
   * <p>
   * The box implementation takes care of checking whether it contains a single value or a scenario value,
   * applying the function to the value for each scenario and packing the return value into a box.
   * <p>
   * This is primarily intended for use by market data factories which might receive single values or
   * scenario values from upstream market data factories.
   *
   * @param <R>  the return type of the function
   * @param fn  a function to apply to the market data in the box
   * @return a result wrapping a box containing the return value of the function
   */
  public abstract <R> MarketDataBox<R> map(Function<T, R> fn);

  /**
   * Applies a function to the contents of the box once for each scenario and returns a box containing
   * the values returned from the function.
   * <p>
   * The {@link #getScenarioCount() scenario count} of the box must be one or it must be equal to the
   * {@code scenarioCount} argument. The {@link #getScenarioCount() scenario count} of the return value
   * will be equal to {@code scenarioCount}.
   * <p>
   * The box implementation takes care of checking whether it contains a single value or a scenario value,
   * applying the function to the value for each scenario and packing the return values into a box.
   * <p>
   * This is primarily intended to be used by perturbations which generate separate market data values for
   * each scenario data by applying a function to the existing value for the scenario.
   *
   * @param scenarioCount  the total number of scenarios
   * @param fn  the function that is invoked with a scenario index and the market data value for that scenario.
   *   The return value is used as the scenario data in the returned box
   * @param <R>  the type of the returned market data
   * @return a box containing market data created by applying the function to the contents of this box
   */
  public abstract <R> MarketDataBox<R> mapWithIndex(int scenarioCount, ObjIntFunction<T, R> fn);

  /**
   * Applies a function to the market data in this box and another box and returns a box containing the result.
   * <p>
   * The box implementation takes care of checking whether the input boxes contain single values or a scenario values,
   * applying the function to the value for each scenario and packing the return value into a box.
   * <p>
   * This is primarily intended for use by market data factories which might receive single values or
   * scenario values from upstream market data factories.
   *
   * @param <U>  the type of market data in the other box
   * @param <R>  the type of the market data returned in the result of the function
   * @param other  another market data box
   * @param fn  the function invoked with the market data from each box. The return value is used to build the data
   *   in the returned box
   * @return a box containing market data created by applying the function to the data in this box and another box
   */
  public abstract <U, R> MarketDataBox<R> combineWith(MarketDataBox<U> other, BiFunction<T, U, R> fn);

  /**
   * Returns a stream over the contents of the box.
   *
   * @return a stream over the contents of the box
   */
  public Stream<T> stream();

}
