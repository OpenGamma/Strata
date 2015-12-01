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

import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A interface for looking up items of market data by ID, used when building market data.
 */
public interface CalculationEnvironment {

  /**
   * Checks if this set of data contains a value for the specified ID.
   *
   * @param id  an ID identifying an item of market data
   * @return true if this set of data contains a value for the specified ID and it is of the expected type
   */
  public abstract boolean containsValue(MarketDataId<?> id);

  /**
   * Checks if this set of data contains a time series for the specified market data ID.
   *
   * @param id  an ID identifying an item of market data
   * @return true if this set of data contains a time series for the specified market data ID
   */
  public abstract boolean containsTimeSeries(ObservableId id);

  /**
   * Returns the number of scenarios for which market data is available.
   *
   * @return the number of scenarios for which market data is available
   */
  public abstract int getScenarioCount();

  /**
   * Returns a market data value.
   * <p>
   * The date of the market data is the same as the valuation date of the calculations.
   *
   * @param <T>  type of the market data
   * @param id  ID of the market data
   * @return a market data value
   * @throws IllegalArgumentException if there is no value for the specified ID
   */
  public abstract <T> MarketDataBox<T> getValue(MarketDataId<T> id);

  /**
   * Returns a time series of market data values.
   *
   * @param id  ID of the market data
   * @return a time series of market data values
   * @throws IllegalArgumentException if there is no time series for the specified ID
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries(ObservableId id);

  /**
   * Returns a map of observable market data values for a set of IDs.
   * <p>
   * The return value is guaranteed to contain a value for every ID. If any values are unavailable this
   * method throws {@code IllegalArgumentException}.
   *
   * @param ids  market data IDs
   * @return a map of market data values for the IDs
   * @throws IllegalArgumentException if there is no value for any of the IDs
   */
  public default Map<ObservableId, MarketDataBox<Double>> getObservableValues(Set<? extends ObservableId> ids) {
    Function<ObservableId, ObservableId> idMapper = id -> id;
    Function<ObservableId, MarketDataBox<Double>> valueMapper = id -> getValue(id);
    return ids.stream().collect(toImmutableMap(idMapper, valueMapper));
  }

  /**
   * Returns the valuation date of the market data.
   *
   * @return the valuation date of the market data
   */
  public abstract MarketDataBox<LocalDate> getValuationDate();

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
