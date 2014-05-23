/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opengamma.service.ServiceContext;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.graph.FunctionBuilder;
import com.opengamma.sesame.graph.Graph;
import com.opengamma.sesame.graph.GraphModel;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.sesame.marketdata.ProxiedMarketDataSource;

/**
 * A cycle initializer to be used  for capturing the
 * inputs to a cycle. This requires additional work as
 * we need to replace the component map so that we can
 * capture the data requested. Similarly we need to
 * switch market data source so we can capture market
 * data requests.
 */
class CapturingCycleInitializer implements CycleInitializer {

  private final ServiceContext _serviceContext;
  private final DefaultCycleRecorder _recorder;
  private final Graph _graph;
  private final MarketDataSource _marketDataSource;

  /**
   * Creates cycle initializer for a capturing cycle.
   *
   * @param serviceContext  the current service context
   * @param cachingManager  the current caching manager
   * @param cycleArguments  the cycle arguments
   * @param graphModel  the graph model for the view
   * @param viewConfig  the config for the view
   * @param inputs  the trade inputs
   */
  public CapturingCycleInitializer(ServiceContext serviceContext,
                                   CachingManager cachingManager,
                                   CycleArguments cycleArguments,
                                   GraphModel graphModel,
                                   ViewConfig viewConfig, List<?> inputs) {

    ProxiedMarketDataSource proxiedMarketDataSource =
        new ProxiedMarketDataSource(cycleArguments.getMarketDataSource());

    ProxiedComponentMap collector = new DefaultProxiedComponentMap();

    // The component map has its components wrapped so they can
    // used for cache invalidation when required. It will receive
    // callbacks from all views which is correct as the cache spans
    // all views but is not what is required here. We need a
    // component map that only receives calls from this view so we
    // need to wrap again
    ComponentMap componentMap = wrap(cachingManager.getComponentMap(), collector);

    // If we are capturing the inputs then we don't want to use
    // memoized functions from the normal cache as that would
    // prevent us hitting the sources
    _graph = graphModel.build(componentMap, new FunctionBuilder());

    _marketDataSource = proxiedMarketDataSource;
    _serviceContext = serviceContext.with(componentMap.getComponents());
    _recorder = new DefaultCycleRecorder(viewConfig,
                                        inputs,
                                        cycleArguments,
                                        proxiedMarketDataSource,
                                        collector);
  }

  @Override
  public MarketDataSource getMarketDataSource() {
    return _marketDataSource;
  }

  @Override
  public ServiceContext getServiceContext() {
    return _serviceContext;
  }

  @Override
  public Graph getGraph() {
    return _graph;
  }

  @Override
  public Results complete(Results results) {
    return _recorder.complete(results);
  }

  // TODO - this could maybe go on component map itself
  private ComponentMap wrap(ComponentMap components, final ProxiedComponentMap collector) {

    Map<Class<?>, Object> wrapped = new HashMap<>();

    // Wrap each component in the map with a proxy, which will
    // automatically notify the collector whenever requests are
    // made from one of the components
    for (Map.Entry<Class<?>, Object> entry : components.getComponents().entrySet()) {

      final Class<?> key = entry.getKey();
      final Object component = entry.getValue();

      // This proxy mechanism works correctly but unfortunately the
      // sources and other components in proxies are not so well behaved
      // so we won't get consistent data recorded
      Object proxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),
                                            new Class<?>[]{key},
                                            new InvocationHandler() {
                                              @Override
                                              public Object invoke(Object proxy,
                                                                   Method method,
                                                                   Object[] args) throws Throwable {

                                                Object result = method.invoke(component, args);
                                                if (result != null) {
                                                  collector.receivedCall(key, result);
                                                }
                                                return result;
                                              }
                                            });
      wrapped.put(key, proxy);
    }

    return ComponentMap.of(wrapped);
  }
}
