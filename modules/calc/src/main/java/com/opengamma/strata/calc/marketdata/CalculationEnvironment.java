/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A interface for looking up items of market data by ID, used when building market data.
 * <p>
 * The standard implementation is {@link MarketEnvironment}.
 */
public interface CalculationEnvironment {

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
   * Checks if this set of data contains a value for the specified ID.
   *
   * @param id  the ID identifying the item of market data
   * @return true if this set of data contains a value for the specified ID and it is of the expected type
   */
  public abstract boolean containsValue(MarketDataId<?> id);

  /**
   * Returns a box containing values for the specified ID if available.
   *
   * @param id  the ID identifying the item of market data
   * @return a box containing values for the specified ID if available
   */
  public abstract <T> Optional<MarketDataBox<T>> findValue(MarketDataId<T> id);

  /**
   * Gets a box that can provide an item of market data for a scenario.
   *
   * @param <T>  the type of the market data
   * @param id  the ID identifying the item of market data
   * @return the box providing access to the market data values for each scenario
   * @throws IllegalArgumentException if no value is found
   */
  public abstract <T> MarketDataBox<T> getValue(MarketDataId<T> id);

  /**
   * Gets a map of observable market data values for a set of IDs.
   * <p>
   * The return value is guaranteed to contain a value for every ID.
   * If any values are unavailable this method throws {@code IllegalArgumentException}.
   * 
   * @param <T>  the type of the observable values set, needed to avoid errors in javac
   * @param ids  market data IDs
   * @return a map of market data values for the IDs
   * @throws IllegalArgumentException if no value matches one or more of the IDs, or an error occurs
   */
  public default <T extends ObservableId> Map<ObservableId, MarketDataBox<Double>> getObservableValues(Set<T> ids) {
    Function<ObservableId, ObservableId> idMapper = id -> id;
    Function<ObservableId, MarketDataBox<Double>> valueMapper = id -> getValue(id);
    return ids.stream().collect(toImmutableMap(idMapper, valueMapper));
  }

  //-------------------------------------------------------------------------

  /**
   * Gets the time-series identified by the specified key, empty if not found.
   *
   * @param id  the ID identifying the item of market data
   * @return the time-series, empty if no time-series found
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries(ObservableId id);

  //-------------------------------------------------------------------------
  /**
   * Returns a {@code CalculationEnvironment} containing no data.
   *
   * @return a {@code CalculationEnvironment} containing no data
   */
  public static CalculationEnvironment empty() {
    return MarketEnvironment.empty();
  }
}
