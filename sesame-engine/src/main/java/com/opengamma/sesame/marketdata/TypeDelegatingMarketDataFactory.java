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
 * Delegates to a {@link MarketDataFactory} by type of {@link MarketDataSpecification}.
 */
public final class TypeDelegatingMarketDataFactory implements MarketDataFactory {

  private final Map<Class<? extends MarketDataSpecification>, MarketDataFactory> _typeMap;
  
  private TypeDelegatingMarketDataFactory(Map<Class<? extends MarketDataSpecification>, MarketDataFactory> typeMap) {
    _typeMap = ImmutableMap.copyOf(ArgumentChecker.notNull(typeMap, "typeMap"));
  }
  
  @Override
  public StrategyAwareMarketDataSource create(MarketDataSpecification spec) {
    MarketDataFactory factory = _typeMap.get(ArgumentChecker.notNull(spec, "spec").getClass());

    if (factory == null) {
      throw new IllegalArgumentException("Unsupported market data specification type " + spec.getClass());
    }
    return factory.create(spec);
  }
  
  public static Builder builder() {
    return new Builder();
  }
  
  /**
   * Builder class for {@link TypeDelegatingMarketDataFactory}.
   */
  public static class Builder {

    private final Map<Class<? extends MarketDataSpecification>, MarketDataFactory> _builderMap = new HashMap<>();
    
    public Builder put(Class<? extends MarketDataSpecification> specType, MarketDataFactory factory) {
      _builderMap.put(specType, factory);
      return this;
    }
    
    public Builder live(MarketDataFactory factory) {
      return put(LiveMarketDataSpecification.class, factory);
    }
    
    public Builder fixedHistorical(MarketDataFactory factory) {
      return put(FixedHistoricalMarketDataSpecification.class, factory);
    }
    
    public Builder snapshot(MarketDataFactory factory) {
      return put(UserMarketDataSpecification.class, factory);
    }
    
    public TypeDelegatingMarketDataFactory build() {
      return new TypeDelegatingMarketDataFactory(_builderMap);
    }
    
  }

}
