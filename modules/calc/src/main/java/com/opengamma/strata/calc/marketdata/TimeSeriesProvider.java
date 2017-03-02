/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.ObservableId;

/**
 * A provider of time-series.
 * <p>
 * This plugin point allows a market data supplier of time-series to be provided.
 */
public interface TimeSeriesProvider {

  /**
   * Returns a time-series provider that is unable to source any time-series.
   * <p>
   * All requests for a time-series will return a failure.
   * This is used to validate that no time-series have been requested that were not
   * already supplied in the input to the market data factory.
   *
   * @return the time-series provider
   */
  public static TimeSeriesProvider none() {
    return NoTimeSeriesProvider.INSTANCE;
  }

  /**
   * Returns a time-series provider that returns an empty time-series for any ID.
   * <p>
   * All requests for a time-series will succeed, returning an empty time-series.
   * This is used for those cases where time-series are considered optional.
   *
   * @return the time-series provider
   */
  public static TimeSeriesProvider empty() {
    return EmptyTimeSeriesProvider.INSTANCE;
  }

  //-------------------------------------------------------------------------
  /**
   * Provides the time-series for the specified identifier.
   * <p>
   * The implementation will provide a time-series for the identifier, returning
   * a failure if unable to do so.
   *
   * @param identifier  the market data identifier to find
   * @return the time-series of market data for the specified identifier
   */
  public abstract Result<LocalDateDoubleTimeSeries> provideTimeSeries(ObservableId identifier);

}
