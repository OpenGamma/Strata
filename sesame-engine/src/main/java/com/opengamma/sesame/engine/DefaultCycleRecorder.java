/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.MarketDataSourceListener;
import com.opengamma.sesame.marketdata.ProxiedMarketDataSource;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 * A simple cycle recorder implementation that captures all market
 * data and component calls made.
 */
// TODO - further work on this class needed to handle the strategy for physical recording
public class DefaultCycleRecorder implements CycleRecorder {

  private final ViewConfig _viewConfig;
  private final List<?> _inputs;
  private final CycleArguments _cycleArguments;
  private final ProxiedMarketDataSource _proxiedMarketDataSource;
  private final ProxiedComponentMap _proxiedComponentMap;
  private final Map<Pair<ExternalIdBundle, FieldName>, Result<?>> _marketDataRequests = new HashMap<>();
  private final Multimap<Class<?>, Object> _componentDataRequests = HashMultimap.create();
  private final MarketDataSourceListener _marketDataSourceListener;
  private final ComponentListener _componentListener;

  public DefaultCycleRecorder(ViewConfig viewConfig,
                              List<?> inputs,
                              CycleArguments cycleArguments,
                              ProxiedMarketDataSource proxiedMarketDataSource, ProxiedComponentMap proxiedComponentMap) {

    _viewConfig = viewConfig;
    _inputs = inputs;
    _cycleArguments = cycleArguments;
    _proxiedMarketDataSource = proxiedMarketDataSource;
    _proxiedComponentMap = proxiedComponentMap;

    _marketDataSourceListener = new MarketDataSourceListener() {
      @Override
      public void requestMade(ExternalIdBundle id, FieldName fieldName, Result<?> result) {
        _marketDataRequests.put(Pairs.of(id, fieldName), result);
      }
    };

    _proxiedMarketDataSource.addListener(_marketDataSourceListener);

    _componentListener = new ComponentListener() {
      @Override
      public void receivedCall(Class<?> componentType, Object result) {
        _componentDataRequests.put(componentType, result);
      }
    };
    _proxiedComponentMap.addListener(_componentListener);

  }

  @Override
  public void complete(Results results) {
    _proxiedMarketDataSource.removeListener(_marketDataSourceListener);
    _proxiedComponentMap.removeListener(_componentListener);

    // Output in whatever way is appropriate for the strategy
  }
}
