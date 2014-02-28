/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.marketdatasnapshot.MarketDataSnapshotSource;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.engine.marketdata.spec.UserMarketDataSpecification;
import com.opengamma.sesame.ValuationTimeFn;
import com.opengamma.util.ArgumentChecker;

/**
 * Factory for {@link MarketDataFn} instances which use market data source from a user snapshot.
 */
public class UserSnapshotMarketDataFnFactory implements MarketDataFnFactory {

  private final ConfigSource _configSource;
  private final MarketDataSnapshotSource _snapshotSource;
  private final HistoricalTimeSeriesSource _historicalTimeSeriesSource;
  private final ValuationTimeFn _valuationTimeFn;
  private final String _currencyMatrixConfigName;
  
  public UserSnapshotMarketDataFnFactory(ConfigSource configSource, MarketDataSnapshotSource snapshotSource,
      HistoricalTimeSeriesSource historicalTimeSeriesSource, ValuationTimeFn valuationTimeFn, String currencyMatrixConfigName) {
    _configSource = configSource;
    _snapshotSource = snapshotSource;
    _historicalTimeSeriesSource = historicalTimeSeriesSource;
    _valuationTimeFn = valuationTimeFn;
    _currencyMatrixConfigName = currencyMatrixConfigName;
  }
  
  @Override
  public MarketDataFn create(MarketDataSpecification spec) {
    if (!(ArgumentChecker.notNull(spec, "spec") instanceof UserMarketDataSpecification)) {
      throw new IllegalArgumentException("Expected " + UserMarketDataSpecification.class + " but was " + spec.getClass());
    }
    UserMarketDataSpecification snapshotMarketDataSpec = (UserMarketDataSpecification) spec;
    RawMarketDataSource dataSource = new SnapshotRawMarketDataSource(_snapshotSource, snapshotMarketDataSpec.getUserSnapshotId(), _historicalTimeSeriesSource, _valuationTimeFn, "", "");
    return new EagerMarketDataFn(dataSource, _configSource, _currencyMatrixConfigName);
  }

}
