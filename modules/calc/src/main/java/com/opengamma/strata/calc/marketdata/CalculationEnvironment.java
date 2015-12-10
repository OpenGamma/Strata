/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A interface for looking up items of market data by ID, used when building market data.
 */
public interface CalculationEnvironment {

  /**
   * Gets the valuation dates of the scenarios, one for each scenario.
   *
   * @return the valuation dates of the scenarios, one for each scenario
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
   * Checks if this set of data contains a value for the specified ID.
   *
   * @param <T>  the type of the market data
   * @param id  the ID identifying the item of market data
   * @return true if this set of data contains a value for the specified ID and it is of the expected type
   */
  public abstract <T> boolean containsValue(MarketDataId<T> id);

  /**
   * Gets a box that can provide an item of market data for a scenario.
   * <p>
   * The market data is valid for the valuation date.
   *
   * @param <T>  the type of the market data
   * @param id  the ID identifying the item of market data
   * @return the box providing access to the market data values for each scenario
   * @throws IllegalArgumentException if no value is found
   * @throws RuntimeException if an unexpected error occurs
   */
  public abstract <T> MarketDataBox<T> getValue(MarketDataId<T> id);

  /**
   * Gets a map of observable market data values for a set of IDs.
   * <p>
   * The return value is guaranteed to contain a value for every ID.
   * If any values are unavailable this method throws {@code IllegalArgumentException}.
   *
   * @param ids  market data IDs
   * @return a map of market data values for the IDs
   * @throws IllegalArgumentException if no value matches one or more of the IDs, or an error occurs
   */
  public default Map<ObservableId, MarketDataBox<Double>> getObservableValues(Set<? extends ObservableId> ids) {
    Function<ObservableId, ObservableId> idMapper = id -> id;
    Function<ObservableId, MarketDataBox<Double>> valueMapper = id -> getValue(id);
    return ids.stream().collect(toImmutableMap(idMapper, valueMapper));
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this set of data contains a time-series for the specified ID.
   *
   * @param id  the ID identifying the item of market data
   * @return true if this set of data contains a time-series for the specified ID
   */
  public abstract boolean containsTimeSeries(ObservableId id);

  /**
   * Gets the time-series identified by the specified key, empty if not found.
   *
   * @param id  the ID identifying the item of market data
   * @return the time-series, empty if no time-series found
   * @throws RuntimeException if an unexpected error occurs
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries(ObservableId id);

  //-------------------------------------------------------------------------
  /**
   * Returns a mutable builder for building a {@code CalculationEnvironment}.
   *
   * @return a mutable builder for building a {@code CalculationEnvironment}
   */
  public static MarketEnvironmentBuilder builder() {
    return MarketEnvironment.builder();
  }

  /**
   * Returns a {@code CalculationEnvironment} containing no data.
   *
   * @return a {@code CalculationEnvironment} containing no data
   */
  public static CalculationEnvironment empty() {
    return MarketEnvironment.empty();
  }
}
