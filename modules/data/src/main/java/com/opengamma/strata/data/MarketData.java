/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Provides access to market data, such as curves, surfaces and time-series.
 * <p>
 * Market data is looked up using subclasses of {@link MarketDataId}.
 * All data is valid for a single date, defined by {@link #getValuationDate()}.
 * When performing calculations with scenarios, only the data of a single scenario is accessible.
 * <p>
 * The standard implementation is {@link ImmutableMarketData}.
 */
public interface MarketData {

  /**
   * Obtains an instance from a valuation date and map of values.
   * <p>
   * Each entry in the map is a single piece of market data, keyed by the matching identifier.
   * For example, an {@link FxRate} can be looked up using an {@link FxRateId}.
   * The caller must ensure that the each entry in the map corresponds with the parameterized
   * type on the identifier.
   *
   * @param valuationDate  the valuation date of the market data
   * @param values  the market data values
   * @return the market data instance containing the values in the map
   * @throws ClassCastException if a value does not match the parameterized type associated with the identifier
   */
  public static MarketData of(LocalDate valuationDate, Map<? extends MarketDataId<?>, ?> values) {
    return ImmutableMarketData.of(valuationDate, values);
  }

  /**
   * Obtains an instance from a valuation date, map of values and time-series.
   *
   * @param valuationDate  the valuation date of the market data
   * @param values  the market data values
   * @param timeSeries  the time-series
   * @return the market data instance containing the values in the map and the time-series
   * @throws ClassCastException if a value does not match the parameterized type associated with the identifier
   */
  public static MarketData of(
      LocalDate valuationDate,
      Map<? extends MarketDataId<?>, ?> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {

    return ImmutableMarketData.builder(valuationDate).values(values).timeSeries(timeSeries).build();
  }

  /**
   * Obtains an instance containing no market data.
   *
   * @param valuationDate  the valuation date of the market data
   * @return empty market data
   */
  public static MarketData empty(LocalDate valuationDate) {
    return ImmutableMarketData.builder(valuationDate).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the valuation date of the market data.
   * <p>
   * All values accessible through this interface have the same valuation date.
   * 
   * @return the valuation date
   */
  public abstract LocalDate getValuationDate();

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
   * If this market data instance contains the identifier, the value will be returned.
   * Otherwise, an exception will be thrown.
   *
   * @param <T>  the type of the market data value
   * @param id  the identifier to find
   * @return the market data value
   * @throws MarketDataNotFoundException if the identifier is not found
   */
  public default <T> T getValue(MarketDataId<T> id) {
    return findValue(id)
        .orElseThrow(() -> new MarketDataNotFoundException(Messages.format(
            "Market data not found for '{}' of type '{}'", id, id.getClass().getSimpleName())));
  }

  /**
   * Finds the market data value associated with the specified identifier.
   * <p>
   * If this market data instance contains the identifier, the value will be returned.
   * Otherwise, an empty optional will be returned.
   *
   * @param <T>  the type of the market data value
   * @param id  the identifier to find
   * @return the market data value, empty if not found
   */
  public abstract <T> Optional<T> findValue(MarketDataId<T> id);

  //-------------------------------------------------------------------------
  /**
   * Gets the market data identifiers.
   *
   * @return the set of market data identifiers
   */
  public abstract Set<MarketDataId<?>> getIds();

  /**
   * Finds the market data identifiers associated with the specified name.
   * <p>
   * This returns the unique identifiers that refer to the specified name.
   * There may be more than one identifier associated with a name as the name is not unique.
   *
   * @param <T>  the type of the market data value
   * @param name  the name to find
   * @return the set of market data identifiers, empty if name not found
   */
  public abstract <T> Set<MarketDataId<T>> findIds(MarketDataName<T> name);

  //-------------------------------------------------------------------------
  /**
   * Gets the time-series identifiers.
   *
   * @return the set of observable identifiers
   */
  public abstract Set<ObservableId> getTimeSeriesIds();

  /**
   * Gets the time-series identified by the specified identifier, empty if not found.
   *
   * @param id  the identifier to find
   * @return the time-series, empty if no time-series found
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries(ObservableId id);

  //-------------------------------------------------------------------------
  /**
   * Combines this market data with another.
   * <p>
   * The result combines both sets of market data.
   * Values are taken from this set of market data if available, otherwise they are taken
   * from the other set.
   * <p>
   * The valuation dates of the sets of market data must be the same.
   *
   * @param other  the other market data
   * @return the combined market data
   */
  public default MarketData combinedWith(MarketData other) {
    return new CombinedMarketData(this, other);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this market data with the specified value.
   * <p>
   * When the result is queried for the specified identifier, the specified value will be returned.
   * <p>
   * For example, this method could be used to replace a curve with a bumped curve.
   *
   * @param <T>  the type of the market data value
   * @param id  the identifier to find
   * @param value  the value to associate with the identifier
   * @return the derived market data with the specified identifier and value
   */
  public default <T> MarketData withValue(MarketDataId<T> id, T value) {
    return ExtendedMarketData.of(id, value, this);
  }

}
