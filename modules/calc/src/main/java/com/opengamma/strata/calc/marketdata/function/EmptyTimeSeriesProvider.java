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
 * Implementation of a time-series provider which returns an empty time series for any ID.
 * <p>
 * This is useful when calculations might require a time series and therefore request it but the
 * user knows that in the current case the time series data won't be used.
 */
class EmptyTimeSeriesProvider implements TimeSeriesProvider {

  /** The single, shared instance of this class. */
  static final EmptyTimeSeriesProvider INSTANCE = new EmptyTimeSeriesProvider();

  @Override
  public Result<LocalDateDoubleTimeSeries> timeSeries(ObservableId id) {
    return Result.success(LocalDateDoubleTimeSeries.empty());
  }

}
