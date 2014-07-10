/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.config.impl.NarrowingConfigSource;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.convention.impl.NarrowingConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.holiday.NarrowingHolidaySource;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.region.impl.NarrowingRegionSource;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.core.security.impl.NarrowingSecuritySource;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueIdentifiable;
import com.opengamma.service.ServiceContext;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.graph.FunctionBuilder;
import com.opengamma.sesame.graph.Graph;
import com.opengamma.sesame.graph.GraphModel;
import com.opengamma.sesame.marketdata.CycleMarketDataFactory;
import com.opengamma.sesame.marketdata.ProxiedCycleMarketData;

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
  private final CycleMarketDataFactory _cycleMarketDataFactory;

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

    ProxiedCycleMarketData proxiedCycleMarketData =
        new ProxiedCycleMarketData(cycleArguments.getCycleMarketDataFactory());

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

    _cycleMarketDataFactory = proxiedCycleMarketData;
    _serviceContext = serviceContext.with(componentMap.getComponents());
    _recorder = new DefaultCycleRecorder(viewConfig,
                                        inputs,
                                        cycleArguments,
                                        proxiedCycleMarketData,
                                        collector);
  }

  @Override
  public CycleMarketDataFactory getCycleMarketDataFactory() {
    return _cycleMarketDataFactory;
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
      // sources and other components in proxies are not so well behaved.
      // In order to get consistent data recorded we need to use
      // source implementations that are well behaved as well.
      InvocationHandler handler = new InvocationHandler() {
        @Override
        public Object invoke(Object proxy,
                             Method method,
                             Object[] args) throws Throwable {

          try {
            if (!method.getName().equals("get")) {
              throw new UnsupportedOperationException("Only calls to get are supported through this " + key.getSimpleName() + " proxy");
            }

            Object result = method.invoke(component, args);
            if (result != null) {

              if (Map.class.isAssignableFrom(method.getReturnType())) {

                for (Object item : Map.class.cast(result).values()) {
                  collector.receivedCall(key, (UniqueIdentifiable) item);
                }
              } else if (Collection.class.isAssignableFrom(method.getReturnType())) {

                for (Object item : Collection.class.cast(result)) {
                  collector.receivedCall(key, (UniqueIdentifiable) item);
                }
              } else {
                collector.receivedCall(key, (UniqueIdentifiable) result);
              }
            }
            return result;
          } catch (Exception e) {
            throw EngineUtils.getCause(e);
          }
        }
      };
      Object proxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class<?>[]{key}, handler);
      if (key == ConfigSource.class) {
        proxy = new NarrowingConfigSource((ConfigSource) proxy);
      } else if (key == SecuritySource.class) {
        proxy = new NarrowingSecuritySource((SecuritySource) proxy);
      } else if (key == ConventionSource.class) {
        proxy = new NarrowingConventionSource((ConventionSource) proxy);
      } else if (key == RegionSource.class) {
        proxy = new NarrowingRegionSource((RegionSource) proxy);
      } else if (key == HolidaySource.class) {
        proxy = new NarrowingHolidaySource((HolidaySource) proxy);
      } else if (key == HistoricalTimeSeriesSource.class) {
        InvocationHandler htsHandler = new InvocationHandler() {
          @Override
          public Object invoke(Object proxy,
                               Method method,
                               Object[] args) throws Throwable {

            try {
              if (!method.getName().equals("getHistoricalTimeSeries")) {
                throw new UnsupportedOperationException(
                    "Only calls to getHistoricalTimeSeries are supported through this HTS proxy");
              }

              Object result = method.invoke(component, args);
              if (result != null) {
                switch (args.length) {
                  case 4:
                    collector.receivedHtsCall(
                        (ExternalIdBundle) args[0], (String) args[1], (String) args[2], (String) args[3],
                        ((HistoricalTimeSeries) result).getTimeSeries());
                    break;
                  case 7:
                    collector.receivedHtsCall(
                        (ExternalIdBundle) args[1], null, null, (String) args[0],
                        ((HistoricalTimeSeries) result).getTimeSeries());
                    break;
                  default:
                    throw new UnsupportedOperationException(
                        "Unable to handle calls to " + args.length + " arg version");
                }
              }
              return result;
            } catch (Exception e) {
              throw EngineUtils.getCause(e);
            }
          }
        };
        proxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class<?>[]{key}, htsHandler);

      } else {
        // Don't proxy any other components
        proxy = component;
      }
      wrapped.put(key, proxy);
    }

    return ComponentMap.of(wrapped);
  }
}
