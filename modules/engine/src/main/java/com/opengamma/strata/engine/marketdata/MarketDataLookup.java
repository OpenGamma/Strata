/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.time.LocalDate;

import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A interface for looking up items of market data by ID, used when building market data.
 */
public interface MarketDataLookup {

  /**
   * Checks if this set of data contains a value for the specified ID and it is of the expected type.
   *
   * @param id  an ID identifying an item of market data
   * @return true if this set of data contains a value for the specified ID and it is of the expected type
   */
  public abstract boolean containsValue(MarketDataId<?> id);

  /**
   * Returns a market data value.
   * <p>
   * The date of the market data is the same as the valuation date of the calculations.
   *
   * @param id  ID of the market data
   * @param <T>  type of the market data
   * @param <I>  type of the market data ID
   * @return a market data value
   * @throws IllegalArgumentException if there is no value for the specified ID
   */
  @SuppressWarnings("unchecked")
  public abstract <T, I extends MarketDataId<T>> T getValue(I id);

  /**
   * Checks if this set of data contains a time series for the specified market data ID.
   *
   * @param id  an ID identifying an item of market data
   * @return true if this set of data contains a time series for the specified market data ID
   */
  public abstract boolean containsTimeSeries(ObservableId id);

  /**
   * Returns a time series of market data values.
   *
   * @param id  ID of the market data
   * @return a time series of market data values
   * @throws IllegalArgumentException if there is no time series for the specified ID
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries(ObservableId id);

  /**
   * Returns the valuation date of the market data.
   *
   * @return the valuation date of the market data
   */
  public LocalDate getValuationDate();
}
