package com.opengamma.strata.examples.exampleccp.uselessboilerplate;

import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.marketdata.functions.TimeSeriesProvider;

public class MyUselessTimeSeriesProvider {

  public static TimeSeriesProvider create() {
    return id -> Result.success(
        LocalDateDoubleTimeSeries.empty()
    );
  }
}
