/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import org.threeten.bp.LocalDate;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.marketdatasnapshot.MarketDataSnapshotSource;
import com.opengamma.engine.marketdata.spec.FixedHistoricalMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.engine.marketdata.spec.UserMarketDataSpecification;
import com.opengamma.id.UniqueId;
import com.opengamma.sesame.ValuationTimeFn;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.util.ArgumentChecker;

/**
 * Creates a {@link MarketDataFn} given a {@link MarketDataSpecification}.
 */
public class SpecificationMarketDataFactory implements MarketDataFactory {

  // TODO do we want to reuse MarketDataSpecification or replace it?
  private final MarketDataSpecification _marketDataSpecification;

  public SpecificationMarketDataFactory(MarketDataSpecification marketDataSpecification) {
    _marketDataSpecification = ArgumentChecker.notNull(marketDataSpecification, "marketDataSpecification");
    if (!(_marketDataSpecification instanceof FixedHistoricalMarketDataSpecification) &&
        !(_marketDataSpecification instanceof UserMarketDataSpecification)) {
      throw new IllegalArgumentException("Only fixed historical and snapshot data sources are currently supported");
    }
  }

  @Override
  public MarketDataFn create(ComponentMap components) {
    ConfigSource configSource = components.getComponent(ConfigSource.class);
    RawMarketDataSource rawDataSource = createRawDataSource(components);
    return new EagerMarketDataFn(rawDataSource, configSource, "BloombergLiveData");
  }

  private RawMarketDataSource createRawDataSource(ComponentMap components) {
    // TODO use time series rating instead of hard coding the data source and field?
    HistoricalTimeSeriesSource timeSeriesSource = components.getComponent(HistoricalTimeSeriesSource.class);
    if (_marketDataSpecification instanceof FixedHistoricalMarketDataSpecification) {
      LocalDate date = ((FixedHistoricalMarketDataSpecification) _marketDataSpecification).getSnapshotDate();
      return new HistoricalRawMarketDataSource(timeSeriesSource, date, "BLOOMBERG", "Market_Value");
    } else if (_marketDataSpecification instanceof UserMarketDataSpecification) {
      MarketDataSnapshotSource snapshotSource = components.getComponent(MarketDataSnapshotSource.class);
      UniqueId snapshotId = ((UserMarketDataSpecification) _marketDataSpecification).getUserSnapshotId();
      ValuationTimeFn valuationTimeFn = components.getComponent(ValuationTimeFn.class);
      return new SnapshotRawMarketDataSource(snapshotSource, snapshotId, timeSeriesSource, valuationTimeFn, "BLOOMBERG", "Market_Value");
    } else {
      throw new IllegalArgumentException("Unexpected spec type " + _marketDataSpecification);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SpecificationMarketDataFactory that = (SpecificationMarketDataFactory) o;

    if (!_marketDataSpecification.equals(that._marketDataSpecification)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return _marketDataSpecification.hashCode();
  }
}
