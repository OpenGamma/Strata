/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import javax.annotation.Nullable;

import org.threeten.bp.LocalDate;

import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.engine.marketdata.spec.FixedHistoricalMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.util.ArgumentChecker;

/**
 * Factory for {@link MarketDataFn} instances which use historical data from a fixed date.
 */
public class FixedHistoricalMarketDataFactory implements MarketDataFactory {

  private final HistoricalTimeSeriesSource _historicalTimeSeriesSource;
  private final String _dataSource;
  private final String _dataProvider;

  public FixedHistoricalMarketDataFactory(HistoricalTimeSeriesSource historicalTimeSeriesSource,
                                          String dataSource,
                                          @Nullable String dataProvider) {
    _historicalTimeSeriesSource = ArgumentChecker.notNull(historicalTimeSeriesSource, "historicalTimeSeriesSource");
    _dataSource = ArgumentChecker.notEmpty(dataSource, "dataSource");
    _dataProvider = dataProvider;
  }
  
  @Override
  public MarketDataSource create(MarketDataSpecification spec) {
    if (!(ArgumentChecker.notNull(spec, "spec") instanceof FixedHistoricalMarketDataSpecification)) {
      throw new IllegalArgumentException("Expected " + FixedHistoricalMarketDataSpecification.class + " but was " + spec.getClass());
    }
    FixedHistoricalMarketDataSpecification historicalSpec = (FixedHistoricalMarketDataSpecification) spec;
    LocalDate snapshotDate = historicalSpec.getSnapshotDate();
    return new FixedHistoricalMarketDataSource(_historicalTimeSeriesSource, snapshotDate, _dataSource, _dataProvider);
  }
}
