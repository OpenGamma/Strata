/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.opengamma.core.change.ChangeEvent;
import com.opengamma.core.change.ChangeListener;
import com.opengamma.core.change.ChangeManager;
import com.opengamma.core.change.ChangeType;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.security.Security;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.id.ObjectId;
import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.sesame.cache.Cacheable;
import com.opengamma.sesame.cache.CachingProxyDecorator;
import com.opengamma.sesame.cache.DefaultCacheInvalidator;
import com.opengamma.sesame.cache.ExecutingMethodsThreadLocal;
import com.opengamma.sesame.cache.MethodInvocationKey;
import com.opengamma.sesame.cache.NoOpCacheInvalidator;
import com.opengamma.sesame.cache.source.CacheAwareConfigSource;
import com.opengamma.sesame.cache.source.CacheAwareConventionSource;
import com.opengamma.sesame.cache.source.CacheAwareHistoricalTimeSeriesSource;
import com.opengamma.sesame.cache.source.CacheAwareRegionSource;
import com.opengamma.sesame.cache.source.CacheAwareSecuritySource;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.function.AvailableImplementations;
import com.opengamma.sesame.function.AvailableOutputs;
import com.opengamma.sesame.graph.CompositeNodeDecorator;
import com.opengamma.sesame.graph.FunctionBuilder;
import com.opengamma.sesame.graph.Graph;
import com.opengamma.sesame.graph.GraphBuilder;
import com.opengamma.sesame.graph.GraphModel;
import com.opengamma.sesame.graph.NodeDecorator;
import com.opengamma.sesame.proxy.ExceptionWrappingProxy;
import com.opengamma.sesame.proxy.TimingProxy;
import com.opengamma.sesame.trace.TracingProxy;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 * Factory for creating instances of {@link View}.
 * This is one of the key classes of the calculation engine. The {@link #createView} methods take a view configuration
 * and returns a view that is ready to be executed.
 * <p>
 * Each view factory contains a cache which is shared by all views it creates.
 */
public class ViewFactory {

  private static final Logger s_logger = LoggerFactory.getLogger(ViewFactory.class);

  /**
   * Default maximum size of the cache.
   * This is used to store return values of methods annotated with {@link Cacheable}.
   * The same cache is shared between all views created by this factory.
   */
  public static final long MAX_CACHE_ENTRIES = 100_000;

  private final ExecutorService _executor;
  private final ComponentMap _components;
  private final AvailableOutputs _availableOutputs;
  private final AvailableImplementations _availableImplementations;
  private final EnumSet<FunctionService> _defaultServices;
  private final Cache<MethodInvocationKey, FutureTask<Object>> _cache;
  private final FunctionModelConfig _defaultConfig;
  private final FunctionBuilder _functionBuilder = new FunctionBuilder();

  public ViewFactory(ExecutorService executor,
                     ComponentMap components,
                     AvailableOutputs availableOutputs,
                     AvailableImplementations availableImplementations,
                     FunctionModelConfig defaultConfig,
                     EnumSet<FunctionService> defaultServices) {
    this(executor, components, availableOutputs, availableImplementations, defaultConfig, defaultServices, MAX_CACHE_ENTRIES);
  }

  public ViewFactory(ExecutorService executor,
                     ComponentMap components,
                     AvailableOutputs availableOutputs,
                     AvailableImplementations availableImplementations,
                     FunctionModelConfig defaultConfig,
                     EnumSet<FunctionService> defaultServices,
                     long maxCacheEntries) {
    _availableOutputs = ArgumentChecker.notNull(availableOutputs, "availableOutputs");
    _availableImplementations = ArgumentChecker.notNull(availableImplementations, "availableImplementations");
    _defaultServices = ArgumentChecker.notNull(defaultServices, "defaultServices");
    _defaultConfig = ArgumentChecker.notNull(defaultConfig, "defaultConfig");
    _executor = ArgumentChecker.notNull(executor, "executor");
    _components = ArgumentChecker.notNull(components, "components");
    int concurrencyLevel = Runtime.getRuntime().availableProcessors() + 2;
    _cache = CacheBuilder.newBuilder().maximumSize(maxCacheEntries).concurrencyLevel(concurrencyLevel).build();
  }

  /**
   * Currently the inputs must be instances of {@link PositionOrTrade} or {@link Security}.
   * This will be relaxed in future.
   * 
   * @param viewConfig  the configuration to use, not null
   * @param inputTypes  the types of the inputs to the calculations, e.g. trades, positions, securities
   * @return the view, not null
   */
  public View createView(ViewConfig viewConfig, Set<Class<?>> inputTypes) {
    return createView(viewConfig, _defaultServices, inputTypes);
  }

  /**
   * Currently the inputs must be instances of {@link PositionOrTrade} or {@link Security}.
   * This will be relaxed in future.
   *
   * @param viewConfig  the configuration to use, not null
   * @param inputTypes  the types of the inputs to the calculations, e.g. trades, positions, securities
   * @return the view, not null
   */
  public View createView(ViewConfig viewConfig, Class<?>... inputTypes) {
    return createView(viewConfig, _defaultServices, Sets.newHashSet(inputTypes));
  }

  /**
   * Currently the inputs must be instances of {@link PositionOrTrade} or {@link Security}.
   * This will be relaxed in future.
   * 
   * @param viewConfig  the configuration to use, not null
   * @param services  the services to run, not null
   * @param inputTypes  the types of the inputs to the calculations, e.g. trades, positions, securities
   * @return the view, not null
   */
  public View createView(ViewConfig viewConfig, EnumSet<FunctionService> services, Class<?>... inputTypes) {
    return createView(viewConfig, services, Sets.newHashSet(inputTypes));
  }

  /**
   * Currently the inputs must be instances of {@link PositionOrTrade} or {@link Security}.
   * This will be relaxed in future.
   *
   * @param viewConfig  the configuration to use, not null
   * @param services  the services to run, not null
   * @param inputTypes  the types of the inputs to the calculations, e.g. trades, positions, securities
   * @return the view, not null
   */
  public View createView(ViewConfig viewConfig, EnumSet<FunctionService> services, Set<Class<?>> inputTypes) {
    NodeDecorator decorator;
    // TODO the CacheInvalidator needs to be created in ViewFactoryComponentFactory and passed in
    // that will allow it to be linked to the decorated sources at the point where they're created
    CacheInvalidator cacheInvalidator;

    if (services.isEmpty()) {
      decorator = ExceptionWrappingProxy.INSTANCE;
      cacheInvalidator = new NoOpCacheInvalidator();
    } else {
      List<NodeDecorator> decorators = Lists.newArrayListWithCapacity(services.size());
      if (services.contains(FunctionService.CACHING)) {
        ExecutingMethodsThreadLocal executingMethods = new ExecutingMethodsThreadLocal();
        CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cache, executingMethods);
        decorators.add(cachingDecorator);
        cacheInvalidator = new DefaultCacheInvalidator(executingMethods, _cache);
      } else {
        cacheInvalidator = new NoOpCacheInvalidator();
      }
      if (services.contains(FunctionService.TIMING)) {
        decorators.add(TimingProxy.INSTANCE);
      }
      if (services.contains(FunctionService.TRACING)) {
        decorators.add(TracingProxy.INSTANCE);
      }
      // Ensure we always have the exception wrapping
      // behaviour, whether or not it is passed
      decorators.add(ExceptionWrappingProxy.INSTANCE);
      decorator = CompositeNodeDecorator.compose(decorators);
    }
    SourceListener sourceListener = new SourceListener();
    Pair<ComponentMap, Collection<ChangeManager>> pair = decorateSources(_components, cacheInvalidator, sourceListener);
    ComponentMap components = pair.getFirst();

    s_logger.debug("building graph model");
    GraphBuilder graphBuilder = new GraphBuilder(_availableOutputs,
                                                 _availableImplementations,
                                                 _components.getComponentTypes(),
                                                 _defaultConfig,
                                                 decorator);
    GraphModel graphModel = graphBuilder.build(viewConfig, inputTypes);

    s_logger.debug("graph model complete, building graph");
    Graph graph = graphModel.build(components, _functionBuilder);
    s_logger.debug("graph complete");

    Collection<ChangeManager> changeManagers = pair.getSecond();
    return new View(viewConfig, graph, _executor, _defaultConfig, decorator,
                    cacheInvalidator, graphModel, sourceListener, changeManagers);
  }

  /**
   * TODO this needs to move into ViewFactoryComponentFactory so the service context has the decorated sources
   * Decorates the sources with cache aware versions that register when data is queried so cache entries can be
   * invalidated when it changes. The returned component map contains the cache aware sources in place of the originals.
   * The returns collection contains the change managers for all decorated sources. This allows listeners to
   * be removed when the view closes and prevents a resource leak.
   *
   * @param components Platform components used by functions
   * @param cacheInvalidator For registering dependencies between values in the cache and the values used to calculate them
   * @param sourceListener Listens for changes in items in the database
   * @return A map of components containing the decorated sources instead of the originals, and a collection of
   *   change managers which had a listener added to them
   */
  private static Pair<ComponentMap, Collection<ChangeManager>> decorateSources(ComponentMap components,
                                                                               CacheInvalidator cacheInvalidator,
                                                                               SourceListener sourceListener) {
    Map<Class<?>, Object> sources = Maps.newHashMap();
    // need to record which ChangeManagers we're listening to so we can remove the listeners and avoid leaks
    Collection<ChangeManager> changeManagers = Lists.newArrayList();

    ConfigSource configSource = components.findComponent(ConfigSource.class);
    if (configSource != null) {
      changeManagers.add(configSource.changeManager());
      sources.put(ConfigSource.class, new CacheAwareConfigSource(configSource, cacheInvalidator));
    }

    RegionSource regionSource = components.findComponent(RegionSource.class);
    if (regionSource != null) {
      changeManagers.add(regionSource.changeManager());
      sources.put(RegionSource.class, new CacheAwareRegionSource(regionSource, cacheInvalidator));
    }

    SecuritySource securitySource = components.findComponent(SecuritySource.class);
    if (securitySource != null) {
      changeManagers.add(securitySource.changeManager());
      sources.put(SecuritySource.class, new CacheAwareSecuritySource(securitySource, cacheInvalidator));
    }

    ConventionSource conventionSource = components.findComponent(ConventionSource.class);
    if (conventionSource != null) {
      changeManagers.add(conventionSource.changeManager());
      sources.put(ConventionSource.class, new CacheAwareConventionSource(conventionSource, cacheInvalidator));
    }

    HistoricalTimeSeriesSource timeSeriesSource = components.findComponent(HistoricalTimeSeriesSource.class);
    if (timeSeriesSource != null) {
      changeManagers.add(timeSeriesSource.changeManager());
      sources.put(HistoricalTimeSeriesSource.class, new CacheAwareHistoricalTimeSeriesSource(timeSeriesSource, cacheInvalidator));
    }

    // TODO HolidaySource (which has a horrible design WRT decorating)

    for (ChangeManager changeManager : changeManagers) {
      changeManager.addChangeListener(sourceListener);
    }
    return Pairs.of(components, changeManagers);
  }

  //----------------------------------------------------------
  /**
   * Listens to sources and records the IDs of any objects that are updated or removed.
   * This information is used to invalidate cache entries between cycles. Cached values are discarded if they were
   * calculated using objects that have since been updated.
   */
  /* package */ static class SourceListener implements ChangeListener {

    private final Queue<ObjectId> _ids = new ConcurrentLinkedQueue<>();

    @Override
    public void entityChanged(ChangeEvent event) {
      if (event.getType() == ChangeType.ADDED) {
        return;
      }
      _ids.add(event.getObjectId());
    }

    /* package */ Collection<ObjectId> getIds() {
      return _ids;
    }

    /* package */ void clear() {
      _ids.clear();
    }
  }
}
