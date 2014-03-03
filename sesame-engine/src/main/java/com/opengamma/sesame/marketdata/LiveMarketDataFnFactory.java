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
import com.opengamma.core.link.ConfigLink;
import com.opengamma.engine.marketdata.spec.LiveMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.financial.currency.CurrencyMatrix;
import com.opengamma.livedata.LiveDataClient;
import com.opengamma.provider.livedata.LiveDataMetaDataProvider;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.jms.JmsConnector;

/**
 * Factory for {@link MarketDataFn} instances which use live market data from any available provider.
 */
public class LiveMarketDataFnFactory implements MarketDataFnFactory {

  private final Map<String, MarketDataFn> _marketDataFnBySource;
  
  public LiveMarketDataFnFactory(Collection<LiveDataMetaDataProvider> providers, JmsConnector jmsConnector, String currencyMatrixConfigName) {
    ImmutableMap.Builder<String, MarketDataFn> builder = ImmutableMap.builder(); 
    for (LiveDataMetaDataProvider provider : providers) {
      LiveDataClient liveDataClient = LiveMarketDataProviderFactoryComponentFactory.createLiveDataClient(provider, jmsConnector);
      RawMarketDataSource rawDataSource = new ResettableLiveRawMarketDataSource(new LiveDataManager(liveDataClient));
      ConfigLink<CurrencyMatrix> configLink = ConfigLink.of(currencyMatrixConfigName, CurrencyMatrix.class);
      MarketDataFn marketDataFn = new EagerMarketDataFn(configLink.resolve(), rawDataSource);
      builder.put(provider.metaData().getDescription(), marketDataFn);
    }
    _marketDataFnBySource = builder.build();
  }
  
  @Override
  public MarketDataFn create(MarketDataSpecification spec) {
    if (!(ArgumentChecker.notNull(spec, "spec") instanceof LiveMarketDataSpecification)) {
      throw new IllegalArgumentException("Expected " + LiveMarketDataSpecification.class + " but was " + spec.getClass());
    }
    LiveMarketDataSpecification liveMarketDataSpec = (LiveMarketDataSpecification) spec;
    MarketDataFn marketDataFn = _marketDataFnBySource.get(liveMarketDataSpec.getDataSource());
    if (marketDataFn == null) {
      throw new IllegalArgumentException("Unsupported live data source: " + liveMarketDataSpec.getDataSource());
    }
    return marketDataFn;
  }

}
