/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.component.factory.livedata.LiveMarketDataProviderFactoryComponentFactory;
import com.opengamma.engine.marketdata.spec.LiveMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.livedata.LiveDataClient;
import com.opengamma.provider.livedata.LiveDataMetaDataProvider;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.jms.JmsConnector;

/**
 * Factory for {@link MarketDataSource} instances which use live market data from any available provider.
 */
public class LiveMarketDataFactory implements MarketDataFactory {

  private final Map<String, MarketDataSource> _marketDataBySource;
  
  public LiveMarketDataFactory(Collection<LiveDataMetaDataProvider> providers, JmsConnector jmsConnector) {
    ImmutableMap.Builder<String, MarketDataSource> builder = ImmutableMap.builder();

    for (LiveDataMetaDataProvider provider : providers) {
      LiveDataClient liveDataClient = LiveMarketDataProviderFactoryComponentFactory.createLiveDataClient(provider, jmsConnector);
      MarketDataSource dataSource = new ResettableLiveMarketDataSource(new LiveDataManager(liveDataClient));
      builder.put(provider.metaData().getDescription(), dataSource);
    }
    _marketDataBySource = builder.build();
  }
  
  @Override
  public MarketDataSource create(MarketDataSpecification spec) {
    if (!(ArgumentChecker.notNull(spec, "spec") instanceof LiveMarketDataSpecification)) {
      throw new IllegalArgumentException("Expected " + LiveMarketDataSpecification.class + " but was " + spec.getClass());
    }
    LiveMarketDataSpecification liveMarketDataSpec = (LiveMarketDataSpecification) spec;
    MarketDataSource marketDataSource = _marketDataBySource.get(liveMarketDataSpec.getDataSource());
    if (marketDataSource == null) {
      throw new IllegalArgumentException("Unsupported live data source: " + liveMarketDataSpec.getDataSource());
    }
    return marketDataSource;
  }

}
