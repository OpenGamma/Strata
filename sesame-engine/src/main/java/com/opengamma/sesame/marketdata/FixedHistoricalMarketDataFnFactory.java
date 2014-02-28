/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.engine.marketdata.spec.FixedHistoricalMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.util.ArgumentChecker;

/**
 * Factory for {@link MarketDataFn} instances which use historical data from a fixed date.
 */
public class FixedHistoricalMarketDataFnFactory implements MarketDataFnFactory {

  private final ConfigSource _configSource;
  private final HistoricalTimeSeriesSource _historicalTimeSeriesSource;
  private final String _currencyMatrixConfigName;
  private final String _dataSource;
  private final String _dataProvider;
  
  public FixedHistoricalMarketDataFnFactory(ConfigSource configSource, HistoricalTimeSeriesSource historicalTimeSeriesSource, String currencyMatrixConfigName, String dataSource, String dataProvider) {
    _configSource = ArgumentChecker.notNull(configSource, "configSource");
    _historicalTimeSeriesSource = ArgumentChecker.notNull(historicalTimeSeriesSource, "historicalTimeSeriesSource");
    _currencyMatrixConfigName = ArgumentChecker.notNull(currencyMatrixConfigName, "currencyMatrixConfigName");
    _dataSource = "null".equals(dataSource) ? null : dataSource;
    _dataProvider = "null".equals(dataProvider) ? null : dataProvider;
  }
  
  @Override
  public MarketDataFn create(MarketDataSpecification spec) {
    if (!(ArgumentChecker.notNull(spec, "spec") instanceof FixedHistoricalMarketDataSpecification)) {
      throw new IllegalArgumentException("Expected " + FixedHistoricalMarketDataSpecification.class + " but was " + spec.getClass());
    }
    FixedHistoricalMarketDataSpecification fixedHistoricalMarketDataSpec = (FixedHistoricalMarketDataSpecification) spec;
    RawMarketDataSource dataSource = new HistoricalRawMarketDataSource(_historicalTimeSeriesSource, fixedHistoricalMarketDataSpec.getSnapshotDate(), _dataSource, _dataProvider);
    return new EagerMarketDataFn(dataSource, _configSource, _currencyMatrixConfigName);
  }

}
