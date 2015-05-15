/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.marketdata.functions.TimeSeriesProvider;

/**
 * Time-series provider that obtains a time-series of an observable value from a JSON resource.
 * <p>
 * The time-series must be available as a resource with a name of the form
 * <code>/timeseries/[id].json</code>, where <code>[id]</code> is the identifier
 * of the observable.
 */
public class ExampleTimeSeriesProvider
    implements TimeSeriesProvider {

  @Override
  public Result<LocalDateDoubleTimeSeries> timeSeries(ObservableId id) {
    return Result.success(ExampleMarketData.loadTimeSeries(id));
  }

}
