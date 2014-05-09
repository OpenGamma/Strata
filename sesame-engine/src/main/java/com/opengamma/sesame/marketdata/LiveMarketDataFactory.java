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

  private final Map<String, LiveDataManager> _liveDataManagerBySource;
  
  public LiveMarketDataFactory(Collection<LiveDataMetaDataProvider> providers, JmsConnector jmsConnector) {
    ImmutableMap.Builder<String, LiveDataManager> builder = ImmutableMap.builder();
    for (LiveDataMetaDataProvider provider : providers) {
      LiveDataClient liveDataClient = LiveMarketDataProviderFactoryComponentFactory.createLiveDataClient(provider, jmsConnector);
      builder.put(provider.metaData().getDescription(), new DefaultLiveDataManager(liveDataClient));
    }
    _liveDataManagerBySource = builder.build();
  }
  
  @Override
  public StrategyAwareMarketDataSource create(MarketDataSpecification spec) {
    if (!(ArgumentChecker.notNull(spec, "spec") instanceof LiveMarketDataSpecification)) {
      throw new IllegalArgumentException("Expected " + LiveMarketDataSpecification.class + " but was " + spec.getClass());
    }
    LiveMarketDataSpecification liveMarketDataSpec = (LiveMarketDataSpecification) spec;
    LiveDataManager liveDataManager = _liveDataManagerBySource.get(liveMarketDataSpec.getDataSource());
    if (liveDataManager == null) {
      throw new IllegalArgumentException("Unsupported live data source: " + liveMarketDataSpec.getDataSource());
    }
    LDClient liveDataClient = new LDClient(liveDataManager);
    return new ResettableLiveMarketDataSource(spec, liveDataClient);
  }

}
