/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.function;

import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A source of time series of observable market data.
 */
public interface TimeSeriesProvider {

  /**
   * Returns a time-series provider that is unable to source any time-series.
   * <p>
   * This is useful when it is not necessary for the engine to source time-series on-demand, for
   * example because all market data is being provided in a snapshot.
   *
   * @return the time-series provider
   */
  public static TimeSeriesProvider none() {
    return NoTimeSeriesProvider.INSTANCE;
  }

  /**
   * Returns a time-series provider that returns an empty time series for any ID.
   * <p>
   * This is useful when calculations might require a time series and therefore request it but the
   * user knows that in the current case the time series data won't be used.
   *
   * @return the time-series provider
   */
  public static TimeSeriesProvider empty() {
    return EmptyTimeSeriesProvider.INSTANCE;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a time series of market data for the specified ID.
   *
   * @param id  the ID of the market data in the time series
   * @return a time series of market data for the specified ID
   * @throws IllegalArgumentException if there is no time series available for the specified ID
   */
  public abstract Result<LocalDateDoubleTimeSeries> timeSeries(ObservableId id);

}
