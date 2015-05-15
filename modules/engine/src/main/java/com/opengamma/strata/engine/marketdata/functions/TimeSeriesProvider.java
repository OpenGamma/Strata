/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.functions;

import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

// TODO Once there's a TimeSeriesSource this will probably be redundant
/**
 * A source of time series of observable market data.
 */
public interface TimeSeriesProvider {

  /**
   * Returns a time series of market data for the specified ID.
   *
   * @param id  the ID of the market data in the time series
   * @return a time series of market data for the specified ID
   * @throws IllegalArgumentException if there is no time series available for the specified ID
   */
  public abstract Result<LocalDateDoubleTimeSeries> timeSeries(ObservableId id);
}
