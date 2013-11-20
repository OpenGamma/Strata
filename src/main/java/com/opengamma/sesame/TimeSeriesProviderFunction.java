/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import java.util.Set;

import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.time.LocalDateRange;

/**
 * TODO experimental implementation to see how it feels
 */
public class TimeSeriesProviderFunction implements MarketDataSeriesProviderFunction {

  private final HistoricalTimeSeriesSource _timeSeriesSource;
  private final String _dataSource;
  private final String _dataProvider;

  public TimeSeriesProviderFunction(HistoricalTimeSeriesSource timeSeriesSource, String dataSource, String dataProvider) {
    _timeSeriesSource = ArgumentChecker.notNull(timeSeriesSource, "timeSeriesSource");
    _dataSource = ArgumentChecker.notEmpty(dataSource, "dataSource");
    _dataProvider = ArgumentChecker.notEmpty(dataProvider, "dataProvider");
  }

  @Override
  public MarketDataFunctionResult requestData(MarketDataRequirement requirement, LocalDateRange range) {
    // TODO implement requestData()
    throw new UnsupportedOperationException("requestData not implemented");
  }

  @Override
  public MarketDataFunctionResult requestData(Set<MarketDataRequirement> requirements, LocalDateRange range) {
    // TODO implement requestData()
    throw new UnsupportedOperationException("requestData not implemented");
  }
}
