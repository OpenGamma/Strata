/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.builders;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.marketdata.id.MarketDataVendor;
import com.opengamma.strata.marketdata.id.ObservableId;

/**
 * Time series provider that handles data that can't be looked up because there was no
 * market data rule for the calculation. It delegates to another provider for looking up time series.
 * <p>
 * When there is no market data rule for a calculation, the {@link ObservableId} instances for the time series
 * a have the vendor {@link MarketDataVendor#NO_RULE}. This builder creates failure results for those
 * IDs and uses the delegate provider to provider the time series for the remaining IDs.
 */
public final class DelegatingTimeSeriesProvider implements TimeSeriesProvider {

  /** Delegate provider used for looking up time series. */
  private final TimeSeriesProvider delegate;

  /**
   * @param delegate  delegate provider used for looking up time series
   */
  public DelegatingTimeSeriesProvider(TimeSeriesProvider delegate) {
    this.delegate = ArgChecker.notNull(delegate, "delegate");
  }

  @Override
  public Result<LocalDateDoubleTimeSeries> timeSeries(ObservableId id) {
    return id.getMarketDataVendor().equals(MarketDataVendor.NO_RULE) ?
        Result.failure(FailureReason.MISSING_DATA, "No market data rule specifying market data vendor for {}", id) :
        delegate.timeSeries(id);
  }
}
