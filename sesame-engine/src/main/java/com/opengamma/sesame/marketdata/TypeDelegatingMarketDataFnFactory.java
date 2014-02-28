/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.engine.marketdata.spec.FixedHistoricalMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.LiveMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.engine.marketdata.spec.UserMarketDataSpecification;
import com.opengamma.util.ArgumentChecker;

/**
 * Delegates to a {@link MarketDataFnFactory} by type of {@link MarketDataSpecification}.
 */
public final class TypeDelegatingMarketDataFnFactory implements MarketDataFnFactory {

  private final Map<Class<? extends MarketDataSpecification>, MarketDataFnFactory> _typeMap;
  
  private TypeDelegatingMarketDataFnFactory(Map<Class<? extends MarketDataSpecification>, MarketDataFnFactory> typeMap) {
    _typeMap = ImmutableMap.copyOf(typeMap);
  }
  
  @Override
  public MarketDataFn create(MarketDataSpecification spec) {
    ArgumentChecker.notNull(spec, "spec");
    MarketDataFnFactory factory = _typeMap.get(spec.getClass());
    if (factory == null) {
      throw new IllegalArgumentException("Unsupported market data specification type " + spec.getClass());
    }
    return factory.create(spec);
  }
  
  public static Builder buildder() {
    return new Builder();
  }
  
  /**
   * Builder class for {@link TypeDelegatingMarketDataFnFactory}.
   */
  public static class Builder {
    
    private final Map<Class<? extends MarketDataSpecification>, MarketDataFnFactory> _builderMap = new HashMap<Class<? extends MarketDataSpecification>, MarketDataFnFactory>();
    
    public Builder put(Class<? extends MarketDataSpecification> clazz, MarketDataFnFactory marketDataFnFactory) {
      _builderMap.put(clazz, marketDataFnFactory);
      return this;
    }
    
    public Builder live(MarketDataFnFactory liveMarketDataFnFactory) {
      return put(LiveMarketDataSpecification.class, liveMarketDataFnFactory);
    }
    
    public Builder fixedHistorical(MarketDataFnFactory fixedHistoricalMarketDataFnFactory) {
      return put(FixedHistoricalMarketDataSpecification.class, fixedHistoricalMarketDataFnFactory);
    }
    
    public Builder snapshot(MarketDataFnFactory userSnapshotMarketDataFnFactory) {
      return put(UserMarketDataSpecification.class, userSnapshotMarketDataFnFactory);
    }
    
    public TypeDelegatingMarketDataFnFactory build() {
      return new TypeDelegatingMarketDataFnFactory(_builderMap);
    }
    
  }

}
