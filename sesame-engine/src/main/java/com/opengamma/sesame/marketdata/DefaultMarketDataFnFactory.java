/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import org.threeten.bp.LocalDate;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.marketdatasnapshot.MarketDataSnapshotSource;
import com.opengamma.engine.marketdata.spec.FixedHistoricalMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.LiveMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.engine.marketdata.spec.UserMarketDataSpecification;
import com.opengamma.financial.currency.CurrencyMatrixConfigPopulator;
import com.opengamma.id.UniqueId;
import com.opengamma.livedata.LiveDataClient;
import com.opengamma.sesame.ValuationTimeFn;

/**
 * 
 */
public class DefaultMarketDataFnFactory implements MarketDataFnFactory {

  private final ConfigSource _configSource;
  private final HistoricalTimeSeriesSource _historicalTimeSeriesSource;
  private final ValuationTimeFn _valuationTimeFn;
  private final MarketDataSnapshotSource _snapshotSource;
  private final LiveDataClient _liveDataClient;
  
  public DefaultMarketDataFnFactory(ConfigSource configSource, HistoricalTimeSeriesSource historicalTimeSeriesSource,
      ValuationTimeFn valuationTimeFn, MarketDataSnapshotSource snapshotSource, LiveDataClient liveDataClient) {
    _configSource = configSource;
    _historicalTimeSeriesSource = historicalTimeSeriesSource;
    _valuationTimeFn = valuationTimeFn;
    _snapshotSource = snapshotSource;
    _liveDataClient = liveDataClient;
  }

  @Override
  public MarketDataFn create(MarketDataSpecification spec) {
    RawMarketDataSource rawDataSource = createRawDataSource(spec);
    return new EagerMarketDataFn(rawDataSource, _configSource, CurrencyMatrixConfigPopulator.BLOOMBERG_LIVE_DATA);
  }
  
  private RawMarketDataSource createRawDataSource(MarketDataSpecification marketDataSpecification) {
    // TODO use time series rating instead of hard coding the data source and field
    if (marketDataSpecification instanceof FixedHistoricalMarketDataSpecification) {
      LocalDate date = ((FixedHistoricalMarketDataSpecification) marketDataSpecification).getSnapshotDate();
      return new HistoricalRawMarketDataSource(_historicalTimeSeriesSource, date, "BLOOMBERG", "Market_Value");
    } else if (marketDataSpecification instanceof UserMarketDataSpecification) {
      UniqueId snapshotId = ((UserMarketDataSpecification) marketDataSpecification).getUserSnapshotId();
      return new SnapshotRawMarketDataSource(_snapshotSource, snapshotId, _historicalTimeSeriesSource, _valuationTimeFn, "BLOOMBERG", "Market_Value");
    } else if (marketDataSpecification instanceof LiveMarketDataSpecification) {
      return new ResettableLiveRawMarketDataSource(new LiveDataManager(_liveDataClient));
    } else {
      throw new IllegalArgumentException("Unexpected spec type " + marketDataSpecification);
    }
  }

}
