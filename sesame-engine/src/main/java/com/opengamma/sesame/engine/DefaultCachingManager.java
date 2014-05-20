/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.FutureTask;

import com.google.common.cache.Cache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opengamma.core.change.ChangeManager;
import com.opengamma.core.config.ConfigSource;
import com.opengamma.core.convention.ConventionSource;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.security.SecuritySource;
import com.opengamma.sesame.cache.CacheInvalidator;
import com.opengamma.sesame.cache.CachingProxyDecorator;
import com.opengamma.sesame.cache.DefaultCacheInvalidator;
import com.opengamma.sesame.cache.ExecutingMethodsThreadLocal;
import com.opengamma.sesame.cache.MethodInvocationKey;
import com.opengamma.sesame.cache.source.CacheAwareConfigSource;
import com.opengamma.sesame.cache.source.CacheAwareConventionSource;
import com.opengamma.sesame.cache.source.CacheAwareHistoricalTimeSeriesSource;
import com.opengamma.sesame.cache.source.CacheAwareRegionSource;
import com.opengamma.sesame.cache.source.CacheAwareSecuritySource;
import com.opengamma.util.ArgumentChecker;

/**
 * Manages the set of cache-aware components, providing them
 * to Views as required.
 */
public class DefaultCachingManager implements CachingManager {

  private final ComponentMap _componentMap;
  private final Cache<MethodInvocationKey, FutureTask<Object>> _cache;
  private final CacheInvalidator _cacheInvalidator;
  private final CachingProxyDecorator _cachingDecorator;

  /**
   * Constructs the set of objects which are used to manage caching
   * within the engine.
   *
   * @param componentMap the map of components to be provided to
   * the engine
   * @param cache the cache to be used by the engine
   */
  public DefaultCachingManager(ComponentMap componentMap, Cache<MethodInvocationKey, FutureTask<Object>> cache) {
    ExecutingMethodsThreadLocal executingMethods = new ExecutingMethodsThreadLocal();
    _cache = ArgumentChecker.notNull(cache, "cache");
    _cacheInvalidator = new DefaultCacheInvalidator(executingMethods, _cache);
    _cachingDecorator = new CachingProxyDecorator(_cache, executingMethods);
    _componentMap = decorateSources(ArgumentChecker.notNull(componentMap, "componentMap"));
  }

  @Override
  public ComponentMap getComponentMap() {
    return _componentMap;
  }

  @Override
  public CacheInvalidator getCacheInvalidator() {
    return _cacheInvalidator;
  }

  @Override
  public CachingProxyDecorator getCachingDecorator() {
    return _cachingDecorator;
  }

  /**
   * Decorates the sources with cache aware versions that register when data is
   * queried so cache entries can be invalidated when it changes. The returned
   * component map contains the cache aware sources in place of the originals.
   *
   * @param components  platform components used by functions
   * @return a component map containing the decorated sources instead of
   * the originals
   */
  private ComponentMap decorateSources(ComponentMap components) {
    // Copy the original set and overwrite the ones we're interested in
    Map<Class<?>, Object> sources = Maps.newHashMap(components.getComponents());

    // need to record which ChangeManagers we're listening to so we can remove the listeners and avoid leaks
    // TODO - use the change managers
    Collection<ChangeManager> changeManagers = Lists.newArrayList();

    // TODO - could we do all this wrapping with dynamic proxying?
    ConfigSource configSource = components.findComponent(ConfigSource.class);
    if (configSource != null) {
      changeManagers.add(configSource.changeManager());
      sources.put(ConfigSource.class, new CacheAwareConfigSource(configSource, _cacheInvalidator));
    }

    RegionSource regionSource = components.findComponent(RegionSource.class);
    if (regionSource != null) {
      changeManagers.add(regionSource.changeManager());
      sources.put(RegionSource.class, new CacheAwareRegionSource(regionSource, _cacheInvalidator));
    }

    SecuritySource securitySource = components.findComponent(SecuritySource.class);
    if (securitySource != null) {
      changeManagers.add(securitySource.changeManager());
      sources.put(SecuritySource.class, new CacheAwareSecuritySource(securitySource, _cacheInvalidator));
    }

    ConventionSource conventionSource = components.findComponent(ConventionSource.class);
    if (conventionSource != null) {
      changeManagers.add(conventionSource.changeManager());
      sources.put(ConventionSource.class, new CacheAwareConventionSource(conventionSource, _cacheInvalidator));
    }

    HistoricalTimeSeriesSource timeSeriesSource = components.findComponent(HistoricalTimeSeriesSource.class);
    if (timeSeriesSource != null) {
      changeManagers.add(timeSeriesSource.changeManager());
      sources.put(HistoricalTimeSeriesSource.class, new CacheAwareHistoricalTimeSeriesSource(timeSeriesSource, _cacheInvalidator));
    }

    // TODO HolidaySource (which has a horrible design WRT decorating)

    // TODO - we will need to register change listeners

    return ComponentMap.of(sources);
  }

}
