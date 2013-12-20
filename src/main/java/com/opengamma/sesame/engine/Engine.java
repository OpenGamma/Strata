/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.core.security.Security;
import com.opengamma.sesame.DefaultValuationTimeFn;
import com.opengamma.sesame.ValuationTimeFn;
import com.opengamma.sesame.cache.CacheAwareValuationTimeFn;
import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.sesame.cache.CachingProxyDecorator;
import com.opengamma.sesame.cache.DefaultCacheInvalidator;
import com.opengamma.sesame.cache.ExecutingMethodsThreadLocal;
import com.opengamma.sesame.cache.NoOpCacheInvalidator;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.graph.CompositeNodeDecorator;
import com.opengamma.sesame.graph.Graph;
import com.opengamma.sesame.graph.GraphBuilder;
import com.opengamma.sesame.graph.GraphModel;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.proxy.TimingProxy;
import com.opengamma.sesame.trace.TracingProxy;
import com.opengamma.util.ArgumentChecker;

import net.sf.ehcache.CacheManager;

/**
 * TODO does the engine actually need to exist? all it does is store a couple of fields and create a view
 * could the views be created directly? or should it just be renamed ViewFactory?
 */
public class Engine {

  private static final Logger s_logger = LoggerFactory.getLogger(Engine.class);

  private final ExecutorService _executor;
  private final ComponentMap _components;
  private final AvailableOutputs _availableOutputs;
  private final AvailableImplementations _availableImplementations;
  private final EnumSet<EngineService> _defaultServices;
  private final CacheManager _cacheManager;
  private final FunctionConfig _defaultConfig;

  /* package */ Engine(ExecutorService executor,
                       AvailableOutputs availableOutputs,
                       AvailableImplementations availableImplementations) {
    this(executor,
         ComponentMap.EMPTY,
         availableOutputs,
         availableImplementations,
         FunctionConfig.EMPTY,
         CacheManager.getInstance(),
         EngineService.DEFAULT_SERVICES);
  }

  // TODO parameter to allow arbitrary NodeDecorators to be passed in?
  public Engine(ExecutorService executor,
                ComponentMap components,
                AvailableOutputs availableOutputs,
                AvailableImplementations availableImplementations,
                FunctionConfig defaultConfig,
                CacheManager cacheManager,
                EnumSet<EngineService> defaultServices) {
    _availableOutputs = ArgumentChecker.notNull(availableOutputs, "availableOutputs");
    _availableImplementations = ArgumentChecker.notNull(availableImplementations, "availableImplementations");
    _defaultServices = ArgumentChecker.notNull(defaultServices, "defaultServices");
    _cacheManager = ArgumentChecker.notNull(cacheManager, "cacheManager");
    _defaultConfig = ArgumentChecker.notNull(defaultConfig, "defaultConfig");
    _executor = ArgumentChecker.notNull(executor, "executor");
    // TODO wrap sources in cache aware versions
    // TODO listen for changes in the source data
    _components = ArgumentChecker.notNull(components, "components");
  }

  /*public interface Listener {

    void cycleComplete(Results results);
  }*/


  /**
   * Currently the inputs must be instances of {@link PositionOrTrade} or {@link Security}. This will be relaxed
   * in future.
   */
  public View createView(ViewDef viewDef, List<?> inputs) {
    return createView(viewDef, inputs, _defaultServices);
  }

  /**
   * Currently the inputs must be instances of {@link PositionOrTrade} or {@link Security}. This will be relaxed
   * in future.
   * TODO parameter to allow arbitrary NodeDecorators to be passed in?
   * TODO should this logic be in View?
   */
  public View createView(ViewDef viewDef, List<?> inputs, EnumSet<EngineService> services) {
    CompositeNodeDecorator decorator;
    CacheInvalidator cacheInvalidator;
    if (services.isEmpty()) {
      decorator = new CompositeNodeDecorator(NodeDecorator.IDENTITY);
      cacheInvalidator = new NoOpCacheInvalidator();
    } else {
      List<NodeDecorator> decorators = Lists.newArrayListWithCapacity(services.size());
      if (services.contains(EngineService.CACHING)) {
        ExecutingMethodsThreadLocal executingMethods = new ExecutingMethodsThreadLocal();
        CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheManager, executingMethods);
        decorators.add(cachingDecorator);
        cacheInvalidator = new DefaultCacheInvalidator(executingMethods, cachingDecorator.getCache());
      } else {
        cacheInvalidator = new NoOpCacheInvalidator();
      }
      if (services.contains(EngineService.TIMING)) {
        decorators.add(TimingProxy.INSTANCE);
      }
      if (services.contains(EngineService.TRACING)) {
        decorators.add(TracingProxy.INSTANCE);
      }
      decorator = new CompositeNodeDecorator(decorators);
    }

    // TODO everything below here could move into the View constructor

    // TODO is this the right place for this logic? should there be a component map pre-populated with them?
    // as they're completely standard components always provided by the engine
    // need to supplement components with a MarketDataFn and ValuationTimeFn that are
    // under our control so we can switch out the backing impls each cycle if necessary
    DelegatingMarketDataFn marketDataFn = new DelegatingMarketDataFn();
    DefaultValuationTimeFn valuationTimeFn = new DefaultValuationTimeFn();
    Map<Class<?>, Object> componentOverrides = Maps.newHashMap();
    componentOverrides.put(MarketDataFn.class, marketDataFn);
    componentOverrides.put(ValuationTimeFn.class, new CacheAwareValuationTimeFn(valuationTimeFn, cacheInvalidator));
    ComponentMap components = _components.with(componentOverrides);

    s_logger.debug("building graph model");
    GraphBuilder graphBuilder = new GraphBuilder(_availableOutputs,
                                                 _availableImplementations,
                                                 components,
                                                 _defaultConfig,
                                                 decorator);
    GraphModel graphModel = graphBuilder.build(viewDef, inputs);
    s_logger.debug("graph model complete, building graph");
    Graph graph = graphModel.build(components);
    s_logger.debug("graph complete");
    return new View(viewDef, graph, inputs, _executor, marketDataFn, valuationTimeFn,
                    components, _defaultConfig, decorator, cacheInvalidator);
  }
}
