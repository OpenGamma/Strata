/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataNotFoundException;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ScenarioMarketDataId;
import com.opengamma.strata.basics.market.ScenarioMarketDataValue;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Provides access to market data across one or more scenarios.
 * <p>
 * Market data is looked up using subclasses of {@link MarketDataId}.
 * All data is valid for a single date, defined by {@link #getValuationDate()}.
 * <p>
 * There are two ways to access the available market data.
 * <p>
 * The first way is to use the access methods on this interface that return the data
 * associated with a single identifier for all scenarios. The two key methods are
 * {@link #getValue(MarketDataId)} and {@link #getScenarioValue(ScenarioMarketDataId)}.
 * <p>
 * The second way is to use the method {@link #scenarios()} or {@link #scenario(int)}.
 * These return all the data associated with a single scenario.
 * This approach is convenient for single scenario pricers.
 * <p>
 * The standard implementation is {@link ImmutableScenarioMarketData}.
 */
public interface ScenarioMarketData {

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a valuation date, map of values and time-series.
   * <p>
   * The valuation date and map of values must have the same number of scenarios.
   * 
   * @param scenarioCount  the number of scenarios
   * @param valuationDate  the valuation dates associated with all scenarios
   * @param values  the market data values, one for each scenario
   * @param timeSeries  the time-series
   * @return a set of market data containing the values in the map
   */
  public static ScenarioMarketData of(
      int scenarioCount,
      LocalDate valuationDate,
      Map<? extends MarketDataId<?>, MarketDataBox<?>> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {

    return of(scenarioCount, MarketDataBox.ofSingleValue(valuationDate), values, timeSeries);
  }

  /**
   * Obtains an instance from a valuation date, map of values and time-series.
   * <p>
   * The valuation date and map of values must have the same number of scenarios.
   * 
   * @param scenarioCount  the number of scenarios
   * @param valuationDate  the valuation dates associated with the market data, one for each scenario
   * @param values  the market data values, one for each scenario
   * @param timeSeries  the time-series
   * @return a set of market data containing the values in the map
   */
  public static ScenarioMarketData of(
      int scenarioCount,
      MarketDataBox<LocalDate> valuationDate,
      Map<? extends MarketDataId<?>, MarketDataBox<?>> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {

    return ImmutableScenarioMarketData.of(scenarioCount, valuationDate, values, timeSeries);
  }

  /**
   * Obtains a market data instance that contains no data and has no scenarios.
   *
   * @return an empty instance
   */
  public static ScenarioMarketData empty() {
    return ImmutableScenarioMarketData.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets a box that can provide the valuation date of each scenario.
   *
   * @return the valuation dates of the scenarios
   */
  public abstract MarketDataBox<LocalDate> getValuationDate();

  /**
   * Gets the number of scenarios.
   *
   * @return the number of scenarios
   */
  public abstract int getScenarioCount();

  //-------------------------------------------------------------------------
  /**
   * Returns a stream of market data, one for each scenario.
   * <p>
   * The stream will return instances of {@link MarketData}, where each represents
   * a single scenario view of the complete set of data.
   *
   * @return the stream of market data, one for the each scenario
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public default Stream<MarketData> scenarios() {
    return IntStream.range(0, getScenarioCount())
        .mapToObj(scenarioIndex -> SingleScenarioMarketData.of(this, scenarioIndex));
  }

  /**
   * Returns market data for a single scenario.
   * <p>
   * This returns a view of the market data for the single specified scenario.
   *
   * @param scenarioIndex  the scenario index
   * @return the market data for the specified scenario
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public default MarketData scenario(int scenarioIndex) {
    return SingleScenarioMarketData.of(this, scenarioIndex);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this market data contains a value for the specified identifier.
   *
   * @param id  the identifier to find
   * @return true if the market data contains a value for the identifier
   */
  public default boolean containsValue(MarketDataId<?> id) {
    return findValue(id).isPresent();
  }

  /**
   * Gets the market data value associated with the specified identifier.
   * <p>
   * The result is a box that provides data for all scenarios.
   * If this market data instance contains the identifier, the value will be returned.
   * Otherwise, an exception will be thrown.
   *
   * @param <T>  the type of the market data value
   * @param id  the identifier to find
   * @return the market data value box providing data for all scenarios
   * @throws MarketDataNotFoundException if the identifier is not found
   */
  public default <T> MarketDataBox<T> getValue(MarketDataId<T> id) {
    return findValue(id)
        .orElseThrow(() -> new MarketDataNotFoundException(Messages.format(
            "Market data not found for '{}' of type '{}'", id, id.getClass().getSimpleName())));
  }

  /**
   * Finds the market data value associated with the specified identifier.
   * <p>
   * The result is a box that provides data for all scenarios.
   * If this market data instance contains the identifier, the value will be returned.
   * Otherwise, an empty optional will be returned.
   *
   * @param <T>  the type of the market data value
   * @param id  the identifier to find
   * @return the market data value box providing data for all scenarios, empty if not found
   */
  public abstract <T> Optional<MarketDataBox<T>> findValue(MarketDataId<T> id);

  //-------------------------------------------------------------------------
  /**
   * Gets an object containing market data for multiple scenarios.
   * <p>
   * There are many possible ways to store scenario market data for a data type. For example, if the single
   * values are doubles, the scenario value might simply be a {@code List<Double>} or it might be a wrapper
   * class that stores the values more efficiently in a {@code double[]}.
   * <p>
   * If the market data contains a single value for the identifier or a scenario value of the wrong type,
   * a value of the required type is created by invoking {@link ScenarioMarketDataId#createScenarioValue}.
   * <p>
   * Normally this should not be necessary. It is assumed the required scenario values will be created by the
   * perturbations that create scenario data. However there is no mechanism in the market data system to guarantee
   * that scenario values of a particular type are available. If they are not they are created on demand.
   * <p>
   * Values returned from this method might be cached for efficiency.
   *
   * @param id  the identifier to find
   * @param <T>  the type of the individual market data values used when performing calculations for one scenario
   * @param <U>  the type of the object containing the market data for all scenarios
   * @return an object containing market data for multiple scenarios
   * @throws IllegalArgumentException if no value is found
   */
  @SuppressWarnings("unchecked")
  public default <T, U extends ScenarioMarketDataValue<T>> U getScenarioValue(ScenarioMarketDataId<T, U> id) {
    MarketDataBox<T> box = getValue(id.getMarketDataId());

    if (box.isSingleValue()) {
      return id.createScenarioValue(box, getScenarioCount());
    }
    ScenarioMarketDataValue<T> scenarioValue = box.getScenarioValue();
    if (id.getScenarioMarketDataType().isInstance(scenarioValue)) {
      return (U) scenarioValue;
    }
    return id.createScenarioValue(box, getScenarioCount());
  }

  /**
   * Returns set of market data which combines the data from this set of data with another set.
   * <p>
   * If the same item of data is available in both sets, it will be taken from this set.
   * <p>
   * Both sets of data must contain the same number of scenarios, or one of them must have one scenario.
   * If one of the sets of data has one scenario, the combined set will have the scenario count
   * of the other set.
   *
   * @param other  another set of market data
   * @return a set of market data combining the data in this set with the data in the other
   */
  public default ScenarioMarketData combinedWith(ScenarioMarketData other) {
    return new CombinedScenarioMarketData(this, other);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the time-series associated with the specified identifier, empty if not found.
   * <p>
   * Time series are not affected by scenarios, therefore there is a single time-series
   * for each identifier which is shared between all scenarios.
   *
   * @param id  the identifier to find
   * @return the time-series, empty if no time-series found
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries(ObservableId id);

}
