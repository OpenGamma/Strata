/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.function;

import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Implementation of a time-series provider which always returns missing data failures.
 * <p>
 * This is designed to be used when it is not necessary to source time-series on-demand,
 * for example because all required market data is expected to be present in a snapshot.
 */
public class EmptyTimeSeriesProvider implements TimeSeriesProvider {

  /** The single, shared instance of this class. */
  static final EmptyTimeSeriesProvider INSTANCE = new EmptyTimeSeriesProvider();
  
  @Override
  public Result<LocalDateDoubleTimeSeries> timeSeries(ObservableId id) {
    return Result.failure(FailureReason.MISSING_DATA, "No time-series provider configured", id);
  }

}
